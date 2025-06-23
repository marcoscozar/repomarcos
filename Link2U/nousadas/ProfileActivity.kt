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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityProfileBinding
import com.example.myapplication.model.Usuario
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.myapplication.model.SupabaseStorageService
import com.google.firebase.Timestamp
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


class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var db = Firebase.firestore
    private var selectedImageUri: Uri? = null
    private var selectedImageUri1: Uri? = null
    private var selectedImageUri2: Uri? = null
    private var selectedImageUri3: Uri? = null
    private lateinit var usuario: Usuario

    private lateinit var profileImage: CircleImageView
    private lateinit var imagen1: ImageView
    private lateinit var imagen2: ImageView
    private lateinit var imagen3: ImageView
    val retrofit = Retrofit.Builder()
        .baseUrl("https://ptfvjswhcnwcyoossfrt.supabase.co") // Reemplaza con tu proyecto
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(SupabaseStorageService::class.java)

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                profileImage.setImageURI(uri)
            }
        }
    }
    private val pickImage1 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            selectedImageUri1 = uri
            Glide.with(this)
                .load(uri)
                .into(binding.imv1)

            // ✅ Mueve aquí la lógica de subida
            usuario?.let { user ->
                uploadImageToSupabase(imagen1, uri, this, user.nombre_usuario.toString()) { downloadUrl ->
                    val update = mapOf("imagen1" to downloadUrl)
                    db.collection("usuarios")
                        .whereEqualTo("e_mail", user.e_mail)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot) {
                                document.reference.update(update)
                            }
                        }
                }
            }
        }
    }
    private val pickImage2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            selectedImageUri1 = uri
            Glide.with(this)
                .load(uri)
                .into(binding.imv2)

            // ✅ Mueve aquí la lógica de subida
            usuario?.let { user ->
                uploadImageToSupabase(imagen2, uri, this, user.nombre_usuario.toString()) { downloadUrl ->
                    val update = mapOf("imagen2" to downloadUrl)
                    db.collection("usuarios")
                        .whereEqualTo("e_mail", user.e_mail)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot) {
                                document.reference.update(update)
                            }
                        }
                }
            }
        }
    }
    private val pickImage3 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            selectedImageUri1 = uri
            Glide.with(this)
                .load(uri)
                .into(binding.imv3)
            // ✅ Mueve aquí la lógica de subida
            usuario?.let { user ->
                uploadImageToSupabase(imagen3, uri, this, user.nombre_usuario.toString()) { downloadUrl ->
                    val update = mapOf("imagen3" to downloadUrl)
                    db.collection("usuarios")
                        .whereEqualTo("e_mail", user.e_mail)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot) {
                                document.reference.update(update)
                            }
                        }
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        imagen1 = binding.imv1; imagen2 = binding.imv2; imagen3 = binding.imv3
        binding.buttonAddFriend.visibility = View.GONE
        db = FirebaseFirestore.getInstance()
        val email = intent.getStringExtra("email")
        Log.d("EMAIL",email.toString())

        // Set up delete button click listeners
        binding.btnDelete1.setOnClickListener {
            deleteImage(1)
        }
        binding.btnDelete2.setOnClickListener {
            deleteImage(2)
        }
        binding.btnDelete3.setOnClickListener {
            deleteImage(3)
        }

        db.collection("usuarios")
            .whereEqualTo("e_mail",email).get()
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
        imagen1.setOnClickListener {
            openImagePicker1()
        }
        imagen2.setOnClickListener {
            openImagePicker2()
        }
        imagen3.setOnClickListener {
            openImagePicker3()

        }
    }
    private fun openImagePicker1() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage1.launch(intent)
    }
    private fun openImagePicker2() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage2.launch(intent)
    }
    private fun openImagePicker3() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage3.launch(intent)
    }
    fun formatTimestamp(timestamp: Timestamp): String {
        val date = Date(timestamp.seconds * 1000) // Convertir segundos a milisegundos
        val sdf = SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }

    fun uploadImageToSupabase(imageView: ImageView, imageUri: Uri, context: Context, username: String, onSuccess: (String) -> Unit) {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(imageUri) ?: return
        val fileBytes = inputStream.readBytes()
        val requestBody = fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        var imagen = 0

        if(imageView == imagen1){
            imagen = 1
        }else if(imageView == imagen2){
            imagen = 2
        }else if(imageView == imagen3){
            imagen = 3
        }


        val filePart = MultipartBody.Part.createFormData(
            "file",
            "perfil.jpg",
            requestBody
        )

        val authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB0ZnZqc3doY253Y3lvb3NzZnJ0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDc0MDg4MDQsImV4cCI6MjA2Mjk4NDgwNH0.8K2P087GLNqcndYB_1FJW2tples19SUGb9t_5GByigk"
        val bucket = "images"
        val formattedDate = formatTimestamp(Timestamp.now())
        val fileName = "$username/imagen${imagen}_${formattedDate}.jpg"// 🗂️ imagen única por usuario

        val deleteCall = service.deleteImage(authHeader, bucket, fileName)
        deleteCall.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d("Supabase", "Imagen previa eliminada (o no existía): ${response.code()}")

                // Paso 2: Subir la nueva imagen
                val uploadCall = service.uploadImage(authHeader, bucket, fileName, filePart)
                uploadCall.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            Log.d("Supabase", "Imagen subida con éxito")
                            val publicUrl =
                                "https://ptfvjswhcnwcyoossfrt.supabase.co/storage/v1/object/public/$bucket/$fileName"
                            onSuccess(publicUrl)
                        } else {
                            Log.e(
                                "Supabase",
                                "Error al subir imagen: ${response.code()} - ${
                                    response.errorBody()?.string()
                                }"
                            )
                            Toast.makeText(
                                context,
                                "Error al subir imagen a Supabase",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("Supabase", "Fallo al subir imagen", t)
                        Toast.makeText(
                            context,
                            "Fallo al subir imagen a Supabase",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.w("Supabase", "No se pudo eliminar la imagen previa (puede no existir)", t)

                // Aun si falla, intentamos subir la imagen
                val uploadCall = service.uploadImage(authHeader, bucket, fileName, filePart)
                uploadCall.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            Log.d("Supabase", "Imagen subida con éxito")
                            val publicUrl =
                                "https://ptfvjswhcnwcyoossfrt.supabase.co/storage/v1/object/public/$bucket/$fileName"
                            onSuccess(publicUrl)
                        } else {
                            Log.e(
                                "Supabase",
                                "Error al subir imagen: ${response.code()} - ${
                                    response.errorBody()?.string()
                                }"
                            )
                            Toast.makeText(
                                context,
                                "Error al subir imagen a Supabase",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("Supabase", "Fallo al subir imagen", t)
                        Toast.makeText(
                            context,
                            "Fallo al subir imagen a Supabase",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

        })
    }

    private fun deleteImage(imageNumber: Int) {
        usuario?.let { user ->
            val update = when (imageNumber) {
                1 -> mapOf("imagen1" to "")
                2 -> mapOf("imagen2" to "")
                3 -> mapOf("imagen3" to "")
                else -> return
            }

            // Update Firestore
            db.collection("usuarios")
                .whereEqualTo("e_mail", user.e_mail)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        document.reference.update(update)
                    }
                }

            // Clear the image view
            when (imageNumber) {
                1 -> binding.imv1.setImageResource(0)
                2 -> binding.imv2.setImageResource(0)
                3 -> binding.imv3.setImageResource(0)
            }

            // Delete from Supabase
            val bucket = "images"
            val formattedDate = formatTimestamp(Timestamp.now())
            val fileName = "${user.nombre_usuario}/imagen${imageNumber}_${formattedDate}.jpg"
            val authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB0ZnZqc3doY253Y3lvb3NzZnJ0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDc0MDg4MDQsImV4cCI6MjA2Mjk4NDgwNH0.8K2P087GLNqcndYB_1FJW2tples19SUGb9t_5GByigk"

            val deleteCall = service.deleteImage(authHeader, bucket, fileName)
            deleteCall.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("Supabase", "Imagen eliminada: ${response.code()}")
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("Supabase", "Error al eliminar imagen", t)
                }
            })
        }
    }
}