package com.lylo.tablet.livekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack

/** Identité du participant "avatar" côté agent LiveKit (Beyond Presence), miroir du web. */
private const val AVATAR_PARTICIPANT_IDENTITY = "bey-avatar-agent"

/**
 * Portage de src/components/interaction/AvatarVideo.tsx (AvatarVideoStream) : cherche le
 * participant LiveKit d'identité [AVATAR_PARTICIPANT_IDENTITY], récupère sa piste vidéo caméra
 * et l'attache à un TextureViewRenderer LiveKit, en se resynchronisant à chaque
 * TrackSubscribed/TrackUnsubscribed/ParticipantConnected/ParticipantDisconnected reçu sur
 * room.events (Flow public et stable du SDK, préféré ici à une API à callbacks).
 *
 * Rendu uniquement quand une Room est active et que ce participant + sa piste existent ;
 * l'appelant (écran) doit afficher le fallback image statique dans les autres cas, exactement
 * comme côté web (AvatarVideo bascule entre <video> LiveKit et image de fond figée).
 */
@Composable
fun LiveKitAvatarVideoView(
    room: Room,
    modifier: Modifier = Modifier,
    onTrackAvailable: (Boolean) -> Unit = {},
) {
    var videoTrack by remember(room) { mutableStateOf<VideoTrack?>(null) }

    fun refreshTrack() {
        val avatarParticipant = room.remoteParticipants.values
            .firstOrNull { it.identity?.value == AVATAR_PARTICIPANT_IDENTITY }
        val track = avatarParticipant
            ?.getTrackPublication(Track.Source.CAMERA)
            ?.track as? VideoTrack
        videoTrack = track
        onTrackAvailable(track != null)
    }

    LaunchedEffect(room) {
        refreshTrack()
        room.events.collect { event ->
            when (event) {
                is RoomEvent.TrackSubscribed,
                is RoomEvent.TrackUnsubscribed,
                is RoomEvent.ParticipantConnected,
                is RoomEvent.ParticipantDisconnected,
                is RoomEvent.Connected,
                -> refreshTrack()
                else -> Unit
            }
        }
    }

    val currentTrack = videoTrack
    if (currentTrack != null) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                TextureViewRenderer(ctx).also { renderer ->
                    room.initVideoRenderer(renderer)
                    // Remplit le rond (équivalent de object-cover côté web) au lieu de letterboxer.
                    renderer.setScalingType(livekit.org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    currentTrack.addRenderer(renderer)
                }
            },
            update = { renderer ->
                currentTrack.addRenderer(renderer)
            },
        )

        DisposableEffect(currentTrack) {
            onDispose { /* le renderer est recréé par AndroidView à la prochaine recomposition si besoin */ }
        }
    }
}
