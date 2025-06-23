package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapters.ChatAdapter
import com.example.myapplication.model.Chat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.example.myapplication.databinding.FragmentMensajesBinding
import com.google.firebase.firestore.ListenerRegistration

class MensajesFragment : Fragment() {

    private var _binding: FragmentMensajesBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUserId: String
    private val chats = mutableListOf<Chat>()
    private var chatsListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMensajesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.email ?: return

        // Configurar RecyclerView
        setupRecyclerView()

        // Cargar chats
        loadChats()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { chat ->
            // Obtener el otro participante del chat
            val otherUserEmail = chat.otherUserId
            
            // Obtener el nombre del usuario
            db.collection("usuarios")
                .whereEqualTo("e_mail", otherUserEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val username = documents.first().getString("nombre_usuario") ?: otherUserEmail
                        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                            putExtra("chatId", chat.id)
                            putExtra("otherUserId", otherUserEmail)
                            putExtra("otherUserName", username)
                        }
                        startActivity(intent)
                    }
                }
        }
        
        binding.recyclerViewChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }

    private fun loadChats() {
        // Limpiar listeners anteriores
        chatsListener?.remove()
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()

        // Escuchar cambios en tiempo real en la colección de chats
        chatsListener = db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || !isAdded) {
                    return@addSnapshotListener
                }

                // Limpiar la lista de chats
                chats.clear()

                // Procesar cada chat
                snapshot?.documents?.forEach { doc ->
                    val chatId = doc.id
                    val participants = doc.get("participants") as? List<String> ?: emptyList()
                    val otherUserEmail = participants.firstOrNull { it != currentUserId } ?: return@forEach
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val lastMessageTime = doc.getTimestamp("lastMessageTime") ?: Timestamp.now()
                    val lastMessageSenderId = doc.getString("lastMessageSenderId") ?: ""

                    // Limpiar listener anterior para este chat si existe
                    messageListeners[chatId]?.remove()

                    // Escuchar cambios en los mensajes de este chat
                    val messageListener = db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener { messagesSnapshot, messagesError ->
                            if (messagesError != null || !isAdded) {
                                return@addSnapshotListener
                            }

                            // Obtener el último mensaje
                            val lastMessageDoc = messagesSnapshot?.documents?.firstOrNull()
                            val updatedLastMessage = lastMessageDoc?.getString("message") ?: lastMessage
                            val updatedLastMessageTime = lastMessageDoc?.getTimestamp("timestamp") ?: lastMessageTime
                            val updatedLastMessageSenderId = lastMessageDoc?.getString("senderId") ?: lastMessageSenderId

                            // Contar mensajes no leídos
                            db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .whereEqualTo("receiverId", currentUserId)
                                .whereEqualTo("isRead", false)
                                .get()
                                .addOnSuccessListener { unreadMessages ->
                                    if (!isAdded) return@addOnSuccessListener

                                    val unreadCount = unreadMessages.size()

                                    val chat = Chat(
                                        id = chatId,
                                        otherUserId = otherUserEmail,
                                        lastMessage = updatedLastMessage,
                                        lastMessageTime = updatedLastMessageTime,
                                        unreadCount = unreadCount,
                                        lastMessageSenderId = updatedLastMessageSenderId
                                    )

                                    // Actualizar o añadir el chat a la lista
                                    val existingIndex = chats.indexOfFirst { it.id == chat.id }
                                    if (existingIndex != -1) {
                                        chats[existingIndex] = chat
                                    } else {
                                        chats.add(chat)
                                    }

                                    // Ordenar la lista por último mensaje
                                    chats.sortByDescending { it.lastMessageTime }
                                    updateUI(chats)
                                }
                        }

                    // Guardar el listener para limpiarlo después
                    messageListeners[chatId] = messageListener
                }
            }
    }

    private fun updateUI(chats: List<Chat>) {
        if (!isAdded) return

        if (chats.isEmpty()) {
            binding.textViewNoMensajes.visibility = View.VISIBLE
            binding.recyclerViewChats.visibility = View.GONE
        } else {
            binding.textViewNoMensajes.visibility = View.GONE
            binding.recyclerViewChats.visibility = View.VISIBLE
            chatAdapter.updateChats(chats)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar todos los listeners
        chatsListener?.remove()
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
        _binding = null
    }
} 