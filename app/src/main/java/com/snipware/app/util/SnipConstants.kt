package com.snipware.app.util

/** Ported 1:1 from constants.js so card behavior matches the web app exactly. */
object SnipConstants {
    /** Lines shown before a card's code preview truncates with "N more lines · view full". */
    const val COLLAPSE_AT = 7

    /** Tags that render with the red "warning" tag style when the snippet is also messy. */
    val WARN_TAGS: Set<String> = setOf(
        "wip", "broken", "fix-later", "do-not-use", "TODO", "urgent", "URGENT"
    )

    /**
     * Library size above which search does an FTS4 pre-filter pass before
     * native fuzzy scoring, instead of scoring every snippet on every
     * keystroke. Below this, the full native scan runs directly -- it's
     * already fast at this scale, and it's the only path that gives
     * edit-distance typo tolerance (e.g. "phyton" -> "Python"), which a
     * plain FTS MATCH can't do. See SnippetRepository.search().
     */
    const val FTS_PREFILTER_THRESHOLD = 300
}
