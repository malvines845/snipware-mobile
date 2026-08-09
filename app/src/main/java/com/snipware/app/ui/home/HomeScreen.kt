package com.snipware.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.snipware.app.data.model.Snippet
import com.snipware.app.ui.components.LanguageFilterRow
import com.snipware.app.ui.components.PlaceholderFillDialog
import com.snipware.app.ui.components.SnipSearchBar
import com.snipware.app.ui.components.SnippetCard
import com.snipware.app.ui.theme.SnipAccent
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.theme.SnipTextDim
import com.snipware.app.ui.theme.SnipTextMid
import com.snipware.app.util.PlaceholderUtils

/**
 * onViewFull/onEdit are the only two callbacks that need to bubble up to
 * the NavHost (both navigate elsewhere); copying and pin-toggling are
 * self-contained here since they're just clipboard + ViewModel calls that
 * don't leave this screen -- same split CodeViewerScreen uses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onViewFull: (Snippet) -> Unit,
    onEdit: (Snippet) -> Unit,
    onAddClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var placeholderTarget by remember { mutableStateOf<Snippet?>(null) }

    fun copyText(value: String, snippet: Snippet) {
        clipboard.setText(AnnotatedString(value))
        viewModel.registerCopy(snippet)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun onCardCopy(snippet: Snippet) {
        if (PlaceholderUtils.extract(snippet.code).isNotEmpty()) {
            placeholderTarget = snippet
        } else {
            copyText(snippet.code, snippet)
        }
    }

    Scaffold(
        containerColor = SnipBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Snipware", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${state.totalCount} snippets · ${state.favoriteCount} pinned",
                            style = MaterialTheme.typography.labelSmall,
                            color = SnipTextMid
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SnipBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = SnipAccent) {
                Icon(Icons.Filled.Add, contentDescription = "Add snippet", tint = SnipBg)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SnipBg)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            SnipSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LanguageFilterRow(
                languages = state.allLanguagesInUse,
                activeFilter = state.activeFilter,
                onFilterSelected = viewModel::onFilterChange,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (state.snippets.isEmpty()) {
                EmptyState(hasQuery = state.query.isNotBlank())
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(state.snippets, key = { it.id }) { snippet ->
                        SnippetCard(
                            snippet = snippet,
                            onCopy = { onCardCopy(snippet) },
                            onEdit = { onEdit(snippet) },
                            onViewFull = { onViewFull(snippet) },
                            onLongPress = { viewModel.toggleFavorite(snippet) }
                        )
                    }
                }
            }
        }
    }

    placeholderTarget?.let { snippet ->
        PlaceholderFillDialog(
            placeholderNames = PlaceholderUtils.extract(snippet.code),
            onDismiss = { placeholderTarget = null },
            onConfirm = { values ->
                val resolved = PlaceholderUtils.resolve(snippet.code, values)
                placeholderTarget = null
                copyText(resolved, snippet)
            }
        )
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = SnipTextDim,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = if (hasQuery) "No snippets match your search" else "No snippets yet",
                style = MaterialTheme.typography.bodyLarge,
                color = SnipTextMid,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (!hasQuery) {
                Text(
                    text = "Tap + to save your first one",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnipTextDim,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
