package com.lylo.tablet.robot

import android.content.Context
import android.os.SystemClock
import com.lylo.tablet.data.LocalPrefs

/** Délai par défaut entre deux messages (évite la boucle quand il y a du monde). */
private const val DEFAULT_COOLDOWN_S = 60

/**
 * Message d'accueil vocal quand le robot détecte quelqu'un (voir RobotController, suivi du
 * regard), dans la langue de l'interface. Actif seulement quand [enabled] (page d'accueil) et
 * que le réglage de Configuration du robot l'autorise. Lecture via [VoicePlayer].
 */
object GreetingPlayer {
    @Volatile var enabled = false
    @Volatile var language: String = "fr"

    /** Réglage de la page Configuration du robot (persisté) : coupe le message d'accueil. */
    @Volatile var soundEnabled = true
        private set

    /**
     * Délai minimum (s) entre deux messages ; 0 = à chaque détection (jamais deux messages
     * en même temps : une détection pendant la lecture est ignorée).
     */
    @Volatile var cooldownSeconds = DEFAULT_COOLDOWN_S
        private set

    /** Choix proposés dans Configuration du robot. */
    val COOLDOWN_CHOICES = listOf(0, 30, 60, 120, 300)

    private var lastPlayedAt = 0L

    fun init(context: Context) {
        VoicePlayer.init(context)
        val prefs = LocalPrefs(context)
        soundEnabled = prefs.getString(LocalPrefs.KEY_GREETING_ENABLED) != "false"
        cooldownSeconds = prefs.getString(LocalPrefs.KEY_GREETING_COOLDOWN_S)?.toIntOrNull() ?: DEFAULT_COOLDOWN_S
    }

    fun setSoundEnabled(context: Context, value: Boolean) {
        soundEnabled = value
        LocalPrefs(context).putString(LocalPrefs.KEY_GREETING_ENABLED, value.toString())
        if (!value) stop()
    }

    fun setCooldownSeconds(context: Context, value: Int) {
        cooldownSeconds = value
        LocalPrefs(context).putString(LocalPrefs.KEY_GREETING_COOLDOWN_S, value.toString())
    }

    @Synchronized
    fun onPersonDetected() {
        if (!enabled || !soundEnabled || VoicePlayer.isPlaying) return
        val now = SystemClock.elapsedRealtime()
        if (lastPlayedAt != 0L && now - lastPlayedAt < cooldownSeconds * 1_000L) return
        if (VoicePlayer.play(VoicePlayer.Cue.GREETING, language)) lastPlayedAt = now
    }

    fun stop() = VoicePlayer.stop()
}
