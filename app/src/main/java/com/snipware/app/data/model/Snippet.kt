package com.snipware.app.data.model

import java.util.UUID

/**
 * Domain-level snippet model. Field-for-field equivalent of the `Snippet`
 * typedef in the original web app's db.js, with two intentional renames
 * for Kotlin conventions:
 *   - JS `tags` (comma-joined string) is kept as a String here too, so the
 *     native fuzzy-search bridge and the original scoring weights carry
 *     over unchanged. Use `tagList` for a parsed List<String> in the UI.
 *   - JS `created_at`/`createdAt` inconsistency is resolved to one field,
 *     `createdAt: Long` (epoch millis), matching how sync.js actually
 *     stores it locally.
 */
data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val code: String,
    val language: String,
    val tags: String = "",
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val messy: Boolean = false,
    val copyCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {
    /** Parsed view of the comma-joined [tags] string, for chips/UI display. */
    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

enum class SyncStatus { PENDING, SYNCED }
