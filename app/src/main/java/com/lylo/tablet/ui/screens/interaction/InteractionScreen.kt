package com.lylo.tablet.ui.screens.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lylo.tablet.BuildConfig
import com.lylo.tablet.R
import com.lylo.tablet.data.model.SessionState
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.components.AnswerConfirmation
import com.lylo.tablet.ui.components.AvatarSection
import com.lylo.tablet.ui.components.BottomBar
import com.lylo.tablet.ui.components.ChoiceGrid
import com.lylo.tablet.ui.components.ConfirmChoice
import com.lylo.tablet.ui.components.GeneratingLoader
import com.lylo.tablet.ui.components.IntensitySelector
import com.lylo.tablet.ui.components.Navbar
import com.lylo.tablet.ui.components.PerfumeNameForm
import com.lylo.tablet.ui.components.PrimaryPillButton
import com.lylo.tablet.ui.components.StepProgress
import com.lylo.tablet.ui.components.TranscriptPanel
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.viewmodel.SessionViewModel
import io.livekit.android.room.Room

/**
 * Portage de src/app/interaction/page.tsx : avatar + question courante + StepProgress, puis
 * bascule entre ChoiceGrid (questionnaire normal), AnswerConfirmation (awaiting_confirmation) et
 * IntensitySelector (asking_intensity) selon sessionState — exactement comme côté web.
 * Navigue automatiquement vers /recommendations quand sessionState passe à "completed".
 */
@Composable
fun InteractionScreen(
    sessionViewModel: SessionViewModel,
    room: Room?,
    avatarFallback: Any,
    avatarEnabled: Boolean,
    onNavigateToRecommendations: () -> Unit,
    onNavigateHome: () -> Unit,
    onInterrupt: () -> Unit,
    onResumeListen: () -> Unit,
    onResume: () -> Unit,
    onMicToggle: (Boolean) -> Unit,
) {
    val strings = LocalStringResolver.current
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val questionCount by sessionViewModel.questionCount.collectAsState()
    val questions by sessionViewModel.questions.collectAsState()
    val currentQuestionIndex by sessionViewModel.currentQuestionIndex.collectAsState()
    val confirmationData by sessionViewModel.confirmationData.collectAsState()
    val hiddenChoices by sessionViewModel.hiddenChoices.collectAsState()
    val clickSelectionMode by sessionViewModel.clickSelectionMode.collectAsState()
    val agentState by sessionViewModel.agentState.collectAsState()
    val agentName by sessionViewModel.agentName.collectAsState()
    val transcripts by sessionViewModel.transcripts.collectAsState()
    val avatarDisabled by sessionViewModel.avatarDisabled.collectAsState()
    val answerFormat by sessionViewModel.answerFormat.collectAsState()

    LaunchedEffect(sessionState) {
        if (!BuildConfig.DEV_MODE && sessionState == SessionState.COMPLETED) {
            onNavigateToRecommendations()
        }
    }

    if (sessionState == SessionState.GENERATING_FORMULAS) {
        GeneratingLoader()
        return
    }

    val currentStep = currentQuestionIndex + 1
    val totalSteps = if (questionCount > 0) questionCount else 4
    val currentQuestion = questions.getOrNull(currentQuestionIndex)

    // ── Sélection au clic (portage de ChoiceGrid.tsx) : jusqu'à `required` cartes selon le format
    // du questionnaire, puis bouton Valider. Cartes cliquables à chaque étape top_2/bottom_2, en plus
    // de la réponse orale — la première des deux qui arrive fait avancer la question.
    val required = if (clickSelectionMode == "bottom_2") answerFormat.negativeCount else answerFormat.positiveCount
    var clickSelected by remember { mutableStateOf<List<String>>(emptyList()) }
    var submitted by remember { mutableStateOf(false) }
    LaunchedEffect(currentQuestion) { clickSelected = emptyList(); submitted = false }
    // Nouvelle étape, ou réponse actée à l'oral → sélection remise à zéro.
    LaunchedEffect(clickSelectionMode) {
        if (clickSelectionMode == null) { clickSelected = emptyList(); submitted = false }
    }
    // Sélection visuelle simple quand les cartes ne sont pas cliquables (pas d'action déclenchée)
    var voiceSelected by remember(currentQuestion) { mutableStateOf<String?>(null) }

    // État du panneau transcript levé ici (et non dans BottomBar) : TranscriptPanel doit être
    // rendu en overlay plein écran sur le Box racine ci-dessous, pas contraint à la hauteur de
    // la pilule BottomBar — voir le commentaire dans BottomBar.kt.
    var transcriptOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Navbar(showActions = false, transparent = true, onLogoClick = onNavigateHome)

            Column(
                modifier = Modifier.weight(1f).fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp).widthIn(max = 720.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AvatarSection(
                        name = "",
                        role = "",
                        fallbackImageUrl = avatarFallback,
                        avatarEnabled = avatarEnabled,
                        room = room,
                        avatarDisabled = avatarDisabled,
                        size = 120.dp,
                    )
                    StepProgress(currentStep = currentStep, totalSteps = totalSteps, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        text = currentQuestion?.question ?: strings.get(R.string.interaction_waiting),
                        style = MaterialTheme.typography.headlineSmall,
                        color = LyloPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    when {
                        sessionState == SessionState.AWAITING_CONFIRMATION && confirmationData != null -> {
                            val data = confirmationData!!
                            AnswerConfirmation(
                                top2 = data.top2.map { ConfirmChoice(it) },
                                bottom2 = data.bottom2.map { ConfirmChoice(it) },
                                positiveLabel = answerFormat.positiveLabel,
                                negativeLabel = answerFormat.negativeLabel,
                            )
                        }
                        sessionState == SessionState.ASKING_INTENSITY -> IntensitySelector()
                        sessionState == SessionState.ASKING_PERFUME_NAME -> PerfumeNameForm(
                            onSubmit = { name -> sessionViewModel.submitPerfumeName(name) },
                        )
                        else -> Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ChoiceGrid(
                                choices = currentQuestion?.choices ?: emptyList(),
                                hiddenChoices = hiddenChoices,
                                selectedLabels = if (clickSelectionMode != null) clickSelected else listOfNotNull(voiceSelected),
                                clickable = clickSelectionMode != null && !submitted,
                                showNumbers = true,
                                modifier = Modifier.weight(1f),
                                onSelect = { label ->
                                    if (clickSelectionMode != null) {
                                        if (!submitted) {
                                            clickSelected = when {
                                                clickSelected.contains(label) -> clickSelected - label
                                                clickSelected.size >= required -> clickSelected
                                                else -> clickSelected + label
                                            }
                                        }
                                    } else {
                                        voiceSelected = label
                                    }
                                },
                            )
                            if (clickSelectionMode != null && clickSelected.size == required) {
                                PrimaryPillButton(
                                    text = if (submitted) strings.get(R.string.choice_grid_submitted) else strings.get(R.string.choice_grid_validate),
                                    onClick = {
                                        if (clickSelected.size == required && !submitted) {
                                            submitted = true
                                            sessionViewModel.submitClickAnswer(clickSelected)
                                        }
                                    },
                                    enabled = !submitted,
                                    trailingIcon = null,
                                )
                            }
                        }
                    }
                }

                BottomBar(
                    agentState = agentState,
                    sessionState = sessionState,
                    agentName = agentName,
                    transcriptOpen = transcriptOpen,
                    onTranscriptToggle = { transcriptOpen = !transcriptOpen },
                    onInterrupt = onInterrupt,
                    onResumeListen = onResumeListen,
                    onResume = onResume,
                    onMicToggle = onMicToggle,
                    currentQuestionIndex = currentQuestionIndex,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }

        // Overlay plein écran ancré à droite (portage fidèle de TranscriptPanel.tsx :
        // `fixed top-0 right-0 h-full`) — rendu ici, sur le Box racine fillMaxSize(), et non
        // dans BottomBar, pour ne pas être contraint à la hauteur de la pilule de contrôle.
        TranscriptPanel(
            open = transcriptOpen,
            transcripts = transcripts,
            agentName = agentName,
            onToggle = { transcriptOpen = !transcriptOpen },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}
