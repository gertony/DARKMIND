package com.example.darkmind.data

import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.darkmind.R

class EntryAdapter(private val entries: List<Entry>) : RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {

    class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmotion: TextView = view.findViewById(R.id.tvEmotion)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvContext: TextView = view.findViewById(R.id.tvContext)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context

        holder.tvEmotion.text = getEmotionEmoji(entry.emotion)
        holder.tvDate.text = formatDate(entry.timestamp)
        holder.tvContext.text = entry.context

        holder.itemView.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_entry_detail, null)

            view.findViewById<TextView>(R.id.tvDetailText).text = entry.text
            view.findViewById<TextView>(R.id.tvDetailTrigger).text = "Desencadenante: ${entry.trigger}"
            view.findViewById<TextView>(R.id.tvDetailEmotion).text = getEmotionEmoji(entry.emotion)
            view.findViewById<TextView>(R.id.tvDetailDate).text = formatDate(entry.timestamp)

            val imageView = view.findViewById<ImageView>(R.id.ivDetailImage)
            if (!entry.imageUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(entry.imageUri)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val drawable = Drawable.createFromStream(input, uri.toString())
                        imageView.setImageDrawable(drawable)
                    }
                } catch (e: Exception) {
                    imageView.visibility = View.GONE
                }
            } else {
                imageView.visibility = View.GONE
            }

            val btnPlay = view.findViewById<Button>(R.id.btnPlayAudio)
            if (!entry.audioUri.isNullOrEmpty()) {
                btnPlay.setOnClickListener {
                    val mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, Uri.parse(entry.audioUri))
                        prepare()
                        start()
                    }
                }
            } else {
                btnPlay.visibility = View.GONE
            }

            builder.setView(view)
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }


    override fun getItemCount(): Int = entries.size

    private fun getEmotionEmoji(code: Int): String {
        return when (code) {
            1 -> "😢"
            2 -> "😠"
            3 -> "😊"
            4 -> "😨"
            5 -> "😐"
            else -> "❓"
        }
    }
}
