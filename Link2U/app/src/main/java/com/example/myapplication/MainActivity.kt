package com.example.myapplication

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.dialogs.FriendRequestDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val denied = permissions.filterValues { !it }
            if (denied.isNotEmpty()) {
                Toast.makeText(this, "Se requieren permisos para continuar", Toast.LENGTH_LONG).show()
            } else {
                Log.d("PERMISOS", "Todos los permisos fueron concedidos")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestAllPermissions()

        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email_recuperado = prefs.getString("email", null)
        Log.d("EMAILLLL", email_recuperado.toString())
        if (email_recuperado != null) {
            binding.button.text = "COMENZAR"
            binding.tvReg.visibility = View.GONE
            binding.textview.visibility = View.GONE
        } else {
            binding.button.text = "Iniciar sesión"
            binding.tvReg.visibility = View.VISIBLE
            binding.textview.visibility = View.VISIBLE
        }

        binding.pantallaCompleta.setOnClickListener {
            val intent = Intent(this, SplashScreen::class.java)
            intent.putExtra("email", email_recuperado)
            startActivity(intent)
        }

        binding.button.setOnClickListener {
            startActivity(Intent(this, Log_in::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        binding.tvReg.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Notificaciones
        intent.getStringExtra("request_id")?.let { handleFriendRequest(it) }
        intent.getStringExtra("chatId")?.let { handleChatNotification(it) }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        } else {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun handleFriendRequest(requestId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("friend_requests")
            .document(requestId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fromName = document.getString("from_name") ?: return@addOnSuccessListener
                    val fromEmail = document.getString("from_email") ?: return@addOnSuccessListener
                    val toEmail = document.getString("to_email") ?: return@addOnSuccessListener

                    FriendRequestDialog(
                        this,
                        requestId,
                        fromName,
                        fromEmail,
                        toEmail
                    ).show()
                }
            }
    }

    private fun handleChatNotification(chatId: String) {
        val intent = Intent(this, NavegacionActivity::class.java).apply {
            putExtra("chatId", chatId)
        }
        startActivity(intent)
    }

    override fun onStop() {
        super.onStop()
    }
}
