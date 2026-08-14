package com.snipware.app.ui.viewer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.snipware.app.ui.codeeditor.SoraCodeEditor
import com.snipware.app.ui.components.PlaceholderFillDialog
import com.snipware.app.ui.theme.SnipAccent
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.theme.SnipDanger
import com.snipware.app.ui.theme.SnipPin
import com.snipware.app.ui.theme.SnipTextDim
import io.github.rosemoe.sora.widget.CodeEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeViewerScreen(
    viewModel: CodeViewerViewModel,
    snippetId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var editorHandle by remember { mutableStateOf<CodeEditor?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(snippetId) { viewModel.load(snippetId) }

    val snippet = state.snippet

    fun copyText(value: String) {
        clipboard.setText(AnnotatedString(value))
        viewModel.registerCopy()
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = SnipBg,
        topBar = {
            TopAppBar(
                title = { Text(snippet?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (snippet != null) {
                        if (state.isEditingCode) {
                            IconButton(onClick = {
                                editorHandle?.setText(snippet.code)
                                viewModel.exitCodeEdit()
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel edit")
                            }
                            IconButton(onClick = {
                                val newCode = editorHandle?.text?.toString() ?: snippet.code
                                viewModel.onEditedCodeChange(newCode)
                                viewModel.saveCodeEdit()
                            }) {
                                Icon(Icons.Filled.Check, contentDescription = "Save code", tint = SnipAccent)
                            }
                        } else {
                            IconButton(onClick = viewModel::toggleWordWrap) {
                                Icon(
                                    Icons.Filled.WrapText,
                                    contentDescription = "Toggle word wrap",
                                    tint = if (state.wordWrap) SnipAccent else SnipTextDim
                                )
                            }
                            IconButton(onClick = viewModel::toggleLocked) {
                                Icon(
                                    if (snippet.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                    contentDescription = "Toggle locked (hidden from assistant)",
                                    tint = SnipTextDim
                                )
                            }
                            IconButton(onClick = viewModel::toggleFavorite) {
                                Icon(
                                    if (snippet.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Toggle pinned",
                                    tint = if (snippet.isFavorite) SnipPin else SnipTextDim
                                )
                            }
                            IconButton(onClick = viewModel::enterCodeEdit) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit code")
                            }
                            IconButton(onClick = {
                                if (state.placeholderNames.isNotEmpty()) {
                                    viewModel.openPlaceholderDialog()
                                } else {
                                    copyText(snippet.code)
                                }
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = SnipAccent)
                            }
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = SnipDanger)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SnipBg)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SnipBg)
                .padding(padding)
                .navigationBarsPadding()
        ) {
            if (snippet != null) {
                SoraCodeEditor(
                    text = snippet.code,
                    language = snippet.language,
                    editable = state.isEditingCode,
                    wordWrap = state.wordWrap,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    onEditorReady = { editorHandle = it }
                )
            }
        }
    }

    if (state.showPlaceholderDialog) {
        PlaceholderFillDialog(
            placeholderNames = state.placeholderNames,
            onDismiss = viewModel::closePlaceholderDialog,
            onConfirm = { values ->
                val resolved = viewModel.resolvePlaceholders(values)
                viewModel.closePlaceholderDialog()
                copyText(resolved)
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete snippet?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDeleted)
                }) {
                    Text("Delete", color = SnipDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
