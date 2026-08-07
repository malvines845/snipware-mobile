package com.snipware.app.ui.codeeditor

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

/**
 * One-time setup for Sora-Editor's TextMate engine (real syntax highlighting,
 * same idea as the Prism.js grammars the original web app used).
 *
 * IMPORTANT -- this needs grammar assets this repo does NOT ship, because
 * they're binary/text assets pulled from the sora-editor project itself and
 * this port was generated without network access to fetch them. Before this
 * compiles usefully (it will still compile, just fall back to no
 * highlighting), copy the following from
 * https://github.com/Rosemoe/sora-editor -> app/src/main/assets/textmate/
 * into this project's app/src/main/assets/textmate/:
 *   - languages.json          (manifest mapping scopeName -> grammar file)
 *   - the .tmLanguage.json grammar files it references, for whichever of
 *     LangUtils.LANGUAGES you want highlighted (the demo app already
 *     bundles java/kotlin/python/js/ts/html/css/etc., which covers almost
 *     all of Snipware's language list out of the box)
 *   - a theme file (e.g. darcula.json) for ThemeRegistry below
 *
 * This is the single most library-version-sensitive file in the project --
 * if a class below has moved, check the current guide at
 * https://project-sora.github.io/sora-editor-docs/guide/using-language
 * and adjust just this file; nothing else depends on these exact APIs.
 */
object TextMateSetup {
    private var initialized = false

    private const val THEME_NAME = "snipware-dark"
    private const val THEME_ASSET_PATH = "textmate/snipware-dark.json"
    private const val LANGUAGES_MANIFEST = "textmate/languages.json"

    /** Maps Snipware's language names (LangUtils.LANGUAGES) to TextMate scope names. */
    private val SCOPE_NAMES: Map<String, String> = mapOf(
        "JavaScript" to "source.js",
        "TypeScript" to "source.ts",
        "Python" to "source.python",
        "Kotlin" to "source.kotlin",
        "Java" to "source.java",
        "Dart" to "source.dart",
        "SQL" to "source.sql",
        "HTML" to "text.html.basic",
        "CSS" to "source.css",
        "Bash" to "source.shell",
        "Go" to "source.go",
        "Rust" to "source.rust",
        "C++" to "source.cpp",
        "PHP" to "source.php",
        "Swift" to "source.swift"
    )

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized) return

        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(context.applicationContext.assets)
        )

        runCatching {
            GrammarRegistry.getInstance().loadGrammars(LANGUAGES_MANIFEST)
        }.onFailure {
            // Falls back to plain (unhighlighted) text if grammar assets
            // haven't been added yet -- see the class doc above.
        }

        runCatching {
            ThemeRegistry.getInstance().loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        context.assets.open(THEME_ASSET_PATH),
                        THEME_ASSET_PATH,
                        null
                    ),
                    THEME_NAME
                )
            )
            ThemeRegistry.getInstance().setTheme(THEME_NAME)
        }

        initialized = true
    }

    /** Builds a TextMateLanguage for [snipwareLanguage], or null if unmapped (falls back to plain text). */
    fun languageFor(snipwareLanguage: String): TextMateLanguage? {
        val scope = SCOPE_NAMES[snipwareLanguage] ?: return null
        return runCatching { TextMateLanguage.create(scope, true) }.getOrNull()
    }
}
