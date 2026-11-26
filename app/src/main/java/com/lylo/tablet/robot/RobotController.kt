package com.lylo.tablet.robot

import android.content.Context
import android.os.HandlerThread
import android.util.Log
import com.ainirobot.coreservice.client.ApiListener
import com.ainirobot.coreservice.client.RobotApi
import com.ainirobot.coreservice.client.listener.ActionListener
import com.ainirobot.coreservice.client.listener.CommandListener
import com.ainirobot.coreservice.client.module.ModuleCallbackApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.text.Normalizer

private const val TAG = "RobotController"
private const val NAVIGATION_REQUEST_ID = 2001
private const val PLACE_LIST_REQUEST_ID = 2002
private const val COORDINATE_DEVIATION_METERS = 0.2
private const val NAVIGATION_TIMEOUT_MS = 120_000L

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

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private val _navigation = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navigation: StateFlow<NavigationState> = _navigation.asStateFlow()

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
        override fun onRecovery() { _available.value = true }
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
                }

                override fun handleApiDisabled() {
                    Log.w(TAG, "Robot API disabled")
                    connecting = false
                    _available.value = false
                }

                override fun handleApiDisconnected() {
                    Log.w(TAG, "Robot API disconnected")
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
            _navigation.value = NavigationState.Failed(goal, "Le contrôle du robot n'est pas disponible.")
            return
        }
        _navigation.value = NavigationState.Moving(goal)

        fetchPlaces { places, error ->
            val resolved = when {
                error != null -> null
                else -> places.firstOrNull { it == goal }
                    ?: places.firstOrNull { normalize(it) == normalize(goal) }
            }
            if (resolved == null) {
                _navigation.value = NavigationState.Failed(
                    goal,
                    error ?: "Le point \"$goal\" est introuvable sur la carte active.",
                )
                return@fetchPlaces
            }
            startNavigation(resolved)
        }
    }

    fun stopNavigation() {
        @Suppress("DEPRECATION")
        runCatching { robotApi.stopNavigation(NAVIGATION_REQUEST_ID) }
            .onFailure { Log.w(TAG, "stopNavigation failed", it) }
        _navigation.value = NavigationState.Idle
    }

    /** Remet l'état à Idle une fois le résultat affiché. */
    fun clearNavigationResult() {
        if (_navigation.value !is NavigationState.Moving) _navigation.value = NavigationState.Idle
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
                        _navigation.value = if (responseString == "true") {
                            NavigationState.Arrived(goal)
                        } else {
                            NavigationState.Failed(goal, responseString ?: extraData ?: "Navigation terminée sans succès.")
                        }
                    }

                    @Deprecated("Deprecated in robot SDK")
                    override fun onError(errorCode: Int, errorString: String?) {
                        Log.w(TAG, "Navigation error goal=$goal code=$errorCode message=$errorString")
                        _navigation.value = NavigationState.Failed(goal, errorString ?: errorMessage(goal, errorCode))
                    }

                    @Deprecated("Deprecated in robot SDK")
                    override fun onStatusUpdate(status: Int, data: String?) {
                        Log.d(TAG, "Navigation status goal=$goal status=$status data=$data")
                    }
                },
            )
            if (code < 0) {
                _navigation.value = NavigationState.Failed(goal, errorMessage(goal, code))
            }
        }.onFailure {
            Log.e(TAG, "Unable to start navigation goal=$goal", it)
            _navigation.value = NavigationState.Failed(goal, it.message ?: "Impossible de lancer la navigation.")
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
