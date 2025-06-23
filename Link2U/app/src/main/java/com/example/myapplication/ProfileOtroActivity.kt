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

class ProfileOtroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var db = Firebase.firestore
    private lateinit var usuario: Usuario
    private lateinit var currentUser: Usuario
    private lateinit var imagen1: ImageView
    private lateinit var imagen2: ImageView
    private lateinit var imagen3: ImageView
    private lateinit var email: String


    val retrofit = Retrofit.Builder()
        .baseUrl("https://ptfvjswhcnwcyoossfrt.supabase.co") // Reemplaza con tu proyecto
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(SupabaseStorageService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.button3.visibility = View.GONE
        binding.btnDelete1.visibility = View.GONE
        binding.btnDelete2.visibility = View.GONE
        binding.btnDelete3.visibility = View.GONE
        imagen1 = binding.imv1; imagen2 = binding.imv2; imagen3 = binding.imv3


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

                                        // Cargar datos del usuario actual y verificar estado de amistad
                                        loadCurrentUserAndCheckFriendStatus()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.d("Hugo", e.toString())
                            }

                        binding.button3.setOnClickListener {
                            val intent = Intent(this, AjustesPerfil::class.java)
                            intent.putExtra("email",email)
                            intent.putExtra("username", usuario?.nombre_usuario)
                            startActivity(intent)
                        }
                        binding.buttonAddFriend.setOnClickListener {
                            sendFriendRequest()
                        }
                    }
                }
        }
    }

    private fun loadCurrentUserAndCheckFriendStatus() {
        val authEmail = FirebaseAuth.getInstance().currentUser?.email
        if (authEmail == null) {
            binding.buttonAddFriend.visibility = View.GONE
            return
        }

        db.collection("usuarios")
            .whereEqualTo("e_mail", authEmail)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val user = result.documents[0].toObject(Usuario::class.java)
                    if (user != null) {
                        currentUser = user
                        // Una vez que tenemos los datos del usuario actual, verificamos el estado de amistad
                        checkFriendRequestStatus()
                    }
                }
            }
    }

    private fun checkFriendRequestStatus() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        // Verificar si ya son amigos (en ambas direcciones)
        val query1 = db.collection("friendships")
            .whereEqualTo("user1_email", currentUserEmail)
            .whereEqualTo("user2_email", usuario.e_mail)
            .get()

        val query2 = db.collection("amigos")
            .whereEqualTo("user2_email", usuario.e_mail)
            .whereEqualTo("user1_email", currentUserEmail)
            .get()

        query1.addOnSuccessListener { result1 ->
            if (!result1.isEmpty) {
                showAlreadyFriend()
                return@addOnSuccessListener
            }

            query2.addOnSuccessListener { result2 ->
                if (!result2.isEmpty) {
                    showAlreadyFriend()
                    return@addOnSuccessListener
                }

                // Si no son amigos, verificar si hay una solicitud pendiente
                db.collection("friend_requests")
                    .whereEqualTo("from_email", currentUserEmail)
                    .whereEqualTo("to_email", usuario.e_mail)
                    .get()
                    .addOnSuccessListener { requestResult ->
                        if (!requestResult.isEmpty) {
                            binding.buttonAddFriend.text = "Solicitud enviada"
                            binding.buttonAddFriend.isEnabled = false
                        }
                    }
            }
        }
    }

    private fun showAlreadyFriend() {
        binding.buttonAddFriend.visibility = View.GONE
        binding.textAlreadyFriend.visibility = View.VISIBLE
    }

    private fun sendFriendRequest() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        binding.buttonAddFriend.isEnabled = false

        val friendRequest = hashMapOf(
            "from_email" to currentUserEmail,
            "to_email" to usuario.e_mail,
            "from_name" to currentUser.nombre_usuario,
            "status" to "pending",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        db.collection("friend_requests")
            .add(friendRequest)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud de amistad enviada", Toast.LENGTH_SHORT).show()
                binding.buttonAddFriend.text = "Solicitud enviada"
                binding.buttonAddFriend.isEnabled = false
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar la solicitud", Toast.LENGTH_SHORT).show()
                binding.buttonAddFriend.isEnabled = true
            }
    }
}