package com.example.myapplication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.NavegacionActivityBinding
import com.example.myapplication.model.Usuario
import com.example.myapplication.services.FriendRequestService
import com.example.myapplication.services.MessageNotificationService
import com.example.myapplication.dialogs.FriendRequestDialog
import com.example.myapplication.ui.NearbyViewModel
import com.example.myapplication.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NavegacionActivity : AppCompatActivity() {

    private lateinit var binding: NavegacionActivityBinding
    private lateinit var viewModel: NearbyViewModel
    private lateinit var friendRequestService: FriendRequestService
    private lateinit var messageNotificationService: MessageNotificationService

    private val bluetoothRequestCode = 1001
    private val locationRequestCode = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NavegacionActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this)[NearbyViewModel::class.java]

        // Inicializar servicios de notificación
        friendRequestService = FriendRequestService(this)
        messageNotificationService = MessageNotificationService(this)
        messageNotificationService.setBottomNavigation(binding.navView)

        // Iniciar escucha de notificaciones si el usuario está autenticado
        FirebaseAuth.getInstance().currentUser?.email?.let { email ->
            friendRequestService.startListeningForRequests(email)
            messageNotificationService.startListeningForMessages(email)
        }

        // Configurar navegación
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        binding.navView.setupWithNavController(navController)

        // Configurar ViewModel compartido
        val email = intent.getStringExtra("email")
        val sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        sharedViewModel.email.value = email

        // Configurar usuario
        if (email != null) {
            setupUser(email)
        }

        // Manejar notificaciones de amistad
        val requestId = intent.getStringExtra("request_id")
        if (requestId != null) {
            handleFriendRequest(requestId)
        }

        // Manejar notificación de mensaje
        val chatId = intent.getStringExtra("chatId")
        if (chatId != null) {
            handleChatNotification(chatId)
        }

        // Iniciar verificación de servicios (sin solicitar permisos)
        checkAndRequestBluetooth()
    }

    private fun setupUser(email: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios").whereEqualTo("e_mail", email).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val usuario = documents.first().toObject(Usuario::class.java)
                    viewModel.setUsername(usuario.nombre_usuario)
                    // Iniciar Nearby si los servicios están activos
                    if (checkAllServicesEnabled()) {
                        startNearby()
                    }
                }
            }
    }

    private fun checkAndRequestBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, bluetoothRequestCode)
        } else {
            checkLocationSettings()
        }
    }

    private fun checkLocationSettings() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Por favor, activa la ubicación", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, locationRequestCode)
        } else {
            startNearby()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            bluetoothRequestCode -> {
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter

                if (bluetoothAdapter?.isEnabled == true) {
                    checkLocationSettings()
                } else {
                    redirectToErrorScreen()
                }
            }
            locationRequestCode -> {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if (checkAllServicesEnabled()) {
                        startNearby()
                    }
                } else {
                    redirectToErrorScreen()
                }
            }
        }
    }

    private fun redirectToErrorScreen() {
        val intent = Intent(this, ErrorPermisosActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun checkAllServicesEnabled(): Boolean {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return bluetoothAdapter?.isEnabled == true &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun startNearby() {
        viewModel.startAdvertising(this)
        viewModel.startDiscovery(this)
    }

    override fun onResume() {
        super.onResume()
        if (checkAllServicesEnabled()) {
            startNearby()
        }
    }

    private fun handleFriendRequest(requestId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("friend_requests")
            .document(requestId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fromName = document.getString("from_name") ?: return@addOnSuccessListener
                    val fromEmail = document.getString("from_email") ?: return@addOnSuccessListener
                    val toEmail = document.getString("to_email") ?: return@addOnSuccessListener

                    FriendRequestDialog(
                        this,
                        requestId,
                        fromName,
                        fromEmail,
                        toEmail
                    ).show()
                }
            }
    }

    private fun handleChatNotification(chatId: String) {
        binding.navView.selectedItemId = R.id.navigation_mensajes
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopNearby(this)
        friendRequestService.stopListening()
        messageNotificationService.stopListening()
    }

    override fun onStop() {
        super.onStop()
    }
}
