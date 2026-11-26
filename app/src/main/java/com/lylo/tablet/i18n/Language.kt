package com.lylo.tablet.i18n

import com.lylo.tablet.data.LocalPrefs

/** Portage de src/lib/language.ts (+ SUPPORTED_LOCALES de LanguageContext.tsx) — même ordre que le web. */
enum class AppLanguage(val code: String) {
    FR("fr"), EN("en"), ES("es"), DE("de"), AR("ar"), RU("ru");

    /** RTL_LOCALES côté web : l'arabe inverse le sens de lecture de toute l'interface. */
    val isRtl: Boolean get() = this == AR

    companion object {
        fun fromCode(code: String?): AppLanguage? = entries.firstOrNull { it.code == code }
    }
}

object LanguageStore {

    /** Portage de resolveStoredLanguage() : lit plusieurs clés par compat, "fr" par défaut. */
    fun resolveStoredLanguage(prefs: LocalPrefs): AppLanguage {
        val candidates = listOf(
            prefs.getString(LocalPrefs.KEY_AVATAR_LOCALE),
            prefs.getString(LocalPrefs.KEY_LANGUAGE),
            prefs.getString(LocalPrefs.KEY_LOCALE),
        )
        for (value in candidates) {
            AppLanguage.fromCode(value)?.let { return it }
        }
        return AppLanguage.FR
    }

    /** Portage de persistLanguage() : écrit les 3 clés en parallèle, comme côté web. */
    fun persistLanguage(prefs: LocalPrefs, language: AppLanguage) {
        prefs.putString(LocalPrefs.KEY_AVATAR_LOCALE, language.code)
        prefs.putString(LocalPrefs.KEY_LANGUAGE, language.code)
        prefs.putString(LocalPrefs.KEY_LOCALE, language.code)
    }
}
