package com.lylo.tablet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lylo.tablet.data.Brand
import com.lylo.tablet.data.LocalPrefs
import com.lylo.tablet.data.model.AnswerFormat
import com.lylo.tablet.data.model.FormulaType
import com.lylo.tablet.data.model.Gender
import com.lylo.tablet.data.model.ParticipantQuizState
import com.lylo.tablet.data.model.ProfileStep
import com.lylo.tablet.data.model.Question
import com.lylo.tablet.data.model.QuizAnswer
import com.lylo.tablet.data.model.QuizPhase
import com.lylo.tablet.data.model.QuizProfile
import com.lylo.tablet.data.model.QuizStep
import com.lylo.tablet.data.remote.ApiClient
import com.lylo.tablet.data.remote.GenerateFormulasRequest
import com.lylo.tablet.data.remote.GenerateMultiFormulasRequest
import com.lylo.tablet.data.remote.MultiParticipantPayload
import com.lylo.tablet.i18n.LanguageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/**
 * Portage complet de src/app/quiz/page.tsx (592 lignes). Le composant React tenait tout son
 * état en useState locaux — on le reproduit fidèlement ici en StateFlow pour piloter QuizScreen,
 * en respectant exactement la même machine à états : profile (solo ou multi-participant, étape
 * par étape : gender → age → [pregnant] → dislikes → [dislikes_detail] → allergies →
 * [allergies_detail]) puis questionnaire (top2 → bottom2 → confirm, avec bascule automatique de
 * participant en mode multi), puis intensité et nom du parfum (une étape par personne en duo).
 */
class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = LocalPrefs(application)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val language = LanguageStore.resolveStoredLanguage(prefs).also { LanguageStore.persistLanguage(prefs, it) }
    val questionCount: Int = (prefs.getString(LocalPrefs.KEY_DEPTH) ?: "12").toIntOrNull() ?: 12

    private val participantColors: List<String> = run {
        val raw = prefs.getString(LocalPrefs.KEY_PARTICIPANT_COLORS)
        if (raw.isNullOrBlank()) emptyList()
        else runCatching {
            raw.trim('[', ']').split(",").map { it.trim().trim('"') }.filter { it.isNotEmpty() }
        }.getOrElse { emptyList() }
    }
    val isMulti: Boolean = participantColors.size > 1

    // ── État commun ──────────────────────────────────────────────────────
    private val _step = MutableStateFlow(QuizStep.PROFILE)
    val step: StateFlow<QuizStep> = _step.asStateFlow()

    private val _profileStep = MutableStateFlow(ProfileStep.GENDER)
    val profileStep: StateFlow<ProfileStep> = _profileStep.asStateFlow()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _phase = MutableStateFlow(QuizPhase.TOP2)
    val phase: StateFlow<QuizPhase> = _phase.asStateFlow()

    private val _top2 = MutableStateFlow<List<String>>(emptyList())
    val top2: StateFlow<List<String>> = _top2.asStateFlow()

    private val _bottom2 = MutableStateFlow<List<String>>(emptyList())
    val bottom2: StateFlow<List<String>> = _bottom2.asStateFlow()

    // Format du questionnaire actif : nb de choix « le plus » / « le moins », cadrage
    private val _answerFormat = MutableStateFlow(AnswerFormat.DEFAULT)
    val answerFormat: StateFlow<AnswerFormat> = _answerFormat.asStateFlow()

    // ── Mode solo ────────────────────────────────────────────────────────
    private val _profile = MutableStateFlow(QuizProfile())
    val profile: StateFlow<QuizProfile> = _profile.asStateFlow()

    private val _answers = MutableStateFlow<List<QuizAnswer>>(emptyList())
    val answers: StateFlow<List<QuizAnswer>> = _answers.asStateFlow()

    private val _formulaType = MutableStateFlow<FormulaType?>(null)
    val formulaType: StateFlow<FormulaType?> = _formulaType.asStateFlow()

    private var perfumeName: String = ""

    // ── Mode multi ───────────────────────────────────────────────────────
    private val _participants = MutableStateFlow(
        participantColors.map { ParticipantQuizState(color = it) },
    )
    val participants: StateFlow<List<ParticipantQuizState>> = _participants.asStateFlow()

    private val _currentParticipantIndex = MutableStateFlow(0)
    val currentParticipantIndex: StateFlow<Int> = _currentParticipantIndex.asStateFlow()

    private val _multiProfileStep = MutableStateFlow(ProfileStep.GENDER)
    val multiProfileStep: StateFlow<ProfileStep> = _multiProfileStep.asStateFlow()

    // ── Résultat final ───────────────────────────────────────────────────
    var navigateToResults: (() -> Unit)? = null

    val currentParticipant: ParticipantQuizState? get() = _participants.value.getOrNull(_currentParticipantIndex.value)
    val currentColor: String? get() = if (isMulti) participantColors.getOrNull(_currentParticipantIndex.value) else null

    // ════════════════════════════════════════════════════════════════════
    // Chargement des questions (GET /api/questions)
    // ════════════════════════════════════════════════════════════════════
    fun loadQuestionsIfNeeded() {
        if (_step.value != QuizStep.QUESTIONNAIRE || _questions.value.isNotEmpty()) return
        _loading.value = true
        viewModelScope.launch {
            try {
                val res = ApiClient.service.getQuestions(questionCount, language.code, Brand.active.id)
                if (res.isSuccessful) {
                    val body = res.body()
                    // Portage de quiz/page.tsx : Array.isArray(data) ? data : data.questions ?? [].
                    // Le backend renvoie tantôt un tableau brut, tantôt {"group_id":...,"questions":[...]}.
                    val array = when {
                        body is kotlinx.serialization.json.JsonArray -> body
                        body is kotlinx.serialization.json.JsonObject -> body["questions"] as? kotlinx.serialization.json.JsonArray
                        else -> null
                    }
                    _questions.value = if (array != null) {
                        json.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(Question.serializer()), array)
                    } else {
                        emptyList()
                    }
                    _answerFormat.value = AnswerFormat.parse((body as? kotlinx.serialization.json.JsonObject)?.get("answer_format"))
                } else {
                    _error.value = errorLoadingQuestions
                }
            } catch (e: Exception) {
                _error.value = e.message ?: errorLoadingQuestions
            } finally {
                _loading.value = false
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // PROFIL SOLO — advanceProfile()
    // ════════════════════════════════════════════════════════════════════
    /** Met à jour le profil solo sans avancer d'étape — utilisé par les champs texte (âge, allergies). */
    fun updateProfile(updates: QuizProfile.() -> QuizProfile) {
        _profile.value = _profile.value.updates()
    }

    fun advanceProfile(updates: QuizProfile.() -> QuizProfile) {
        val next = _profile.value.updates()
        _profile.value = next
        when (_profileStep.value) {
            ProfileStep.GENDER -> _profileStep.value = ProfileStep.AGE
            ProfileStep.AGE -> _profileStep.value = if (next.gender == Gender.FEMME) ProfileStep.PREGNANT else ProfileStep.DISLIKES
            ProfileStep.PREGNANT -> _profileStep.value = ProfileStep.DISLIKES
            ProfileStep.DISLIKES -> _profileStep.value = if (next.hasDislikedNotes == true) ProfileStep.DISLIKES_DETAIL else ProfileStep.ALLERGIES
            ProfileStep.DISLIKES_DETAIL -> _profileStep.value = ProfileStep.ALLERGIES
            ProfileStep.ALLERGIES -> {
                if (next.hasAllergies == true) _profileStep.value = ProfileStep.ALLERGIES_DETAIL
                else _step.value = QuizStep.QUESTIONNAIRE
            }
            ProfileStep.ALLERGIES_DETAIL -> _step.value = QuizStep.QUESTIONNAIRE
        }
    }

    fun goBackProfileStep(visibleSteps: List<ProfileStep>, currentIdx: Int) {
        if (currentIdx > 0) _profileStep.value = visibleSteps[currentIdx - 1]
    }

    // ════════════════════════════════════════════════════════════════════
    // PROFIL MULTI — advanceMultiProfile()
    // ════════════════════════════════════════════════════════════════════
    fun updateCurrentParticipantProfile(updates: QuizProfile.() -> QuizProfile) {
        val idx = _currentParticipantIndex.value
        _participants.value = _participants.value.toMutableList().also { list ->
            val p = list[idx]
            list[idx] = p.copy(profile = p.profile.updates())
        }
    }

    fun advanceMultiProfile(updates: QuizProfile.() -> QuizProfile) {
        updateCurrentParticipantProfile(updates)
        val next = currentParticipant?.profile ?: return
        when (_multiProfileStep.value) {
            ProfileStep.GENDER -> _multiProfileStep.value = ProfileStep.AGE
            ProfileStep.AGE -> _multiProfileStep.value = if (next.gender == Gender.FEMME) ProfileStep.PREGNANT else ProfileStep.DISLIKES
            ProfileStep.PREGNANT -> _multiProfileStep.value = ProfileStep.DISLIKES
            ProfileStep.DISLIKES -> _multiProfileStep.value = if (next.hasDislikedNotes == true) ProfileStep.DISLIKES_DETAIL else ProfileStep.ALLERGIES
            ProfileStep.DISLIKES_DETAIL -> _multiProfileStep.value = ProfileStep.ALLERGIES
            ProfileStep.ALLERGIES -> {
                if (next.hasAllergies == true) _multiProfileStep.value = ProfileStep.ALLERGIES_DETAIL
                else advanceToNextParticipantOrQuestionnaire()
            }
            ProfileStep.ALLERGIES_DETAIL -> advanceToNextParticipantOrQuestionnaire()
        }
    }

    fun goBackMultiProfileStep(visibleSteps: List<ProfileStep>, currentIdx: Int) {
        if (currentIdx > 0) _multiProfileStep.value = visibleSteps[currentIdx - 1]
    }

    private fun advanceToNextParticipantOrQuestionnaire() {
        val nextIdx = _currentParticipantIndex.value + 1
        if (nextIdx < participantColors.size) {
            _currentParticipantIndex.value = nextIdx
            _multiProfileStep.value = ProfileStep.GENDER
        } else {
            _currentParticipantIndex.value = 0
            _step.value = QuizStep.QUESTIONNAIRE
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // QUESTIONNAIRE — top2 / bottom2 / confirm
    // ════════════════════════════════════════════════════════════════════
    fun toggleTopSelect(label: String) {
        _top2.value = when {
            _top2.value.contains(label) -> _top2.value - label
            _top2.value.size >= _answerFormat.value.positiveCount -> _top2.value
            else -> _top2.value + label
        }
    }

    fun toggleBottomSelect(label: String) {
        _bottom2.value = when {
            _bottom2.value.contains(label) -> _bottom2.value - label
            _bottom2.value.size >= _answerFormat.value.negativeCount -> _bottom2.value
            else -> _bottom2.value + label
        }
    }

    fun setPhase(p: QuizPhase) { _phase.value = p }

    fun confirmAnswer() {
        val currentQuestion = _questions.value.getOrNull(_currentIndex.value) ?: return
        val allLabels = currentQuestion.choices.map { it.label }
        val remaining = allLabels.filter { it !in _top2.value && it !in _bottom2.value }
        val format = _answerFormat.value
        val negativeCount = format.negativeCount
        val bottom2Final = if (_bottom2.value.size == negativeCount) _bottom2.value else (_bottom2.value + remaining).take(negativeCount)

        val answer = QuizAnswer(
            questionId = currentQuestion.id,
            questionText = currentQuestion.question,
            top2 = _top2.value,
            bottom2 = bottom2Final,
            positiveLabel = format.positiveLabel,
            negativeLabel = format.negativeLabel,
        )

        if (isMulti) {
            val idx = _currentParticipantIndex.value
            val updated = _participants.value.toMutableList().also { list ->
                list[idx] = list[idx].copy(answers = list[idx].answers + answer)
            }
            _participants.value = updated

            val nextParticipantIdx = idx + 1
            if (nextParticipantIdx < participantColors.size) {
                _currentParticipantIndex.value = nextParticipantIdx
                resetAnswerPhase()
            } else {
                _currentParticipantIndex.value = 0
                if (_currentIndex.value + 1 < _questions.value.size) {
                    _currentIndex.value += 1
                    resetAnswerPhase()
                } else {
                    // Toutes les questions terminées — chaque participant choisit son
                    // intensité (léger/équilibré/intense) à tour de rôle avant soumission.
                    _participants.value = updated
                    _currentParticipantIndex.value = 0
                    _phase.value = QuizPhase.INTENSITY
                }
            }
        } else {
            val newAnswers = _answers.value + answer
            _answers.value = newAnswers
            if (_currentIndex.value + 1 < _questions.value.size) {
                _currentIndex.value += 1
                resetAnswerPhase()
            } else {
                _phase.value = QuizPhase.INTENSITY
            }
        }
    }

    private fun resetAnswerPhase() {
        _phase.value = QuizPhase.TOP2
        _top2.value = emptyList()
        _bottom2.value = emptyList()
    }

    // ════════════════════════════════════════════════════════════════════
    // CHOIX D'INTENSITÉ (solo + multi) — puis nom du parfum avant génération
    // ════════════════════════════════════════════════════════════════════
    fun selectIntensity(type: FormulaType) {
        if (isMulti) {
            val idx = _currentParticipantIndex.value
            _participants.value = _participants.value.toMutableList().also { list ->
                list[idx] = list[idx].copy(formulaType = type)
            }
        } else {
            _formulaType.value = type
        }
        _phase.value = QuizPhase.PERFUME_NAME
    }

    // ════════════════════════════════════════════════════════════════════
    // NOM DU PARFUM (solo + multi) — dernière étape avant génération ; en duo, chaque personne
    // enchaîne intensité puis nom, à tour de rôle comme pour les questions.
    // ════════════════════════════════════════════════════════════════════
    fun submitPerfumeName(name: String) {
        if (isMulti) {
            val idx = _currentParticipantIndex.value
            val updated = _participants.value.toMutableList().also { list ->
                list[idx] = list[idx].copy(perfumeName = name)
            }
            _participants.value = updated

            val nextParticipantIdx = idx + 1
            if (nextParticipantIdx < participantColors.size) {
                _currentParticipantIndex.value = nextParticipantIdx
                _phase.value = QuizPhase.INTENSITY
            } else {
                submitMultiAnswers(updated)
            }
        } else {
            perfumeName = name
            submitAnswers(_answers.value)
        }
    }

    private fun submitAnswers(finalAnswers: List<QuizAnswer>) {
        _generating.value = true
        viewModelScope.launch {
            try {
                val p = _profile.value
                val res = ApiClient.service.generateFormulas(
                    GenerateFormulasRequest(
                        language = language.code,
                        brand = Brand.active.id,
                        gender = p.gender.wire,
                        age = p.age,
                        hasDislikedNotes = if (p.hasDislikedNotes == true) "oui" else "non",
                        dislikedNotes = p.dislikedNotes.ifBlank { null },
                        hasAllergies = if (p.hasAllergies == true) "oui" else "non",
                        allergies = p.allergies.ifBlank { null },
                        pregnant = p.pregnant ?: false,
                        answers = finalAnswers,
                        formulaType = _formulaType.value?.wire,
                        perfumeName = perfumeName.ifBlank { null },
                    ),
                )
                val body = res.body()
                if (!res.isSuccessful || body == null) throw Exception(errorGenerating)
                // Persisté tel quel (formules brutes), comme localStorage "quiz_formulas" côté web.
                val formulas = body["formulas"] as? JsonArray ?: JsonArray(emptyList())
                prefs.putString(LocalPrefs.KEY_QUIZ_FORMULAS, formulas.toString())
                navigateToResults?.invoke()
            } catch (e: Exception) {
                _error.value = e.message ?: errorGenerating
                _generating.value = false
            }
        }
    }

    private fun submitMultiAnswers(finalParticipants: List<ParticipantQuizState>) {
        _generating.value = true
        viewModelScope.launch {
            try {
                val res = ApiClient.service.generateMultiFormulas(
                    GenerateMultiFormulasRequest(
                        language = language.code,
                        brand = Brand.active.id,
                        participants = finalParticipants.map { p ->
                            MultiParticipantPayload(
                                color = p.color,
                                gender = p.profile.gender.wire,
                                age = p.profile.age,
                                hasDislikedNotes = if (p.profile.hasDislikedNotes == true) "oui" else "non",
                                dislikedNotes = p.profile.dislikedNotes.ifBlank { null },
                                hasAllergies = if (p.profile.hasAllergies == true) "oui" else "non",
                                allergies = p.profile.allergies.ifBlank { null },
                                pregnant = p.profile.pregnant ?: false,
                                answers = p.answers,
                                formulaType = p.formulaType?.wire,
                                perfumeName = p.perfumeName.ifBlank { null },
                            )
                        },
                    ),
                )
                val body = res.body()
                if (!res.isSuccessful || body == null) throw Exception(errorGenerating)
                val participants = body["participants"] as? JsonArray ?: JsonArray(emptyList())
                prefs.putString(LocalPrefs.KEY_QUIZ_MULTI_RESULTS, participants.toString())
                navigateToResults?.invoke()
            } catch (e: Exception) {
                _error.value = e.message ?: errorGenerating
                _generating.value = false
            }
        }
    }

    /** Messages d'erreur localisés (quiz.errorLoadingQuestions / quiz.errorGenerating), fournis par l'écran. */
    var errorGenerating: String = "Error"
    var errorLoadingQuestions: String = "Error"
}
