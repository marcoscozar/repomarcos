package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivitySplashScreenBinding
import com.google.firebase.firestore.FirebaseFirestore


class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        Handler().postDelayed({


            val email = intent.getStringExtra("email")
            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.putString("email",email)
            prefs.apply()
            val provider = intent.getStringExtra("provider") ?: " "
            val ligoogle = intent.getStringExtra("google") ?: " "
            val db = FirebaseFirestore.getInstance()
            db.collection("usuarios")
                .whereEqualTo("e_mail",email)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val intent = Intent(this, NavegacionActivity::class.java)
                        intent.putExtra("email",email)
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    } else {
                        val intent = Intent(this, RegisterActivity::class.java)
                        intent.putExtra("textemail",email)
                        intent.putExtra("google",ligoogle)
                        intent.putExtra("provider",provider)
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }
                }

        },2000
        )
    }
}