package com.example.darkmind.data

import androidx.room.*

@Dao
interface EntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Entry)

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("SELECT * FROM entries WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getEntriesForUser(userId: String): List<Entry>

    @Query("SELECT * FROM entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<Entry>

    @Query("SELECT * FROM entries WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedEntriesForUser(userId: String): List<Entry>



}
