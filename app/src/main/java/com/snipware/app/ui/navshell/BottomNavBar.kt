package com.snipware.app.ui.navshell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snipware.app.ui.theme.SnipAccent
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.theme.SnipBorder
import com.snipware.app.ui.theme.SnipSurface
import com.snipware.app.ui.theme.SnipText
import com.snipware.app.ui.theme.SnipTextDim

enum class BottomNavTab { ALL, PINNED, ASSISTANT, ACCOUNT }

@Composable
fun SnipBottomNav(
    currentTab: BottomNavTab?,
    onAllClick: () -> Unit,
    onPinnedClick: () -> Unit,
    onNewClick: () -> Unit,
    onAssistantClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SnipSurface)
            .border(width = 1.dp, color = SnipBorder)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(
            icon = Icons.Outlined.GridView,
            label = "All",
            selected = currentTab == BottomNavTab.ALL,
            onClick = onAllClick
        )
        NavItem(
            icon = Icons.Outlined.StarBorder,
            label = "Pinned",
            selected = currentTab == BottomNavTab.PINNED,
            onClick = onPinnedClick
        )
        NewPill(onClick = onNewClick)
        NavItem(
            icon = Icons.Outlined.HelpOutline,
            label = "Assistant",
            selected = currentTab == BottomNavTab.ASSISTANT,
            onClick = onAssistantClick
        )
        NavItem(
            icon = Icons.Outlined.Person,
            label = "Account",
            selected = currentTab == BottomNavTab.ACCOUNT,
            onClick = onAccountClick
        )
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) SnipText else SnipTextDim
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 10.sp, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NewPill(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(shape)
            .background(SnipAccent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = "New snippet", tint = SnipBg, modifier = Modifier.size(20.dp))
        Text("New", color = SnipBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
