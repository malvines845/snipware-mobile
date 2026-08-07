package com.snipware.app.data.repository

import com.snipware.app.data.local.SnippetDao
import com.snipware.app.data.local.toDomain
import com.snipware.app.data.local.toEntity
import com.snipware.app.data.model.Snippet
import com.snipware.app.data.model.SyncStatus
import com.snipware.app.data.search.FuzzySearch
import com.snipware.app.data.sync.SyncGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Single source of truth for snippet data. Combines local persistence
 * (Room), native fuzzy ranking, and the sync gateway placeholder --
 * everything above this (ViewModels, UI) is unaware of any of those
 * three being swapped out independently.
 */
class SnippetRepository(
    private val dao: SnippetDao,
    private val syncGateway: SyncGateway
) {
    fun observeAll(): Flow<List<Snippet>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeLanguagesInUse(): Flow<List<String>> = dao.observeLanguagesInUse()

    suspend fun getById(id: String): Snippet? = dao.getById(id)?.toDomain()

    /** Creates or updates a snippet, marking it pending sync (mirrors persistSnippet() in db.js). */
    suspend fun upsert(snippet: Snippet) {
        val toSave = snippet.copy(
            updatedAt = Instant.now().toString(),
            syncStatus = SyncStatus.PENDING
        )
        dao.upsert(toSave.toEntity())
        syncGateway.queueUpsert(toSave)
    }

    suspend fun delete(snippet: Snippet) {
        dao.deleteById(snippet.id)
        syncGateway.queueDelete(snippet.id)
    }

    suspend fun incrementCopyCount(id: String) {
        dao.incrementCopyCount(id)
    }

    suspend fun toggleFavorite(snippet: Snippet) {
        upsert(snippet.copy(isFavorite = !snippet.isFavorite))
    }

    suspend fun toggleLocked(snippet: Snippet) {
        upsert(snippet.copy(isLocked = !snippet.isLocked))
    }

    /** Ranks [snippets] against [query] using the native fuzzy-match engine. */
    fun search(query: String, snippets: List<Snippet>): List<Snippet> =
        FuzzySearch.rank(query, snippets)

    /** Pulls remote snippets (no-op until SyncGateway has a real backend) and merges them in. */
    suspend fun syncPull() {
        val remote = syncGateway.pull()
        if (remote.isNotEmpty()) {
            dao.upsertAll(remote.map { it.toEntity() })
        }
    }
}
