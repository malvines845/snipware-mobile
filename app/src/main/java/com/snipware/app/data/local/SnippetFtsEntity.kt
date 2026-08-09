package com.snipware.app.data.local

import androidx.room.Entity
import androidx.room.Fts4
import com.snipware.app.data.model.Snippet

/**
 * FTS4 index over title/tags/language/code, used as a fast candidate
 * pre-filter before native fuzzy scoring on large libraries (see
 * SnippetRepository.search() and SnipConstants.FTS_PREFILTER_THRESHOLD).
 *
 * This is deliberately a STANDALONE Fts4 table (its own columns, manually
 * kept in sync in SnippetDao's upsertWithFts/deleteWithFts) rather than an
 * external-content table (`@Fts4(contentEntity = SnippetEntity::class)`).
 * External-content FTS in Room aliases SQLite's integer `rowid` to the
 * content entity's primary key, which requires that key to be an
 * Int/Long -- but Snippet.id is a String (UUID), chosen so IDs never
 * collide across devices before sync reconciliation. Duplicating the
 * searchable text into its own shadow table avoids that conflict at the
 * cost of a small amount of extra storage.
 *
 * No explicit `rowid` PrimaryKey field is declared here -- per Room's own
 * docs, it's optional on Fts4 entities and SQLite provides it implicitly;
 * declaring it is only needed if you want to reference it directly in a
 * query, which nothing here does (everything looks up by [snippetId]).
 */
@Fts4
@Entity(tableName = "snippets_fts")
data class SnippetFtsEntity(
    /** Back-reference to Snippet.id. Not unique-constrained at the DB level --
     *  SnippetDao always deletes any existing row for an id before inserting
     *  a fresh one, so duplicates never accumulate in practice. */
    val snippetId: String,
    val title: String,
    val tags: String,
    val language: String,
    val code: String
)

fun Snippet.toFtsEntity(): SnippetFtsEntity = SnippetFtsEntity(
    snippetId = id,
    title = title,
    tags = tags,
    language = language,
    code = code
)

