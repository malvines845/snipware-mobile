package com.snipware.app.ui.codeeditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula

/**
 * Wraps Sora-Editor's [CodeEditor] (real syntax-highlighted code widget,
 * replacing the CodeMirror instance from pierce.html) for use in Compose.
 *
 * Design note: while mounted, the live [CodeEditor] instance -- handed back
 * via [onEditorReady] -- is treated as the source of truth for its own text,
 * rather than [text] being pushed in on every recomposition. Fighting the
 * editor's own cursor/selection state with setText() on each keystroke would
 * make typing unusable. Callers (see CodeViewerScreen) read
 * `editor.text.toString()` when they actually need the current value (e.g.
 * on Save), and can call `editor.setText(...)` themselves for one-off resets
 * (e.g. on Cancel).
 */
@Composable
fun SoraCodeEditor(
    text: String,
    language: String,
    editable: Boolean,
    modifier: Modifier = Modifier,
    wordWrap: Boolean = true,
    onEditorReady: (CodeEditor) -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { TextMateSetup.ensureInitialized(context) }

    var lastSetLanguage by remember { mutableStateOf<String?>(null) }
    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            CodeEditor(ctx).apply {
                colorScheme = SchemeDarcula()
                setText(text)
                isEditable = editable
                isWordwrap = wordWrap
                TextMateSetup.languageFor(language)?.let { setEditorLanguage(it) }
                lastSetLanguage = language
                editorRef = this
                onEditorReady(this)
            }
        },
        update = { editor ->
            editor.isEditable = editable
            editor.isWordwrap = wordWrap
            if (language != lastSetLanguage) {
                TextMateSetup.languageFor(language)?.let { editor.setEditorLanguage(it) }
                lastSetLanguage = language
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose { editorRef?.release() }
    }
}
