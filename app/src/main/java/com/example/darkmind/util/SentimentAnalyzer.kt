// SentimentAnalyzer.kt
package com.example.darkmind.utils

import android.content.Context
import android.util.Log // Importar Log para depuración
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.HashMap // Importar HashMap explícitamente

class SentimentAnalyzer(context: Context) {

    private val interpreter: Interpreter
    private val wordIndex: Map<String, Int>
    // Ajustar maxLen para que coincida con la longitud máxima de secuencia del modelo Python.
    // La última salida del script Python indicó 'Longitud máxima de secuencia: 7'.
    private val maxLen = 7

    init {
        // Cargar el modelo TFLite y el tokenizer al inicializar la clase
        try {
            interpreter = Interpreter(loadModelFile(context, "sentiment_model.tflite"))
            wordIndex = loadTokenizer(context)
            Log.d("SentimentAnalyzer", "Modelo TFLite y Tokenizer cargados exitosamente.")
        } catch (e: IOException) {
            Log.e("SentimentAnalyzer", "Error al cargar el modelo o el tokenizer: ${e.message}", e)
            throw RuntimeException("No se pudo cargar el modelo o el tokenizer.", e)
        } catch (e: JSONException) {
            Log.e("SentimentAnalyzer", "Error al parsear el tokenizer.json: ${e.message}", e)
            throw RuntimeException("Error al leer el archivo tokenizer.json.", e)
        }
    }

    /**
     * Carga el archivo del modelo TensorFlow Lite desde los assets de la aplicación.
     * @param context Contexto de la aplicación para acceder a los assets.
     * @param modelName Nombre del archivo del modelo (ej. "sentiment_model.tflite").
     * @return MappedByteBuffer del archivo del modelo.
     * @throws IOException Si el archivo no se puede abrir o leer.
     */
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Carga el vocabulario (word_index) del tokenizer desde el archivo tokenizer.json.
     * @param context Contexto de la aplicación para acceder a los assets.
     * @return Un mapa de palabras a sus índices.
     * @throws IOException Si el archivo no se puede abrir o leer.
     * @throws JSONException Si el archivo JSON no tiene el formato esperado.
     */
    private fun loadTokenizer(context: Context): Map<String, Int> {
        val jsonStr = context.assets.open("tokenizer.json").bufferedReader().use { it.readText() }

        val jsonObject = JSONObject(jsonStr)
        val config = jsonObject.getJSONObject("config")

        // Obtener "word_index" como String y luego parsearlo a JSONObject
        val wordIndexStr = config.getString("word_index")
        val wordIndexJson = JSONObject(wordIndexStr) // Parsear la cadena a un JSONObject

        val tokenizerMap = HashMap<String, Int>() // Usar HashMap para mutabilidad
        val keys = wordIndexJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            tokenizerMap[key] = wordIndexJson.getInt(key)
        }
        return tokenizerMap
    }

    /**
     * Preprocesa el texto de entrada para que sea compatible con el modelo TFLite.
     * Convierte el texto a una secuencia de índices de palabras y la "padea" a la longitud máxima.
     * @param text El texto de la entrada del diario.
     * @return Un IntArray que representa la secuencia preprocesada.
     */
    private fun preprocess(text: String): IntArray {
        // Convertir a minúsculas
        val lowercasedText = text.lowercase(Locale.ROOT)
        // Reemplazar caracteres de puntuación y símbolos con espacios, pero mantener números y letras
        // Esto imita más de cerca el comportamiento predeterminado de Keras Tokenizer
        val cleanedText = lowercasedText.replace(Regex("[^a-z0-9áéíóúüñ\\s]"), " ")
            .replace(Regex("\\s+"), " ") // Reemplazar múltiples espacios con uno solo
            .trim() // Eliminar espacios al inicio/final

        val words = cleanedText.split(" ")
            .filter { it.isNotBlank() }

        val sequence = words.map { word -> wordIndex[word] ?: wordIndex["<OOV>"] ?: 0 }

        val padded = IntArray(maxLen) { 0 } // Array de ceros para el padding
        // Copiar la secuencia al array padded, truncando si es más larga que maxLen
        for (i in sequence.indices.take(maxLen)) {
            padded[i] = sequence[i]
        }
        Log.d("SentimentAnalyzer", "Texto original: '$text'")
        Log.d("SentimentAnalyzer", "Texto limpio (preprocesado): '$cleanedText'") // Nuevo log para depuración
        Log.d("SentimentAnalyzer", "Secuencia preprocesada (indices): ${padded.joinToString()}")
        return padded
    }

    /**
     * Realiza la predicción de sentimiento para un texto dado utilizando el modelo TFLite.
     * @param text El texto a analizar.
     * @return La probabilidad de que el sentimiento sea positivo (valor entre 0 y 1).
     */
    fun predictSentiment(text: String): Float {
        val input = preprocess(text)

        // ByteBuffer para la entrada del modelo.
        // El modelo espera float32, por lo que cada entero de 'input' debe convertirse a float.
        // 4 * maxLen bytes porque cada float es de 4 bytes.
        val byteBuffer = ByteBuffer.allocateDirect(4 * maxLen)
        byteBuffer.order(ByteOrder.nativeOrder()) // Asegurar el orden de bytes nativo

        // Poner los valores enteros como floats en el ByteBuffer
        for (i in input) {
            byteBuffer.putFloat(i.toFloat())
        }
        byteBuffer.rewind() // Resetear la posición del buffer al principio

        // ByteBuffer para la salida del modelo (un solo float para la probabilidad)
        val outputBuffer = ByteBuffer.allocateDirect(4) // 4 bytes para un float
        outputBuffer.order(ByteOrder.nativeOrder())

        // Ejecutar el modelo
        interpreter.run(byteBuffer, outputBuffer)

        outputBuffer.rewind() // Resetear la posición del buffer de salida
        val result = outputBuffer.float // Obtener el resultado como float
        Log.d("SentimentAnalyzer", "Predicción cruda: $result")
        return result
    }

    /**
     * Convierte la probabilidad de sentimiento en una etiqueta textual.
     * @param prob La probabilidad de sentimiento (entre 0 y 1).
     * @return Una cadena de texto que describe el sentimiento.
     */
    fun getSentimentLabel(prob: Float): String {
        return when {
            prob > 0.75 -> "Muy Positivo"
            prob > 0.6 -> "Positivo"
            prob > 0.4 -> "Neutral"
            prob > 0.25 -> "Negativo"
            else -> "Muy Negativo"
        }
    }
}