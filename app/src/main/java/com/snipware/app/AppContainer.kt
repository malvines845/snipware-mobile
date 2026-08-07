package com.snipware.app

import android.content.Context
import com.snipware.app.data.local.SnipwareDatabase
import com.snipware.app.data.repository.SnippetRepository
import com.snipware.app.data.sync.NoOpSyncGateway
import com.snipware.app.data.sync.SyncGateway

/**
 * Tiny manual dependency container -- deliberately not using Hilt/Koin to
 * keep this "small project" port dependency-light. Swap [NoOpSyncGateway]
 * for a real implementation here once Supabase sync is wired up
 * (see data/sync/SyncGateway.kt); nothing else needs to change.
 */
class AppContainer(context: Context) {
    private val database = SnipwareDatabase.getInstance(context)

    val syncGateway: SyncGateway = NoOpSyncGateway()

    val snippetRepository = SnippetRepository(
        dao = database.snippetDao(),
        syncGateway = syncGateway
    )
}
