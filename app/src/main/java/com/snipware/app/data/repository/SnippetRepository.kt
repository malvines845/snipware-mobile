package com.snipware.app.data.repository

import com.snipware.app.data.local.SnippetDao
import com.snipware.app.data.local.toDomain
import com.snipware.app.data.local.toEntity
import com.snipware.app.data.local.toFtsEntity
import com.snipware.app.data.model.Snippet
import com.snipware.app.data.model.SyncStatus
import com.snipware.app.data.search.FuzzySearch
import com.snipware.app.data.sync.SyncGateway
import com.snipware.app.util.SnipConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Single source of truth for snippet data. Combines local persistence
 * (Room), native fuzzy ranking, an FTS4 pre-filter for large libraries,
 * and the sync gateway placeholder -- everything above this (ViewModels,
 * UI) is unaware of any of those being swapped out independently.
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
        dao.upsertWithFts(toSave.toEntity(), toSave.toFtsEntity())
        syncGateway.queueUpsert(toSave)
    }

    suspend fun delete(snippet: Snippet) {
        dao.deleteWithFts(snippet.id)
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
     * Ranks [snippets] against [query] using the native fuzzy-match engine.
     *
     * For libraries at or below [SnipConstants.FTS_PREFILTER_THRESHOLD], every
     * snippet is scored directly -- identical behavior (and identical typo
     * tolerance) to before. Above that, an FTS4 MATCH query first narrows
     * [snippets] down to a candidate set, and only those get the full native
     * score, so a huge library doesn't mean scoring every row on every
     * keystroke. If the query sanitizes down to nothing FTS-searchable (e.g.
     * pure punctuation), it falls back to scoring everything, same as below
     * the threshold.
     */
    suspend fun search(query: String, snippets: List<Snippet>): List<Snippet> {
        if (query.isBlank() || snippets.isEmpty()) return snippets

        val candidates = if (snippets.size > SnipConstants.FTS_PREFILTER_THRESHOLD) {
            val matchQuery = buildFtsMatchQuery(query)
            if (matchQuery == null) {
                snippets
            } else {
                val matchedIds = dao.ftsMatchSnippetIds(matchQuery).toSet()
                snippets.filter { it.id in matchedIds }
            }
        } else {
            snippets
        }

        return FuzzySearch.rank(query, candidates)
    }

    /**
     * Turns free-typed search text into a safe FTS4 MATCH expression: strips
     * everything but letters/digits (sidesteps FTS's quote/NEAR/column-filter
     * syntax entirely -- no risk of a stray `"` throwing a SQLiteException),
     * drops FTS's boolean keywords if someone literally typed "and"/"or"/etc.
     * as a search word, and adds a trailing `*` per token for prefix matching.
     * Returns null if nothing searchable is left after sanitizing.
     */
    private fun buildFtsMatchQuery(raw: String): String? {
        val reservedWords = setOf("and", "or", "not", "near")
        val tokens = raw.lowercase()
            .map { c -> if (c.isLetterOrDigit()) c else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotBlank() && it !in reservedWords }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }

    /** Pulls remote snippets (no-op until SyncGateway has a real backend) and merges them in. */
    suspend fun syncPull() {
        val remote = syncGateway.pull()
        if (remote.isNotEmpty()) {
            remote.forEach { dao.upsertWithFts(it.toEntity(), it.toFtsEntity()) }
        }
    }
}

