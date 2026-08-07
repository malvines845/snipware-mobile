package com.snipware.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snipware.app.data.model.Snippet
import com.snipware.app.data.repository.SnippetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { NEWEST, OLDEST, AZ, ZA }

const val LANGUAGE_FILTER_ALL = "All"

data class HomeUiState(
    val snippets: List<Snippet> = emptyList(),
    val allLanguagesInUse: List<String> = emptyList(),
    val query: String = "",
    val activeFilter: String = LANGUAGE_FILTER_ALL,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val totalCount: Int = 0,
    val favoriteCount: Int = 0
)

/**
 * Drives the home/list screen. Mirrors the render()/renderSidebar() logic
 * in the original ui_render.js: filter by language, then either fuzzy-rank
 * by query (native) or sort by [SortOrder] when there's no active search.
 */
class HomeViewModel(private val repository: SnippetRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val activeFilter = MutableStateFlow(LANGUAGE_FILTER_ALL)
    private val sortOrder = MutableStateFlow(SortOrder.NEWEST)

    private val allSnippets = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val languagesInUse = repository.observeLanguagesInUse()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<HomeUiState> = combine(
        allSnippets, languagesInUse, query, activeFilter, sortOrder
    ) { all, languages, q, filter, sort ->
        var visible = if (filter == LANGUAGE_FILTER_ALL) all else all.filter { it.language == filter }

        visible = if (q.isBlank()) {
            when (sort) {
                SortOrder.NEWEST -> visible.sortedByDescending { it.createdAt }
                SortOrder.OLDEST -> visible.sortedBy { it.createdAt }
                SortOrder.AZ -> visible.sortedBy { it.title.lowercase() }
                SortOrder.ZA -> visible.sortedByDescending { it.title.lowercase() }
            }
        } else {
            // While actively searching, relevance ranking (native fuzzy score)
            // takes over from the manual sort order -- same behavior as the
            // original app's search bar.
            repository.search(q, visible)
        }

        HomeUiState(
            snippets = visible,
            allLanguagesInUse = languages,
            query = q,
            activeFilter = filter,
            sortOrder = sort,
            totalCount = all.size,
            favoriteCount = all.count { it.isFavorite }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onFilterChange(language: String) {
        activeFilter.value = language
    }

    fun onSortChange(order: SortOrder) {
        sortOrder.value = order
    }

    fun toggleFavorite(snippet: Snippet) = viewModelScope.launch {
        repository.toggleFavorite(snippet)
    }

    fun toggleLocked(snippet: Snippet) = viewModelScope.launch {
        repository.toggleLocked(snippet)
    }

    fun delete(snippet: Snippet) = viewModelScope.launch {
        repository.delete(snippet)
    }

    fun registerCopy(snippet: Snippet) = viewModelScope.launch {
        repository.incrementCopyCount(snippet.id)
    }
}
