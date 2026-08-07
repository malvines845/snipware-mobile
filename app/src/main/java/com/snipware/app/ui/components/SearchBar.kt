package com.snipware.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.snipware.app.ui.theme.SnipAccent
import com.snipware.app.ui.theme.SnipBorder
import com.snipware.app.ui.theme.SnipSurface2
import com.snipware.app.ui.theme.SnipText
import com.snipware.app.ui.theme.SnipTextDim

@Composable
fun SnipSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search snippets..."
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = SnipTextDim) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SnipTextDim) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = SnipTextDim)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        textStyle = TextStyle(color = SnipText),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SnipSurface2,
            unfocusedContainerColor = SnipSurface2,
            focusedBorderColor = SnipAccent,
            unfocusedBorderColor = SnipBorder,
            cursorColor = SnipAccent
        )
    )
}
