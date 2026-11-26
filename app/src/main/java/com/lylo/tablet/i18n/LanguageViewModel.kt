package com.lylo.tablet.i18n

import android.app.Application
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lylo.tablet.data.LocalPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Portage de src/i18n/LanguageContext.tsx (useTranslation / LanguageProvider).
 *
 * Contrairement au web où les chaînes viennent de fichiers JSON chargés dynamiquement, ici on
 * s'appuie sur les ressources Android standard (res/values/strings.xml + res/values-fr/strings.xml)
 * — c'est l'équivalent natif idiomatique. Ce ViewModel gère uniquement le SWITCH dynamique de
 * langue in-app (le sélecteur FR/EN et le LanguagePicker de /configure doivent changer
 * l'affichage sans redémarrer l'app), en recréant un `Context` localisé à la volée exposé via
 * [LocalAppLocale] / [strings].
 */
class LanguageViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = LocalPrefs(application)

    private val _locale = MutableStateFlow(LanguageStore.resolveStoredLanguage(prefs))
    val locale: StateFlow<AppLanguage> = _locale

    init {
        // Persiste immédiatement (comme le useEffect de LanguageProvider côté web)
        LanguageStore.persistLanguage(prefs, _locale.value)
    }

    fun setLocale(language: AppLanguage) {
        _locale.value = language
        viewModelScope.launch {
            LanguageStore.persistLanguage(prefs, language)
        }
    }

    /** Contexte localisé pour résoudre les string resources dans la langue choisie dynamiquement. */
    fun localizedContext(): android.content.Context {
        val base = getApplication<Application>()
        val locale = Locale(_locale.value.code)
        val config = android.content.res.Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}

/** CompositionLocal exposant le contexte localisé courant à tous les composables enfants. */
val LocalStringResolver = compositionLocalOf<StringResolver> {
    error("StringResolver not provided — wrap your content in LyloAppRoot / ProvideStrings")
}

/** Petit wrapper pour résoudre les string resources depuis un Context localisé. */
class StringResolver(private val context: android.content.Context) {
    fun get(resId: Int): String = context.getString(resId)
    fun get(resId: Int, vararg args: Any): String = context.getString(resId, *args)
}
