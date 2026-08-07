package com.snipware.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {

    @Query("SELECT * FROM snippets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE id = :id")
    suspend fun getById(id: String): SnippetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snippet: SnippetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(snippets: List<SnippetEntity>)

    @Delete
    suspend fun delete(snippet: SnippetEntity)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE snippets SET copyCount = copyCount + 1 WHERE id = :id")
    suspend fun incrementCopyCount(id: String)

    @Query("SELECT DISTINCT language FROM snippets ORDER BY language ASC")
    fun observeLanguagesInUse(): Flow<List<String>>

    @Query("SELECT * FROM snippets WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<SnippetEntity>
}
