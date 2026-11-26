package com.lylo.tablet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lylo.tablet.BuildConfig
import com.lylo.tablet.data.LocalPrefs
import com.lylo.tablet.livekit.LiveKitSessionManager
import com.lylo.tablet.ui.screens.configure.ConfigureScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.lylo.tablet.ui.components.LocalMicMuted
import com.lylo.tablet.ui.screens.formula.FormulaScreen
import com.lylo.tablet.ui.screens.formulas.FormulasScreen
import com.lylo.tablet.ui.screens.home.HomeScreen
import com.lylo.tablet.ui.screens.interaction.InteractionScreen
import com.lylo.tablet.ui.screens.preparation.PreparationScreen
import com.lylo.tablet.ui.screens.quiz.QuizResultsScreen
import com.lylo.tablet.ui.screens.quiz.QuizScreen
import com.lylo.tablet.ui.screens.recommendations.RecommendationsScreen
import com.lylo.tablet.ui.screens.robot.RobotSettingsScreen
import com.lylo.tablet.viewmodel.AuthViewModel
import com.lylo.tablet.viewmodel.SessionViewModel

/** URL publique du front web Lylo, utilisée pour construire les liens de partage de formule
 * (QR code) — voir README.md : NEXT_PUBLIC_APP_URL côté web. */
private const val WEB_APP_BASE_URL = BuildConfig.API_BASE_URL

/**
 * Portage de la navigation Next.js App Router (src/app/.../page.tsx) vers Compose Navigation.
 * Un seul NavHost pilote les 7 écrans + la route /formula (deep link paramétré).
 */
@Composable
fun LyloNavGraph(
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel,
    authViewModel: AuthViewModel,
    liveKitManager: LiveKitSessionManager,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { LocalPrefs(context) }
    val languageViewModel: com.lylo.tablet.i18n.LanguageViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val currentLanguage by languageViewModel.locale.collectAsState()
    val user by authViewModel.user.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.error.collectAsState()
    val loginModalOpen by authViewModel.loginModalOpen.collectAsState()

    val sessionData by sessionViewModel.sessionData.collectAsState()
    // StateFlow (et non plus liveKitManager.room lu une fois) : sans ça, cette valeur restait
    // figée à null pour toute la durée de vie de l'app car un simple `var` ne déclenche aucune
    // recomposition Compose quand connect() l'assigne après le premier rendu — l'avatar vidéo
    // LiveKit ne s'affichait donc jamais, retombant systématiquement sur l'image statique.
    val room by liveKitManager.roomState.collectAsState()
    val micMuted by liveKitManager.micMuted.collectAsState()

    LaunchedEffect(sessionData) {
        val data = sessionData
        if (data != null) {
            liveKitManager.connect(data)
        } else {
            liveKitManager.disconnect()
        }
    }

    // Portage de recommendations/page.tsx et interaction/page.tsx : avatar-h.jpg (Florian) si
    // persona === "male", avatar-f.jpg (Rose) sinon.
    val persona = remember { prefs.getString(LocalPrefs.KEY_PERSONA) }
    val avatarFallback = if (persona == "male") {
        com.lylo.tablet.R.drawable.avatar_h
    } else {
        com.lylo.tablet.R.drawable.avatar_f
    }
    val avatarEnabled = remember { prefs.getString(LocalPrefs.KEY_AVATAR) != "false" }

    CompositionLocalProvider(LocalMicMuted provides micMuted) {
    NavHost(navController = navController, startDestination = LyloDestinations.HOME) {

        composable(LyloDestinations.HOME) {
            HomeScreen(
                user = user,
                currentLanguage = currentLanguage,
                onLanguageSelect = languageViewModel::setLocale,
                onLoginClick = authViewModel::openLoginModal,
                onLogoutClick = authViewModel::logout,
                onGetStartedClick = { navController.navigate(LyloDestinations.CONFIGURE) },
                onEnterSessionCleanup = { if (sessionData != null) sessionViewModel.endSession() },
                onFormulasClick = { navController.navigate(LyloDestinations.FORMULAS) },
                onRobotSettingsClick = { navController.navigate(LyloDestinations.ROBOT_SETTINGS) },
            )
        }

        composable(LyloDestinations.FORMULAS) {
            FormulasScreen(
                user = user,
                onNavigateHome = {
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(LyloDestinations.ROBOT_SETTINGS) {
            RobotSettingsScreen(
                user = user,
                onNavigateHome = {
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(LyloDestinations.CONFIGURE) {
            ConfigureScreen(
                onNavigateBack = { navController.popBackStack() },
                onContinueToQuiz = { navController.navigate(LyloDestinations.QUIZ) },
                onContinueToPreparation = { navController.navigate(LyloDestinations.PREPARATION) },
                onNavigateHome = {
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
                currentLanguage = currentLanguage,
                onLanguageSelect = languageViewModel::setLocale,
            )
        }

        composable(LyloDestinations.QUIZ) {
            QuizScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResults = {
                    navController.navigate(LyloDestinations.QUIZ_RESULTS) {
                        popUpTo(LyloDestinations.QUIZ) { inclusive = true }
                    }
                },
                onNavigateHome = {
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(LyloDestinations.QUIZ_RESULTS) {
            QuizResultsScreen(
                onReturnHome = {
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
                onNavigateHome = {
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
                onMissingResults = {
                    navController.navigate(LyloDestinations.QUIZ) {
                        popUpTo(LyloDestinations.QUIZ_RESULTS) { inclusive = true }
                    }
                },
                webAppBaseUrl = WEB_APP_BASE_URL,
            )
        }

        composable(LyloDestinations.PREPARATION) {
            PreparationScreen(
                sessionViewModel = sessionViewModel,
                room = room,
                avatarFallback = avatarFallback,
                onNavigateToInteraction = {
                    navController.navigate(LyloDestinations.INTERACTION) {
                        popUpTo(LyloDestinations.PREPARATION) { inclusive = true }
                    }
                },
                onNavigateToRecommendations = {
                    navController.navigate(LyloDestinations.RECOMMENDATIONS) {
                        popUpTo(LyloDestinations.PREPARATION) { inclusive = true }
                    }
                },
                onNavigateBackToConfigure = { navController.popBackStack() },
                onNavigateHome = {
                    sessionViewModel.endSession()
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
                onStartSessionIfNeeded = { sessionViewModel.startSession() },
                onInterrupt = { liveKitManager.sendInterrupt() },
                onResumeListen = { liveKitManager.sendResumeListen() },
                onResume = { liveKitManager.sendResume() },
                onMicToggle = { muted -> liveKitManager.setMicrophoneMuted(muted) },
            )
        }

        composable(LyloDestinations.INTERACTION) {
            InteractionScreen(
                sessionViewModel = sessionViewModel,
                room = room,
                avatarFallback = avatarFallback,
                avatarEnabled = avatarEnabled,
                onNavigateToRecommendations = {
                    navController.navigate(LyloDestinations.RECOMMENDATIONS) {
                        popUpTo(LyloDestinations.INTERACTION) { inclusive = true }
                    }
                },
                onNavigateHome = {
                    sessionViewModel.endSession()
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
                onInterrupt = { liveKitManager.sendInterrupt() },
                onResumeListen = { liveKitManager.sendResumeListen() },
                onResume = { liveKitManager.sendResume() },
                onMicToggle = { muted -> liveKitManager.setMicrophoneMuted(muted) },
            )
        }

        composable(LyloDestinations.RECOMMENDATIONS) {
            RecommendationsScreen(
                sessionViewModel = sessionViewModel,
                room = room,
                avatarFallback = avatarFallback,
                avatarEnabled = avatarEnabled,
                webAppBaseUrl = WEB_APP_BASE_URL,
                onReturnHome = {
                    sessionViewModel.endSession()
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
                onInterrupt = { liveKitManager.sendInterrupt() },
                onResumeListen = { liveKitManager.sendResumeListen() },
                onResume = { liveKitManager.sendResume() },
                onMicToggle = { muted -> liveKitManager.setMicrophoneMuted(muted) },
            )
        }

        composable(
            route = LyloDestinations.FORMULA,
            arguments = listOf(
                navArgument("data") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lang") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val data = backStackEntry.arguments?.getString("data")
            val lang = backStackEntry.arguments?.getString("lang")
            LaunchedEffect(lang) {
                com.lylo.tablet.i18n.AppLanguage.fromCode(lang)?.let { languageViewModel.setLocale(it) }
            }
            FormulaScreen(
                encodedData = data,
                onNavigateHome = {
                    navController.navigate(LyloDestinations.HOME) {
                        popUpTo(LyloDestinations.HOME) { inclusive = true }
                    }
                },
            )
        }
    }

    }

    com.lylo.tablet.ui.components.LoginModal(
        open = loginModalOpen,
        isLoading = isLoading,
        error = authError,
        onLoginWithEmail = { email, onResult -> authViewModel.login(email, onResult) },
        onDismiss = authViewModel::closeLoginModal,
    )
}
