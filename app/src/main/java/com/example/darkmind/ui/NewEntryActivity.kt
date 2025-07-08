package com.example.darkmind.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.children
import com.example.darkmind.R
import com.example.darkmind.data.AppDatabase
import com.example.darkmind.data.Entry
import com.example.darkmind.data.EntryRepository
import kotlinx.coroutines.*
import java.io.File
import com.google.firebase.auth.FirebaseAuth


class NewEntryActivity : AppCompatActivity() {

    private lateinit var etText: EditText
    private lateinit var spinnerContext: Spinner
    private lateinit var etTrigger: EditText
    private lateinit var emotionContainer: LinearLayout
    private lateinit var ivImagePreview: ImageView
    private lateinit var tvAudioPreview: TextView
    private lateinit var btnAddPhoto: Button
    private lateinit var btnAddAudio: Button
    private lateinit var btnSave: Button
    private lateinit var audioControls: LinearLayout
    private lateinit var btnStopRecording: Button
    private lateinit var btnPlayAudio: Button
    private lateinit var btnDeleteAudio: Button


    private var imageUri: Uri? = null
    private var audioFile: File? = null
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var selectedEmotion = -1

    private lateinit var repository: EntryRepository

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->

        val cameraGranted = result[Manifest.permission.CAMERA] == true
        val audioGranted = result[Manifest.permission.RECORD_AUDIO] == true

        when {
            cameraGranted && !audioGranted -> showPhotoOptions()
            !cameraGranted && audioGranted -> recordAudio()
            cameraGranted && audioGranted -> {
                // Si por alguna razón ambos se pidieron, asumimos foto como prioridad
                showPhotoOptions()
            }
            else -> Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
        }
    }


    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            ivImagePreview.setImageURI(imageUri)
        } else {
            Toast.makeText(this, "No se tomó la foto", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File.createTempFile("selected_", ".jpg", cacheDir)
                inputStream?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                imageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                ivImagePreview.setImageURI(imageUri)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show()
                imageUri = null
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_entry)

        // UI refs
        etText = findViewById(R.id.etText)
        spinnerContext = findViewById(R.id.spinnerContext)
        etTrigger = findViewById(R.id.etTrigger)
        emotionContainer = findViewById(R.id.emotionContainer)
        ivImagePreview = findViewById(R.id.previewImage)
        tvAudioPreview = findViewById(R.id.tvAudioPreview)
        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        btnAddAudio = findViewById(R.id.btnAddAudio)
        btnSave = findViewById(R.id.btnSave)

        repository = EntryRepository(AppDatabase.getInstance(this).entryDao())

        // Spinner
        val contextOptions = listOf("Casa", "Trabajo", "Escuela", "Con amigos", "Solo", "Otro")
        spinnerContext.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, contextOptions)

        // Emociones
        emotionContainer.children.forEachIndexed { index, view ->
            if (view is ImageView) {
                view.setOnClickListener {
                    selectedEmotion = index + 1
                    emotionContainer.children.forEach {
                        if (it is ImageView) {
                            it.setColorFilter(
                                if (it == view) getColor(R.color.teal_200)
                                else getColor(R.color.gray)
                            )
                        }
                    }
                }
            }
        }

        btnAddPhoto.setOnClickListener {
            requestPhotoPermissionsAndProceed()
        }
        btnAddAudio.setOnClickListener {
            requestAudioPermissionsAndProceed()
        }

        tvAudioPreview.setOnClickListener { playAudio() }
        audioControls = findViewById(R.id.audioControls)
        btnStopRecording = findViewById(R.id.btnStopRecording)
        btnPlayAudio = findViewById(R.id.btnPlayAudio)
        btnDeleteAudio = findViewById(R.id.btnDeleteAudio)

        btnStopRecording.setOnClickListener { stopRecording() }
        btnPlayAudio.setOnClickListener { playAudio() }
        btnDeleteAudio.setOnClickListener { deleteAudio() }

        audioControls.visibility = View.GONE

        btnSave.setOnClickListener { saveEntry() }
    }

    private fun requestPhotoPermissionsAndProceed() {
        val permissions = listOf(Manifest.permission.CAMERA)
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun requestAudioPermissionsAndProceed() {
        val permissions = listOf(Manifest.permission.RECORD_AUDIO)
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun showPhotoOptions() {
        val options = arrayOf("Tomar Foto", "Seleccionar Foto")

        AlertDialog.Builder(this)
            .setTitle("Elegir opción")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun showMediaOptions() {
        val options = arrayOf("Tomar Foto", "Seleccionar Foto", "Grabar Audio (30s)")

        AlertDialog.Builder(this)
            .setTitle("Elegir acción")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickImageLauncher.launch("image/*")
                    2 -> recordAudio()
                }
            }.show()
    }

    private fun takePhoto() {
        val photoFile = File.createTempFile("photo_", ".jpg", cacheDir).apply { deleteOnExit() }
        imageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        takePictureLauncher.launch(imageUri!!)
    }

    private fun recordAudio() {
        audioFile = File.createTempFile("audio_", ".3gp", cacheDir).apply { deleteOnExit() }

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(audioFile!!.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            prepare()
            start()
        }

        tvAudioPreview.text = "🎙 Grabando..."
        audioControls.visibility = View.VISIBLE
        btnStopRecording.visibility = View.VISIBLE
        btnPlayAudio.visibility = View.GONE
        btnDeleteAudio.visibility = View.GONE
        btnAddAudio.isEnabled = false

        // En caso no pare manualmente, se detiene a los 30s
        Handler(Looper.getMainLooper()).postDelayed({
            if (recorder != null) {
                stopRecording()
            }
        }, 30_000)
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            tvAudioPreview.text = "✅ Audio grabado: ${audioFile!!.name}"
        } catch (e: Exception) {
            tvAudioPreview.text = "❌ Error al detener"
        } finally {
            btnStopRecording.visibility = View.GONE
            btnPlayAudio.visibility = View.VISIBLE
            btnDeleteAudio.visibility = View.VISIBLE
            btnAddAudio.isEnabled = true
        }
    }
    private fun deleteAudio() {
        player?.release()
        recorder?.release()
        audioFile?.delete()
        audioFile = null

        tvAudioPreview.text = "🎤 Audio eliminado"
        audioControls.visibility = View.GONE
        btnAddAudio.isEnabled = true
    }


    private fun playAudio() {
        if (audioFile?.exists() == true) {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(audioFile!!.absolutePath)
                prepare()
                start()
            }
            Toast.makeText(this, "▶ Reproduciendo audio...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No hay audio grabado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveEntry() {
        val text = etText.text.toString().trim()
        val context = spinnerContext.selectedItem?.toString() ?: ""
        val trigger = etTrigger.text.toString().trim()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        if (text.isEmpty() || selectedEmotion == -1 || userId == null) {
            Toast.makeText(this, "Falta completar campos o no has iniciado sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val entry = Entry(
            text = text,
            emotion = selectedEmotion,
            context = context,
            trigger = trigger,
            imageUri = imageUri?.toString(),
            audioUri = audioFile?.toURI()?.toString(),
            isSynced = false,
            userId = userId // 👈 Se registra el usuario que la creó
        )

        CoroutineScope(Dispatchers.IO).launch {
            repository.insert(entry)
            runOnUiThread {
                Toast.makeText(this@NewEntryActivity, "Entrada guardada", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        recorder?.release()
        player?.release()
    }
}
