package com.lylo.tablet.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lylo.tablet.R
import com.lylo.tablet.data.model.ConfirmationData
import com.lylo.tablet.data.model.Gender
import com.lylo.tablet.data.model.ProfileStep
import com.lylo.tablet.data.model.QuizPhase
import com.lylo.tablet.data.model.QuizStep
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.components.AnswerConfirmation
import com.lylo.tablet.ui.components.ChoiceGrid
import com.lylo.tablet.ui.components.ConfirmChoice
import com.lylo.tablet.ui.components.GeneratingLoader
import com.lylo.tablet.ui.components.Navbar
import com.lylo.tablet.ui.components.StepProgress
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.ParticipantColors
import com.lylo.tablet.viewmodel.QuizViewModel

/**
 * Portage intégral de src/app/quiz/page.tsx (le mode "Visuel" — sans voix ni LiveKit). Écran le
 * plus complexe du front après /interaction : profil solo/multi étape-par-étape puis
 * questionnaire top2/bottom2/confirm avec bascule automatique entre participants.
 */
@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: QuizViewModel = viewModel(),
) {
    val strings = LocalStringResolver.current

    LaunchedEffect(Unit) {
        viewModel.navigateToResults = onNavigateToResults
        viewModel.errorGenerating = strings.get(R.string.quiz_error_generating)
        viewModel.errorLoadingQuestions = strings.get(R.string.quiz_error_loading_questions)
    }

    val step by viewModel.step.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val generating by viewModel.generating.collectAsState()

    LaunchedEffect(step) { viewModel.loadQuestionsIfNeeded() }

    if (generating) {
        GeneratingLoader()
        return
    }

    if (error != null) {
        Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "", color = Color(0xFFEF4444), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Text(
                    strings.get(R.string.quiz_back),
                    color = LyloPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp).clickable { onNavigateBack() },
                )
            }
        }
        return
    }

    val phase by viewModel.phase.collectAsState()

    when {
        step == QuizStep.PROFILE -> QuizProfileStepScreen(viewModel, onNavigateHome)
        phase == QuizPhase.INTENSITY -> QuizIntensityScreen(viewModel, onNavigateHome)
        phase == QuizPhase.PERFUME_NAME -> QuizPerfumeNameScreen(viewModel, onNavigateHome)
        step == QuizStep.QUESTIONNAIRE -> {
            if (loading) {
                Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = LyloPrimary)
                        Text(strings.get(R.string.quiz_loading), color = LyloPrimary.copy(alpha = 0.6f), modifier = Modifier.padding(top = 12.dp))
                    }
                }
            } else {
                QuizQuestionnaireScreen(viewModel, onNavigateHome)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
// CHOIX D'INTENSITÉ (léger / équilibré / intense) — avant génération
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun QuizIntensityScreen(viewModel: QuizViewModel, onNavigateHome: () -> Unit) {
    val strings = LocalStringResolver.current
    val isMulti = viewModel.isMulti
    val participants by viewModel.participants.collectAsState()
    val currentParticipantIndex by viewModel.currentParticipantIndex.collectAsState()
    val formulaType by viewModel.formulaType.collectAsState()

    val activeType = if (isMulti) participants.getOrNull(currentParticipantIndex)?.formulaType else formulaType
    val currentColor = if (isMulti) viewModel.currentColor else null
    val colorDef = currentColor?.let { c -> ParticipantColors.firstOrNull { it.id == c } }
    val bgColor = colorDef?.bg ?: LyloBackgroundLight

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        DecorativeBackgroundBlobs()

        Column(modifier = Modifier.fillMaxSize()) {
            Navbar(showActions = false, transparent = true, onLogoClick = onNavigateHome)

            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.widthIn(max = 480.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    if (isMulti && colorDef != null) {
                        ColorTurnBanner(colorDef, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            strings.get(R.string.quiz_intensity_question),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Light,
                            color = LyloPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Column(modifier = Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            IntensityCard(
                                icon = Icons.Default.Air,
                                label = strings.get(R.string.quiz_intensity_frais),
                                description = strings.get(R.string.quiz_intensity_frais_desc),
                                selected = activeType == com.lylo.tablet.data.model.FormulaType.FRAIS,
                                onClick = { viewModel.selectIntensity(com.lylo.tablet.data.model.FormulaType.FRAIS) },
                            )
                            IntensityCard(
                                icon = Icons.Default.Balance,
                                label = strings.get(R.string.quiz_intensity_mix),
                                description = strings.get(R.string.quiz_intensity_mix_desc),
                                selected = activeType == com.lylo.tablet.data.model.FormulaType.MIX,
                                onClick = { viewModel.selectIntensity(com.lylo.tablet.data.model.FormulaType.MIX) },
                            )
                            IntensityCard(
                                icon = Icons.Default.LocalFireDepartment,
                                label = strings.get(R.string.quiz_intensity_puissant),
                                description = strings.get(R.string.quiz_intensity_puissant_desc),
                                selected = activeType == com.lylo.tablet.data.model.FormulaType.PUISSANT,
                                onClick = { viewModel.selectIntensity(com.lylo.tablet.data.model.FormulaType.PUISSANT) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
// NOM DU PARFUM — dernière étape avant génération
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun QuizPerfumeNameScreen(viewModel: QuizViewModel, onNavigateHome: () -> Unit) {
    val strings = LocalStringResolver.current
    val isMulti = viewModel.isMulti
    // Recomposé à chaque changement de participant : le champ repart vide pour la personne suivante.
    val currentParticipantIndex by viewModel.currentParticipantIndex.collectAsState()
    val currentColor = if (isMulti) viewModel.currentColor else null
    val colorDef = currentColor?.let { c -> ParticipantColors.firstOrNull { it.id == c } }
    val bgColor = colorDef?.bg ?: LyloBackgroundLight

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        DecorativeBackgroundBlobs()

        Column(modifier = Modifier.fillMaxSize()) {
            Navbar(showActions = false, transparent = true, onLogoClick = onNavigateHome)

            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.widthIn(max = 512.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isMulti && colorDef != null) {
                        ColorTurnBanner(colorDef, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            strings.get(R.string.quiz_perfume_name_question),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Light,
                            color = LyloPrimary,
                            textAlign = TextAlign.Center,
                        )
                        androidx.compose.runtime.key(currentParticipantIndex) {
                            com.lylo.tablet.ui.components.PerfumeNameForm(
                                onSubmit = { name -> viewModel.submitPerfumeName(name) },
                                showSubtitle = false,
                                translucentField = false,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntensityCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) LyloPrimary else Color.White)
            .border(2.dp, if (selected) LyloPrimary else LyloPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.White else LyloPrimary, modifier = Modifier.padding(end = 16.dp))
        Column {
            Text(label, color = if (selected) Color.White else LyloPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, color = if (selected) Color.White.copy(alpha = 0.8f) else LyloPrimary.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
// PROFIL — une question à la fois
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun QuizProfileStepScreen(viewModel: QuizViewModel, onNavigateHome: () -> Unit) {
    val strings = LocalStringResolver.current
    val isMulti = viewModel.isMulti
    val profile by viewModel.profile.collectAsState()
    val profileStep by viewModel.profileStep.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val currentParticipantIndex by viewModel.currentParticipantIndex.collectAsState()
    val multiProfileStep by viewModel.multiProfileStep.collectAsState()

    val activeProfile = if (isMulti) participants.getOrNull(currentParticipantIndex)?.profile ?: return else profile
    val activeProfileStep = if (isMulti) multiProfileStep else profileStep

    val visibleSteps = ProfileStep.entries
        .filter { s ->
            when (s) {
                ProfileStep.PREGNANT -> activeProfile.gender == Gender.FEMME
                ProfileStep.DISLIKES_DETAIL -> activeProfile.hasDislikedNotes == true
                ProfileStep.ALLERGIES_DETAIL -> activeProfile.hasAllergies == true
                else -> true
            }
        }
    val currentProfileIndex = visibleSteps.indexOf(activeProfileStep).coerceAtLeast(0)

    val currentColor = if (isMulti) viewModel.currentColor else null
    val colorDef = currentColor?.let { c -> ParticipantColors.firstOrNull { it.id == c } }
    val bgColor = colorDef?.bg ?: LyloBackgroundLight

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        DecorativeBackgroundBlobs()

        Column(modifier = Modifier.fillMaxSize()) {
            Navbar(showActions = false, transparent = true, onLogoClick = onNavigateHome)

            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.widthIn(max = 480.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    if (isMulti && colorDef != null) {
                        ColorTurnBanner(colorDef, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        StepProgress(currentStep = currentProfileIndex + 1, totalSteps = visibleSteps.size)

                        when (activeProfileStep) {
                            ProfileStep.GENDER -> ProfileQuestionGender(strings.get(R.string.quiz_profile_gender_question), activeProfile) { g ->
                                if (isMulti) viewModel.advanceMultiProfile { copy(gender = g, pregnant = if (g == Gender.HOMME) false else pregnant) }
                                else viewModel.advanceProfile { copy(gender = g, pregnant = if (g == Gender.HOMME) false else pregnant) }
                            }
                            ProfileStep.AGE -> ProfileQuestionAge(activeProfile.age, onChange = { v ->
                                if (isMulti) viewModel.updateCurrentParticipantProfile { copy(age = v) }
                                else viewModel.updateProfile { copy(age = v) }
                            }, onContinue = {
                                if (isMulti) viewModel.advanceMultiProfile { this } else viewModel.advanceProfile { this }
                            })
                            ProfileStep.PREGNANT -> ProfileQuestionYesNo(strings.get(R.string.quiz_profile_pregnant_question), activeProfile.pregnant) { v ->
                                if (isMulti) viewModel.advanceMultiProfile { copy(pregnant = v) } else viewModel.advanceProfile { copy(pregnant = v) }
                            }
                            ProfileStep.DISLIKES -> ProfileQuestionYesNo(strings.get(R.string.quiz_profile_dislikes_question), activeProfile.hasDislikedNotes) { v ->
                                if (isMulti) viewModel.advanceMultiProfile { copy(hasDislikedNotes = v) } else viewModel.advanceProfile { copy(hasDislikedNotes = v) }
                            }
                            ProfileStep.DISLIKES_DETAIL -> ProfileQuestionTextDetail(
                                question = strings.get(R.string.quiz_profile_dislikes_detail_question),
                                placeholder = strings.get(R.string.quiz_profile_dislikes_placeholder),
                                icon = Icons.Default.Block,
                                value = activeProfile.dislikedNotes,
                                onChange = { v ->
                                    if (isMulti) viewModel.updateCurrentParticipantProfile { copy(dislikedNotes = v) }
                                    else viewModel.updateProfile { copy(dislikedNotes = v) }
                                },
                                onContinue = {
                                    if (isMulti) viewModel.advanceMultiProfile { this } else viewModel.advanceProfile { this }
                                },
                            )
                            ProfileStep.ALLERGIES -> ProfileQuestionYesNoNullable(strings.get(R.string.quiz_profile_allergies_question), activeProfile.hasAllergies) { v ->
                                if (isMulti) viewModel.advanceMultiProfile { copy(hasAllergies = v) } else viewModel.advanceProfile { copy(hasAllergies = v) }
                            }
                            ProfileStep.ALLERGIES_DETAIL -> ProfileQuestionTextDetail(
                                question = strings.get(R.string.quiz_profile_allergies_detail_question),
                                placeholder = strings.get(R.string.quiz_profile_allergies_placeholder),
                                icon = Icons.Default.Warning,
                                value = activeProfile.allergies,
                                onChange = { v ->
                                    if (isMulti) viewModel.updateCurrentParticipantProfile { copy(allergies = v) }
                                    else viewModel.updateProfile { copy(allergies = v) }
                                },
                                onContinue = {
                                    if (isMulti) viewModel.advanceMultiProfile { this } else viewModel.advanceProfile { this }
                                },
                            )
                        }

                        if (currentProfileIndex > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 16.dp).clickable {
                                    if (isMulti) viewModel.goBackMultiProfileStep(visibleSteps, currentProfileIndex)
                                    else viewModel.goBackProfileStep(visibleSteps, currentProfileIndex)
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = LyloPrimary, modifier = Modifier.padding(end = 4.dp))
                                Text(strings.get(R.string.quiz_back), color = LyloPrimary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Décoration de fond très discrète : deux cercles flous à faible opacité, reproduisant les
 * `div` en `bg-gradient-to-l`/`bg-gradient-to-tr` opacity-[0.03]/[0.05] blur-[120px] du React
 * (quiz/page.tsx). Pas besoin d'un dégradé pixel-perfect, un cercle flou suffit.
 */
@Composable
private fun BoxScope.DecorativeBackgroundBlobs() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(260.dp)
            .offset(x = 60.dp, y = (-40).dp)
            .blur(120.dp)
            .background(LyloPrimary.copy(alpha = 0.03f), CircleShape),
    )
    Box(
        modifier = Modifier
            .size(260.dp)
            .align(Alignment.BottomStart)
            .offset(x = (-60).dp, y = 60.dp)
            .blur(120.dp)
            .background(LyloPrimary.copy(alpha = 0.05f), CircleShape),
    )
}

@Composable
private fun ColorTurnBanner(colorDef: com.lylo.tablet.ui.theme.ParticipantColor, modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorDef.bg)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.padding(end = 8.dp).background(colorDef.text, CircleShape).size(12.dp))
        Text(
            strings.get(R.string.quiz_your_turn).replace("{name}", strings.get(colorDef.labelRes).replaceFirstChar { c -> c.uppercase() }),
            color = colorDef.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProfileQuestionGender(question: String, profile: com.lylo.tablet.data.model.QuizProfile, onSelect: (Gender) -> Unit) {
    val strings = LocalStringResolver.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
        Text(question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light, color = LyloPrimary, textAlign = TextAlign.Center)
        Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigChoiceButton(strings.get(R.string.quiz_profile_male), profile.gender == Gender.HOMME) { onSelect(Gender.HOMME) }
            BigChoiceButton(strings.get(R.string.quiz_profile_female), profile.gender == Gender.FEMME) { onSelect(Gender.FEMME) }
        }
    }
}

@Composable
private fun ProfileQuestionYesNo(question: String, value: Boolean?, onSelect: (Boolean) -> Unit) {
    val strings = LocalStringResolver.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
        Text(question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light, color = LyloPrimary, textAlign = TextAlign.Center)
        Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigChoiceButton(strings.get(R.string.quiz_yes), value == true) { onSelect(true) }
            BigChoiceButton(strings.get(R.string.quiz_no), value == false) { onSelect(false) }
        }
    }
}

@Composable
private fun ProfileQuestionYesNoNullable(question: String, value: Boolean?, onSelect: (Boolean) -> Unit) =
    ProfileQuestionYesNo(question, value, onSelect)

@Composable
private fun ProfileQuestionAge(age: String, onChange: (String) -> Unit, onContinue: () -> Unit) {
    val strings = LocalStringResolver.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
        Text(strings.get(R.string.quiz_profile_age_question), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light, color = LyloPrimary, textAlign = TextAlign.Center)
        Row(
            modifier = Modifier
                .padding(top = 20.dp)
                .widthIn(max = 260.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .background(Color(0xFFFAFAF9))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Cake, contentDescription = null, tint = LyloPrimary, modifier = Modifier.padding(end = 10.dp))
            BasicTextField(
                value = age,
                onValueChange = { v -> if (v.all { it.isDigit() }) onChange(v) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = LyloPrimary),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (age.trim().isNotEmpty()) onContinue() }),
                decorationBox = { inner ->
                    if (age.isEmpty()) Text(strings.get(R.string.quiz_profile_age_placeholder), color = LyloPrimary.copy(alpha = 0.35f), style = MaterialTheme.typography.bodyLarge)
                    inner()
                },
            )
        }
        com.lylo.tablet.ui.components.LyloButton(
            onClick = onContinue,
            enabled = age.trim().isNotEmpty(),
            modifier = Modifier.padding(top = 20.dp),
        ) {
            Text(strings.get(R.string.quiz_continue))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** Question texte libre (allergies, notes non désirées) : champ + bouton Continuer. */
@Composable
private fun ProfileQuestionTextDetail(
    question: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    onChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val strings = LocalStringResolver.current
    val allergies = value
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
        Text(question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light, color = LyloPrimary, textAlign = TextAlign.Center)
        Row(
            modifier = Modifier
                .padding(top = 20.dp)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .background(Color(0xFFFAFAF9))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = LyloPrimary, modifier = Modifier.padding(end = 10.dp))
            BasicTextField(
                value = allergies,
                onValueChange = onChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = LyloPrimary),
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (allergies.trim().isNotEmpty()) onContinue() }),
                decorationBox = { inner ->
                    if (allergies.isEmpty()) Text(placeholder, color = LyloPrimary.copy(alpha = 0.35f), style = MaterialTheme.typography.bodyMedium)
                    inner()
                },
            )
        }
        com.lylo.tablet.ui.components.LyloButton(
            onClick = onContinue,
            enabled = allergies.trim().isNotEmpty(),
            modifier = Modifier.padding(top = 20.dp),
        ) {
            Text(strings.get(R.string.quiz_continue))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun BigChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) LyloPrimary else Color.White)
            .border(2.dp, if (selected) LyloPrimary else LyloPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Text(label, color = if (selected) Color.White else LyloPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

// ════════════════════════════════════════════════════════════════════════
// QUESTIONNAIRE — top2 / bottom2 / confirm
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun QuizQuestionnaireScreen(viewModel: QuizViewModel, onNavigateHome: () -> Unit) {
    val strings = LocalStringResolver.current
    val questions by viewModel.questions.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val phase by viewModel.phase.collectAsState()
    val top2 by viewModel.top2.collectAsState()
    val bottom2 by viewModel.bottom2.collectAsState()
    val currentParticipantIndex by viewModel.currentParticipantIndex.collectAsState()
    val answerFormat by viewModel.answerFormat.collectAsState()
    val positiveCount = answerFormat.positiveCount
    val negativeCount = answerFormat.negativeCount

    val currentQuestion = questions.getOrNull(currentIndex)
    val isMulti = viewModel.isMulti
    val currentColor = viewModel.currentColor
    val colorDef = currentColor?.let { c -> ParticipantColors.firstOrNull { it.id == c } }
    val bgColor = if (isMulti) colorDef?.bg ?: LyloBackgroundLight else LyloBackgroundLight

    val visibleChoices = when (phase) {
        QuizPhase.BOTTOM2 -> currentQuestion?.choices?.filter { it.label !in top2 } ?: emptyList()
        else -> currentQuestion?.choices ?: emptyList()
    }
    // Note : INTENSITY / PERFUME_NAME ne sont jamais actifs ici (QuizScreen route vers leurs
    // écrans dédiés avant d'atteindre celui-ci) — branches gardées pour l'exhaustivité du when.
    val selected = when (phase) { QuizPhase.TOP2 -> top2; QuizPhase.BOTTOM2 -> bottom2; else -> emptyList() }
    val countOne = strings.get(R.string.quiz_pick_count_one)
    val countMany = strings.get(R.string.quiz_pick_count_many)
    val phaseHint = when (phase) {
        QuizPhase.TOP2 -> answerFormat.selectionHint(positive = true, countOne, countMany)
        QuizPhase.BOTTOM2 -> answerFormat.selectionHint(positive = false, countOne, countMany)
        else -> ""
    }
    fun imageOf(label: String): String? = currentQuestion?.choices?.firstOrNull { it.label == label }?.image
        ?.takeIf { it.isNotBlank() }
        ?.let { img -> if (img.startsWith("http")) img else "${com.lylo.tablet.BuildConfig.API_BASE_URL}$img" }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        DecorativeBackgroundBlobs()

        Column(modifier = Modifier.fillMaxSize()) {
            Navbar(showActions = false, transparent = true, onLogoClick = onNavigateHome)

            Column(
                modifier = Modifier.weight(1f).fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isMulti && colorDef != null) {
                    ColorTurnBanner(colorDef, modifier = Modifier.padding(top = 8.dp).widthIn(max = 640.dp))
                }

                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StepProgress(currentStep = currentIndex + 1, totalSteps = questions.size)
                    Text(
                        currentQuestion?.question ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                        color = LyloPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (phaseHint.isNotEmpty()) {
                        Text(phaseHint.uppercase(), style = MaterialTheme.typography.labelSmall, color = LyloPrimary, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    if (phase == QuizPhase.CONFIRM) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .padding(16.dp),
                        ) {
                            AnswerConfirmation(
                                top2 = top2.map { ConfirmChoice(it, imageOf(it)) },
                                bottom2 = bottom2.map { ConfirmChoice(it, imageOf(it)) },
                                positiveLabel = answerFormat.positiveLabel,
                                negativeLabel = answerFormat.negativeLabel,
                            )
                        }
                    } else {
                        ChoiceGrid(
                            choices = visibleChoices,
                            hiddenChoices = emptyList(),
                            selectedLabels = selected,
                            clickable = true,
                            modifier = Modifier.widthIn(max = 900.dp),
                            onSelect = { label ->
                                if (phase == QuizPhase.TOP2) viewModel.toggleTopSelect(label) else viewModel.toggleBottomSelect(label)
                            },
                        )
                    }
                }

                Box(modifier = Modifier.padding(bottom = 16.dp)) {
                    when (phase) {
                        QuizPhase.TOP2 -> com.lylo.tablet.ui.components.LyloButton(
                            onClick = { viewModel.setPhase(if (negativeCount > 0) QuizPhase.BOTTOM2 else QuizPhase.CONFIRM) },
                            enabled = top2.size >= positiveCount,
                            variant = com.lylo.tablet.ui.components.LyloButtonVariant.OUTLINE,
                        ) { Text(strings.get(R.string.quiz_continue)) }

                        QuizPhase.BOTTOM2 -> com.lylo.tablet.ui.components.LyloButton(
                            onClick = { viewModel.setPhase(QuizPhase.CONFIRM) },
                            enabled = bottom2.size >= negativeCount,
                            variant = com.lylo.tablet.ui.components.LyloButtonVariant.OUTLINE,
                        ) { Text(strings.get(R.string.quiz_continue)) }

                        QuizPhase.CONFIRM -> {
                            val isLastParticipant = !isMulti || currentParticipantIndex == (if (isMulti) viewModel.participants.value.size - 1 else 0)
                            val isLastQuestion = currentIndex + 1 >= questions.size
                            val isVeryLast = isLastParticipant && isLastQuestion
                            com.lylo.tablet.ui.components.LyloButton(onClick = { viewModel.confirmAnswer() }) {
                                Text(if (isVeryLast) strings.get(R.string.quiz_submit) else strings.get(R.string.quiz_next))
                            }
                        }

                        QuizPhase.INTENSITY, QuizPhase.PERFUME_NAME -> Unit
                    }
                }
            }
        }
    }
}
