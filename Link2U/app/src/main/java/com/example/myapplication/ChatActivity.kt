package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.adapters.MessageAdapter
import com.example.myapplication.model.Message
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUserId: String
    private lateinit var otherUserId: String
    private lateinit var profileImage: ShapeableImageView
    private lateinit var userName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()
        currentUserId = FirebaseAuth.getInstance().currentUser?.email ?: return
        otherUserId = intent.getStringExtra("otherUserId") ?: return

        // Inicializar vistas del header
        profileImage = findViewById(R.id.profileImage)
        userName = findViewById(R.id.userName)

        // Configurar click listener para el nombre de usuario
        userName.setOnClickListener {
            val intent = Intent(this, ProfileAmigoActivity::class.java).apply {
                putExtra("email", otherUserId)
                putExtra("username", userName.text.toString())
            }
            startActivity(intent)
        }

        // Cargar información del perfil
        loadUserProfile()

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewMessages)
        messageAdapter = MessageAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        // Configurar UI
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)

        // Cargar mensajes
        loadMessages()

        // Configurar botón de enviar
        buttonSend.setOnClickListener {
            val messageText = editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                editTextMessage.text.clear()
            }
        }
    }

    private fun loadUserProfile() {
        db.collection("usuarios")
            .whereEqualTo("e_mail", otherUserId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    // Cargar nombre de usuario
                    val nombreUsuario = document.getString("nombre_usuario")
                    userName.text = nombreUsuario

                    // Cargar imagen de perfil
                    val imagenPerfil = document.getString("imagen_perfil")
                    if (!imagenPerfil.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imagenPerfil)
                            .circleCrop()
                            .into(profileImage)
                    } else {
                        // Si no hay imagen de perfil, mostrar una imagen por defecto
                        profileImage.setImageResource(R.drawable.default_profile)
                    }
                } else {
                    Toast.makeText(this, "No se encontró el perfil del usuario", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar el perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMessages() {
        val chatId = getChatId(currentUserId, otherUserId)
        
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)
                } ?: emptyList()
                
                messageAdapter.updateMessages(messages)
                if (messages.isNotEmpty()) {
                    recyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }
    }

    private fun sendMessage(content: String) {
        val chatId = getChatId(currentUserId, otherUserId)
        val message = Message(
            senderId = currentUserId,
            receiverId = otherUserId,
            content = content,
            timestamp = Timestamp.now()
        )

        // Guardar mensaje en Firestore
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                // Actualizar último mensaje en la colección de chats
                db.collection("chats")
                    .document(chatId)
                    .set(mapOf(
                        "lastMessage" to content,
                        "lastMessageTime" to Timestamp.now(),
                        "participants" to listOf(currentUserId, otherUserId)
                    ))
            }
    }

    private fun getChatId(user1: String, user2: String): String {
        // Crear un ID único para el chat ordenando los emails alfabéticamente
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }
} 