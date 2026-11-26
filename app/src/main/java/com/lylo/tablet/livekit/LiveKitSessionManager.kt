package com.lylo.tablet.livekit

import android.app.Application
import com.lylo.tablet.data.model.SessionData
import com.lylo.tablet.data.model.TranscriptMessage
import com.lylo.tablet.data.model.TranscriptSender
import com.lylo.tablet.viewmodel.SessionViewModel
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.DataPublishReliability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Petit helper pour sérialiser une List&lt;String&gt; en JsonArray dans un buildJsonObject { }. */
private fun JsonObjectBuilder.putJsonArray(key: String, values: List<String>) {
    put(key, JsonArray(values.map { JsonPrimitive(it) }))
}

/**
 * Portage de src/components/livekit/LiveKitSession.tsx — connexion à la Room LiveKit, écoute
 * du data channel "state" (SessionViewModel.handleDataMessage), envoi sur le control channel
 * "control" (réponses en mode clic + interrupt/resume), écoute des transcriptions et gestion
 * basique du device de sortie audio.
 *
 * Utilise le SDK officiel `io.livekit:livekit-android` (LiveKit Android Components/Client SDK).
 * Le cycle de vie (connect/disconnect) suit l'apparition/disparition de [SessionViewModel.sessionData],
 * exactement comme le `<LiveKitRoom serverUrl={...} token={...} connect={sessionData != null}>`
 * du composant React.
 */
class LiveKitSessionManager(
    private val application: Application,
    private val sessionViewModel: SessionViewModel,
) {
    private val scope = CoroutineScope(SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    // Exposé en StateFlow (et non en simple `var`) car LyloNavGraph.kt lit cette valeur pour la
    // passer à AvatarSection/InteractionScreen : un `var` classique ne déclenche aucune
    // recomposition Compose quand connect()/disconnect() le réassigne après le premier rendu —
    // c'était exactement la cause du bug "l'avatar reste bloqué sur l'image statique" (la Room
    // se connectait bien en interne, mais l'UI ne le voyait jamais).
    private val _room = MutableStateFlow<Room?>(null)
    val roomState: StateFlow<Room?> = _room.asStateFlow()
    val room: Room?
        get() = _room.value

    private var eventJob: Job? = null
    private var pendingClickJob: Job? = null
    private var pendingPerfumeNameJob: Job? = null

    // État réel du micro local, partagé par toutes les BottomBar (une par écran) — portage du
    // useState initialisé depuis la track micro + synchronisé sur TrackMuted/TrackUnmuted côté web :
    // l'icône ne se désynchronise plus en changeant d'écran.
    private val _micMuted = MutableStateFlow(false)
    val micMuted: StateFlow<Boolean> = _micMuted.asStateFlow()
    private var timeoutJob: Job? = null

    /** Se connecte à la room décrite par [data]. Doit être appelé depuis un scope avec permissions déjà accordées. */
    fun connect(data: SessionData) {
        disconnect() // sécurité : une seule room active à la fois

        val newRoom = LiveKit.create(appContext = application)
        _room.value = newRoom

        scope.launch {
            try {
                newRoom.connect(url = data.livekitUrl, token = data.token)
                // Publie le micro local (équivalent de <LiveKitRoom audio={true} video={false}>)
                newRoom.localParticipant.setMicrophoneEnabled(true)
                sessionViewModel.setConnectionError(false)
                _micMuted.value = false
            } catch (e: Exception) {
                sessionViewModel.handleConnectionTimeout()
                return@launch
            }
        }

        observeRoomEvents(newRoom)
        observePendingClickAnswer(newRoom)
        observePendingPerfumeName(newRoom)
        startConnectionTimeoutWatchdog(newRoom)
    }

    fun disconnect() {
        eventJob?.cancel()
        pendingClickJob?.cancel()
        pendingPerfumeNameJob?.cancel()
        timeoutJob?.cancel()
        room?.disconnect()
        room?.release()
        _room.value = null
    }

    /**
     * Portage de RoomEventLogger + DataChannelListener + TranscriptionListener +
     * synchro de l'état du micro réunis en un seul flux d'événements LiveKit.
     */
    @OptIn(io.livekit.android.annotations.Beta::class)
    private fun observeRoomEvents(room: Room) {
        eventJob = scope.launch {
            room.events.collect { event ->
                when (event) {
                    is RoomEvent.Connected -> {
                        timeoutJob?.cancel()
                    }

                    is RoomEvent.DataReceived -> {
                        // Le SDK expose le topic sur lequel la donnée a été publiée : on ne
                        // traite que le topic "state" pour l'état de session (équivalent
                        // useDataChannel("state", ...) côté web).
                        if (event.topic == "state") {
                            sessionViewModel.handleDataMessage(event.data)
                        }
                    }

                    is RoomEvent.TranscriptionReceived -> {
                        event.transcriptionSegments.forEach { segment ->
                            val sender = if (event.participant?.sid == room.localParticipant.sid) {
                                TranscriptSender.USER
                            } else {
                                TranscriptSender.AGENT
                            }
                            sessionViewModel.upsertTranscript(
                                TranscriptMessage(
                                    id = segment.id,
                                    sender = sender,
                                    text = segment.text,
                                    timestamp = segment.firstReceivedTime,
                                )
                            )
                        }
                    }

                    is RoomEvent.TrackMuted, is RoomEvent.TrackUnmuted, is RoomEvent.TrackPublished -> {
                        syncMicMuted(room)
                    }

                    is RoomEvent.Disconnected -> {
                        // Pas de retry automatique ici : c'est le rôle du watchdog de connexion
                        // (startConnectionTimeoutWatchdog) et de SessionViewModel.handleConnectionTimeout()
                        // en cas d'échec initial. Une déconnexion en cours de session normale
                        // n'est pas retentée automatiquement, comme côté web (RoomEventLogger
                        // se contente de logger ce cas).
                    }

                    else -> Unit
                }
            }
        }
    }

    /**
     * Portage de ClickModeController : dès que SessionViewModel expose un pendingClickAnswer
     * (réponse en mode clic), on l'envoie sur le control channel puis on le vide.
     */
    private fun observePendingClickAnswer(room: Room) {
        pendingClickJob = scope.launch {
            sessionViewModel.pendingClickAnswer.collect { pending ->
                if (pending == null) return@collect
                val payload = buildJsonObject {
                    put("type", pending.type)
                    put("question_id", pending.questionId)
                    putJsonArray("values", pending.values)
                    pending.top2?.let { putJsonArray("top_2", it) }
                }
                sendControlMessage(room, payload.toString())
                sessionViewModel.clearPendingClickAnswer()
            }
        }
    }

    /** Envoie un message JSON arbitraire sur le control channel (interrupt / resume / resume_listen / réponses clic). */
    fun sendControlMessage(room: Room? = this.room, jsonPayload: String) {
        val target = room ?: return
        scope.launch {
            runCatching {
                target.localParticipant.publishData(
                    jsonPayload.toByteArray(Charsets.UTF_8),
                    DataPublishReliability.RELIABLE,
                    "control",
                )
            }
        }
    }

    fun sendInterrupt() = sendControlMessage(jsonPayload = """{"type":"interrupt"}""")
    fun sendResumeListen() = sendControlMessage(jsonPayload = """{"type":"resume_listen"}""")
    fun sendResume() = sendControlMessage(jsonPayload = """{"type":"resume"}""")

    /**
     * Portage de PerfumeNameController : dès que SessionViewModel expose un nom de parfum saisi
     * (étape asking_perfume_name), on l'envoie sur le control channel puis on le vide.
     */
    private fun observePendingPerfumeName(room: Room) {
        pendingPerfumeNameJob = scope.launch {
            sessionViewModel.pendingPerfumeName.collect { name ->
                if (name == null) return@collect
                val payload = buildJsonObject {
                    put("type", "perfume_name_submitted")
                    put("name", name)
                }
                sendControlMessage(room, payload.toString())
                sessionViewModel.clearPendingPerfumeName()
            }
        }
    }

    fun setMicrophoneMuted(muted: Boolean) {
        _micMuted.value = muted
        scope.launch {
            runCatching { room?.localParticipant?.setMicrophoneEnabled(!muted) }
            room?.let { syncMicMuted(it) }
        }
    }

    private fun syncMicMuted(room: Room) {
        val pub = room.localParticipant.getTrackPublication(io.livekit.android.room.track.Track.Source.MICROPHONE)
        if (pub != null) _micMuted.value = pub.muted
    }

    /**
     * Portage du timeout de 10s dans RoomEventLogger : si la room n'est pas "Connected" après
     * 10 secondes, on déclenche le même mécanisme de retry/abandon que côté web.
     */
    private fun startConnectionTimeoutWatchdog(room: Room) {
        timeoutJob = scope.launch {
            kotlinx.coroutines.delay(10_000)
            if (room.state != Room.State.CONNECTED) {
                sessionViewModel.handleConnectionTimeout()
            }
        }
    }
}
