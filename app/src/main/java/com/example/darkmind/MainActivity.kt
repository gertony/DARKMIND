package com.example.darkmind

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.darkmind.login.LoginActivity
import com.example.darkmind.ui.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user != null) {
            // Usuario ya autenticado → ir al Home
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // No autenticado → ir al Login
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Finaliza esta activity para que no se pueda volver atrás
        finish()
    }
}
