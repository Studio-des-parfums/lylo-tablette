package com.lylo.tablet.robot

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

private const val TAG = "VoicePlayer"
private const val AUDIO_ROOT = "audio"
private const val FALLBACK_LANGUAGE = "fr"

/**
 * Lecture des audios embarqués : assets/audio/<catégorie>/<langue>/<catégorie>_<langue>_<nn>.wav
 * (voir assets/audio/README.md). Une variante tirée au hasard ; repli sur le français.
 * Un seul audio à la fois : un nouvel audio coupe celui en cours. Catégorie vide = silence.
 */
object VoicePlayer {
    /** Catégories (= noms de dossiers dans assets/audio). */
    object Cue {
        const val GREETING = "greetings"
        const val FORMULA_INTRO = "formula_intro"
        const val GUIDE_START = "guide_start"
        const val GUIDE_ARRIVED = "guide_arrived"
        const val GOODBYE = "goodbye"
    }

    private var appContext: Context? = null
    private var player: MediaPlayer? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val isPlaying: Boolean @Synchronized get() = player != null

    /** Joue un audio de [category] dans [language] ; renvoie false si aucun fichier. */
    @Synchronized
    fun play(category: String, language: String): Boolean {
        val context = appContext ?: return false
        val path = pickFile(context, category, language)
            ?: pickFile(context, category, FALLBACK_LANGUAGE)
            ?: run {
                Log.d(TAG, "Aucun audio pour $category ($language)")
                return false
            }
        release()
        return runCatching {
            context.assets.openFd(path).use { fd ->
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    // this@VoicePlayer : dans ce bloc `apply`, un simple release() visait
                    // MediaPlayer.release() — le lecteur restait marqué « en cours » et bloquait
                    // tous les messages d'accueil suivants.
                    setOnCompletionListener { this@VoicePlayer.release() }
                    setOnErrorListener { _, _, _ -> this@VoicePlayer.release(); true }
                    prepare()
                    start()
                }
            }
            Log.d(TAG, "Joué : $path")
            true
        }.getOrElse {
            Log.w(TAG, "Lecture impossible : $path", it)
            release()
            false
        }
    }

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

    private fun pickFile(context: Context, category: String, lang: String): String? =
        context.assets.list("$AUDIO_ROOT/$category/$lang")
            ?.filter { it.endsWith(".wav") || it.endsWith(".mp3") }
            ?.randomOrNull()
            ?.let { "$AUDIO_ROOT/$category/$lang/$it" }
}
