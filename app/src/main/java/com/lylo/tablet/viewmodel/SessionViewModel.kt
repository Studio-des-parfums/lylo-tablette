package com.lylo.tablet.viewmodel

import com.lylo.tablet.robot.RobotController
import com.lylo.tablet.robot.RobotDestination
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lylo.tablet.BuildConfig
import com.lylo.tablet.data.LocalPrefs
import com.lylo.tablet.data.Brand
import com.lylo.tablet.data.MockData
import com.lylo.tablet.data.model.AnswerFormat
import com.lylo.tablet.data.model.AgentState
import com.lylo.tablet.data.model.AnswerSaved
import com.lylo.tablet.data.model.ConfirmationData
import com.lylo.tablet.data.model.Formula
import com.lylo.tablet.data.model.PendingClickAnswer
import com.lylo.tablet.data.model.Question
import com.lylo.tablet.data.model.SessionData
import com.lylo.tablet.data.model.SessionState
import com.lylo.tablet.data.model.TranscriptMessage
import com.lylo.tablet.data.model.TranscriptSender
import com.lylo.tablet.data.remote.ApiClient
import com.lylo.tablet.data.remote.StartSessionRequest
import com.lylo.tablet.i18n.LanguageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Portage de src/context/SessionContext.tsx. Centralise tout l'état de la session de
 * consultation IA — piloté à la fois par les appels API REST (démarrage) et par les événements
 * reçus sur le data channel LiveKit "state" (voir livekit/LiveKitSessionManager.kt qui appelle
 * [handleDataMessage]) et le control channel "control" (voir [submitClickAnswer]).
 */
class SessionViewModel(application: Application) : AndroidViewModel(application) {

    val devMode: Boolean = BuildConfig.DEV_MODE
    private val prefs = LocalPrefs(application)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _sessionData = MutableStateFlow<SessionData?>(null)
    val sessionData: StateFlow<SessionData?> = _sessionData.asStateFlow()

    private val _sessionState = MutableStateFlow(if (devMode) SessionState.COMPLETED else SessionState.IDLE)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _agentState = MutableStateFlow(AgentState.INITIALIZING)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private val _profile = MutableStateFlow<Map<String, String>>(emptyMap())
    val profile: StateFlow<Map<String, String>> = _profile.asStateFlow()

    private val _missingFields = MutableStateFlow<List<String>>(emptyList())
    val missingFields: StateFlow<List<String>> = _missingFields.asStateFlow()

    private val _answers = MutableStateFlow<List<AnswerSaved>>(emptyList())
    val answers: StateFlow<List<AnswerSaved>> = _answers.asStateFlow()

    private val _formulas = MutableStateFlow(if (devMode) MockData.formulas else emptyList())
    val formulas: StateFlow<List<Formula>> = _formulas.asStateFlow()

    // Référence en base de la formule sélectionnée (mode vocal) — remplie par select_formula côté
    // agent (événement formula_selected), utilisée pour l'envoi par mail à la demande.
    private val _formulaReference = MutableStateFlow<String?>(null)
    val formulaReference: StateFlow<String?> = _formulaReference.asStateFlow()

    // ── Accompagnement par le robot piloté par l'agent vocal ──
    // Messages JSON à envoyer à l'agent sur le control channel (LiveKitSessionManager).
    private val _guideControlMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val guideControlMessages: SharedFlow<String> = _guideControlMessages.asSharedFlow()
    /** Session démarrée avec la capacité d'accompagnement (robot connecté + lieu configuré). */
    @Volatile private var guideEnabledForSession = false
    /** Trajet demandé par l'agent (robot_navigate) : inutile de lui annoncer le départ. */
    @Volatile private var navigationRequestedByAgent = false

    init {
        // Départ manuel (bouton Emmène-moi), arrivée ou échec : on prévient l'agent.
        viewModelScope.launch {
            RobotController.navigationEvents.collect { event ->
                if (!guideEnabledForSession || _sessionData.value == null) return@collect
                val payload = when (event) {
                    is RobotController.NavigationState.Moving ->
                        if (navigationRequestedByAgent) null else """{"type":"robot_navigation_started"}"""
                    is RobotController.NavigationState.Arrived -> """{"type":"robot_arrived"}"""
                    is RobotController.NavigationState.Failed ->
                        buildJsonObject { put("type", "robot_failed"); put("message", event.message) }.toString()
                    RobotController.NavigationState.Idle -> null
                }
                if (event is RobotController.NavigationState.Arrived || event is RobotController.NavigationState.Failed) {
                    navigationRequestedByAgent = false
                }
                payload?.let { _guideControlMessages.tryEmit(it) }
            }
        }
    }

    /** Format de réponse du questionnaire de la session (nb de choix, cadrage). */
    private val _answerFormat = MutableStateFlow(AnswerFormat.DEFAULT)
    val answerFormat: StateFlow<AnswerFormat> = _answerFormat.asStateFlow()

    private val _pendingPerfumeName = MutableStateFlow<String?>(null)
    val pendingPerfumeName: StateFlow<String?> = _pendingPerfumeName.asStateFlow()

    private val _transcripts = MutableStateFlow<List<TranscriptMessage>>(emptyList())
    val transcripts: StateFlow<List<TranscriptMessage>> = _transcripts.asStateFlow()

    private val _questions = MutableStateFlow(if (devMode) MockData.questions else emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _questionCount = MutableStateFlow(if (devMode) MockData.questions.size else 0)
    val questionCount: StateFlow<Int> = _questionCount.asStateFlow()

    private val _agentName = MutableStateFlow("Rose")
    val agentName: StateFlow<String> = _agentName.asStateFlow()

    private val _hiddenChoices = MutableStateFlow<List<String>>(emptyList())
    val hiddenChoices: StateFlow<List<String>> = _hiddenChoices.asStateFlow()

    private val _requestingEmail = MutableStateFlow(false)
    val requestingEmail: StateFlow<Boolean> = _requestingEmail.asStateFlow()

    private val _noCreditsError = MutableStateFlow(false)
    val noCreditsError: StateFlow<Boolean> = _noCreditsError.asStateFlow()

    private val _connectionError = MutableStateFlow(false)
    val connectionError: StateFlow<Boolean> = _connectionError.asStateFlow()

    private val _clickSelectionMode = MutableStateFlow<String?>(null) // "top_2" | "bottom_2" | null
    val clickSelectionMode: StateFlow<String?> = _clickSelectionMode.asStateFlow()

    private var clickQuestionId: Int? = null
    private var clickTop2Values: List<String> = emptyList()

    private val _pendingClickAnswer = MutableStateFlow<PendingClickAnswer?>(null)
    val pendingClickAnswer: StateFlow<PendingClickAnswer?> = _pendingClickAnswer.asStateFlow()

    private val _confirmationData = MutableStateFlow<ConfirmationData?>(null)
    val confirmationData: StateFlow<ConfirmationData?> = _confirmationData.asStateFlow()

    private val _avatarDisabled = MutableStateFlow(false)
    val avatarDisabled: StateFlow<Boolean> = _avatarDisabled.asStateFlow()

    private var isRetrying = false

    fun setConnectionError(value: Boolean) {
        _connectionError.value = value
    }

    fun setSessionState(state: SessionState) {
        _sessionState.value = state
    }

    /** Portage de startSession() — POST /api/session/start puis GET /api/session/{id}. */
    fun startSession() {
        if (devMode) return
        viewModelScope.launch {
            _sessionState.value = SessionState.CONNECTING

            val language = LanguageStore.resolveStoredLanguage(prefs)
            LanguageStore.persistLanguage(prefs, language)

            val voiceGender = prefs.getString(LocalPrefs.KEY_PERSONA) ?: "female"
            _agentName.value = if (voiceGender == "male") "Florian" else "Rose"

            val depth = prefs.getString(LocalPrefs.KEY_DEPTH) ?: "1"
            val questionCountValue = depth.toIntOrNull() ?: 1
            _questionCount.value = questionCountValue

            val mode = prefs.getString(LocalPrefs.KEY_MODE) ?: "guided"
            val inputMode = prefs.getString(LocalPrefs.KEY_INPUT_MODE) ?: "voice"
            val avatar = prefs.getString(LocalPrefs.KEY_AVATAR) != "false"
            // Compte connecté (client ou membre de l'équipe) : rattache la session à son
            // propriétaire et décompte le quota du client côté back. L'email du visiteur, lui,
            // est saisi à la fin (modale "Recevoir par mail").
            val ownerEmail = runCatching {
                prefs.getString(LocalPrefs.KEY_AUTH_USER)?.let {
                    json.parseToJsonElement(it).jsonObject["email"]?.jsonPrimitive?.content
                }
            }.getOrNull()

            // Accompagnement proposé par l'agent seulement si le robot peut réellement y aller.
            // L'agent annonce le NOM du lieu (« la salle de création »), pas le point de la carte.
            val destination = RobotDestination.END_OF_JOURNEY
            val guideDestination = destination.spokenLabel(language.code)
                .takeIf { destination.mapPoint(prefs) != null && RobotController.available.value }
            guideEnabledForSession = guideDestination != null
            navigationRequestedByAgent = false

            try {
                val res = ApiClient.service.startSession(
                    StartSessionRequest(
                        language = language.code,
                        voiceGender = if (voiceGender == "male") "male" else "female",
                        questionCount = questionCountValue,
                        mode = mode,
                        inputMode = inputMode,
                        brand = Brand.active.id,
                        ownerEmail = ownerEmail,
                        avatar = avatar,
                        guideDestination = guideDestination,
                    )
                )

                if (res.code() == 403) {
                    _noCreditsError.value = true
                    _sessionState.value = SessionState.IDLE
                    return@launch
                }

                val body = res.body()
                if (!res.isSuccessful || body == null) {
                    _connectionError.value = true
                    _sessionState.value = SessionState.IDLE
                    return@launch
                }

                _sessionData.value = SessionData(
                    sessionId = body.sessionId,
                    roomName = body.roomName,
                    token = body.token,
                    livekitUrl = body.livekitUrl,
                    identity = body.identity,
                )
                _sessionState.value = SessionState.COLLECTING_PROFILE

                val sessionInfo = ApiClient.service.getSession(body.sessionId).body()
                sessionInfo?.questions?.let {
                    _questions.value = it
                    _questionCount.value = it.size
                }
                _answerFormat.value = AnswerFormat.parse(sessionInfo?.answerFormat)
            } catch (e: Exception) {
                _connectionError.value = true
                _sessionState.value = SessionState.IDLE
            }
        }
    }

    /**
     * Portage de handleDataMessage() : parse un event JSON reçu sur le data channel "state"
     * de LiveKit et met à jour l'état correspondant. Appelé par LiveKitSessionManager.
     */
    fun handleDataMessage(payload: ByteArray) {
        try {
            val text = payload.toString(Charsets.UTF_8)
            val root = json.parseToJsonElement(text).jsonObject
            val type = root["type"]?.jsonPrimitive?.content ?: return

            when (type) {
                "profile_update" -> {
                    val field = root["field"]?.jsonPrimitive?.content ?: return
                    val value = root["value"]?.jsonPrimitive?.content ?: ""
                    _profile.value = _profile.value + (field to value)
                    _missingFields.value = root["missing_fields"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                }

                "state_change" -> {
                    val state = root["state"]?.jsonPrimitive?.content ?: return
                    _sessionState.value = SessionState.fromWire(state)
                }

                "top_2_selected" -> {
                    _hiddenChoices.value = root["top_2"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    // Le choix est acté (oral ou clic) — les cartes redeviennent non cliquables
                    // jusqu'à la prochaine étape qui les réactivera (step_asking_bottom_2).
                    _clickSelectionMode.value = null
                }

                "bottom_2_selected" -> {
                    _clickSelectionMode.value = null
                }

                "answer_saved" -> {
                    _hiddenChoices.value = emptyList()
                    val questionId = root["question_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return
                    val top2 = root["top_2"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val bottom2 = root["bottom_2"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val isNew = _answers.value.none { it.questionId == questionId }
                    if (isNew) _currentQuestionIndex.value += 1
                    _answers.value = _answers.value.filterNot { it.questionId == questionId } +
                        AnswerSaved(questionId, top2, bottom2)
                }

                "formulas_generated" -> {
                    _formulas.value = Formula.parseList(root["formulas"])
                    _sessionState.value = SessionState.COMPLETED
                }

                "robot_navigate" -> {
                    // L'agent a obtenu l'accord du visiteur : le robot part vers le lieu configuré.
                    val goal = RobotDestination.END_OF_JOURNEY.mapPoint(prefs)
                    if (guideEnabledForSession && !goal.isNullOrBlank()) {
                        navigationRequestedByAgent = true
                        RobotController.navigateTo(goal)
                    }
                }

                "formula_selected" -> {
                    root["formula"]?.let { Formula.parse(it) }?.let { _formulas.value = listOf(it) }
                    root["reference"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let { _formulaReference.value = it }
                    _sessionState.value = SessionState.CUSTOMIZATION
                }

                "formula_updated" -> {
                    root["formula"]?.let { Formula.parse(it) }?.let { _formulas.value = listOf(it) }
                }

                "agent_state" -> {
                    val state = root["state"]?.jsonPrimitive?.content ?: return
                    _agentState.value = AgentState.fromWire(state)
                }

                // Envoyés systématiquement (pas seulement en input_mode="click") — active les cartes
                // cliquables en plus de la réponse orale, voir ChoiceGrid. Le premier des deux (clic
                // ou réponse orale comprise par l'agent) qui arrive fait avancer la question.
                "step_asking_top_2" -> {
                    _clickSelectionMode.value = "top_2"
                    clickQuestionId = root["question_id"]?.jsonPrimitive?.content?.toIntOrNull()
                }

                "step_asking_bottom_2" -> {
                    _clickSelectionMode.value = "bottom_2"
                    clickQuestionId = root["question_id"]?.jsonPrimitive?.content?.toIntOrNull()
                }

                "step_awaiting_confirmation" -> {
                    val questionId = root["question_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val top2 = root["top_2"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val bottom2 = root["bottom_2"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    _confirmationData.value = ConfirmationData(questionId, top2, bottom2)
                    _sessionState.value = SessionState.AWAITING_CONFIRMATION
                }

                "step_asking_intensity" -> {
                    _sessionState.value = SessionState.ASKING_INTENSITY
                }

                "step_asking_perfume_name" -> {
                    _sessionState.value = SessionState.ASKING_PERFUME_NAME
                }

                "requesting_email" -> {
                    val requesting = root["requesting_email"]?.jsonPrimitive?.content?.toBoolean() ?: false
                    if (requesting) _requestingEmail.value = true
                }

                "avatar_disabled" -> {
                    _avatarDisabled.value = true
                }
            }
        } catch (_: Exception) {
            // Message malformé — ignoré, comme côté web (catch silencieux).
        }
    }

    fun upsertTranscript(msg: TranscriptMessage) {
        val idx = _transcripts.value.indexOfFirst { it.id == msg.id }
        _transcripts.value = if (idx >= 0) {
            _transcripts.value.toMutableList().also { it[idx] = it[idx].copy(text = msg.text) }
        } else {
            _transcripts.value + msg
        }
    }

    /**
     * Portage de submitClickAnswer() : construit le PendingClickAnswer que le control channel
     * LiveKit doit envoyer (voir LiveKitSessionManager.observePendingClickAnswer).
     */
    fun submitClickAnswer(values: List<String>) {
        when (_clickSelectionMode.value) {
            "top_2" -> {
                clickTop2Values = values
                _pendingClickAnswer.value = PendingClickAnswer(
                    type = "questionnaire_top_2",
                    questionId = clickQuestionId ?: return,
                    values = values,
                )
            }
            "bottom_2" -> {
                _pendingClickAnswer.value = PendingClickAnswer(
                    type = "questionnaire_bottom_2",
                    questionId = clickQuestionId ?: return,
                    values = values,
                    top2 = clickTop2Values,
                )
                clickTop2Values = emptyList()
            }
        }
        _clickSelectionMode.value = null
    }

    fun clearPendingClickAnswer() {
        _pendingClickAnswer.value = null
    }

    /** Portage de submitPerfumeName() : envoyé sur le control channel par LiveKitSessionManager. */
    fun submitPerfumeName(name: String) {
        _pendingPerfumeName.value = name
    }

    fun clearPendingPerfumeName() {
        _pendingPerfumeName.value = null
    }

    /** Remplace la formule sélectionnée affichée (ex. note remplacée au clic sur la pyramide). */
    fun replaceSelectedFormula(formula: Formula) {
        _formulas.value = listOf(formula)
    }

    /** Portage de endSession() : DELETE fire-and-forget + reset complet de l'état. */
    fun endSession() {
        isRetrying = false
        val sessionId = _sessionData.value?.sessionId
        if (sessionId != null) {
            viewModelScope.launch {
                runCatching { ApiClient.service.deleteSession(sessionId) }
            }
        }
        _sessionData.value = null
        _sessionState.value = SessionState.IDLE
        _profile.value = emptyMap()
        _missingFields.value = emptyList()
        _answers.value = emptyList()
        _formulas.value = emptyList()
        _formulaReference.value = null
        _transcripts.value = emptyList()
        _questions.value = emptyList()
        _currentQuestionIndex.value = 0
        _hiddenChoices.value = emptyList()
        _requestingEmail.value = false
        _connectionError.value = false
        _agentState.value = AgentState.INITIALIZING
        _clickSelectionMode.value = null
        clickQuestionId = null
        clickTop2Values = emptyList()
        _pendingClickAnswer.value = null
        _pendingPerfumeName.value = null
        _confirmationData.value = null
        guideEnabledForSession = false
        navigationRequestedByAgent = false
    }

    /** Portage de handleConnectionTimeout() : un seul retry, sinon abandon. */
    fun handleConnectionTimeout() {
        if (!isRetrying) {
            isRetrying = true
            endSession()
            isRetrying = true // endSession() remet à false, on remet à true comme côté web
            startSession()
        } else {
            isRetrying = false
            _connectionError.value = true
            endSession()
        }
    }
}
