package com.example.darkmind.data
import android.util.Log
import kotlinx.coroutines.tasks.await
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.net.URI

import com.google.firebase.firestore.FirebaseFirestore

class EntryRepository(private val dao: EntryDao) {

    suspend fun insert(entry: Entry) = dao.insert(entry)


    suspend fun getUnsynced() = dao.getUnsyncedEntries()

    suspend fun update(entry: Entry) = dao.update(entry)

    suspend fun delete(entry: Entry) = dao.delete(entry)
    suspend fun getForUser(userId: String) = dao.getEntriesForUser(userId)
    suspend fun syncUnsyncedEntries(userId: String, firestore: FirebaseFirestore) {
        val unsynced = dao.getUnsyncedEntries()
        val storage = FirebaseStorage.getInstance()

        for (entry in unsynced) {
            try {
                var uploadedImageUrl: String? = null
                var uploadedAudioUrl: String? = null

                // 🔼 Subir imagen si existe
                entry.imageUri?.let { uriStr ->
                    val uri = Uri.parse(uriStr)
                    val imageRef = storage.reference.child("$userId/images/${System.currentTimeMillis()}.jpg")
                    imageRef.putFile(uri).await()
                    uploadedImageUrl = imageRef.downloadUrl.await().toString()
                }

                entry.audioUri?.let { uriStr ->
                    val uri = Uri.parse(uriStr)
                    val audioRef = storage.reference.child("$userId/audios/${System.currentTimeMillis()}.3gp")
                    audioRef.putFile(uri).await()
                    uploadedAudioUrl = audioRef.downloadUrl.await().toString()
                }


                // 🔄 Crear mapa para Firestore
                val entryMap = mapOf(
                    "text" to entry.text,
                    "emotion" to entry.emotion,
                    "context" to entry.context,
                    "trigger" to entry.trigger,
                    "imageUri" to uploadedImageUrl,
                    "audioUri" to uploadedAudioUrl,
                    "timestamp" to entry.timestamp
                )

                firestore.collection("entries")
                    .document(userId)
                    .collection("entries")
                    .add(entryMap)
                    .await()

                // ✅ Marcar como sincronizado
                dao.update(entry.copy(isSynced = true))
            } catch (e: Exception) {
                Log.e("SyncError", "Error al subir entry: ${e.message}")
            }
        }
    }

    suspend fun downloadFromFirestore(userId: String, firestore: FirebaseFirestore) {
        val remoteEntries = firestore
            .collection("entries")
            .document(userId)
            .collection("entries")
            .get()
            .await()

        val localEntries = dao.getEntriesForUser(userId)
        val localTimestamps = localEntries.map { it.timestamp }.toSet()

        val storage = FirebaseStorage.getInstance()

        for (doc in remoteEntries) {
            val timestamp = doc.getLong("timestamp") ?: continue
            if (timestamp in localTimestamps) continue

            var localImagePath: String? = null
            var localAudioPath: String? = null

            val remoteImageUri = doc.getString("imageUri")
            if (!remoteImageUri.isNullOrEmpty()) {
                try {
                    val imageRef = storage.getReferenceFromUrl(remoteImageUri)
                    val imageFile = File.createTempFile("img_", ".jpg")
                    imageRef.getFile(imageFile).await()
                    localImagePath = imageFile.toURI().toString()
                } catch (e: Exception) {
                    Log.e("SyncDownload", "Error descargando imagen: ${e.message}")
                }
            }

            // ↓↓↓ DESCARGAR audio si existe ↓↓↓
            val remoteAudioUri = doc.getString("audioUri")
            if (!remoteAudioUri.isNullOrEmpty()) {
                try {
                    val audioRef = storage.getReferenceFromUrl(remoteAudioUri)
                    val audioFile = File.createTempFile("aud_", ".3gp")
                    audioRef.getFile(audioFile).await()
                    localAudioPath = audioFile.toURI().toString()
                } catch (e: Exception) {
                    Log.e("SyncDownload", "Error descargando audio: ${e.message}")
                }
            }

            val entry = Entry(
                userId = userId,
                text = doc.getString("text") ?: "",
                emotion = (doc.getLong("emotion") ?: 0).toInt(),
                context = doc.getString("context") ?: "",
                trigger = doc.getString("trigger") ?: "",
                imageUri = localImagePath,
                audioUri = localAudioPath,
                timestamp = timestamp,
                isSynced = true
            )

            dao.insert(entry)
        }
    }




}
