package com.lylo.tablet.ui.screens.preparation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lylo.tablet.BuildConfig
import com.lylo.tablet.R
import com.lylo.tablet.data.model.SessionState
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.components.AvatarSection
import com.lylo.tablet.ui.components.BottomBar
import com.lylo.tablet.ui.components.Navbar
import com.lylo.tablet.ui.components.TranscriptPanel
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.glassPanel
import com.lylo.tablet.viewmodel.SessionViewModel
import io.livekit.android.room.Room

/**
 * Portage de src/app/preparation/page.tsx : démarre la session au montage (si pas déjà démarrée),
 * affiche l'avatar + 3 cartes de conseils (parler fort / environnement calme / connexion stable),
 * navigue automatiquement vers /interaction (questionnaire) ou /recommendations (completed) selon
 * sessionState — piloté par SessionViewModel + les events reçus via LiveKitSessionManager.
 */
@Composable
fun PreparationScreen(
    sessionViewModel: SessionViewModel,
    room: Room?,
    avatarFallback: Any,
    onNavigateToInteraction: () -> Unit,
    onNavigateToRecommendations: () -> Unit,
    onNavigateBackToConfigure: () -> Unit,
    onNavigateHome: () -> Unit,
    onStartSessionIfNeeded: () -> Unit,
    onInterrupt: () -> Unit,
    onResumeListen: () -> Unit,
    onResume: () -> Unit,
    onMicToggle: (Boolean) -> Unit,
) {
    val strings = LocalStringResolver.current
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val sessionData by sessionViewModel.sessionData.collectAsState()
    val agentName by sessionViewModel.agentName.collectAsState()
    val agentState by sessionViewModel.agentState.collectAsState()
    val noCreditsError by sessionViewModel.noCreditsError.collectAsState()
    val transcripts by sessionViewModel.transcripts.collectAsState()
    val avatarDisabled by sessionViewModel.avatarDisabled.collectAsState()

    LaunchedEffect(Unit) {
        if (!BuildConfig.DEV_MODE && sessionData == null) {
            onStartSessionIfNeeded()
        }
    }

    LaunchedEffect(sessionState) {
        if (BuildConfig.DEV_MODE) return@LaunchedEffect
        when (sessionState) {
            SessionState.QUESTIONNAIRE -> onNavigateToInteraction()
            SessionState.COMPLETED -> onNavigateToRecommendations()
            else -> Unit
        }
    }

    if (noCreditsError) {
        Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(strings.get(R.string.auth_no_sessions), color = androidx.compose.ui.graphics.Color(0xFFEF4444), style = MaterialTheme.typography.titleMedium)
                Text(
                    strings.get(R.string.preparation_back),
                    color = LyloPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { onNavigateBackToConfigure() },
                )
            }
        }
        return
    }

    val isConnecting = sessionState == SessionState.IDLE || sessionState == SessionState.CONNECTING

    // Levé ici pour que TranscriptPanel soit rendu en overlay plein écran sur le Box racine
    // fillMaxSize() ci-dessous, et non contraint à la hauteur de la pilule BottomBar.
    var transcriptOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Navbar(showActions = false, transparent = true, onLogoClick = onNavigateHome)

            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.widthIn(max = 640.dp).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PreparationAvatar(
                        agentName = agentName,
                        room = room,
                        avatarFallback = avatarFallback,
                        avatarDisabled = avatarDisabled,
                    )

                    Text(
                        strings.get(R.string.preparation_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = LyloPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                    Text(
                        strings.get(R.string.preparation_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LyloPrimary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AdviceCard(Icons.Default.Mic, strings.get(R.string.preparation_speak_title), strings.get(R.string.preparation_speak_desc), Modifier.weight(1f))
                        AdviceCard(Icons.Default.VolumeOff, strings.get(R.string.preparation_quiet_title), strings.get(R.string.preparation_quiet_desc), Modifier.weight(1f))
                        AdviceCard(Icons.Default.Wifi, strings.get(R.string.preparation_connection_title), strings.get(R.string.preparation_connection_desc), Modifier.weight(1f))
                    }

                    if (sessionData != null) {
                        BottomBar(
                            agentState = agentState,
                            sessionState = sessionState,
                            agentName = agentName,
                            transcriptOpen = transcriptOpen,
                            onTranscriptToggle = { transcriptOpen = !transcriptOpen },
                            onInterrupt = onInterrupt,
                            onResumeListen = onResumeListen,
                            onResume = onResume,
                            onMicToggle = onMicToggle,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }

                    Text(
                        strings.get(R.string.preparation_footer),
                        style = MaterialTheme.typography.labelSmall,
                        color = LyloPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }

        if (isConnecting) {
            Box(
                modifier = Modifier.fillMaxSize().background(LyloBackgroundLight.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = LyloPrimary)
                    Text(
                        strings.get(R.string.preparation_connecting),
                        style = MaterialTheme.typography.titleMedium,
                        color = LyloPrimary,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        strings.get(R.string.preparation_please_wait),
                        style = MaterialTheme.typography.bodySmall,
                        color = LyloPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        // Overlay plein écran ancré à droite — voir le commentaire équivalent dans
        // InteractionScreen.kt / BottomBar.kt.
        TranscriptPanel(
            open = transcriptOpen,
            transcripts = transcripts,
            agentName = agentName,
            onToggle = { transcriptOpen = !transcriptOpen },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/**
 * Portage du bloc avatar de preparation/page.tsx : halo `bg-primary/20 blur-2xl animate-pulse`
 * derrière le cercle (en plus du ai-glow statique déjà géré par AvatarSection), puis un badge
 * `{agentName} AI` — fond primary plein, texte blanc, uppercase — sous le cercle. Ce badge est
 * volontairement différent de celui rendu par AvatarSection.tsx (bandeau blanc translucide
 * chevauchant, utilisé sur l'écran Interaction) : on ne réutilise donc pas le bandeau intégré à
 * AvatarSection (name/role vides ci-dessous) et on dessine le nôtre par-dessus.
 */
@Composable
private fun PreparationAvatar(
    agentName: String,
    room: Room?,
    avatarFallback: Any,
    avatarDisabled: Boolean,
) {
    val avatarSize = 160.dp
    val transition = rememberInfiniteTransition(label = "avatar-pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "avatar-pulse-alpha",
    )

    Box(contentAlignment = Alignment.TopCenter) {
        Box(contentAlignment = Alignment.Center) {
            // Halo animé — absolute inset-0 bg-primary/20 rounded-full blur-2xl animate-pulse
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .blur(24.dp)
                    .clip(CircleShape)
                    .background(LyloPrimary.copy(alpha = pulseAlpha), CircleShape),
            )

            AvatarSection(
                name = "",
                role = "",
                fallbackImageUrl = avatarFallback,
                avatarEnabled = true,
                room = room,
                avatarDisabled = avatarDisabled,
                size = avatarSize,
            )
        }

        // Badge nom — fond primary plein, texte blanc, uppercase, sous le cercle.
        Box(
            modifier = Modifier
                .padding(top = avatarSize - 8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(LyloPrimary, RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                text = "$agentName AI".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun AdviceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .glassPanel()
            .padding(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = LyloPrimary, modifier = Modifier.size(28.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, color = LyloPrimary, modifier = Modifier.padding(top = 8.dp))
        Text(desc, style = MaterialTheme.typography.bodySmall, color = LyloPrimary.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
    }
}
