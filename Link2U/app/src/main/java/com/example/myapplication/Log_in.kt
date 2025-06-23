package com.example.myapplication

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityLogInBinding
import com.example.myapplication.model.Usuario
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import org.mindrot.jbcrypt.BCrypt

class Log_in : AppCompatActivity() {
    private lateinit var binding: ActivityLogInBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth
        session()

        // Añadir el listener para el texto de contraseña olvidada
        binding.textView5.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Si has olvidado la contraseña, escribe al correo para que los administradores puedan reestablecerla.")
                .setPositiveButton("Email") { _, _ ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        data = Uri.parse("mailto:soportelink@link2u.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Contraseña olvidada")
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }

        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()

        // Configurar el launcher para Google Sign-In
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                Log.d("EMAIL",account.email.toString())

                db.collection("usuarios")
                    .whereEqualTo("e_mail", account.email).get()
                    .addOnSuccessListener { result ->
                        Log.d("RESULT",result.isEmpty.toString())
                        if(!result.isEmpty){
                            for (document in result) {
                                if (!result.isEmpty && document.getString("provider") == "google") {
                                    firebaseAuthWithGoogle(account.idToken!!)
                                } else if (!result.isEmpty && document.getString("provider") == " ") {
                                    Toast.makeText(this, "Email ya registrado", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }else{
                            firebaseAuthWithGoogle(account.idToken!!)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d("Hugo", e.toString())
                    }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Error en el inicio de sesión con Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener del botón Google
        binding.botonGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvReg.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.button2.setOnClickListener {
            if (binding.editTextTextEmailAddress2.text.toString() != "" && binding.editTextTextPassword2.text.toString() != "") {
                binding.errorEmail.text = ""
                binding.errorPassword.text = ""
                signIn(
                    binding.editTextTextEmailAddress2.text.toString(),
                    binding.editTextTextPassword2.text.toString()
                )
            } else {
                if (binding.editTextTextEmailAddress2.text.toString() == "") {
                    binding.errorEmail.text = "Debes introducir un email"
                }else{
                    binding.errorEmail.text = ""
                }
                if (binding.editTextTextPassword2.text.toString() == "") {
                    binding.errorPassword.text = "Debes introducir una contraseña."
                }else{
                    binding.errorPassword.text = ""
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun session() {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email_recuperado = prefs.getString("email", null)
        if (email_recuperado != null) {
            val intent = Intent(this, SplashScreen::class.java)
            intent.putExtra("email", email_recuperado)
            startActivity(intent)
        }
    }

    private fun signIn(email: String, password: String) {
        Toast.makeText(
            baseContext,
            "Bienvenido: Hola",
            Toast.LENGTH_SHORT
        )
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                    val intent = Intent(this, SplashScreen::class.java)
                    intent.putExtra("email",email)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    db.collection("usuarios")
                        .whereEqualTo("e_mail", email).get()
                        .addOnSuccessListener { result ->
                            if (!result.isEmpty) {
                                binding.errorEmail.text = ""
                                for (document in result) {
                                    val hashedPassword = document.getString("contraseña")
                                    if (hashedPassword != null && !BCrypt.checkpw(password, hashedPassword)) {
                                        binding.errorPassword.text =
                                            "Contraseña incorrecta"
                                    }
                                }
                            } else {
                                binding.errorEmail.text =
                                    "El email introducido no existe."
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding document", e)
                            Log.d("Hugo", e.toString())
                        }
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {}

    private fun reload() {}

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                        val user = auth.currentUser
                        val email = user?.email

                            // Guardar el email en las preferencias
                            val prefs = getSharedPreferences(
                                getString(R.string.prefs_file),
                                Context.MODE_PRIVATE
                            ).edit()
                            prefs.putString("email", email)
                            prefs.apply()

                            Toast.makeText(
                                this,
                                "Bienvenido: ${user?.displayName}",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this, SplashScreen::class.java)
                            intent.putExtra("google", "ligoogle")
                            intent.putExtra("email", email)
                            intent.putExtra("provider","google")
                            startActivity(intent)
                            finish()
                        }
            }
    }

    companion object {
        private const val TAG = "EmailPassword"
    }

}
