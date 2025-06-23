package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityProfileBinding
import com.example.myapplication.model.Usuario
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.myapplication.model.SupabaseStorageService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.firebase.auth.FirebaseAuth

class ProfileAmigoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var db = Firebase.firestore
    private lateinit var usuario: Usuario
    private lateinit var currentUser: Usuario
    private lateinit var auth: FirebaseAuth
    private lateinit var emailAmigo: String
    private lateinit var nombreAmigo: String

    private lateinit var profileImage: CircleImageView
    private lateinit var imagen1: ImageView
    private lateinit var imagen2: ImageView
    private lateinit var imagen3: ImageView
    private lateinit var email: String

    val retrofit = Retrofit.Builder()
        .baseUrl("https://ptfvjswhcnwcyoossfrt.supabase.co")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(SupabaseStorageService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.buttonAddFriend.visibility = View.GONE
        binding.btnDelete1.visibility = View.GONE
        binding.btnDelete2.visibility = View.GONE
        binding.btnDelete3.visibility = View.GONE
        imagen1 = binding.imv1; imagen2 = binding.imv2; imagen3 = binding.imv3

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()

        // Obtener datos del amigo
        emailAmigo = intent.getStringExtra("email") ?: ""
        nombreAmigo = intent.getStringExtra("nombre") ?: ""

        val username = intent.getStringExtra("username")
        if(username != null){
            binding.profileName.text = username

            // Get user email from Firestore
            db.collection("usuarios")
                .whereEqualTo("nombre_usuario", username)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        email = result.documents[0].getString("e_mail").toString()

                        Log.d("EMAIL", email.toString())
                        db.collection("usuarios")
                            .whereEqualTo("e_mail", email).get()
                            .addOnSuccessListener { result ->
                                if (!result.isEmpty) {
                                    val document = result.documents[0]
                                    val user = document.toObject(Usuario::class.java)
                                    if (user != null) {
                                        usuario = user
                                    }
                                    if (user != null) {
                                        Log.d("USUARIO", user.toString())
                                        Log.d("PERFIL", user.imagen_perfil.toString())

                                        binding.label1.text = "Edad: " + user.edad.toString()
                                        binding.label2.text = "Sexo: " + user.sexo_usuario

                                        Glide.with(this)
                                            .load(user.imagen_perfil.toUri())
                                            .placeholder(R.drawable.ic_person)
                                            .error(R.drawable.ic_person)
                                            .into(binding.profileImage2)

                                        Glide.with(this)
                                            .load(user.imagen1.toUri())
                                            .into(binding.imv1)

                                        Glide.with(this)
                                            .load(user.imagen2.toUri())
                                            .into(binding.imv2)

                                        Glide.with(this)
                                            .load(user.imagen3.toUri())
                                            .into(binding.imv3)

                                        Log.d("URIIIIIIIIIIIIIIII", user.imagen_perfil.toUri().toString())

                                        // Check if users are friends
                                        checkFriendshipStatus()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.d("Hugo", e.toString())
                            }
                        val authEmail = FirebaseAuth.getInstance().currentUser?.email
                        if (authEmail != null) {
                            db.collection("usuarios")
                                .whereEqualTo("e_mail", authEmail)
                                .get()
                                .addOnSuccessListener { result ->
                                    if (!result.isEmpty) {
                                        val user = result.documents[0].toObject(Usuario::class.java)
                                        if (user != null) {
                                            currentUser = user
                                        }
                                    }
                                }
                        }

                        binding.button3.setOnClickListener {
                            val intent = Intent(this, InfoPerfil::class.java)
                            intent.putExtra("email",email)
                            intent.putExtra("username", usuario?.nombre_usuario)
                            startActivity(intent)
                        }

                        // Set up message button
                        binding.buttonSendMessage.setOnClickListener {
                            val intent = Intent(this, ChatActivity::class.java).apply {
                                putExtra("otherUserId", email)
                            }
                            startActivity(intent)
                        }

                        // Set up cancel friendship button
                        binding.buttonCancelFriendship.setOnClickListener {
                            showCancelFriendshipDialog()
                        }
                    }
                    }
                }
        }

    private fun checkFriendshipStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        val currentUserEmail = currentUser.email

        db.collection("friendships")
            .whereEqualTo("user1_email", currentUserEmail)
            .whereEqualTo("user2_email", usuario.e_mail)
            .get()
            .addOnSuccessListener { result1 ->
                if (!result1.isEmpty) {
                    // They are friends
                    showFriendButtons()
                } else {
                    // Check the other way around
                    db.collection("friendships")
                        .whereEqualTo("user1_email", usuario.e_mail)
                        .whereEqualTo("user2_email", currentUserEmail)
                        .get()
                        .addOnSuccessListener { result2 ->
                            if (!result2.isEmpty) {
                                // They are friends
                                showFriendButtons()
                            } else {
                                // They are not friends
                                hideFriendButtons()
                            }
                        }
                }
            }
    }

    private fun showFriendButtons() {
        binding.buttonAddFriend.visibility = View.GONE
        binding.buttonSendMessage.visibility = View.VISIBLE
        binding.buttonCancelFriendship.visibility = View.VISIBLE

        binding.buttonSendMessage.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("otherUserId", email)
            }
            startActivity(intent)
        }
    }

    private fun hideFriendButtons() {
        binding.buttonAddFriend.visibility = View.GONE
        binding.buttonSendMessage.visibility = View.GONE
        binding.buttonCancelFriendship.visibility = View.GONE
    }

    private fun showCancelFriendshipDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancelar amistad")
            .setMessage("¿Estás seguro de que quieres cancelar la amistad con ${usuario.nombre_usuario}?")
            .setPositiveButton("Sí") { _, _ ->
                cancelFriendship()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelFriendship() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail == null) return

        // Delete friendship from both directions
        db.collection("friendships")
            .whereEqualTo("user1_email", currentUserEmail)
            .whereEqualTo("user2_email", usuario.e_mail)
            .get()
            .addOnSuccessListener { result1 ->
                result1.documents.forEach { doc ->
                    doc.reference.delete()
                }
            }

        db.collection("friendships")
            .whereEqualTo("user1_email", usuario.e_mail)
            .whereEqualTo("user2_email", currentUserEmail)
            .get()
            .addOnSuccessListener { result2 ->
                result2.documents.forEach { doc ->
                    doc.reference.delete()
                }

                // Eliminar todas las solicitudes de amistad entre ambos usuarios
                // Eliminar solicitudes donde el usuario actual es el remitente
                db.collection("friend_requests")
                    .whereEqualTo("from_email", currentUserEmail)
                    .whereEqualTo("to_email", usuario.e_mail)
                    .get()
                    .addOnSuccessListener { documents ->
                        documents.forEach { doc ->
                            doc.reference.delete()
                        }
                    }

                // Eliminar solicitudes donde el usuario actual es el destinatario
        db.collection("friend_requests")
                    .whereEqualTo("from_email", usuario.e_mail)
                    .whereEqualTo("to_email", currentUserEmail)
                    .get()
                    .addOnSuccessListener { documents ->
                        documents.forEach { doc ->
                            doc.reference.delete()
                        }

                        // Eliminar los chats entre ambos usuarios
                        val chatId1 = "${currentUserEmail}-${usuario.e_mail}"
                        val chatId2 = "${usuario.e_mail}-${currentUserEmail}"

                        // Eliminar el primer chat y sus mensajes
                        db.collection("chats").document(chatId1)
                            .collection("messages")
                            .get()
                            .addOnSuccessListener { messages ->
                                messages.forEach { message ->
                                    message.reference.delete()
                                }
                                db.collection("chats").document(chatId1).delete()
                            }

                        // Eliminar el segundo chat y sus mensajes
                        db.collection("chats").document(chatId2)
                            .collection("messages")
                            .get()
                            .addOnSuccessListener { messages ->
                                messages.forEach { message ->
                                    message.reference.delete()
                                }
                                db.collection("chats").document(chatId2).delete()
            .addOnSuccessListener {
                                        Toast.makeText(this, "Amistad cancelada", Toast.LENGTH_SHORT).show()
                                        hideFriendButtons()
                                        finish()
                                    }
                            }
                    }
            }
    }
}