package com.snipware.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.snipware.app.data.model.Snippet
import com.snipware.app.ui.components.PlaceholderFillDialog
import com.snipware.app.ui.components.SnipSearchBar
import com.snipware.app.ui.components.SnippetCard
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.theme.SnipText
import com.snipware.app.ui.theme.SnipTextDim
import com.snipware.app.ui.theme.SnipTextMid
import com.snipware.app.util.PlaceholderUtils

/**
 * Content only -- no Scaffold/TopAppBar/FAB of its own. Those live in the
 * shared nav shell (SnipwareNavHost) now, matching the original's bottom
 * nav bar (All/Pinned/New/Assistant/Account) instead of a top bar + FAB.
 * Language filtering is intentionally not shown here -- it's headed for a
 * sidebar later, not a chip row under the search bar.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    pinnedOnly: Boolean,
    onViewFull: (Snippet) -> Unit,
    onEdit: (Snippet) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var placeholderTarget by remember { mutableStateOf<Snippet?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val displaySnippets = if (pinnedOnly) state.snippets.filter { it.isFavorite } else state.snippets

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SnipBg)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            SnipSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(SnipTextDim)
            )

            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Sort options", tint = SnipTextMid)
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    listOf(
                        SortOrder.NEWEST to "Newest first",
                        SortOrder.OLDEST to "Oldest first",
                        SortOrder.AZ to "Title A-Z",
                        SortOrder.ZA to "Title Z-A"
                    ).forEach { (order, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = if (state.sortOrder == order) SnipText else SnipTextMid) },
                            onClick = {
                                viewModel.onSortChange(order)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (displaySnippets.isEmpty()) {
            EmptyState(hasQuery = state.query.isNotBlank(), pinnedOnly = pinnedOnly)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(displaySnippets, key = { it.id }) { snippet ->
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
private fun EmptyState(hasQuery: Boolean, pinnedOnly: Boolean) {
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
            val message = when {
                hasQuery -> "No snippets match your search"
                pinnedOnly -> "No pinned snippets yet"
                else -> "No snippets yet"
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = SnipTextMid,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (!hasQuery && !pinnedOnly) {
                Text(
                    text = "Tap New to save your first one",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnipTextDim,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
