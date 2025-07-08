package com.example.darkmind.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.darkmind.login.LoginActivity
import com.example.darkmind.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnNewEntry.setOnClickListener {
            startActivity(Intent(this, NewEntryActivity::class.java))
        }
        binding.btnEmotionsSummary.setOnClickListener {
            startActivity(Intent(this, EmotionSummaryActivity::class.java))
        }
        binding.btnEntriesList.setOnClickListener {
            startActivity(Intent(this, EntriesListActivity::class.java))
        }

        // Puedes personalizar un saludo
        val email = auth.currentUser?.email?.substringBefore('@') ?: "Usuario"
        binding.tvWelcome.text = "Bienvenido, $email"
    }
}
