package com.snipware.app.data.search

import com.snipware.app.data.model.Snippet

/**
 * JNI bridge to search_engine.cpp. Holds a native handle (an in-memory
 * n-gram index + document store living in C++) across the app's lifetime.
 *
 * This is an enforced singleton -- the constructor is private and the only
 * way to get an instance is [getInstance] -- specifically so a second
 * instance can't get created by mistake (e.g. inside a ViewModel instead
 * of AppContainer). A SearchEngine scoped to something shorter-lived than
 * the process (an Activity, a ViewModel) would leak its native handle: it
 * has no Kotlin finalizer wired up, nothing calls [close] automatically
 * when a ViewModel's onCleared() runs, and the whole point of holding a
 * persistent handle is that it's *supposed* to outlive individual screens.
 * One process-lifetime instance, owned by AppContainer, sidesteps the
 * question entirely instead of relying on every call site remembering to
 * clean up correctly.
 */
class SearchEngine private constructor() {

    companion object Factory {
        init {
            System.loadLibrary("fuzzysearch")
        }

        @Volatile
        private var instance: SearchEngine? = null

        fun getInstance(): SearchEngine =
            instance ?: synchronized(this) {
                instance ?: SearchEngine().also { instance = it }
            }
    }

    private val handle: Long = nativeCreate()

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeUpsert(
        handle: Long,
        id: String,
        title: String,
        tags: String,
        language: String,
        code: String
    )
    private external fun nativeRemove(handle: Long, id: String)
    private external fun nativeClear(handle: Long)
    private external fun nativeSearch(handle: Long, query: String): Array<String>

    fun upsert(snippet: Snippet) {
        nativeUpsert(handle, snippet.id, snippet.title, snippet.tags, snippet.language, snippet.code)
    }

    fun remove(id: String) {
        nativeRemove(handle, id)
    }

    /** Wipes and re-indexes everything -- used once at startup to hydrate from Room. */
    fun rebuildAll(snippets: List<Snippet>) {
        nativeClear(handle)
        snippets.forEach { upsert(it) }
    }

    /** Ranked snippet IDs (descending relevance, score > 0 only); empty list if nothing matches. */
    fun search(query: String): List<String> = nativeSearch(handle, query).toList()

    /**
     * Releases the native index. Not called anywhere in normal app
     * operation -- see the class doc for why this is a process-lifetime
     * singleton that's never meant to be torn down early. Exposed for
     * tests that want their own short-lived instance to clean up after
     * itself (tests should not use [getInstance] for this reason -- that
     * shared instance closing would break every other test).
     */
    fun close() {
        nativeDestroy(handle)
    }
}
