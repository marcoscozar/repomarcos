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
import com.example.myapplication.model.SupabaseStorageService
import com.example.myapplication.model.Usuario
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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

class AjustesPerfil : AppCompatActivity() {
    private lateinit var binding: ActivityAjustesPerfilBinding
    private var db = Firebase.firestore
    private var isEditingSocialMedia = false
    private var isEditingAccount = false
    private var email = ""
    private var selectedImageUri: Uri? = null
    private lateinit var usuario: Usuario
    private var oldInstagram = ""
    private var oldTikTok = ""
    private var oldX = ""
    private var oldFacebook = ""
    private lateinit var googleSignInClient: GoogleSignInClient
    val retrofit = Retrofit.Builder()
        .baseUrl("https://ptfvjswhcnwcyoossfrt.supabase.co") // Reemplaza con tu proyecto
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(SupabaseStorageService::class.java)
    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            selectedImageUri = uri
            Glide.with(this)
                .load(uri).error(R.drawable.ic_person)
                .into(binding.profileImage3)

            usuario?.let { user ->
                uploadImageToSupabase(uri, this, user.nombre_usuario.toString()) { downloadUrl ->
                    val update = mapOf("imagen_perfil" to downloadUrl)
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
        binding = ActivityAjustesPerfilBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ajustes_perfil)
        setContentView(binding.root)
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
        binding.logoutButton2.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut()
            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()
            val prefsConfig = getSharedPreferences("config", Context.MODE_PRIVATE).edit()
            prefsConfig.remove("ultima_modificacion_usuario")
            prefsConfig.apply()
            val intent = Intent(this, Log_in::class.java)
            startActivity(intent)
        }
        setupClickListeners()
        binding.profileImage3.setOnClickListener {
            openImagePicker1()
        }

    }
    private fun openImagePicker1() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }
    private fun setupClickListeners() {
        // Botón de editar redes sociales
        binding.texteditarrd.setOnClickListener {
            toggleSocialMediaEditability()
        }

        // Botón de editar cuenta
        binding.texteditarCuenta.setOnClickListener {
            toggleAccountEditability()
        }

        // Botón de cerrar sesión
        binding.logoutButton2.setOnClickListener {
            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()
            val prefsConfig = getSharedPreferences("config", Context.MODE_PRIVATE).edit()
            prefsConfig.remove("ultima_modificacion_usuario")
            prefsConfig.apply()

            val intent = Intent(this, Log_in::class.java)
            startActivity(intent)
        }

        // Botones de guardar y cancelar
        binding.textGuardar.setOnClickListener {
            if (isEditingSocialMedia) {
                saveSocialMediaChanges()
            }
            if (isEditingAccount) {
                saveAccountChanges()
            }
            resetEditMode()
        }

        binding.textCancelar.setOnClickListener {
            resetEditMode()
        }
    }

    private fun toggleSocialMediaEditability() {
        isEditingSocialMedia = !isEditingSocialMedia
        oldInstagram = binding.instagramTextView.text.toString()
        oldTikTok = binding.tiktokTextView.text.toString()
        oldX = binding.xTextView.text.toString()
        oldFacebook = binding.facebookTextView.text.toString()
        updateEditMode()
    }

    private fun toggleAccountEditability() {
        isEditingAccount = !isEditingAccount
        updateEditMode()
    }

    private fun updateEditMode() {
        if (isEditingSocialMedia || isEditingAccount) {
            // Mostrar botones de guardar y cancelar
            binding.textCancelar.visibility = View.VISIBLE
            binding.textGuardar.visibility = View.VISIBLE

            // Actualizar visibilidad de campos de redes sociales
            if (isEditingSocialMedia) {
                binding.instagramTextView.visibility = View.GONE
                binding.instagramInputLayout.visibility = View.VISIBLE
                binding.tiktokTextView.visibility = View.GONE
                binding.tiktokInputLayout.visibility = View.VISIBLE
                binding.xTextView.visibility = View.GONE
                binding.xInputLayout.visibility = View.VISIBLE
                binding.facebookTextView.visibility = View.GONE
                binding.facebookInputLayout.visibility = View.VISIBLE

                // Establecer el texto actual en los campos de edición
                binding.instagramEditText.setText(binding.instagramTextView.text)
                binding.tiktokEditText.setText(binding.tiktokTextView.text)
                binding.xEditText.setText(binding.xTextView.text)
                binding.facebookEditText.setText(binding.facebookTextView.text)
            }

            // Actualizar visibilidad de campos de cuenta
            if (isEditingAccount) {
                binding.usernameText.visibility = View.GONE
                binding.usernameInputLayout.visibility = View.VISIBLE
                binding.usernameEditText.setText(binding.usernameText.text)
            }
        } else {
            resetEditMode()
        }
    }

    private fun resetEditMode() {
        // Ocultar botones de guardar y cancelar
        binding.textCancelar.visibility = View.GONE
        binding.textGuardar.visibility = View.GONE

        // Restaurar visibilidad de campos de redes sociales
        binding.instagramTextView.visibility = View.VISIBLE
        binding.instagramInputLayout.visibility = View.GONE
        binding.tiktokTextView.visibility = View.VISIBLE
        binding.tiktokInputLayout.visibility = View.GONE
        binding.xTextView.visibility = View.VISIBLE
        binding.xInputLayout.visibility = View.GONE
        binding.facebookTextView.visibility = View.VISIBLE
        binding.facebookInputLayout.visibility = View.GONE

        // Limpiar los campos de edición
        binding.instagramEditText.text?.clear()
        binding.tiktokEditText.text?.clear()
        binding.xEditText.text?.clear()
        binding.facebookEditText.text?.clear()

        // Restaurar visibilidad de campos de cuenta
        binding.usernameText.visibility = View.VISIBLE
        binding.usernameInputLayout.visibility = View.GONE

        // Resetear estados de edición
        isEditingSocialMedia = false
        isEditingAccount = false
    }

    private fun saveSocialMediaChanges() {
        val instagramInput = binding.instagramEditText.text.toString().trim()
        val tiktokInput = binding.tiktokEditText.text.toString().trim()
        val xInput = binding.xEditText.text.toString().trim()
        val facebookInput = binding.facebookEditText.text.toString().trim()

        // Si el campo está vacío, se elimina la red social
        binding.instagramTextView.text = if (instagramInput.isEmpty()) "" else instagramInput
        binding.tiktokTextView.text = if (tiktokInput.isEmpty()) "" else tiktokInput
        binding.xTextView.text = if (xInput.isEmpty()) "" else xInput
        binding.facebookTextView.text = if (facebookInput.isEmpty()) "" else facebookInput

        var redesociales = listOf(
            binding.instagramTextView.text.toString(),
            binding.tiktokTextView.text.toString(),
            binding.xTextView.text.toString(),
            binding.facebookTextView.text.toString()
        )
        updateUserListFieldByEmail(email.toString(), redesociales)
    }

    private fun saveAccountChanges() {
        // Guardar cambios del nombre de usuario
        val newUsername = binding.usernameEditText.text.toString()
        if (newUsername.isNotEmpty()) {
            if (puedeCambiarUsuario(this)) {
                // Actualizar UI y datos
                updateUserNameByEmail(email.toString(), newUsername)
            } else {
                Toast.makeText(this, "Solo puedes cambiar tu usuario una vez cada 24 horas", Toast.LENGTH_LONG).show()
            }
        }
    }
    fun guardarFechaCambio(context: Context) {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val timestamp = System.currentTimeMillis() // guarda el tiempo actual en milisegundos
        editor.putLong("ultima_modificacion_usuario", timestamp)
        editor.apply()
    }
    fun puedeCambiarUsuario(context: Context): Boolean {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        val ultimaModificacion = prefs.getLong("ultima_modificacion_usuario", 0L)
        val ahora = System.currentTimeMillis()

        val horas24 = 24 * 60 * 60 * 1000 // 24 horas en milisegundos
        return ahora - ultimaModificacion >= horas24
    }

    fun updateUserListFieldByEmail(email: String, newList: List<String>) {
        db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("usuarios")

        usersRef.whereEqualTo("e_mail", email).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    val docRef = userDoc.reference

                    // Reemplaza completamente la lista
                    docRef.update("redes_sociales", newList)
                        .addOnSuccessListener {
                            Log.d("FirestoreUpdate", "Lista actualizada correctamente.")
                        }
                        .addOnFailureListener { e ->
                            Log.w("FirestoreUpdate", "Error al actualizar la lista", e)
                        }
                } else {
                    Log.d("FirestoreUpdate", "Usuario no encontrado con ese email.")
                }
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreQuery", "Error en la consulta", e)
            }
    }

    fun updateUserNameByEmail(email: String, user_name: String){
        db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("usuarios")
        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios")
            .whereEqualTo("nombre_usuario", user_name)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("DOCUMENTOS",documents.isEmpty.toString())
                if (documents.isEmpty) {
                    binding.usernameText.text = user_name
                    usersRef.whereEqualTo("e_mail", email).get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                val userDoc = documents.documents[0]
                                val docRef = userDoc.reference
                                docRef.update("nombre_usuario", user_name).addOnSuccessListener {
                                    Log.d("FirestoreUpdate", "Lista actualizada correctamente.")
                                }
                                    .addOnFailureListener { e ->
                                        Log.w("FirestoreUpdate", "Error al actualizar la lista", e)
                                    }
                                binding.usernameText.text = user_name
                                guardarFechaCambio(this)
                                Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.d("FirestoreUpdate", "Usuario no encontrado con ese email.")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w("FirestoreQuery", "Error en la consulta", e)
                        }
                    Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    // El nombre ya existe
                    Toast.makeText(this, "Ese nombre de usuario ya está en uso", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al verificar el nombre de usuario: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
    fun formatTimestamp(timestamp: Timestamp): String {
        val date = Date(timestamp.seconds * 1000) // Convertir segundos a milisegundos
        val sdf = SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }
    fun formatTimestampFechaCreacion(timestamp: Timestamp): String {
        val date = Date(timestamp.seconds * 1000) // Convertir segundos a milisegundos
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }
    fun uploadImageToSupabase(imageUri: Uri, context: Context, username: String, onSuccess: (String) -> Unit) {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(imageUri) ?: return
        val fileBytes = inputStream.readBytes()
        val requestBody = fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData(
            "file",
            "perfil.jpg",
            requestBody
        )

        val authHeader =
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB0ZnZqc3doY253Y3lvb3NzZnJ0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDc0MDg4MDQsImV4cCI6MjA2Mjk4NDgwNH0.8K2P087GLNqcndYB_1FJW2tples19SUGb9t_5GByigk"
        val bucket = "images"
        val formattedDate = formatTimestamp(Timestamp.now())
        val fileName = "$username/perfil_${formattedDate}.jpg"// 🗂️ imagen única por usuario

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
}