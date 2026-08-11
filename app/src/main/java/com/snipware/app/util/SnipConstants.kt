package com.snipware.app.util

/** Ported 1:1 from constants.js so card behavior matches the web app exactly. */
object SnipConstants {
    /** Lines shown before a card's code preview truncates with "N more lines · view full". */
    const val COLLAPSE_AT = 7

    /** Tags that render with the red "warning" tag style when the snippet is also messy. */
    val WARN_TAGS: Set<String> = setOf(
        "wip", "broken", "fix-later", "do-not-use", "TODO", "urgent", "URGENT"
    )
}
