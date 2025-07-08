package com.example.darkmind.ui

import android.os.Bundle
import android.util.Log // Importar Log para depuración
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.darkmind.R
import com.example.darkmind.data.AppDatabase
import com.example.darkmind.data.Entry
import com.example.darkmind.utils.SentimentAnalyzer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import java.util.*

class EmotionSummaryActivity : AppCompatActivity() {

    private lateinit var tvSummaryContent: TextView
    private lateinit var analyzer: SentimentAnalyzer // Se inicializa en onCreate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emotion_summary)

        tvSummaryContent = findViewById(R.id.tvSummaryContent)
        analyzer = SentimentAnalyzer(this) // Inicializar aquí

        // Iniciar la coroutine para realizar el análisis en un hilo de fondo
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = analyzeWeekEmotion()
                withContext(Dispatchers.Main) {
                    tvSummaryContent.text = summary
                }
            } catch (e: Exception) {
                Log.e("EmotionSummaryActivity", "Error al analizar emociones: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    tvSummaryContent.text = "Error al cargar el resumen de emociones."
                }
            }
        }
    }

    /**
     * Analiza las emociones de las entradas del diario de la última semana.
     * Calcula un promedio de los puntajes de sentimiento y devuelve un resumen textual.
     */
    private suspend fun analyzeWeekEmotion(): String {
        // Obtener el ID del usuario actual de Firebase Authentication
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.d("EmotionSummaryActivity", "Usuario no autenticado.")
            return "Inicia sesión primero para ver el resumen de emociones."
        }

        // Obtener el DAO de la base de datos Room para acceder a las entradas
        val dao = AppDatabase.getInstance(this).entryDao()
        val entries = dao.getEntriesForUser(userId)

        // Filtrar entradas de la última semana
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Retroceder 7 días para obtener el inicio de la semana
        val lastWeekTimestamp = calendar.timeInMillis // Timestamp de hace 7 días

        // Filtrar entradas que sean de la última semana y no estén vacías
        val recentEntries = entries.filter { it.timestamp >= lastWeekTimestamp && it.text.isNotBlank() }

        Log.d("EmotionSummaryActivity", "Entradas recientes esta semana: ${recentEntries.size}")
        recentEntries.forEach { Log.d("EmotionSummaryActivity", "Texto: '${it.text}'") }

        if (recentEntries.isEmpty()) {
            return "No se han registrado emociones esta semana."
        }

        var sumOfProbabilities = 0f // Suma de las probabilidades de sentimiento
        var numberOfEntries = 0     // Contador de entradas procesadas

        // Bucle ÚNICO para calcular la suma de probabilidades y el total de entradas
        for (entry in recentEntries) {
            val prob = analyzer.predictSentiment(entry.text)
            Log.d("EmotionSummaryActivity", "Texto analizado: '${entry.text}' → Probabilidad: ${"%.2f".format(prob)}")
            sumOfProbabilities += prob
            numberOfEntries++
        }

        // Calcular el promedio de las probabilidades
        val averageSentiment = sumOfProbabilities / numberOfEntries

        Log.d("EmotionSummaryActivity", "Promedio de sentimiento: ${"%.2f".format(averageSentiment)}")

        // Devolver un resumen basado en el promedio de sentimiento
        return when {
            averageSentiment > 0.8 -> "🌞 ¡Semana brillante! Tus emociones son mayormente positivas. Sigue así."
            averageSentiment > 0.6 -> "😊 Fue una buena semana con buenos momentos."
            averageSentiment > 0.4 -> "😐 Semana equilibrada, con altibajos. Tómate un respiro."
            averageSentiment > 0.25 -> "😟 Parece que fue una semana difícil. Cuida de ti."
            else -> "🖤 Emociones muy negativas esta semana. No estás solo, pide apoyo si lo necesitas."
        }
    }
}
