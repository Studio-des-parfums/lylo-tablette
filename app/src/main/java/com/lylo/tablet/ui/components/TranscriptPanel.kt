package com.lylo.tablet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.data.model.TranscriptMessage
import com.lylo.tablet.data.model.TranscriptSender
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloAccentTan
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.LyloTextDark
import com.lylo.tablet.ui.theme.glassPanel

/** Portage de src/components/livekit/TranscriptPanel.tsx. */
@Composable
fun TranscriptPanel(
    open: Boolean,
    transcripts: List<TranscriptMessage>,
    agentName: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStringResolver.current
    val listState = rememberLazyListState()

    LaunchedEffect(transcripts.size, open) {
        if (open && transcripts.isNotEmpty()) {
            listState.animateScrollToItem(transcripts.size - 1)
        }
    }

    AnimatedVisibility(
        visible = open,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .glassPanel(shape = androidx.compose.ui.graphics.RectangleShape),
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        strings.get(R.string.transcript_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = LyloPrimary,
                    )
                    IconButton(onClick = onToggle) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = LyloPrimary.copy(alpha = 0.6f))
                    }
                }

                if (transcripts.isEmpty()) {
                    Text(
                        strings.get(R.string.transcript_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = com.lylo.tablet.ui.theme.LyloPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(transcripts, key = { it.id }) { msg ->
                            TranscriptBubble(msg, agentName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptBubble(msg: TranscriptMessage, agentName: String) {
    val isUser = msg.sender == TranscriptSender.USER
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = if (isUser) 14.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 14.dp))
                .background(if (isUser) LyloAccentTan else LyloPrimary)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!isUser) {
                Text(
                    agentName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                )
            }
            Text(
                msg.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUser) LyloTextDark else androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}
