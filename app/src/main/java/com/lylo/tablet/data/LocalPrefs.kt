package com.lylo.tablet.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Equivalent Android de `localStorage` côté web : stockage clé/valeur simple et synchrone,
 * suffisant ici vu les usages du front (persona, depth, mode, input_mode, avatar,
 * recap_email, participant_colors, quiz_formulas, avatarLocale/language/locale, auth_user...).
 *
 * On utilise SharedPreferences plutôt que DataStore pour ces accès car le code web original
 * les lit de façon synchrone un peu partout au moment du rendu (ex: `localStorage.getItem`
 * directement dans le corps d'un composable) — SharedPreferences reproduit fidèlement ce
 * pattern côté Compose sans complexifier chaque écran avec des coroutines.
 */
class LocalPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("lylo_prefs", Context.MODE_PRIVATE)

    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)
    fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clearSessionKeys() {
        // Nettoyage équivalent à endSession() côté web (ne touche pas aux préférences de config
        // comme persona/depth/mode qui doivent survivre à la fin d'une session).
    }

    companion object {
        // ── Clés miroir de celles utilisées dans le code web (localStorage.getItem/.setItem) ──
        const val KEY_PERSONA = "persona"
        const val KEY_DEPTH = "depth"
        const val KEY_MODE = "mode"
        const val KEY_INPUT_MODE = "input_mode"
        const val KEY_AVATAR = "avatar"
        const val KEY_RECAP_EMAIL = "recap_email"
        const val KEY_RECAP_NAME = "recap_name"
        const val KEY_PARTICIPANT_COUNT = "participant_count"
        const val KEY_PARTICIPANT_COLORS = "participant_colors"
        const val KEY_QUIZ_FORMULAS = "quiz_formulas"
        const val KEY_QUIZ_MULTI_RESULTS = "quiz_multi_results"
        const val KEY_AVATAR_LOCALE = "avatarLocale"
        const val KEY_LANGUAGE = "language"
        const val KEY_LOCALE = "locale"
        const val KEY_AUTH_USER = "auth_user"
        /** Point de la carte du robot vers lequel l'envoyer depuis l'écran de formule. */
        const val KEY_ROBOT_GOAL = "robot_goal"
    }
}
