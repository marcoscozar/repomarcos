package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.example.myapplication.model.SupabaseStorageService
import com.example.myapplication.model.Usuario
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.toString
import org.mindrot.jbcrypt.BCrypt

class RegisterActivity : AppCompatActivity() {
    private lateinit var profileImage: CircleImageView
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var ageInput: TextInputEditText
    private lateinit var usernameError: TextView
    private lateinit var passwordError: TextView
    private lateinit var emailError: TextView
    private lateinit var ageError: TextView
    private lateinit var termsError: TextView
    private lateinit var termsCheckbox: CheckBox
    private var selectedImageUri: Uri? = null
    private lateinit var genderSpinner: AutoCompleteTextView
    private lateinit var genderError: TextView
    private val db = Firebase.firestore
    private var hasError = false
    private lateinit var auth: FirebaseAuth
    private var googleli=""
    private var provider = " "
    private lateinit var binding: ActivityRegisterBinding
    var username = ""
    var password = ""
    var email = ""
    var age = ""
    var gender = ""
    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                profileImage.setImageURI(uri)
            }
        }
    }
    val retrofit = Retrofit.Builder()
        .baseUrl("https://ptfvjswhcnwcyoossfrt.supabase.co") // Reemplaza con tu proyecto
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(SupabaseStorageService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_register)
        setContentView(binding.root)
        enableEdgeToEdge()

        val intent = intent
        val emailRecuperado = intent.getStringExtra("textemail") ?: " "
        provider = intent.getStringExtra("provider") ?: " "
        googleli =intent.getStringExtra("google") ?: " "

        // Initialize views
        profileImage = findViewById(R.id.profileImage)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        emailInput = findViewById(R.id.emailInput)
        ageInput = findViewById(R.id.ageInput)
        usernameError = findViewById(R.id.usernameError)
        passwordError = findViewById(R.id.passwordError)
        emailError = findViewById(R.id.emailError)
        ageError = findViewById(R.id.ageError)
        termsError = findViewById(R.id.termsError)
        termsCheckbox = findViewById(R.id.termsCheckbox)
        genderSpinner = findViewById(R.id.genderSpinner)
        genderError = findViewById(R.id.genderError)
        // Set up profile image click listener
        auth = Firebase.auth
        if (emailRecuperado.isNotBlank()) {
            emailInput.setText(emailRecuperado)
            emailInput.isFocusable = false
            emailInput.isClickable = false
        }
        setupGenderSpinner()

        binding.terms.setOnClickListener {
            showPrivacyPolicyDialog()
        }
        profileImage.setOnClickListener {
            openImagePicker()
        }

        // Set up register button click listener
        findViewById<com.google.android.material.button.MaterialButton>(R.id.registerButton).setOnClickListener {
            validateAndRegister()
        }

    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun clearErrors() {
        usernameError.text = ""
        passwordError.text = ""
        emailError.text = ""
        ageError.text = ""
        termsError.text = ""
        genderError.text = ""
    }

    private fun validateAndRegister() {
       lifecycleScope.launch {
           clearErrors()


           username = usernameInput.text.toString().trim()
           password = passwordInput.text.toString().trim()
           email = emailInput.text.toString().trim()
           age = ageInput.text.toString().trim()
           gender = genderSpinner.text.toString()

           hasError = false

           // Validate inputs
           var existeEmail = comprobarEmailExistente(email)
           var existeUsername = comprobarNombreUsuarioExistente(username)

           if (username.isEmpty()) {
               usernameError.text = "El nombre de usuario es obligatorio"
               hasError = true
           }else if(existeUsername){
               usernameError.text = "El nombre de usuario ya existe"
               hasError = true
           }

           if (password.isEmpty()) {
               passwordError.text = "La contraseña es obligatoria"
               hasError = true
           } else if (!password.matches(Regex("^(?=.*[A-Z])(?=.*\\d).{6,}\$"))) {
               passwordError.text =
                   "La contraseña debe tener al menos 6 caracteres, una mayúscula y un número"
               hasError = true
           }

           if (email.isEmpty()) {
               emailError.text = "El correo es obligatorio"
               hasError = true
           } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
               emailError.text = "Formato de correo incorrecto"
               hasError = true
           }else if(existeEmail){
               emailError.text = "El correo electronico ya esta en uso"
               hasError = true
           }
           if (gender.isEmpty()) {
               genderError.text = "Por favor seleccione un género"
               hasError = true
           }
           if (age.isEmpty()) {
               ageError.text = "La edad es obligatoria"
               hasError = true
           } else if (age.toIntOrNull() == null) {
               ageError.text = "La edad debe ser un número"
               hasError = true
           } else if (Integer.parseInt(age) < 18) {
               ageError.text = "Debes tener al menos 18 años"
               hasError = true
           } else if (Integer.parseInt(age) > 100) {
               ageError.text = "Debes introducir una edad real."
               hasError = true
           }

           if (!termsCheckbox.isChecked) {
               termsError.text = "Debes aceptar los términos y condiciones"
               hasError = true
           }

           if (selectedImageUri == null) {
               Toast.makeText(this@RegisterActivity, "Por favor selecciona una imagen", Toast.LENGTH_SHORT)
                   .show()
               hasError = true
           }

           if (!hasError) {
               val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
               var contraseña = hashedPassword
               selectedImageUri?.let { uri ->
                   uploadImageToSupabase(uri, this@RegisterActivity, username) { downloadUrl ->
                       val redessociales = listOf("", "", "", "")
                       val usuario = Usuario(
                           e_mail = email,
                           contraseña = contraseña,
                           edad = age.toInt(),
                           nombre_usuario = username,
                           fecha_creacion = Timestamp.now(),
                           imagen_perfil = downloadUrl, // URL pública de Firebase Storage
                           sexo_usuario = gender,
                           redes_sociales = redessociales,
                           imagen1 = "",
                           imagen2 = "",
                           imagen3 = "",
                           provider = provider
                       )
                       if (googleli == "ligoogle") {
                           crearUsuarioFB(usuario, "google")
                       } else {
                           createAccount(usuario.e_mail, usuario.contraseña, usuario)
                       }
                   }
               }
           }
       }
    }
    suspend fun comprobarNombreUsuarioExistente(username: String): Boolean {
        return try {
            val result = db.collection("usuarios")
                .whereEqualTo("nombre_usuario", username)
                .get()
                .await()

            !result.isEmpty
        } catch (e: Exception) {
            Log.e("Firestore", "Error comprobando username", e)
            false
        }
    }
    suspend fun comprobarEmailExistente(email: String): Boolean {
        return try {
            val result = db.collection("usuarios")
                .whereEqualTo("e_mail", email)
                .get()
                .await() // Espera el resultado sin bloquear

            !result.isEmpty
        } catch (e: Exception) {
            Log.e("Firestore", "Error comprobando email", e)
            false
        }
    }
    private fun setupGenderSpinner() {
        val genders = arrayOf("Otro", "Masculino", "Femenino")
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, genders)
        genderSpinner.setAdapter(adapter)
        genderSpinner.setText(genders[0], false) // Set default value to "Otro"
    }
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }
    // [END on_start_check_user]

    private fun createAccount(email: String, password: String, usuario: Usuario) {
        // Hash the password before storing

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                    crearUsuarioFB(usuario,"")
                    Toast.makeText(
                        baseContext,
                        "Usuario dado de alta correctamente",
                        Toast.LENGTH_SHORT,
                    ).show()
                    val intent = Intent(this, Log_in::class.java)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI(null)
                }
            }
    }

    private fun crearUsuarioFB(usuario: Usuario, string: String) {
        db.collection("usuarios").add(usuario).addOnSuccessListener { documentReference ->
            Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            if(string=="google"){
                val intent = Intent(this, Log_in::class.java)
                startActivity(intent)
            }
        }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }

    }

    private fun updateUI(user: FirebaseUser?) {
    }

    private fun reload() {
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
    fun formatTimestamp(timestamp: Timestamp): String {
        val date = Date(timestamp.seconds * 1000) // Convertir segundos a milisegundos
        val sdf = SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault())
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

        val formattedDate = formatTimestamp(Timestamp.now())
        val bucket = "images"
        val fileName = "$username/perfil_${formattedDate}.jpg"

        val call = service.uploadImage(
            authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB0ZnZqc3doY253Y3lvb3NzZnJ0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDc0MDg4MDQsImV4cCI6MjA2Mjk4NDgwNH0.8K2P087GLNqcndYB_1FJW2tples19SUGb9t_5GByigk",
            bucket = bucket,
            fileName = fileName,
            file = filePart
        )

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("Supabase", "Imagen subida con éxito")

                    // Construir URL pública con las variables ya definidas
                    val publicUrl = "https://ptfvjswhcnwcyoossfrt.supabase.co/storage/v1/object/public/$bucket/$fileName"

                    onSuccess(publicUrl)
                } else {
                    Log.e("Supabase", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Error al subir imagen a Supabase", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("Supabase", "Fallo al subir imagen", t)
                Toast.makeText(context, "Fallo al subir imagen a Supabase", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun showPrivacyPolicyDialog() {
        val termsText = """
        <b>Bienvenido/a a Link2U</b><br>
        Link2U es una red social para conectar, compartir e interactuar.<br><br>

        <b>TÉRMINOS Y CONDICIONES DE USO</b><br><br>

        <b>1. Aceptación de términos</b><br>
        Al registrarte aceptas estos términos y nuestra política de privacidad.<br><br>

        <b>2. Edad mínima</b><br>
        Debes tener al menos 18 años para usar esta aplicación.<br><br>

        <b>3. Uso aceptable</b><br>
        No se permite contenido ofensivo, ilegal o que viole derechos de terceros.<br><br>

        <b>4. Privacidad</b><br>
        Recopilamos y almacenamos tus datos de forma segura. No los compartimos sin tu consentimiento.<br><br>

        <b>5. Contacto</b><br>
        Si tienes dudas, contáctanos en soportelink@link2u.com.<br><br>

        Al continuar, aceptas los términos y condiciones.
    """.trimIndent()

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Política de Privacidad")
        builder.setMessage(android.text.Html.fromHtml(termsText, android.text.Html.FROM_HTML_MODE_LEGACY))
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }


} 