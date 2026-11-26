package com.lylo.tablet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lylo.tablet.i18n.LanguageViewModel
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.i18n.StringResolver
import com.lylo.tablet.livekit.LiveKitSessionManager
import com.lylo.tablet.navigation.LyloNavGraph
import com.lylo.tablet.ui.components.LyloButton
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.LyloTabletTheme
import com.lylo.tablet.viewmodel.AuthViewModel
import com.lylo.tablet.viewmodel.SessionViewModel

/**
 * Portage de src/app/layout.tsx + page.tsx d'entrée : monte le thème global, les ViewModels
 * "context-like" (Session/Auth/Language) et le NavGraph. Demande les permissions RECORD_AUDIO +
 * CAMERA au démarrage (équivalent du prompt navigateur déclenché par getUserMedia côté web),
 * requises pour LiveKit sur tablette.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LyloApp()
        }
    }

    override fun onStart() {
        super.onStart()
        // Reconnexion au service robot s'il a été perdu pendant que l'app était en arrière-plan.
        com.lylo.tablet.robot.RobotController.connect(this)
    }
}

@Composable
private fun LyloApp() {
    val context = LocalContext.current
    val sessionViewModel: SessionViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val languageViewModel: LanguageViewModel = viewModel()

    val liveKitManager = remember { LiveKitSessionManager(context.applicationContext as android.app.Application, sessionViewModel) }

    var micGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        micGranted = results[Manifest.permission.RECORD_AUDIO] == true
        cameraGranted = results[Manifest.permission.CAMERA] == true
        permissionRequested = true
    }

    LyloTabletTheme {
        // Le contexte localisé est recalculé à chaque changement de langue (sélecteur Navbar,
        // LanguagePicker) ; l'arabe bascule toute l'interface en RTL, comme dir="rtl" côté web.
        val locale by languageViewModel.locale.collectAsState()
        val localizedContext = remember(locale) { languageViewModel.localizedContext() }
        CompositionLocalProvider(
            LocalStringResolver provides StringResolver(localizedContext),
            LocalLayoutDirection provides if (locale.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            // Android 15 (targetSdk 35) impose l'affichage bord à bord : sans ce padding, la navbar
            // passe sous la barre d'état et la TechBar sous la barre de navigation.
            Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight).systemBarsPadding()) {
            if (!micGranted && !permissionRequested) {
                PermissionGateScreen(
                    onRequest = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                    },
                )
            } else {
                LyloNavGraph(
                    sessionViewModel = sessionViewModel,
                    authViewModel = authViewModel,
                    liveKitManager = liveKitManager,
                )
            }
            }
        }
    }
}

/**
 * Portage du prompt de permission micro/caméra requis par LiveKit — le web déclenche
 * implicitement ce prompt via getUserMedia() au moment de rejoindre la room ; sur Android on
 * préfère le demander explicitement au lancement pour éviter un flash d'UI cassée si l'usager
 * refuse en pleine session vocale.
 */
@Composable
private fun PermissionGateScreen(onRequest: () -> Unit) {
    val strings = LocalStringResolver.current
    Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                strings.get(R.string.permission_mic_camera_title),
                style = MaterialTheme.typography.headlineSmall,
                color = LyloPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                strings.get(R.string.permission_mic_camera_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = LyloPrimary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            )
            LyloButton(onClick = onRequest) {
                Text(strings.get(R.string.permission_grant))
            }
        }
    }
}
