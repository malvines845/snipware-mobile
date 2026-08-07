package com.snipware.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.snipware.app.ui.home.LANGUAGE_FILTER_ALL
import com.snipware.app.ui.theme.SnipAccent
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.theme.SnipBorder
import com.snipware.app.ui.theme.SnipSurface2
import com.snipware.app.ui.theme.SnipText
import com.snipware.app.util.LangUtils

@Composable
fun LanguageFilterRow(
    languages: List<String>,
    activeFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = listOf(LANGUAGE_FILTER_ALL) + languages

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(chips) { lang ->
            FilterChipItem(
                label = lang,
                selected = lang == activeFilter,
                onClick = { onFilterSelected(lang) }
            )
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) SnipAccent else SnipSurface2)
            .border(1.dp, if (selected) SnipAccent else SnipBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (label != LANGUAGE_FILTER_ALL) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(LangUtils.colorFor(label))
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) SnipBg else SnipText
        )
    }
}
