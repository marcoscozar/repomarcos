package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivityAjustesPerfilBinding
import com.example.myapplication.databinding.ActivityInfoOtroperfilBinding
import com.example.myapplication.model.SupabaseStorageService
import com.example.myapplication.model.Usuario
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InfoPerfil : AppCompatActivity() {
    private lateinit var binding: ActivityInfoOtroperfilBinding
    private var db = Firebase.firestore
    private var email = ""
    private lateinit var usuario: Usuario


    val retrofit = Retrofit.Builder()
        .baseUrl("https://ptfvjswhcnwcyoossfrt.supabase.co") // Reemplaza con tu proyecto
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(SupabaseStorageService::class.java)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoOtroperfilBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ajustes_perfil)
        setContentView(binding.root)
        binding.emailText2.visibility = View.GONE
        binding.emaillabel2.visibility = View.GONE
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        email = intent.getStringExtra("email").toString()
        val username = intent.getStringExtra("username")
        db.collection("usuarios")
            .whereEqualTo("e_mail",email).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    var user = document.toObject(Usuario::class.java)!!
                    usuario = user
                    binding.instagramTextView.text = usuario?.redes_sociales?.get(0).toString()
                    binding.tiktokTextView.text = usuario?.redes_sociales?.get(1).toString()
                    binding.xTextView.text = usuario?.redes_sociales?.get(2).toString()
                    binding.facebookTextView.text = usuario?.redes_sociales?.get(3).toString()
                    var fechacrecion = formatTimestampFechaCreacion(usuario.fecha_creacion)
                    binding.datetext.text = fechacrecion
                    binding.bthtext.text = usuario.edad.toString()
                    Glide.with(this)
                        .load(user.imagen_perfil.toUri())
                        .into(binding.profileImage3)
                }
            }
            .addOnFailureListener { e ->
                Log.d("Hugo", e.toString())
            }


        binding.emailText2.text = email
        binding.usernameText.text = username

    }


    fun formatTimestampFechaCreacion(timestamp: Timestamp): String {
        val date = Date(timestamp.seconds * 1000) // Convertir segundos a milisegundos
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

}