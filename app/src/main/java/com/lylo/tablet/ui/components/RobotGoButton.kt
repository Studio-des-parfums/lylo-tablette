package com.lylo.tablet.ui.components

import com.lylo.tablet.robot.VoicePlayer

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lylo.tablet.R
import com.lylo.tablet.data.LocalPrefs
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.robot.RobotController
import com.lylo.tablet.robot.RobotController.NavigationState

/**
 * Bouton « Emmène-moi » de la barre d'outils des écrans de formule : le robot accompagne
 * le visiteur vers le point choisi
 * dans RobotSettingsScreen. Masqué si aucun point n'est configuré ou si l'app ne tourne pas
 * sur le robot.
 */
@Composable
fun RobotGoButton(
    modifier: Modifier = Modifier,
    /** Langue des audios de guidage ; null = pas d'audio (mode vocal : l'agent parle déjà). */
    voiceLanguage: String? = null,
) {
    val strings = LocalStringResolver.current
    val context = LocalContext.current
    val goal = remember { LocalPrefs(context).getString(LocalPrefs.KEY_ROBOT_GOAL) }
    val available by RobotController.available.collectAsState()
    val navigation by RobotController.navigation.collectAsState()

    LaunchedEffect(navigation) {
        val message = when (val state = navigation) {
            is NavigationState.Arrived -> {
                // « Nous y voilà… cliquez sur Terminer »
                voiceLanguage?.let { VoicePlayer.play(VoicePlayer.Cue.GUIDE_ARRIVED, it) }
                strings.get(R.string.robot_go_arrived)
            }
            is NavigationState.Failed -> strings.get(R.string.robot_go_failed, state.message)
            else -> null
        }
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            RobotController.clearNavigationResult()
        }
    }

    if (goal.isNullOrBlank() || !available) return

    val moving = navigation is NavigationState.Moving
    PrimaryPillButton(
        text = strings.get(if (moving) R.string.robot_go_moving else R.string.robot_go_button),
        onClick = {
            // « Je vous y emmène, suivez-moi »
            voiceLanguage?.let { VoicePlayer.play(VoicePlayer.Cue.GUIDE_START, it) }
            RobotController.navigateTo(goal)
        },
        trailingIcon = Icons.Default.SmartToy,
        enabled = !moving,
        modifier = modifier,
    )
}
