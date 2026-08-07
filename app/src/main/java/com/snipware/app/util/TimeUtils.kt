package com.snipware.app.util

/** Port of timeAgo() in utils.js: human-readable relative time. */
object TimeUtils {
    fun timeAgo(epochMillis: Long): String {
        val diff = System.currentTimeMillis() - epochMillis
        val mins = diff / 60_000
        if (mins < 1) return "just now"
        if (mins < 60) return "${mins}m ago"
        val hours = diff / 3_600_000
        if (hours < 24) return "${hours}h ago"
        val days = diff / 86_400_000
        return if (days == 1L) "yesterday" else "${days}d ago"
    }
}
