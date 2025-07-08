package com.example.darkmind.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.darkmind.R
import com.example.darkmind.data.AppDatabase
import com.example.darkmind.data.EntryAdapter
import com.example.darkmind.data.EntryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntriesListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var repository: EntryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        setContentView(R.layout.activity_entries_list)

        recyclerView = findViewById(R.id.rvEntries)
        recyclerView.layoutManager = LinearLayoutManager(this)

        repository = EntryRepository(AppDatabase.getInstance(this).entryDao())

        loadEntries()
        val btnSync = findViewById<Button>(R.id.btnSync)
        btnSync.setOnClickListener {
            syncWithFirestore()
        }

    }

    private fun loadEntries() {
        CoroutineScope(Dispatchers.IO).launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val entries = repository.getForUser(userId)
                runOnUiThread {
                    recyclerView.adapter = EntryAdapter(entries)
                }
            } else {
                runOnUiThread {
                    // Mostrar lista vacía o mensaje de error si no hay sesión
                    recyclerView.adapter = EntryAdapter(emptyList())
                }
            }
        }
    }
    private fun syncWithFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "No hay usuario activo", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            repository.syncUnsyncedEntries(userId, firestore)
            repository.downloadFromFirestore(userId, firestore)

            val updatedEntries = repository.getForUser(userId)

            runOnUiThread {
                recyclerView.adapter = EntryAdapter(updatedEntries)
                Toast.makeText(this@EntriesListActivity, "Sincronización completa (subida y bajada)", Toast.LENGTH_SHORT).show()
            }
        }
    }





}
