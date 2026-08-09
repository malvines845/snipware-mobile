package com.snipware.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {

    @Query("SELECT * FROM snippets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE id = :id")
    suspend fun getById(id: String): SnippetEntity?

    /**
     * Writes ONLY the main row -- does not touch snippets_fts. Only called
     * from [upsertWithFts] within this same interface. Do not call this
     * directly from Repository/ViewModel code; it will silently leave the
     * search index out of sync with what's actually in the table.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snippet: SnippetEntity)

    /**
     * Deletes ONLY the main row -- does not touch snippets_fts. Only called
     * from [deleteWithFts] within this same interface. Do not call this
     * directly; see the warning on [upsert].
     */
    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE snippets SET copyCount = copyCount + 1 WHERE id = :id")
    suspend fun incrementCopyCount(id: String)

    @Query("SELECT DISTINCT language FROM snippets ORDER BY language ASC")
    fun observeLanguagesInUse(): Flow<List<String>>

    @Query("SELECT * FROM snippets WHERE syncStatus = 'pending'")
    suspend fun getPendingSync(): List<SnippetEntity>

    // ── FTS4 search index (see SnippetFtsEntity.kt for why it's standalone) ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFtsEntry(entry: SnippetFtsEntity)

    @Query("DELETE FROM snippets_fts WHERE snippetId = :snippetId")
    suspend fun deleteFtsEntry(snippetId: String)

    @Query("SELECT snippetId FROM snippets_fts WHERE snippets_fts MATCH :matchQuery")
    suspend fun ftsMatchSnippetIds(matchQuery: String): List<String>

    /** Keeps the main row and its FTS shadow row in sync as a single atomic write. */
    @Transaction
    suspend fun upsertWithFts(entity: SnippetEntity, fts: SnippetFtsEntity) {
        upsert(entity)
        deleteFtsEntry(entity.id)
        insertFtsEntry(fts)
    }

    @Transaction
    suspend fun deleteWithFts(id: String) {
        deleteById(id)
        deleteFtsEntry(id)
    }
}

