package com.snipware.app.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snipware.app.data.model.Snippet
import com.snipware.app.data.repository.SnippetRepository
import com.snipware.app.util.PlaceholderUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CodeViewerUiState(
    val snippet: Snippet? = null,
    val isEditingCode: Boolean = false,
    val editedCode: String = "",
    val wordWrap: Boolean = false,
    val showPlaceholderDialog: Boolean = false
) {
    val placeholderNames: List<String> get() = snippet?.let { PlaceholderUtils.extract(it.code) } ?: emptyList()
}

/** Backs the full-screen code viewer -- equivalent of openCodeView() + cvEnterEdit/cvExitEdit in crud.js. */
class CodeViewerViewModel(private val repository: SnippetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CodeViewerUiState())
    val uiState: StateFlow<CodeViewerUiState> = _uiState.asStateFlow()

    fun load(id: String) = viewModelScope.launch {
        val snippet = repository.getById(id)
        _uiState.update { it.copy(snippet = snippet) }
    }

    fun toggleWordWrap() = _uiState.update { it.copy(wordWrap = !it.wordWrap) }

    fun enterCodeEdit() = _uiState.update {
        it.copy(isEditingCode = true, editedCode = it.snippet?.code.orEmpty())
    }

    fun onEditedCodeChange(value: String) = _uiState.update { it.copy(editedCode = value) }

    fun exitCodeEdit() = _uiState.update { it.copy(isEditingCode = false) }

    fun saveCodeEdit() = viewModelScope.launch {
        val current = _uiState.value.snippet ?: return@launch
        val newCode = _uiState.value.editedCode
        if (newCode.isBlank()) return@launch
        val updated = current.copy(code = newCode)
        repository.upsert(updated)
        _uiState.update { it.copy(snippet = updated, isEditingCode = false) }
    }

    fun toggleFavorite() = viewModelScope.launch {
        val current = _uiState.value.snippet ?: return@launch
        repository.toggleFavorite(current)
        _uiState.update { it.copy(snippet = current.copy(isFavorite = !current.isFavorite)) }
    }

    fun toggleLocked() = viewModelScope.launch {
        val current = _uiState.value.snippet ?: return@launch
        repository.toggleLocked(current)
        _uiState.update { it.copy(snippet = current.copy(isLocked = !current.isLocked)) }
    }

    fun openPlaceholderDialog() = _uiState.update { it.copy(showPlaceholderDialog = true) }
    fun closePlaceholderDialog() = _uiState.update { it.copy(showPlaceholderDialog = false) }

    /** Resolves {{placeholders}} with [values] and returns the final text to copy/share. */
    fun resolvePlaceholders(values: Map<String, String>): String {
        val code = _uiState.value.snippet?.code.orEmpty()
        return PlaceholderUtils.resolve(code, values)
    }

    fun registerCopy() = viewModelScope.launch {
        _uiState.value.snippet?.let { repository.incrementCopyCount(it.id) }
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        _uiState.value.snippet?.let { repository.delete(it) }
        onDeleted()
    }
}
