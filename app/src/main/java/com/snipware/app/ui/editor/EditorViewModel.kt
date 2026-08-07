package com.snipware.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snipware.app.data.model.Snippet
import com.snipware.app.data.repository.SnippetRepository
import com.snipware.app.util.LangUtils
import com.snipware.app.util.PlaceholderUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val editingId: String? = null,
    val title: String = "",
    val code: String = "",
    val language: String = "JavaScript",
    val tags: String = "",
    val languageManuallySet: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false
) {
    val isEditing: Boolean get() = editingId != null
    val placeholderNames: List<String> get() = PlaceholderUtils.extract(code)
    val hasPlaceholders: Boolean get() = placeholderNames.isNotEmpty()
    val canSave: Boolean get() = title.isNotBlank() && code.isNotBlank()
}

/** Backs the add/edit snippet screen -- equivalent of openAdd()/openEdit() in crud.js. */
class EditorViewModel(private val repository: SnippetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** Loads an existing snippet into the form (openEdit equivalent). No-op for new snippets. */
    fun loadForEdit(id: String) = viewModelScope.launch {
        val snippet = repository.getById(id) ?: return@launch
        _uiState.update {
            it.copy(
                editingId = snippet.id,
                title = snippet.title,
                code = snippet.code,
                language = snippet.language,
                tags = snippet.tags,
                languageManuallySet = true
            )
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    fun onCodeChange(value: String) = _uiState.update { current ->
        // Mirrors the original's auto-detect-on-paste UX: only suggest a
        // language while the user hasn't picked one manually themselves.
        val suggested = if (!current.languageManuallySet) LangUtils.detect(value) else null
        current.copy(
            code = value,
            language = suggested ?: current.language
        )
    }

    fun onLanguageChange(value: String) =
        _uiState.update { it.copy(language = value, languageManuallySet = true) }

    fun onTagsChange(value: String) = _uiState.update { it.copy(tags = value) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.canSave || state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val existing = state.editingId?.let { repository.getById(it) }
            val toSave = (existing ?: Snippet(title = "", code = "", language = "")).copy(
                id = existing?.id ?: state.editingId ?: java.util.UUID.randomUUID().toString(),
                title = state.title.trim(),
                code = state.code,
                language = state.language,
                tags = state.tags
            )
            repository.upsert(toSave)
            _uiState.update { it.copy(isSaving = false, saved = true) }
            onSaved()
        }
    }
}
