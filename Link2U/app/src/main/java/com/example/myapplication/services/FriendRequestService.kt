package com.example.myapplication.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.FriendRequestsActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.ProfileAmigoActivity
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FriendRequestService(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var listenerRegistration: ListenerRegistration? = null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Solicitudes de amistad",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de solicitudes de amistad"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startListeningForRequests(userEmail: String) {
        listenerRegistration = db.collection("friend_requests")
            .whereEqualTo("to_email", userEmail)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val fromName = change.document.getString("from_name") ?: "Alguien"
                        val requestId = change.document.id
                        showFriendRequestNotification(fromName, requestId)
                    }
                }
            }
    }

    private fun showFriendRequestNotification(fromName: String, requestId: String) {
        // Intent para abrir la actividad de solicitudes de amistad
        val intent = Intent(context, FriendRequestsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_person)
            .setContentTitle("Nueva solicitud de amistad")
            .setContentText("$fromName quiere ser tu amigo")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$fromName quiere ser tu amigo. Toca para ver las solicitudes."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()

        // Mostrar la notificación
        notificationManager.notify(requestId.hashCode(), notification)
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    companion object {
        private const val CHANNEL_ID = "friend_requests_channel"
    }
} 