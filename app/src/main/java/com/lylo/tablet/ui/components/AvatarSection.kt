package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lylo.tablet.livekit.LiveKitAvatarVideoView
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.aiGlow
import io.livekit.android.room.Room

/**
 * Portage combiné de src/components/interaction/AvatarSection.tsx + AvatarVideo.tsx : cercle
 * avatar avec halo ai-glow, bascule automatique entre le flux vidéo LiveKit temps réel (si une
 * Room est active et que le participant avatar publie une piste caméra) et une image statique
 * de repli (fallbackImageUrl — équivalent /avatar-f.jpg ou /avatar-h.jpg selon la persona choisie).
 *
 * Layout : un Box englobant en Alignment.TopCenter, contenant le cercle avatar puis, positionnés
 * par-dessus via des offsets négatifs calculés depuis [size], le badge point primary et le
 * bandeau nom/rôle — reproduisant les `absolute bottom-2 right-2` / `absolute bottom-0 ...
 * translate-y-1/2` du CSS d'origine.
 */
@Composable
fun AvatarSection(
    name: String,
    role: String,
    fallbackImageUrl: Any = com.lylo.tablet.R.drawable.avatar_placeholder,
    modifier: Modifier = Modifier,
    avatarEnabled: Boolean = true,
    room: Room? = null,
    avatarDisabled: Boolean = false,
    size: Dp = 160.dp,
) {
    var liveTrackAvailable by remember(room) { mutableStateOf(false) }
    val showLiveVideo = avatarEnabled && room != null && !avatarDisabled
    val showFallback = !showLiveVideo || !liveTrackAvailable

    Box(modifier = modifier.padding(bottom = 16.dp), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .size(size)
                .aiGlow(),
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape),
            ) {
                if (showLiveVideo) {
                    LiveKitAvatarVideoView(
                        room = room!!,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        onTrackAvailable = { liveTrackAvailable = it },
                    )
                }

                if (showFallback) {
                    AsyncImage(
                        model = fallbackImageUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    if (showLiveVideo) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Badge point — absolute bottom-2 right-2
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(LyloPrimary)
                    .border(4.dp, LyloBackgroundLight, CircleShape),
            )
        }

        // Bandeau nom/rôle — chevauche le bas du cercle
        if (name.isNotEmpty() || role.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = size - 14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(text = name, style = MaterialTheme.typography.bodyMedium, color = LyloPrimary)
            }
        }
    }
}
