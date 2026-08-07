package com.snipware.app.util

import androidx.compose.ui.graphics.Color

/**
 * Per-language accent colors + the heuristic language auto-detector, ported
 * from constants.js (LC map) and utils.js SECTION 11 (detectLang()).
 */
object LangUtils {

    val LANGUAGES: List<String> = listOf(
        "JavaScript", "TypeScript", "Python", "Kotlin", "Java", "Dart", "SQL",
        "HTML", "CSS", "Bash", "Go", "Rust", "C++", "PHP", "Swift", "Other"
    )

    private val LANGUAGE_COLORS: Map<String, Color> = mapOf(
        "JavaScript" to Color(0xFFF7DF1E),
        "TypeScript" to Color(0xFF3178C6),
        "Python" to Color(0xFF3776AB),
        "Kotlin" to Color(0xFF7F52FF),
        "Java" to Color(0xFFED8B00),
        "Dart" to Color(0xFF0175C2),
        "SQL" to Color(0xFF336791),
        "HTML" to Color(0xFFE34F26),
        "CSS" to Color(0xFF1572B6),
        "Bash" to Color(0xFF4EAA25),
        "Go" to Color(0xFF00ACD7),
        "Rust" to Color(0xFFCE422B),
        "C++" to Color(0xFF00599C),
        "PHP" to Color(0xFF777BB4),
        "Swift" to Color(0xFFFA7343),
        "Other" to Color(0xFF6B6B70)
    )

    fun colorFor(language: String): Color = LANGUAGE_COLORS[language] ?: LANGUAGE_COLORS.getValue("Other")

    /**
     * Lightweight heuristic language auto-detector -- ported 1:1 from
     * detectLang() in utils.js. Used to pre-fill the language field when
     * pasting code into the editor screen.
     */
    fun detect(code: String): String? {
        if (code.isBlank()) return null
        val l = code.lowercase()

        fun score(vararg checks: Pair<Boolean, Int>): Int =
            checks.sumOf { (matched, weight) -> if (matched) weight else 0 }

        val htmlScore = score(
            l.contains("<!doctype") to 5,
            l.contains("<html") to 4,
            l.contains("</html>") to 4,
            (l.contains("<head>") || l.contains("<head ")) to 3,
            (l.contains("<body>") || l.contains("<body ")) to 3,
            (l.contains("<div") || l.contains("<span") || l.contains("<p>")) to 2,
            (l.contains("<script") || l.contains("<style")) to 2,
            (l.contains("<meta ") || l.contains("<link ")) to 2
        )
        if (htmlScore >= 4) return "HTML"

        val cssScore = score(
            l.contains("@media") to 3,
            l.contains("display:") to 2,
            l.contains("font-size:") to 2,
            (l.contains("margin:") || l.contains("padding:")) to 2,
            (l.contains("border:") || l.contains("background:")) to 2,
            l.contains(":root") to 3,
            l.contains("color:") to 1,
            (l.contains("<div") || l.contains("<html") || l.contains("<body")) to -5
        )

        val results = listOf(
            "Python" to score(
                code.contains("def ") to 3, code.contains("elif ") to 3,
                l.contains("print(") to 2, l.contains("self.") to 2, l.contains("__init__") to 3
            ),
            "Kotlin" to score(
                code.contains("fun ") to 3, (code.contains("val ") || code.contains("var ")) to 2,
                l.contains("data class") to 3, l.contains("companion object") to 3
            ),
            "Java" to score(
                l.contains("public class") to 3, l.contains("system.out.println") to 3,
                code.contains("@Override") to 3
            ),
            "Swift" to score(
                code.contains("func ") to 3, (l.contains("var ") || l.contains("let ")) to 2,
                l.contains("guard ") to 3, l.contains("struct ") to 3, l.contains(" -> ") to 2
            ),
            "TypeScript" to score(
                l.contains("interface ") to 3,
                (l.contains(": string") || l.contains(": number")) to 3,
                l.contains("type ") to 2
            ),
            "JavaScript" to score(
                (code.contains("const ") || code.contains("let ")) to 2, code.contains("=>") to 2,
                l.contains("fetch(") to 3, l.contains("console.log") to 2
            ),
            "SQL" to score(
                l.contains("select ") to 2, l.contains(" from ") to 2,
                l.contains("create table") to 3, l.contains("insert into") to 3
            ),
            "CSS" to cssScore,
            "HTML" to htmlScore,
            "Bash" to score(
                code.startsWith("#!/bin/bash") to 5, l.contains("echo ") to 2, code.startsWith("#!/") to 3
            ),
            "Rust" to score(
                code.contains("fn ") to 3, l.contains("let mut") to 3,
                l.contains("impl ") to 3, l.contains("::") to 2
            ),
            "Go" to score(
                code.contains("func ") to 3, l.contains("package ") to 3, l.contains(":=") to 3
            ),
            "PHP" to score(
                l.contains("<?php") to 5, l.contains("$") to 2, l.contains("echo ") to 2
            )
        )

        val best = results.maxByOrNull { it.second } ?: return null
        return if (best.second >= 3) best.first else null
    }
}
