package com.snipware.app.data.search

import com.snipware.app.data.model.Snippet

/**
 * JNI bridge to fuzzy_search.cpp. Mirrors editDistance()/fuzzyScore()/
 * snippetScore() from the original web app's utils.js, just running as
 * native code so ranking a large snippet library on every keystroke
 * stays instant.
 */
object FuzzySearch {

    init {
        System.loadLibrary("fuzzysearch")
    }

    /** Raw Levenshtein distance between [a] and [b]. Mostly useful for tests. */
    external fun nativeEditDistance(a: String, b: String): Int

    /**
     * Scores every snippet (by parallel field arrays) against [query] in a
     * single JNI call. Returns an IntArray of scores in the same order as
     * the input, so index i of the result corresponds to index i of the
     * input arrays.
     */
    external fun nativeScoreSnippets(
        query: String,
        titles: Array<String>,
        tags: Array<String>,
        languages: Array<String>,
        codes: Array<String>
    ): IntArray

    /**
     * Ranks [snippets] against [query], returning only matches (score > 0)
     * sorted by descending relevance. Returns [snippets] unchanged when
     * [query] is blank (nothing to rank against).
     */
    fun rank(query: String, snippets: List<Snippet>): List<Snippet> {
        if (query.isBlank() || snippets.isEmpty()) return snippets

        val titles = Array(snippets.size) { snippets[it].title }
        val tags = Array(snippets.size) { snippets[it].tags }
        val languages = Array(snippets.size) { snippets[it].language }
        val codes = Array(snippets.size) { snippets[it].code }

        val scores = nativeScoreSnippets(query, titles, tags, languages, codes)

        return snippets.indices
            .filter { scores[it] > 0 }
            .sortedByDescending { scores[it] }
            .map { snippets[it] }
    }
}
