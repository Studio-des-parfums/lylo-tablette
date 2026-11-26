package com.lylo.tablet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lylo.tablet.data.LocalPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Portage de l'état local de src/components/configure/ConfigPanel.tsx (persona/depth/mode/
 * inputMode/avatar/participantCount/selectedColors — mode duo : 1 personne, ou 2 avec chacune sa couleur) + la persistance localStorage
 * effectuée dans handleContinue().
 */
class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = LocalPrefs(application)

    private val _persona = MutableStateFlow("")
    val persona: StateFlow<String> = _persona.asStateFlow()

    private val _depth = MutableStateFlow("")
    val depth: StateFlow<String> = _depth.asStateFlow()

    private val _mode = MutableStateFlow("")
    val mode: StateFlow<String> = _mode.asStateFlow()

    private val _inputMode = MutableStateFlow("voice")
    val inputMode: StateFlow<String> = _inputMode.asStateFlow()

    private val _avatar = MutableStateFlow(true)
    val avatar: StateFlow<Boolean> = _avatar.asStateFlow()

    private val _participantCount = MutableStateFlow(1)
    val participantCount: StateFlow<Int> = _participantCount.asStateFlow()

    private val _selectedColors = MutableStateFlow<List<String>>(listOf(""))
    val selectedColors: StateFlow<List<String>> = _selectedColors.asStateFlow()

    fun setPersona(v: String) { _persona.value = v }
    fun setDepth(v: String) { _depth.value = v }
    fun setMode(v: String) { _mode.value = v }
    fun setInputMode(v: String) { _inputMode.value = v }
    fun toggleAvatar() { _avatar.value = !_avatar.value }

    fun setParticipantCount(n: Int) {
        _participantCount.value = n
        _selectedColors.value = List(n) { "" }
    }

    fun selectColor(participantIndex: Int, colorId: String) {
        _selectedColors.value = _selectedColors.value.toMutableList().also {
            while (it.size <= participantIndex) it.add("")
            it[participantIndex] = colorId
        }
    }

    fun colorsValid(): Boolean {
        if (_inputMode.value != "quiz") return true
        val count = _participantCount.value
        if (count == 1) return true
        val active = _selectedColors.value.take(count)
        return active.all { it.isNotEmpty() } && active.toSet().size == count
    }

    fun isFormComplete(): Boolean {
        val quiz = _inputMode.value == "quiz"
        val base = if (quiz) _depth.value.isNotEmpty() else _persona.value.isNotEmpty() && _depth.value.isNotEmpty() && _mode.value.isNotEmpty()
        return base && colorsValid()
    }

    /** Portage de handleContinue() : persiste tout dans localStorage (SharedPreferences ici). */
    fun persistAndContinue() {
        prefs.putString(LocalPrefs.KEY_PERSONA, _persona.value)
        prefs.putString(LocalPrefs.KEY_DEPTH, _depth.value)
        prefs.putString(LocalPrefs.KEY_MODE, _mode.value)
        prefs.putString(LocalPrefs.KEY_INPUT_MODE, _inputMode.value)
        prefs.putString(LocalPrefs.KEY_AVATAR, _avatar.value.toString())
        // L'email n'est plus demandé ici (saisi à la fin, modale « Recevoir par mail ») : on
        // purge une éventuelle valeur restée d'une ancienne version.
        prefs.remove(LocalPrefs.KEY_RECAP_EMAIL)

        if (_inputMode.value == "quiz") {
            prefs.putString(LocalPrefs.KEY_PARTICIPANT_COUNT, _participantCount.value.toString())
            if (_participantCount.value > 1) {
                val colorsJson = "[" + _selectedColors.value.take(_participantCount.value).joinToString(",") { "\"$it\"" } + "]"
                prefs.putString(LocalPrefs.KEY_PARTICIPANT_COLORS, colorsJson)
            } else {
                prefs.remove(LocalPrefs.KEY_PARTICIPANT_COLORS)
            }
        }
    }

    fun usedColorsExcluding(index: Int): List<String> =
        _selectedColors.value.filterIndexed { i, _ -> i != index }
}
