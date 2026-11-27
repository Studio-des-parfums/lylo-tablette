package com.lylo.tablet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog qui conserve la densité de l'app. Un Dialog Compose ouvre sa propre fenêtre et repart
 * de la densité système : sur le robot OrionStar (560 dpi, corrigée dans MainActivity), les
 * modales réapparaissaient zoomées ×3,5. À utiliser à la place de [Dialog].
 */
@Composable
fun LyloDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val appDensity = LocalDensity.current
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        CompositionLocalProvider(LocalDensity provides appDensity, content = content)
    }
}
