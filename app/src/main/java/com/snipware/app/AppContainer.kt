package com.snipware.app

import android.content.Context
import com.snipware.app.data.local.SnipwareDatabase
import com.snipware.app.data.repository.SnippetRepository
import com.snipware.app.data.search.SearchEngine
import com.snipware.app.data.sync.NoOpSyncGateway
import com.snipware.app.data.sync.SyncGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tiny manual dependency container -- deliberately not using Hilt/Koin to
 * keep this "small project" port dependency-light. Swap [NoOpSyncGateway]
 * for a real implementation here once Supabase sync is wired up
 * (see data/sync/SyncGateway.kt); nothing else needs to change.
 */
class AppContainer(context: Context) {
    private val database = SnipwareDatabase.getInstance(context)

    /** Process-lifetime singleton -- one native index, not one per screen/ViewModel. */
    private val searchEngine = SearchEngine.getInstance()

    val syncGateway: SyncGateway = NoOpSyncGateway()

    val snippetRepository = SnippetRepository(
        dao = database.snippetDao(),
        syncGateway = syncGateway,
        searchEngine = searchEngine
    )

    init {
        // Warm the native index up front so it's usually already hydrated by
        // the time the user reaches the search bar. Not required for
        // correctness -- SnippetRepository.search() calls
        // ensureSearchIndexHydrated() defensively too -- this just avoids
        // paying that cost on the first keystroke if it hasn't finished yet.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            snippetRepository.ensureSearchIndexHydrated()
        }
    }
}
