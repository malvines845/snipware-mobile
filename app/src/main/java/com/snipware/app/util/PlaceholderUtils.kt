package com.snipware.app.util

/**
 * Port of SECTION 10 (PLACEHOLDER SYSTEM) from utils.js. Snippets can embed
 * `{{name}}` placeholders that get filled in at copy-time ("Smart Copy" in
 * the original UI) instead of hand-editing the snippet every time.
 */
object PlaceholderUtils {
    private val PLACEHOLDER_REGEX = Regex("\\{\\{([^}]+)\\}\\}")

    /** Extracts unique placeholder names from [code], in first-seen order. */
    fun extract(code: String): List<String> {
        val seen = LinkedHashSet<String>()
        for (match in PLACEHOLDER_REGEX.findAll(code)) {
            val name = match.groupValues[1].trim()
            if (name.isNotEmpty()) seen.add(name)
        }
        return seen.toList()
    }

    /** Replaces `{{name}}` in [code] with values from [values]; leaves unresolved ones as-is. */
    fun resolve(code: String, values: Map<String, String>): String =
        PLACEHOLDER_REGEX.replace(code) { match ->
            val rawName = match.groupValues[1]
            values[rawName.trim()] ?: "{{$rawName}}"
        }
}
