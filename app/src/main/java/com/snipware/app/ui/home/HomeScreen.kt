package com.snipware.app.ui.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snipware.app.data.model.Snippet
import com.snipware.app.ui.components.LanguageFilterRow
import com.snipware.app.ui.components.SnipSearchBar
import com.snipware.app.ui.components.SnippetCard
import com.snipware.app.ui.theme.SnipAccent
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.theme.SnipTextDim
import com.snipware.app.ui.theme.SnipTextMid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSnippetClick: (Snippet) -> Unit,
    onAddClick: () -> Unit,
    onCopy: (Snippet) -> Unit,
    onLongPressSnippet: (Snippet) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

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
                            onClick = { onSnippetClick(snippet) },
                            onCopy = { onCopy(snippet) },
                            onToggleFavorite = { viewModel.toggleFavorite(snippet) },
                            onLongPress = { onLongPressSnippet(snippet) }
                        )
                    }
                }
            }
        }
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
