package com.snipware.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.snipware.app.data.model.Snippet
import com.snipware.app.data.model.SyncStatus

/**
 * Room table replacing the `snippets` IndexedDB object store from db.js.
 * Kept as a plain, flat table -- no relations needed for this app.
 */
@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey val id: String,
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
    /** "pending" | "synced" -- stored as raw string so a future sync engine
     *  can add states without a schema migration; mapped to [SyncStatus] at
     *  the repository boundary. */
    val syncStatus: String = "pending"
)

fun SnippetEntity.toDomain(): Snippet = Snippet(
    id = id,
    title = title,
    code = code,
    language = language,
    tags = tags,
    isFavorite = isFavorite,
    isLocked = isLocked,
    messy = messy,
    copyCount = copyCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = if (syncStatus == "synced") SyncStatus.SYNCED else SyncStatus.PENDING
)

fun Snippet.toEntity(): SnippetEntity = SnippetEntity(
    id = id,
    title = title,
    code = code,
    language = language,
    tags = tags,
    isFavorite = isFavorite,
    isLocked = isLocked,
    messy = messy,
    copyCount = copyCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = if (syncStatus == SyncStatus.SYNCED) "synced" else "pending"
)
