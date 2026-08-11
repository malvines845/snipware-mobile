package com.snipware.app.ui.navshell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.snipware.app.ui.theme.SnipBg
import com.snipware.app.ui.theme.SnipTextDim
import com.snipware.app.ui.theme.SnipTextMid

/**
 * Assistant (assistant.js's Gemini-backed feature) and Account (Supabase
 * auth) aren't ported yet -- see README.md. This keeps their bottom-nav
 * entries honest (present, tappable, clearly "not yet") rather than dead
 * buttons or missing entirely, which would look inconsistent with the
 * original's 5-tab nav.
 */
@Composable
fun ComingSoonScreen(icon: ImageVector, title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SnipBg)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = SnipTextDim, modifier = Modifier.size(40.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SnipTextMid,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = SnipTextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
