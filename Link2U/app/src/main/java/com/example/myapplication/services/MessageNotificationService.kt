package com.example.myapplication.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.myapplication.ChatActivity
import com.example.myapplication.MensajesFragment
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView

class MessageNotificationService(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var listenerRegistration: ListenerRegistration? = null
    private var bottomNav: BottomNavigationView? = null
    private var badge: BadgeDrawable? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

    init {
        createNotificationChannel()
    }

    fun setBottomNavigation(bottomNav: BottomNavigationView) {
        this.bottomNav = bottomNav
        badge = bottomNav.getOrCreateBadge(R.id.navigation_mensajes)
        badge?.isVisible = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mensajes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de mensajes nuevos"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startListeningForMessages(userEmail: String) {
        // Escuchar cambios en los mensajes de todos los chats
        listenerRegistration = db.collection("chats")
            .whereArrayContains("participants", userEmail)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                var totalUnreadCount = 0

                snapshot?.documents?.forEach { doc ->
                    val chatId = doc.id

                    // Contar mensajes no leídos para este chat
                    db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .whereEqualTo("receiverId", userEmail)
                        .whereEqualTo("isRead", false)
                        .get()
                        .addOnSuccessListener { messages ->
                            val unreadCount = messages.size()
                            totalUnreadCount += unreadCount
                            updateBadge(totalUnreadCount)
                        }

                    // Escuchar nuevos mensajes en este chat
                    if (!messageListeners.containsKey(chatId)) {
                        val listener = db.collection("chats")
                            .document(chatId)
                            .collection("messages")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .addSnapshotListener { messageSnapshot, messageError ->
                                if (messageError != null) return@addSnapshotListener

                                messageSnapshot?.documentChanges?.forEach { change ->
                                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                        val message = change.document
                                        val senderId = message.getString("senderId") ?: return@forEach
                                        val content = message.getString("content") ?: return@forEach

                                        // Solo mostrar notificación si el mensaje es de otro usuario
                                        if (senderId != userEmail) {
                                            // ✅ OMITIR SI ESTÁS EN EL FRAGMENTO DE MENSAJES
                                            if (isInMensajesFragment()) return@forEach
                                            if (isInChatActivity()) return@forEach

                                            // Obtener el nombre del remitente
                                            db.collection("usuarios")
                                                .whereEqualTo("e_mail", senderId)
                                                .get()
                                                .addOnSuccessListener { result ->
                                                    if (!result.isEmpty) {
                                                        val senderName = result.documents[0].getString("nombre_usuario") ?: "Alguien"
                                                        showMessageNotification(senderName, content, chatId, senderId)
                                                    }
                                                }
                                        }
                                    }
                                }
                            }
                        messageListeners[chatId] = listener
                    }
                }

                // Limpiar listeners de chats que ya no existen
                val currentChatIds = snapshot?.documents?.map { it.id } ?: emptyList()
                messageListeners.keys.filter { it !in currentChatIds }.forEach { chatId ->
                    messageListeners[chatId]?.remove()
                    messageListeners.remove(chatId)
                }
            }
    }


    private fun updateBadge(count: Int) {
        badge?.let {
            if (count > 0) {
                it.isVisible = true
                it.number = count
            } else {
                it.isVisible = false
            }
        }
    }

    private fun showMessageNotification(senderName: String, message: String, chatId: String, senderId: String) {
        // Intent para abrir el chat
        val intent = Intent(context, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("otherUserId", senderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Crear la notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_message)
            .setContentTitle("Nuevo mensaje de $senderName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$senderName: $message"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        // Mostrar la notificación
        notificationManager.notify(chatId.hashCode(), notification)
    }

    private fun isInMensajesFragment(): Boolean {
        val activity = context as? FragmentActivity ?: return false
        val navHostFragment = activity.supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment ?: return false

        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        return currentFragment is MensajesFragment
    }
    private fun isInChatActivity(): Boolean {
        return context is ChatActivity
    }


    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
    }

    companion object {
        private const val CHANNEL_ID = "messages_channel"
    }
}