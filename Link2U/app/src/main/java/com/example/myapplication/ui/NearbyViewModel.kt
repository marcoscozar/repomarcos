package com.example.myapplication.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.User
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore

class NearbyViewModel : ViewModel() {
    private val TAG = "NearbyViewModel"
    private val SERVICE_ID = "com.example.myapplication.nearby"
    private val gson = Gson()
    private val db = FirebaseFirestore.getInstance()

    private val _nearbyUsers = MutableStateFlow<List<User>>(emptyList())
    val nearbyUsers: StateFlow<List<User>> = _nearbyUsers.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentUsername: String? = null
    private var currentDeviceId: String = UUID.randomUUID().toString()
    private val activeConnections = mutableMapOf<String, String>() // endpointId -> deviceId
    private var isAdvertising = false
    private var isDiscovering = false
    private var reconnectJob: kotlinx.coroutines.Job? = null
    private var currentContext: Context? = null

    fun setUsername(username: String) {
        currentUsername = username
    }

    fun startAdvertising(context: Context) {
        currentContext = context
        if (isAdvertising) return
        isAdvertising = true
        _connectionState.value = ConnectionState.ADVERTISING

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        Nearby.getConnectionsClient(context).startAdvertising(
            currentUsername ?: "Unknown User",
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully")
            startReconnectLoop(context)
        }.addOnFailureListener {
            Log.e(TAG, "Failed to start advertising", it)
            _connectionState.value = ConnectionState.ERROR
            isAdvertising = false
        }
    }

    fun startDiscovery(context: Context) {
        currentContext = context
        if (isDiscovering) return
        isDiscovering = true
        _connectionState.value = ConnectionState.DISCOVERING

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        Nearby.getConnectionsClient(context).startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started successfully")
        }.addOnFailureListener {
            Log.e(TAG, "Failed to start discovery", it)
            _connectionState.value = ConnectionState.ERROR
            isDiscovering = false
        }
    }

    private fun startReconnectLoop(context: Context) {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            while (true) {
                delay(30000) // Reintentar cada 30 segundos
                if (!isAdvertising) {
                    startAdvertising(context)
                }
                if (!isDiscovering) {
                    startDiscovery(context)
                }
            }
        }
    }

    fun stopNearby(context: Context) {
        reconnectJob?.cancel()
        reconnectJob = null
        isAdvertising = false
        isDiscovering = false
        _connectionState.value = ConnectionState.DISCONNECTED
        _nearbyUsers.value = emptyList()
        activeConnections.clear()
        Nearby.getConnectionsClient(context).stopAdvertising()
        Nearby.getConnectionsClient(context).stopDiscovery()
        Nearby.getConnectionsClient(context).stopAllEndpoints()
        currentContext = null
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with ${info.endpointName}")
            currentContext?.let { context ->
                Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connection established with $endpointId")
                    _connectionState.value = ConnectionState.CONNECTED
                    currentContext?.let { context ->
                        // Enviar nuestro ID de dispositivo al dispositivo conectado
                        val deviceInfo = DeviceInfo(currentDeviceId, currentUsername ?: "Unknown User")
                        Nearby.getConnectionsClient(context).sendPayload(
                            endpointId,
                            Payload.fromBytes(gson.toJson(deviceInfo).toByteArray())
                        )
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected by $endpointId")
                    removeUser(endpointId)
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Connection error with $endpointId")
                    removeUser(endpointId)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            removeUser(endpointId)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: ${info.endpointName}")
            // Ignorar si encontramos nuestro propio dispositivo
            if (info.endpointName == currentUsername) {
                Log.d(TAG, "Ignoring self-discovery")
                return
            }
            currentContext?.let { context ->
                Nearby.getConnectionsClient(context).requestConnection(
                    currentUsername ?: "Unknown User",
                    endpointId,
                    connectionLifecycleCallback
                )
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            removeUser(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val json = String(payload.asBytes()!!)
                val deviceInfo = gson.fromJson(json, DeviceInfo::class.java)
                
                // Ignorar si recibimos nuestro propio ID
                if (deviceInfo.deviceId == currentDeviceId) {
                    Log.d(TAG, "Ignoring self-payload")
                    return
                }

                // Almacenar la relación entre endpointId y deviceId
                activeConnections[endpointId] = deviceInfo.deviceId

                // Actualizar la lista de usuarios
                val currentUsers = _nearbyUsers.value.toMutableList()
                if (!currentUsers.any { it.id == deviceInfo.deviceId }) {
                    // Obtener la imagen de perfil del usuario desde Firestore
                    db.collection("usuarios")
                        .whereEqualTo("nombre_usuario", deviceInfo.username)
                        .get()
                        .addOnSuccessListener { result ->
                            if (!result.isEmpty) {
                                val userDoc = result.documents[0]
                                val profileImage = userDoc.getString("imagen_perfil") ?: ""
                                currentUsers.add(User(deviceInfo.deviceId, deviceInfo.username, profileImage))
                                _nearbyUsers.value = currentUsers
                            } else {
                                currentUsers.add(User(deviceInfo.deviceId, deviceInfo.username))
                                _nearbyUsers.value = currentUsers
                            }
                        }
                        .addOnFailureListener {
                    currentUsers.add(User(deviceInfo.deviceId, deviceInfo.username))
                    _nearbyUsers.value = currentUsers
                        }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // No necesitamos manejar actualizaciones de transferencia para este caso
        }
    }

    private fun removeUser(endpointId: String) {
        val deviceId = activeConnections[endpointId]
        if (deviceId != null) {
            val currentUsers = _nearbyUsers.value.toMutableList()
            currentUsers.removeAll { it.id == deviceId }
            _nearbyUsers.value = currentUsers
            activeConnections.remove(endpointId)
        }
    }
}

data class DeviceInfo(
    val deviceId: String,
    val username: String
)

enum class ConnectionState {
    DISCONNECTED,
    ADVERTISING,
    DISCOVERING,
    CONNECTED,
    ERROR
}
