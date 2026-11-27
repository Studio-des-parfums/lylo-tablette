package com.lylo.tablet.robot

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.SystemClock
import android.util.Log
import com.lylo.tablet.data.LocalPrefs

private const val TAG = "GreetingPlayer"
private const val GREETINGS_DIR = "audio/greetings"
private const val FALLBACK_LANGUAGE = "fr"
/** Délai par défaut entre deux messages (évite la boucle quand il y a du monde). */
private const val DEFAULT_COOLDOWN_S = 60

/**
 * Message d'accueil vocal quand le robot détecte quelqu'un (voir RobotController, suivi du
 * regard). Audios embarqués : assets/audio/greetings/<langue>/greeting_<langue>_<nn>.wav, une
 * variante tirée au hasard ; repli sur le français si la langue n'a pas de fichier.
 * Actif seulement quand [enabled] (page d'accueil).
 */
object GreetingPlayer {
    @Volatile var enabled = false
    @Volatile var language: String = FALLBACK_LANGUAGE

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

    private var appContext: Context? = null
    private var player: MediaPlayer? = null
    private var lastPlayedAt = 0L

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = LocalPrefs(context)
        soundEnabled = prefs.getString(LocalPrefs.KEY_GREETING_ENABLED) != "false"
        cooldownSeconds = prefs.getString(LocalPrefs.KEY_GREETING_COOLDOWN_S)?.toIntOrNull() ?: DEFAULT_COOLDOWN_S
    }

    fun setCooldownSeconds(context: Context, value: Int) {
        cooldownSeconds = value
        LocalPrefs(context).putString(LocalPrefs.KEY_GREETING_COOLDOWN_S, value.toString())
    }

    fun setSoundEnabled(context: Context, value: Boolean) {
        soundEnabled = value
        LocalPrefs(context).putString(LocalPrefs.KEY_GREETING_ENABLED, value.toString())
        if (!value) stop()
    }

    @Synchronized
    fun onPersonDetected() {
        val context = appContext ?: return
        if (!enabled || !soundEnabled || player != null) return
        val now = SystemClock.elapsedRealtime()
        if (lastPlayedAt != 0L && now - lastPlayedAt < cooldownSeconds * 1_000L) return

        val path = pickFile(context, language) ?: pickFile(context, FALLBACK_LANGUAGE) ?: run {
            Log.w(TAG, "Aucun audio d'accueil dans $GREETINGS_DIR")
            return
        }
        runCatching {
            context.assets.openFd(path).use { fd ->
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    setOnCompletionListener { release() }
                    setOnErrorListener { _, _, _ -> release(); true }
                    prepare()
                    start()
                }
            }
            lastPlayedAt = now
            Log.d(TAG, "Accueil joué : $path")
        }.onFailure {
            Log.w(TAG, "Lecture impossible : $path", it)
            release()
        }
    }

    /** Coupe un message en cours (ex. le visiteur lance une session). */
    @Synchronized
    fun stop() {
        player?.runCatching { if (isPlaying) stop() }
        release()
    }

    @Synchronized
    private fun release() {
        player?.release()
        player = null
    }

    private fun pickFile(context: Context, lang: String): String? =
        context.assets.list("$GREETINGS_DIR/$lang")
            ?.filter { it.endsWith(".wav") || it.endsWith(".mp3") }
            ?.randomOrNull()
            ?.let { "$GREETINGS_DIR/$lang/$it" }
}
