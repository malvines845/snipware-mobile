package com.snipware.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snipware.app.ui.theme.CodeFontFamily
import com.snipware.app.ui.theme.SnipAccentDim
import com.snipware.app.ui.theme.SnipDanger
import com.snipware.app.ui.theme.SnipSearchBg
import com.snipware.app.ui.theme.SnipSearchBgFocus
import com.snipware.app.ui.theme.SnipSearchBorder
import com.snipware.app.ui.theme.SnipSearchPlaceholder
import com.snipware.app.ui.theme.SnipText
import com.snipware.app.ui.theme.SnipTextMid

/**
 * Matches #searchBar / .search-icon-btn exactly: monospace input text, a
 * near-black input background distinct from the surrounding surface, and
 * ONE leading icon that swaps between a search glyph and a clear glyph
 * (turning red once there's text) rather than separate leading+trailing icons.
 */
@Composable
fun SnipSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search snippets..."
) {
    var isFocused by remember { mutableStateOf(false) }
    val hasValue = query.isNotEmpty()

    val iconTint = when {
        hasValue -> SnipDanger
        isFocused -> SnipAccentDim
        else -> SnipTextMid
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .onFocusChanged { isFocused = it.isFocused },
        placeholder = { Text(placeholder, color = SnipSearchPlaceholder, fontFamily = CodeFontFamily, fontSize = 13.sp) },
        leadingIcon = {
            IconButton(onClick = { if (hasValue) onQueryChange("") }) {
                Icon(
                    if (hasValue) Icons.Filled.Clear else Icons.Filled.Search,
                    contentDescription = if (hasValue) "Clear" else null,
                    tint = iconTint
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(6.dp),
        textStyle = TextStyle(color = SnipText, fontFamily = CodeFontFamily, fontSize = 13.sp),
        keyboardOptions = KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SnipSearchBgFocus,
            unfocusedContainerColor = SnipSearchBg,
            focusedBorderColor = SnipAccentDim,
            unfocusedBorderColor = SnipSearchBorder,
            cursorColor = SnipAccentDim
        )
    )
}
