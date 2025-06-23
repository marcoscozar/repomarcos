package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val onChatClick: (Chat) -> Unit) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var chats: List<Chat> = emptyList()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.email ?: ""

    fun updateChats(newChats: List<Chat>) {
        chats = newChats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textUsername: TextView = itemView.findViewById(R.id.textUsername)
        private val textLastMessage: TextView = itemView.findViewById(R.id.textLastMessage)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val textUnreadCount: TextView = itemView.findViewById(R.id.textUnreadCount)
        private val unreadDot: View = itemView.findViewById(R.id.unreadDot)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val chat = chats[position]
                    // Marcar mensajes como leídos al abrir el chat
                    if (chat.unreadCount > 0) {
                        markMessagesAsRead(chat.id)
                    }
                    onChatClick(chat)
                }
            }
        }

        fun bind(chat: Chat) {
            // Obtener el nombre de usuario del otro participante
            db.collection("usuarios")
                .whereEqualTo("e_mail", chat.otherUserId)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val username = result.documents[0].getString("nombre_usuario") ?: ""
                        textUsername.text = username
                    }
                }

            // Obtener el último mensaje
            db.collection("chats")
                .document(chat.id)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val message = result.documents[0]
                        val content = message.getString("content") ?: ""
                        val timestamp = message.getTimestamp("timestamp")
                        val senderId = message.getString("senderId") ?: ""

                        // Formatear el último mensaje
                        textLastMessage.text = if (senderId == currentUserId) {
                            "Tú: $content"
                        } else {
                            content
                        }

                        // Formatear la hora
                        timestamp?.let {
                            val date = Date(it.seconds * 1000)
                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            textTime.text = sdf.format(date)
                        }
                    }
                }

            // Mostrar indicador de mensajes no leídos
            if (chat.unreadCount > 0) {
                textUnreadCount.visibility = View.VISIBLE
                textUnreadCount.text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString()
                unreadDot.visibility = View.VISIBLE
            } else {
                textUnreadCount.visibility = View.GONE
                unreadDot.visibility = View.GONE
            }
        }

        private fun markMessagesAsRead(chatId: String) {
            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener { result ->
                    result.documents.forEach { doc ->
                        doc.reference.update("isRead", true)
                    }
                }
        }
    }
} 