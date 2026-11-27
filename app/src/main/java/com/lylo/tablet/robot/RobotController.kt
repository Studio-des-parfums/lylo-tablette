package com.lylo.tablet.robot

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.ainirobot.coreservice.client.ApiListener
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.listener.ActionListener
import com.ainirobot.coreservice.client.listener.CommandListener
import com.ainirobot.coreservice.client.module.ModuleCallbackApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.text.Normalizer

private const val TAG = "RobotController"
private const val NAVIGATION_REQUEST_ID = 2001
private const val PLACE_LIST_REQUEST_ID = 2002
private const val COORDINATE_DEVIATION_METERS = 0.2
private const val NAVIGATION_TIMEOUT_MS = 120_000L
private const val CHARGE_REQUEST_ID = 3
private const val CHARGE_TIMEOUT_MS = 300_000L
private const val FOCUS_REQUEST_ID = 4
/** Distance (m) en deçà de laquelle le robot se tourne vers la personne. */
private const val FOCUS_MAX_DISTANCE_M = 1.5f
/** Délai avant d'abandonner une personne sortie du champ, puis de chercher la suivante. */
private const val FOCUS_LOST_TIMEOUT_MS = 5_000L
private const val FOCUS_RESTART_DELAY_MS = 1_500L
// Statut observé sur le robot quand le suivi accroche une personne (« Track target succeed »).
// STATUS_GUEST_APPEAR (1002) du SDK n'est jamais émis par SmartFocusFollow.
private const val STATUS_TRACK_TARGET_SUCCEED = 1013

/**
 * Pont vers le SDK OrionStar (robotservice.jar), repris de nono-le-robot.
 * Sur une tablette sans services robot, la connexion échoue silencieusement :
 * [available] reste à false et l'UI masque les actions robot.
 */
object RobotController {
    private val robotApi: RobotApi by lazy { RobotApi.getInstance() }
    private var callbackThread: HandlerThread? = null
    @Volatile private var connecting = false
    @Volatile private var connected = false

    // ── Suivi du regard (SmartFocusFollow) ──
    @Volatile private var focusWanted = false
    @Volatile private var focusRunning = false
    @Volatile private var chargeReturning = false
    private var focusHandler: Handler? = null
    private val focusScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _navigation = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navigation: StateFlow<NavigationState> = _navigation.asStateFlow()

    /**
     * Chaque départ / arrivée / échec, sans perte : contrairement à [navigation] (StateFlow qui
     * ne garde que la dernière valeur, remise à Idle dès le toast affiché), utilisé pour
     * prévenir l'agent vocal (SessionViewModel).
     */
    private val _navigationEvents = MutableSharedFlow<NavigationState>(extraBufferCapacity = 8)
    val navigationEvents: SharedFlow<NavigationState> = _navigationEvents.asSharedFlow()

    private var navigationValue: NavigationState
        get() = _navigation.value
        set(value) {
            _navigation.value = value
            if (value !is NavigationState.Idle) _navigationEvents.tryEmit(value)
        }

    sealed interface NavigationState {
        data object Idle : NavigationState
        data class Moving(val goal: String) : NavigationState
        data class Arrived(val goal: String) : NavigationState
        data class Failed(val goal: String, val message: String) : NavigationState
    }

    private val moduleCallback = object : ModuleCallbackApi() {
        override fun onSendRequest(reqId: Int, reqType: String?, reqText: String?, reqParam: String?): Boolean = true
        override fun onHWReport(hwFunction: Int, cmdType: String?, hwReport: String?) {}
        override fun onSuspend() { _available.value = false }
        override fun onRecovery() { _available.value = true; tryStartFocus() }
    }

    /** Idempotent : appelé au démarrage de l'app et à chaque retour au premier plan (comme nono). */
    fun connect(context: Context) {
        if (connected || connecting) return
        connecting = true
        runCatching {
            val thread = callbackThread ?: HandlerThread("RobotSdkCallbacks").also {
                it.start()
                callbackThread = it
            }
            robotApi.connectServer(context.applicationContext, object : ApiListener {
                override fun handleApiConnected() {
                    Log.d(TAG, "Robot API connected")
                    connecting = false
                    connected = true
                    robotApi.setCallback(moduleCallback)
                    robotApi.setResponseThread(thread)
                    _available.value = true
                    focusHandler = Handler(thread.looper)
                    tryStartFocus()
                }

                override fun handleApiDisabled() {
                    Log.w(TAG, "Robot API disabled")
                    connecting = false
                    _available.value = false
                }

                override fun handleApiDisconnected() {
                    Log.w(TAG, "Robot API disconnected")
                    focusRunning = false
                    connecting = false
                    connected = false
                    _available.value = false
                }
            })
        }.onFailure {
            // Tablette classique : pas de services robot installés.
            Log.w(TAG, "Robot SDK unavailable, robot features disabled", it)
            connecting = false
            _available.value = false
        }
    }

    init {
        // Pas de suivi du regard pendant un déplacement : la navigation pilote les moteurs.
        focusScope.launch {
            _navigation.collect { state ->
                if (state is NavigationState.Moving) pauseFocus() else tryStartFocus()
            }
        }
    }

    /**
     * Suivi du regard : le robot se tourne vers la personne la plus proche (≤ [FOCUS_MAX_DISTANCE_M])
     * et la suit. Actif tant que l'app est au premier plan, sauf pendant les déplacements
     * (« Emmène-moi », retour à la recharge). Relancé automatiquement quand la personne est perdue.
     */
    fun setFocusEnabled(enabled: Boolean) {
        focusWanted = enabled
        if (enabled) tryStartFocus() else pauseFocus()
    }

    private fun tryStartFocus() {
        if (!focusWanted || focusRunning || chargeReturning || !_available.value) return
        if (_navigation.value is NavigationState.Moving) return
        focusRunning = true
        runCatching {
            val code = robotApi.startSmartFocusFollow(
                FOCUS_REQUEST_ID,
                FOCUS_LOST_TIMEOUT_MS,
                FOCUS_MAX_DISTANCE_M,
                object : ActionListener() {
                    override fun onResult(status: Int, responseString: String?, extraData: String?) {
                        Log.d(TAG, "Focus follow ended status=$status response=$responseString")
                        onFocusEnded()
                    }

                    @Deprecated("Deprecated in robot SDK")
                    override fun onError(errorCode: Int, errorString: String?) {
                        Log.d(TAG, "Focus follow error code=$errorCode message=$errorString")
                        onFocusEnded()
                    }

                    @Deprecated("Deprecated in robot SDK")
                    override fun onStatusUpdate(status: Int, data: String?) {
                        Log.d(TAG, "Focus follow status=$status data=$data")
                        // Une personne entre dans le champ : message d'accueil (si page d'accueil).
                        if (status == STATUS_TRACK_TARGET_SUCCEED) GreetingPlayer.onPersonDetected()
                    }
                },
            )
            Log.d(TAG, "Focus follow start code=$code")
            if (code < 0) onFocusEnded()
        }.onFailure {
            Log.w(TAG, "Unable to start focus follow", it)
            focusRunning = false
        }
    }

    /** Personne perdue (ou refus) : on attend un peu puis on cherche la suivante. */
    private fun onFocusEnded() {
        focusRunning = false
        focusHandler?.postDelayed({ tryStartFocus() }, FOCUS_RESTART_DELAY_MS)
    }

    private fun pauseFocus() {
        focusHandler?.removeCallbacksAndMessages(null)
        if (!focusRunning) return
        focusRunning = false
        runCatching { robotApi.stopSmartFocusFollow(FOCUS_REQUEST_ID) }
            .onFailure { Log.w(TAG, "stopSmartFocusFollow failed", it) }
    }

    /** Points enregistrés sur la carte active du robot. */
    fun fetchPlaces(onResult: (places: List<String>, error: String?) -> Unit) {
        if (!_available.value) {
            onResult(emptyList(), "Robot non connecté.")
            return
        }
        runCatching {
            robotApi.getPlaceList(PLACE_LIST_REQUEST_ID, object : CommandListener() {
                override fun onResult(status: Int, message: String?, extraData: String?) {
                    onResult(parsePlaceNames(message), null)
                }

                @Deprecated("Deprecated in robot SDK")
                override fun onError(errorCode: Int, errorString: String?) {
                    Log.w(TAG, "getPlaceList failed code=$errorCode message=$errorString")
                    onResult(emptyList(), errorString ?: "Impossible de récupérer les points du robot ($errorCode).")
                }
            })
        }.onFailure {
            onResult(emptyList(), it.message ?: "Impossible de récupérer les points du robot.")
        }
    }

    /**
     * Envoie le robot vers un point de la carte. Contrairement à nono, on ne retente pas
     * avec le nom brut si la liste des points échoue : l'erreur est remontée telle quelle.
     */
    fun navigateTo(goal: String) {
        if (_navigation.value is NavigationState.Moving) return
        if (!_available.value) {
            navigationValue = NavigationState.Failed(goal, "Le contrôle du robot n'est pas disponible.")
            return
        }
        navigationValue = NavigationState.Moving(goal)

        fetchPlaces { places, error ->
            val resolved = when {
                error != null -> null
                else -> places.firstOrNull { it == goal }
                    ?: places.firstOrNull { normalize(it) == normalize(goal) }
            }
            if (resolved == null) {
                navigationValue = NavigationState.Failed(
                    goal,
                    error ?: "Le point \"$goal\" est introuvable sur la carte active.",
                )
                return@fetchPlaces
            }
            startNavigation(resolved)
        }
    }

    /**
     * Fin de parcours (« Terminer ») : renvoie le robot à sa station de recharge et l'y amarre.
     * startNaviToAutoChargeAction vise la station enregistrée par le robot lui-même : aucune
     * dépendance au nom du point (« Point de recharge » varie selon la langue de la carte).
     * Fire-and-forget : l'écran retourne à l'accueil sans attendre l'arrivée. Renvoie false si
     * l'ordre n'a pas pu être envoyé.
     */
    fun returnToCharge(): Boolean {
        if (!_available.value) return false
        if (_navigation.value is NavigationState.Moving) stopNavigation()
        chargeReturning = true
        pauseFocus()
        @Suppress("DEPRECATION")
        return runCatching {
            val code = robotApi.startNaviToAutoChargeAction(
                CHARGE_REQUEST_ID,
                CHARGE_TIMEOUT_MS,
                object : ActionListener() {
                    override fun onResult(status: Int, responseString: String?, extraData: String?) {
                        Log.d(TAG, "Return to charge result status=$status response=$responseString")
                        chargeReturning = false
                        tryStartFocus()
                    }

                    @Deprecated("Deprecated in robot SDK")
                    override fun onError(errorCode: Int, errorString: String?) {
                        Log.w(TAG, "Return to charge error code=$errorCode message=$errorString")
                        chargeReturning = false
                        tryStartFocus()
                    }
                },
            )
            if (code < 0) {
                Log.w(TAG, "Return to charge refused code=$code")
                chargeReturning = false
                tryStartFocus()
            }
            code >= 0
        }.onFailure { Log.e(TAG, "Unable to return to charge", it) }
            .getOrDefault(false)
    }

    fun stopNavigation() {
        @Suppress("DEPRECATION")
        runCatching { robotApi.stopNavigation(NAVIGATION_REQUEST_ID) }
            .onFailure { Log.w(TAG, "stopNavigation failed", it) }
        navigationValue = NavigationState.Idle
    }

    /** Remet l'état à Idle une fois le résultat affiché. */
    fun clearNavigationResult() {
        if (_navigation.value !is NavigationState.Moving) navigationValue = NavigationState.Idle
    }

    private fun startNavigation(goal: String) {
        @Suppress("DEPRECATION")
        runCatching {
            val code = robotApi.startNavigation(
                NAVIGATION_REQUEST_ID,
                goal,
                COORDINATE_DEVIATION_METERS,
                NAVIGATION_TIMEOUT_MS,
                object : ActionListener() {
                    override fun onResult(status: Int, responseString: String?, extraData: String?) {
                        Log.d(TAG, "Navigation result goal=$goal status=$status response=$responseString")
                        navigationValue = if (responseString == "true") {
                            NavigationState.Arrived(goal)
                        } else {
                            NavigationState.Failed(goal, responseString ?: extraData ?: "Navigation terminée sans succès.")
                        }
                    }

                    @Deprecated("Deprecated in robot SDK")
                    override fun onError(errorCode: Int, errorString: String?) {
                        Log.w(TAG, "Navigation error goal=$goal code=$errorCode message=$errorString")
                        navigationValue = NavigationState.Failed(goal, errorString ?: errorMessage(goal, errorCode))
                    }

                    @Deprecated("Deprecated in robot SDK")
                    override fun onStatusUpdate(status: Int, data: String?) {
                        Log.d(TAG, "Navigation status goal=$goal status=$status data=$data")
                    }
                },
            )
            if (code < 0) {
                navigationValue = NavigationState.Failed(goal, errorMessage(goal, code))
            }
        }.onFailure {
            Log.e(TAG, "Unable to start navigation goal=$goal", it)
            navigationValue = NavigationState.Failed(goal, it.message ?: "Impossible de lancer la navigation.")
        }
    }

    private fun parsePlaceNames(message: String?): List<String> {
        if (message.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(message)
            (0 until array.length()).mapNotNull { i ->
                array.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
            }
        }.onFailure { Log.w(TAG, "Unable to parse place list: $message", it) }
            .getOrDefault(emptyList())
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim().lowercase().replace("’", "'"), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("\\s+".toRegex(), " ")

    private fun errorMessage(goal: String, errorCode: Int): String = when (errorCode) {
        -10013 -> "Le robot n'est pas localisé sur la carte."
        -10014 -> "Le point \"$goal\" n'existe pas sur la carte active."
        else -> "Erreur de navigation ($errorCode) vers \"$goal\"."
    }
}
