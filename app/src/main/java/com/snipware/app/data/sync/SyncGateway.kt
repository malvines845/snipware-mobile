package com.snipware.app.data.sync

import com.snipware.app.data.model.Snippet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over remote sync -- Supabase in the original web app
 * (see sync.js: SECTION 4 auth, SECTION 5 sync engine). Deliberately kept
 * out of [com.snipware.app.data.repository.SnippetRepository]'s internals
 * so swapping the no-op placeholder below for a real implementation never
 * touches the repository, ViewModels, or UI.
 *
 * TODO(sync): implement with supabase-kt (postgrest-kt + realtime-kt +
 * gotrue-kt: https://github.com/supabase-community/supabase-kt) once
 * auth/sync is wanted. Behavior to match from sync.js:
 *   - queue offline mutations (upsert/delete) in a local outbox, flush
 *     when [isOnline] flips true (SYNC.flush)
 *   - pull-on-login: fetch all rows for the current user, upsert locally
 *     (SYNC.pull)
 *   - realtime subscription on the `snippets` table, filtered by
 *     user_id, to mirror live edits from other devices (SYNC.startRealtime)
 *   - retry with a cap (original gives up after 5 retries per job)
 */
interface SyncGateway {
    val isOnline: StateFlow<Boolean>
    val currentUserId: StateFlow<String?>

    suspend fun queueUpsert(snippet: Snippet)
    suspend fun queueDelete(snippetId: String)

    /**
     * Fetches remote snippets to merge in locally (see
     * SnippetRepository.syncPull(), which writes them to Room and keeps the
     * native search index in sync).
     *
     * NOTE for the real implementation: this signature only communicates
     * additions/updates, not remote deletions. A real sync engine needs its
     * own way to learn "these IDs were deleted elsewhere" (e.g. tombstone
     * rows, a deleted-since cursor, or a realtime DELETE event) and must
     * route that through SnippetRepository.delete() -- same as any local
     * delete, so both Room and the search index stay consistent -- or a
     * snippet removed on another device will keep showing up in search here
     * forever even after syncPull() runs.
     */
    suspend fun pull(): List<Snippet>

    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun logout()
}

/**
 * Local-only placeholder. Every snippet just stays in Room with
 * syncStatus = PENDING forever -- functionally identical to the web app
 * running fully offline / logged out. Swap this out in
 * SnipwareApplication once real sync is implemented.
 */
class NoOpSyncGateway : SyncGateway {
    override val isOnline: StateFlow<Boolean> = MutableStateFlow(false)
    override val currentUserId: StateFlow<String?> = MutableStateFlow(null)

    override suspend fun queueUpsert(snippet: Snippet) {
        // no-op: local-only for now
    }

    override suspend fun queueDelete(snippetId: String) {
        // no-op: local-only for now
    }

    override suspend fun pull(): List<Snippet> = emptyList()

    override suspend fun login(email: String, password: String): Result<Unit> =
        Result.failure(NotImplementedError("Supabase sync isn't wired up yet — see SyncGateway.kt"))

    override suspend fun register(email: String, password: String): Result<Unit> =
        Result.failure(NotImplementedError("Supabase sync isn't wired up yet — see SyncGateway.kt"))

    override suspend fun logout() {
        // no-op
    }
}
