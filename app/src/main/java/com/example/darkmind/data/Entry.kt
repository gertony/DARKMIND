package com.example.darkmind.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val text: String,
    val emotion: Int,
    val context: String,
    val trigger: String,
    val imageUri: String?,
    val audioUri: String?,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isSynced")
    val isSynced: Boolean = false
)
