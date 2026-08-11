package com.snipware.app.data.repository

import com.snipware.app.data.local.SnippetDao
import com.snipware.app.data.local.toDomain
import com.snipware.app.data.local.toEntity
import com.snipware.app.data.model.Snippet
import com.snipware.app.data.model.SyncStatus
import com.snipware.app.data.search.SearchEngine
import com.snipware.app.data.sync.SyncGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Single source of truth for snippet data. Room (via [dao]) is pure
 * persistence; the native [searchEngine] is a rebuildable in-memory cache
 * kept incrementally in sync on every write -- see [upsert]/[delete] and
 * [ensureSearchIndexHydrated]. Everything above this (ViewModels, UI) is
 * unaware either of those, or [syncGateway], are swappable independently.
 */
class SnippetRepository(
    private val dao: SnippetDao,
    private val syncGateway: SyncGateway,
    private val searchEngine: SearchEngine
) {
    private val hydrationMutex = Mutex()
    private var hydrated = false

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
        searchEngine.upsert(toSave)
        syncGateway.queueUpsert(toSave)
    }

    suspend fun delete(snippet: Snippet) {
        dao.deleteById(snippet.id)
        searchEngine.remove(snippet.id)
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

    /**
     * Ranks [snippets] against [query] using the native search engine
     * (trigram candidate narrowing + fuzzy scoring, entirely in C++ -- see
     * search_engine.cpp). [snippets] is expected to already be
     * language-filtered by the caller; the native engine's own ranked ID
     * list is intersected against it, so language filtering and relevance
     * ranking compose correctly regardless of which happens "first".
     */
    suspend fun search(query: String, snippets: List<Snippet>): List<Snippet> {
        if (query.isBlank() || snippets.isEmpty()) return snippets
        ensureSearchIndexHydrated()

        val bySnippetId = snippets.associateBy { it.id }
        return searchEngine.search(query).mapNotNull { bySnippetId[it] }
    }

    /**
     * Populates the native search index from Room exactly once per process.
     * Safe to call repeatedly/concurrently -- callers after the first
     * either see it already done (fast path) or suspend until an
     * in-progress hydration finishes (via the mutex), never racing a
     * search against a half-built index.
     */
    suspend fun ensureSearchIndexHydrated() {
        if (hydrated) return
        hydrationMutex.withLock {
            if (hydrated) return@withLock
            val all = dao.observeAll().first().map { it.toDomain() }
            searchEngine.rebuildAll(all)
            hydrated = true
        }
    }

    /** Pulls remote snippets (no-op until SyncGateway has a real backend) and merges them in. */
    suspend fun syncPull() {
        val remote = syncGateway.pull()
        remote.forEach { pulled ->
            // Marked SYNCED, not PENDING -- this data just came *from* the
            // server, so re-queuing it for upload would be a pointless
            // round-trip (upsert() is for locally-originated edits).
            val synced = pulled.copy(syncStatus = SyncStatus.SYNCED)
            dao.upsert(synced.toEntity())
            searchEngine.upsert(synced)
        }
    }
}
