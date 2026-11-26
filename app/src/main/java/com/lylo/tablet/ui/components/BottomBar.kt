package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.data.model.AgentState
import com.lylo.tablet.data.model.SessionState
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary

/**
 * Portage de src/components/livekit/BottomBar.tsx : barre de contrôle flottante
 * (stop/reprendre — statut — micro/conversation).
 * Les actions interrupt/resume/mic sont déléguées à l'appelant (écran) via callbacks, qui les
 * relaie à LiveKitSessionManager.
 *
 * Le TranscriptPanel n'est PLUS rendu ici (contrairement à une première version) : côté web c'est
 * un panneau `fixed top-0 right-0 h-full` qui coulisse sur TOUTE la hauteur de l'écran depuis la
 * droite, positionné par rapport au viewport entier — pas par rapport à la petite pilule de
 * contrôle. Le rendre à l'intérieur du Box de BottomBar le contraignait à la hauteur de cette
 * pilule (d'où le bug "le transcript s'affiche en bas"). L'état transcriptOpen est donc levé
 * (hoisted) vers l'écran appelant, qui doit rendre TranscriptPanel dans son propre Box racine
 * (fillMaxSize), en overlay par-dessus tout le reste — voir InteractionScreen.kt.
 */
@Composable
fun BottomBar(
    agentState: AgentState,
    sessionState: SessionState,
    agentName: String,
    transcriptOpen: Boolean,
    onTranscriptToggle: () -> Unit,
    onInterrupt: () -> Unit,
    onResumeListen: () -> Unit,
    onResume: () -> Unit,
    onMicToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    currentQuestionIndex: Int = 0,
) {
    val strings = LocalStringResolver.current
    var interrupted by remember { mutableStateOf(false) }
    // État réel de la track micro (partagé via LiveKitSessionManager) — l'icône reste synchronisée
    // même après un changement d'écran ou un mute forcé par l'interruption.
    val muted = LocalMicMuted.current
    var resumeAsked by remember { mutableStateOf(false) }

    // Retour en standby → le bouton « j'ai une question » est réaffiché.
    LaunchedEffect(sessionState) {
        if (sessionState == SessionState.STANDBY) resumeAsked = false
    }

    // Question suivante → micro réactivé et interruption levée.
    var previousQuestionIndex by remember { mutableStateOf(currentQuestionIndex) }
    LaunchedEffect(currentQuestionIndex) {
        if (previousQuestionIndex != currentQuestionIndex) {
            previousQuestionIndex = currentQuestionIndex
            onMicToggle(false)
            interrupted = false
        }
    }

    val canInterrupt = !interrupted && agentState == AgentState.SPEAKING

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .border(1.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .width(360.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gauche : stop / reprendre
            Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                if (interrupted) {
                    IconButton(onClick = { onResumeListen(); onMicToggle(false); interrupted = false }) {
                        Icon(Icons.Default.PlayCircle, contentDescription = strings.get(R.string.interaction_resume), tint = LyloPrimary, modifier = Modifier.size(26.dp))
                    }
                } else if (canInterrupt) {
                    IconButton(onClick = { onInterrupt(); onMicToggle(true); interrupted = true }) {
                        Icon(Icons.Default.StopCircle, contentDescription = strings.get(R.string.interaction_stop), tint = LyloPrimary, modifier = Modifier.size(26.dp))
                    }
                }
            }

            VerticalDivider()

            // Centre : statut ou bouton "j'ai une question"
            Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                if (sessionState == SessionState.STANDBY && !resumeAsked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { onResume(); resumeAsked = true },
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = LyloPrimary, modifier = Modifier.size(15.dp))
                        Text(strings.get(R.string.interaction_resume_question), style = MaterialTheme.typography.labelMedium, color = LyloPrimary)
                    }
                } else {
                    BottomBarStatus(agentState, agentName)
                }
            }

            VerticalDivider()

            // Droite : micro + conversation
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMicToggle(!muted) }) {
                    Icon(
                        if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (muted) strings.get(R.string.interaction_mic_enable) else strings.get(R.string.interaction_mic_disable),
                        tint = if (muted) androidx.compose.ui.graphics.Color(0xFFEF4444) else LyloPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                IconButton(onClick = onTranscriptToggle) {
                    Icon(
                        if (transcriptOpen) Icons.Default.Close else Icons.Default.Chat,
                        contentDescription = strings.get(R.string.transcript_title),
                        tint = LyloPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

/** État réel du micro local (LiveKitSessionManager.micMuted), fourni par LyloNavGraph. */
val LocalMicMuted = compositionLocalOf { false }

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(20.dp)
            .background(LyloPrimary.copy(alpha = 0.2f)),
    )
}

@Composable
private fun BottomBarStatus(agentState: AgentState, agentName: String) {
    val strings = LocalStringResolver.current
    val (label, dim) = when (agentState) {
        AgentState.INITIALIZING -> strings.get(R.string.interaction_connecting) to false
        AgentState.SPEAKING -> strings.get(R.string.interaction_agent_speaking).replace("{name}", agentName) to false
        AgentState.THINKING -> strings.get(R.string.interaction_agent_thinking) to true
        AgentState.LISTENING -> strings.get(R.string.interaction_agent_listening) to false
        AgentState.IDLE -> strings.get(R.string.interaction_agent_idle) to true
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (agentState == AgentState.LISTENING) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = LyloPrimary, modifier = Modifier.size(15.dp))
        } else if (agentState == AgentState.IDLE) {
            Icon(Icons.Default.MicOff, contentDescription = null, tint = LyloPrimary.copy(alpha = 0.4f), modifier = Modifier.size(15.dp))
        } else {
            PulsingDots(color = if (dim) LyloPrimary.copy(alpha = 0.4f) else LyloPrimary)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (dim) LyloPrimary.copy(alpha = 0.5f) else LyloPrimary,
            maxLines = 1,
        )
    }
}
