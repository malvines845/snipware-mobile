package com.snipware.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snipware.app.data.model.Snippet
import com.snipware.app.ui.theme.CodeFontFamily
import com.snipware.app.ui.theme.SnipAccentDim
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.theme.SnipBorder
import com.snipware.app.ui.theme.SnipDanger
import com.snipware.app.ui.theme.SnipLockBlue
import com.snipware.app.ui.theme.SnipMessyBorder
import com.snipware.app.ui.theme.SnipMessyTitle
import com.snipware.app.ui.theme.SnipPreviewText
import com.snipware.app.ui.theme.SnipSurface
import com.snipware.app.ui.theme.SnipSurface2
import com.snipware.app.ui.theme.SnipSurface3
import com.snipware.app.ui.theme.SnipText
import com.snipware.app.ui.theme.SnipTextDim
import com.snipware.app.ui.theme.SnipTextMid
import com.snipware.app.util.LangUtils
import com.snipware.app.util.PlaceholderUtils
import com.snipware.app.util.SnipConstants
import com.snipware.app.util.TimeUtils

/**
 * Mirrors the original .card / .card-hdr / .cpreview / .cfoot markup in
 * ui_render.js as closely as Compose allows: colored top strip (language,
 * or danger when messy-and-unpinned), lbadge/phbadge/lock-badge row, dark
 * monospace code preview with a bottom fade + 7-line collapse, tag chips,
 * and a footer separated by a rule.
 *
 * Interaction matches the original too: tapping the card body (not an
 * icon) copies the snippet -- same "grab code fast" behavior as the web
 * app's card click handler -- while the edit icon opens the editor and
 * "N more lines - view full" (shown only past the collapse threshold)
 * opens the full viewer. Long-press toggles pinned, standing in for
 * whatever desktop-oriented affordance the original used there.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SnippetCard(
    snippet: Snippet,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onViewFull: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val langColor = LangUtils.colorFor(snippet.language)
    val lineCount = snippet.code.count { it == '\n' } + 1
    val isDangerAccent = snippet.messy && !snippet.isFavorite
    val topStripColor = if (isDangerAccent) SnipDanger else langColor
    val borderColor = when {
        isDangerAccent -> SnipMessyBorder
        snippet.isLocked -> SnipLockBlue.copy(alpha = 0.35f)
        else -> SnipBorder
    }
    val placeholderCount = PlaceholderUtils.extract(snippet.code).size
    val tags = snippet.tagList
    val cardShape = RoundedCornerShape(10.dp)
    val truncated = lineCount > SnipConstants.COLLAPSE_AT

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(SnipSurface)
            .border(1.dp, borderColor, cardShape)
            .combinedClickable(onClick = onCopy, onLongClick = onLongPress)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(topStripColor)
        )

        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            // card-hdr: title + edit/copy icon actions
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = snippet.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (snippet.messy) FontWeight.SemiBold else FontWeight.Bold,
                    fontSize = if (snippet.messy) 13.5.sp else 14.sp,
                    color = if (snippet.messy) SnipMessyTitle else SnipText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    CardIconButton(icon = Icons.Filled.Edit, contentDescription = "Edit", onClick = onEdit)
                    CardIconButton(icon = Icons.Outlined.ContentCopy, contentDescription = "Copy code", onClick = onCopy)
                }
            }

            // Badge row: language, smart-copy placeholders, locked
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LangBadge(language = snippet.language, color = langColor)
                if (placeholderCount > 0) PlaceholderBadge(count = placeholderCount)
                if (snippet.isLocked) LockedBadge()
            }

            CodePreview(code = snippet.code, isLocked = snippet.isLocked)

            if (truncated) {
                ViewFullButton(
                    moreLines = lineCount - SnipConstants.COLLAPSE_AT,
                    onClick = onViewFull
                )
            }

            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.forEach { tag ->
                        TagChip(tag = tag, isWarning = snippet.messy && SnipConstants.WARN_TAGS.contains(tag))
                    }
                }
            }

            CardFooter(lineCount = lineCount, createdAt = snippet.createdAt)
        }
    }
}

@Composable
private fun CardIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = SnipTextMid, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LangBadge(language: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SnipSurface3)
            .border(1.dp, SnipBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
        Text(language, fontFamily = CodeFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = SnipTextMid)
    }
}

@Composable
private fun PlaceholderBadge(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SnipAccentDim.copy(alpha = 0.07f))
            .border(1.dp, SnipAccentDim.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Icon(Icons.Filled.Bolt, contentDescription = null, tint = SnipAccentDim, modifier = Modifier.size(9.dp))
        Text("smart · $count ph", fontFamily = CodeFontFamily, fontSize = 10.sp, color = SnipAccentDim)
    }
}

@Composable
private fun LockedBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(SnipLockBlue.copy(alpha = 0.08f))
            .border(1.dp, SnipLockBlue.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = SnipLockBlue, modifier = Modifier.size(9.dp))
        Text("hidden from assistant", fontFamily = CodeFontFamily, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = SnipLockBlue)
    }
}

@Composable
private fun CodePreview(code: String, isLocked: Boolean) {
    val previewShape = RoundedCornerShape(6.dp)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(previewShape)
            .background(SnipBg)
            .border(1.dp, SnipBorder, previewShape)
    ) {
        Text(
            text = code,
            fontFamily = CodeFontFamily,
            fontSize = 11.sp,
            lineHeight = 18.sp,
            // Locked snippets stay unreadable at a glance without relying on
            // Modifier.blur() (real gaussian blur needs API 31 on some Compose
            // versions; this minSdk targets 26), so we fade the text instead.
            color = if (isLocked) SnipPreviewText.copy(alpha = 0.12f) else SnipPreviewText,
            maxLines = SnipConstants.COLLAPSE_AT,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 9.dp, horizontal = 11.dp)
        )
        // Bottom fade -- matches .cpreview::after's linear-gradient(transparent, var(--bg)).
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SnipBg.copy(alpha = 0f), SnipBg),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        if (isLocked) {
            Box(modifier = Modifier.fillMaxSize().background(SnipBg.copy(alpha = 0.55f)))
        }
    }
}

@Composable
private fun ViewFullButton(moreLines: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = SnipTextDim, modifier = Modifier.size(13.dp))
        Text(
            text = "$moreLines more line${if (moreLines != 1) "s" else ""} · view full",
            fontFamily = CodeFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = SnipTextDim
        )
    }
}

@Composable
private fun TagChip(tag: String, isWarning: Boolean) {
    val shape = RoundedCornerShape(99.dp)
    Text(
        text = "#$tag",
        fontFamily = CodeFontFamily,
        fontSize = 10.sp,
        color = if (isWarning) Color(0xFFE06060) else SnipTextDim,
        modifier = Modifier
            .clip(shape)
            .background(if (isWarning) SnipDanger.copy(alpha = 0.08f) else SnipSurface2)
            .border(1.dp, if (isWarning) SnipDanger.copy(alpha = 0.3f) else SnipBorder, shape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun CardFooter(lineCount: Int, createdAt: Long) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SnipBorder))
        Spacer(modifier = Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(TimeUtils.timeAgo(createdAt), fontFamily = CodeFontFamily, fontSize = 10.sp, color = SnipTextDim)
            Text(
                "$lineCount line${if (lineCount != 1) "s" else ""}",
                fontFamily = CodeFontFamily,
                fontSize = 10.sp,
                color = SnipTextDim
            )
        }
    }
}
