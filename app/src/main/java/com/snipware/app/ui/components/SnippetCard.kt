package com.snipware.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snipware.app.data.model.Snippet
import com.snipware.app.ui.theme.CodeTextStyle
import com.snipware.app.ui.theme.SnipAccent
import com.snipware.app.ui.theme.SnipBorder
import com.snipware.app.ui.theme.SnipDanger
import com.snipware.app.ui.theme.SnipPin
import com.snipware.app.ui.theme.SnipSurface
import com.snipware.app.ui.theme.SnipTextDim
import com.snipware.app.ui.theme.SnipTextMid
import com.snipware.app.util.LangUtils
import com.snipware.app.util.TimeUtils

/** Max lines of code shown in the collapsed preview -- mirrors previewH()'s intent in utils.js. */
private const val PREVIEW_MAX_LINES = 6

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SnippetCard(
    snippet: Snippet,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val langColor = LangUtils.colorFor(snippet.language)
    val lineCount = snippet.code.count { it == '\n' } + 1

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(containerColor = SnipSurface),
        border = BorderStroke(1.dp, SnipBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: language dot + name, badges, favorite toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(langColor)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = snippet.language,
                    style = MaterialTheme.typography.labelSmall,
                    color = SnipTextMid
                )

                if (snippet.messy) {
                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Messy",
                        tint = SnipDanger,
                        modifier = Modifier.size(12.dp)
                    )
                }
                if (snippet.isLocked) {
                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Locked (hidden from assistant)",
                        tint = SnipTextDim,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = if (snippet.isFavorite) "Unpin" else "Pin",
                        tint = if (snippet.isFavorite) SnipPin else SnipTextDim,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(6.dp))

            Text(
                text = snippet.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.size(8.dp))

            // Code preview -- syntax highlighting is reserved for the full
            // viewer (Sora-Editor); the list preview stays plain monospace
            // for scroll performance across a large snippet library.
            Text(
                text = snippet.code,
                style = CodeTextStyle,
                color = SnipTextMid,
                maxLines = PREVIEW_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$lineCount lines · ${TimeUtils.timeAgo(snippet.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SnipTextDim
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = SnipAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
