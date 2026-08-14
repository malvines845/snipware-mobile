package com.snipware.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.snipware.app.ui.theme.CodeTextStyle
import com.snipware.app.ui.theme.SnipAccent
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.theme.SnipBorder
import com.snipware.app.ui.theme.SnipSurface2
import com.snipware.app.ui.theme.SnipText
import com.snipware.app.ui.theme.SnipTextDim
import com.snipware.app.util.LangUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    editingId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(editingId) {
        if (editingId != null) viewModel.loadForEdit(editingId)
    }

    Scaffold(
        containerColor = SnipBg,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Snippet" else "New Snippet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp).height(20.dp),
                            color = SnipAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.save(onDone) },
                            enabled = state.canSave
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Save",
                                tint = if (state.canSave) SnipAccent else SnipTextDim
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SnipBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SnipBg)
                .padding(padding)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = snipFieldColors()
            )

            Text("Language", style = MaterialTheme.typography.labelSmall, color = SnipTextDim)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LangUtils.LANGUAGES) { lang ->
                    FilterChip(
                        selected = lang == state.language,
                        onClick = { viewModel.onLanguageChange(lang) },
                        label = { Text(lang) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SnipAccent,
                            selectedLabelColor = SnipBg,
                            containerColor = SnipSurface2,
                            labelColor = SnipText
                        )
                    )
                }
            }

            OutlinedTextField(
                value = state.tags,
                onValueChange = viewModel::onTagsChange,
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = snipFieldColors()
            )

            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Code") },
                textStyle = CodeTextStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                colors = snipFieldColors()
            )

            if (state.hasPlaceholders) {
                Text(
                    text = "Placeholders detected: ${state.placeholderNames.joinToString(", ") { "{{$it}}" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnipAccent
                )
            }
        }
    }
}

@Composable
private fun snipFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SnipSurface2,
    unfocusedContainerColor = SnipSurface2,
    focusedBorderColor = SnipAccent,
    unfocusedBorderColor = SnipBorder,
    cursorColor = SnipAccent,
    focusedTextColor = SnipText,
    unfocusedTextColor = SnipText
)
