package com.snipware.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.snipware.app.data.repository.SnippetRepository
import com.snipware.app.ui.editor.EditorViewModel
import com.snipware.app.ui.home.HomeViewModel
import com.snipware.app.ui.viewer.CodeViewerViewModel

class ViewModelFactory(private val repository: SnippetRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(repository) as T
        modelClass.isAssignableFrom(EditorViewModel::class.java) ->
            EditorViewModel(repository) as T
        modelClass.isAssignableFrom(CodeViewerViewModel::class.java) ->
            CodeViewerViewModel(repository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
