package com.lylo.tablet.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lylo.tablet.R
import com.lylo.tablet.data.ShareableFormulaCodec
import com.lylo.tablet.data.model.Formula
import com.lylo.tablet.data.model.SizeOption
import com.lylo.tablet.data.remote.ApiClient
import com.lylo.tablet.data.remote.SaveFormulaRequest
import com.lylo.tablet.data.remote.SimilarPerfume
import com.lylo.tablet.i18n.AppLanguage
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.print.printFormula
import com.lylo.tablet.ui.components.FormulaCard
import com.lylo.tablet.ui.components.FormulaCardVariant
import com.lylo.tablet.ui.components.FormulaPyramidPanel
import com.lylo.tablet.ui.components.FormulaQrCode
import com.lylo.tablet.ui.components.Navbar
import com.lylo.tablet.ui.components.PrimaryPillButton
import com.lylo.tablet.ui.components.RobotGoButton
import com.lylo.tablet.ui.components.RoundIconButton
import com.lylo.tablet.ui.components.RoundIconStyle
import com.lylo.tablet.ui.components.SendFormulaMailButton
import com.lylo.tablet.ui.components.SimilarPerfumesState
import com.lylo.tablet.ui.components.fetchSimilarPerfumes
import com.lylo.tablet.ui.theme.LyloAccentTan
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.ParticipantColor
import com.lylo.tablet.ui.theme.ParticipantColors
import com.lylo.tablet.viewmodel.QuizResultsViewModel
import kotlinx.coroutines.launch

/** Portage de src/app/quiz/results/page.tsx — routeur solo/duo selon la détection du ViewModel. */
@Composable
fun QuizResultsScreen(
    onReturnHome: () -> Unit,
    onNavigateHome: () -> Unit,
    onMissingResults: () -> Unit,
    webAppBaseUrl: String,
    viewModel: QuizResultsViewModel = viewModel(),
) {
    // Rien en mémoire → retour au quiz (router.push("/quiz") côté web).
    LaunchedEffect(Unit) { if (!viewModel.hasResults) onMissingResults() }
    if (!viewModel.hasResults) return

    val exit = {
        viewModel.clearResults()
        onReturnHome()
    }

    if (viewModel.isMulti) {
        // Duo : chaque personne suit à tour de rôle le même parcours que le solo, puis passe la
        // main ; la dernière termine.
        val currentIdx by viewModel.currentIdx.collectAsState()
        val participant = viewModel.multiParticipants[currentIdx]
        val isLast = currentIdx == viewModel.multiParticipants.size - 1
        // key : état (choix, référence, notes remplacées…) remis à zéro pour chaque personne.
        key(participant.color) {
            FormulaJourney(
                initialFormulas = participant.formulas,
                participant = JourneyParticipant(participant.color, currentIdx, viewModel.multiParticipants.size),
                language = viewModel.language,
                webAppBaseUrl = webAppBaseUrl,
                recapEmail = viewModel.recapEmail,
                recapName = viewModel.recapName,
                onExit = exit,
                onFinish = { if (isLast || !viewModel.nextParticipant()) exit() },
                onNavigateHome = onNavigateHome,
            )
        }
    } else {
        FormulaJourney(
            initialFormulas = viewModel.soloFormulas.orEmpty(),
            participant = null,
            language = viewModel.language,
            webAppBaseUrl = webAppBaseUrl,
            recapEmail = viewModel.recapEmail,
            recapName = viewModel.recapName,
            onExit = exit,
            onFinish = null,
            onNavigateHome = onNavigateHome,
        )
    }
}

private data class JourneyParticipant(val color: String, val index: Int, val total: Int)

/**
 * Parcours d'une personne : choix d'une formule → pyramide + personnalisation + actions.
 * Utilisé tel quel en solo, et une fois par personne en mode duo (participant renseigné).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormulaJourney(
    initialFormulas: List<Formula>,
    participant: JourneyParticipant?,
    language: AppLanguage,
    webAppBaseUrl: String,
    recapEmail: String?,
    recapName: String?,
    /** « Retour à l'accueil » : quitte le parcours sans terminer. */
    onExit: () -> Unit,
    /** Duo uniquement : « Passer à la personne suivante » / « Terminer ». */
    onFinish: (() -> Unit)?,
    onNavigateHome: () -> Unit,
) {
    val strings = LocalStringResolver.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val formulas = remember { mutableStateListOf<Formula>().also { it.addAll(initialFormulas) } }
    val colorDef: ParticipantColor? = participant?.let { p -> ParticipantColors.firstOrNull { it.id == p.color } }
    val isLastParticipant = participant != null && participant.index == participant.total - 1

    var chosen by remember { mutableStateOf<Int?>(null) }
    // Passe à true au clic sur « Continuer » — affiche l'écran final (formule + personnalisation
    // + actions), à la place du récap immédiat après sélection d'une carte.
    var confirmed by remember { mutableStateOf(false) }
    var reference by remember { mutableStateOf("") }
    var savingReference by remember { mutableStateOf(false) }
    // Échec de POST /formulas/save : sans référence, l'envoi par mail est impossible — on
    // l'affiche avec un bouton « Réessayer » plutôt que de masquer silencieusement le bouton mail.
    var referenceError by remember { mutableStateOf(false) }
    // Référence déjà obtenue par formule : évite de recréer une entrée en base (et une nouvelle
    // référence) quand l'utilisateur revient sur une carte déjà choisie.
    val referencesByIndex = remember { mutableStateMapOf<Int, String>() }
    val selectedSizes = remember { mutableStateMapOf<Int, SizeOption>() }
    // Parfums ressemblants mis en cache par formule (recherche LLM + web non relancée).
    val similarByIndex = remember { mutableStateMapOf<Int, List<SimilarPerfume>>() }
    val similarLoading = remember { mutableStateMapOf<Int, Boolean>() }
    val similarError = remember { mutableStateMapOf<Int, Boolean>() }

    fun sizeFor(i: Int) = selectedSizes[i] ?: SizeOption.ML30

    fun handleChoose(index: Int) {
        chosen = index
        referencesByIndex[index]?.let { reference = it; return }
        val formula = formulas.getOrNull(index) ?: return
        reference = ""
        referenceError = false
        savingReference = true
        scope.launch {
            try {
                val res = ApiClient.service.saveFormula(
                    SaveFormulaRequest(
                        formula = formula.toJson(),
                        customerEmail = recapEmail,
                        customerName = recapName,
                        participantColor = participant?.color,
                        language = language.code,
                    ),
                )
                val ref = if (res.isSuccessful) res.body()?.reference else null
                if (!ref.isNullOrEmpty()) {
                    reference = ref
                    referencesByIndex[index] = ref
                } else {
                    referenceError = true
                }
            } catch (_: Exception) {
                referenceError = true
            } finally {
                savingReference = false
            }
        }
    }

    fun fetchSimilar(index: Int) {
        val formula = formulas.getOrNull(index) ?: return
        similarLoading[index] = true
        similarError[index] = false
        scope.launch {
            val result = fetchSimilarPerfumes(formula, language)
            if (result == null) similarError[index] = true else similarByIndex[index] = result
            similarLoading[index] = false
        }
    }

    if (formulas.isEmpty()) return
    val selectedFormula = chosen?.let { formulas.getOrNull(it) }

    Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight)) {
        // Halo décoratif (opacity-5 côté web)
        Box(modifier = Modifier.align(Alignment.TopStart).padding(start = 200.dp, top = 150.dp).size(384.dp).blur(100.dp).background(LyloPrimary.copy(alpha = 0.05f), CircleShape))
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 200.dp, bottom = 150.dp).size(384.dp).blur(100.dp).background(LyloAccentTan.copy(alpha = 0.05f), CircleShape))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            Column(modifier = Modifier.fillMaxSize()) {
                Navbar(showActions = false, transparent = true, onLogoClick = onNavigateHome)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (participant != null && colorDef != null) {
                        ParticipantBanner(colorDef, participant, Modifier.widthIn(max = 896.dp).padding(top = if (confirmed) 8.dp else 0.dp))
                    }

                    if (!confirmed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.width(24.dp).height(1.dp).background(LyloPrimary.copy(alpha = 0.4f)))
                                Text(strings.get(R.string.quiz_results_label).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = LyloPrimary)
                                Box(Modifier.width(24.dp).height(1.dp).background(LyloPrimary.copy(alpha = 0.4f)))
                            }
                            Text(
                                if (chosen == null) strings.get(R.string.quiz_results_title) else strings.get(R.string.quiz_results_title_chosen),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = LyloPrimary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Text(
                                if (chosen == null) strings.get(R.string.quiz_results_subtitle) else strings.get(R.string.quiz_results_subtitle_chosen),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = LyloPrimary.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }

                    if (chosen == null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            formulas.forEachIndexed { i, formula ->
                                Box(
                                    modifier = Modifier
                                        .width(340.dp)
                                        .height(minOf(screenHeight * 0.68f, 720.dp))
                                        .shadow(4.dp, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { handleChoose(i) },
                                ) {
                                    FormulaCard(
                                        name = formula.profile,
                                        sizes = formula.sizes,
                                        variant = FormulaCardVariant.COMPARISON,
                                        selectedSize = sizeFor(i),
                                        onSelectedSizeChange = { selectedSizes[i] = it },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                        Text(
                            strings.get(R.string.recommendations_return_home),
                            style = MaterialTheme.typography.labelMedium,
                            color = LyloPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { onExit() }.padding(4.dp),
                        )
                    } else if (selectedFormula != null && !confirmed) {
                        val chosenIdx = chosen!!
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            formulas.forEachIndexed { i, formula ->
                                val isChosen = i == chosenIdx
                                Box(
                                    modifier = Modifier
                                        .width(320.dp)
                                        .height(minOf(screenHeight * 0.56f, 620.dp))
                                        .scale(if (isChosen) 1.02f else 0.97f)
                                        .alpha(if (isChosen) 1f else 0.3f)
                                        .shadow(4.dp, RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(2.dp, if (isChosen) LyloPrimary else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { chosen = null },
                                ) {
                                    FormulaCard(
                                        name = formula.profile,
                                        sizes = formula.sizes,
                                        variant = FormulaCardVariant.COMPARISON,
                                        selectedSize = sizeFor(i),
                                        onSelectedSizeChange = { selectedSizes[i] = it },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    if (isChosen) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Box(
                                                modifier = Modifier.shadow(12.dp, CircleShape).clip(CircleShape).background(LyloPrimary.copy(alpha = 0.9f)).padding(12.dp),
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            strings.get(R.string.quiz_results_tap_other_card),
                            style = MaterialTheme.typography.labelSmall,
                            color = LyloPrimary.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                        )
                        PrimaryPillButton(text = strings.get(R.string.quiz_results_continue), onClick = { confirmed = true })
                    } else if (selectedFormula != null) {
                        val chosenIdx = chosen!!
                        Box(modifier = Modifier.widthIn(max = 896.dp).fillMaxWidth().padding(top = if (participant == null) 24.dp else 0.dp), contentAlignment = Alignment.Center) {
                            IconButton(onClick = { confirmed = false }, modifier = Modifier.align(Alignment.CenterStart).size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.get(R.string.quiz_results_back_to_choices), tint = LyloPrimary, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                strings.get(R.string.quiz_results_personalize_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = LyloPrimary,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (selectedFormula.description.isNotEmpty()) {
                            Text(
                                selectedFormula.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = LyloPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(max = 672.dp),
                            )
                        }
                        FormulaPyramidPanel(
                            formula = selectedFormula,
                            selectedSize = sizeFor(chosenIdx),
                            onSelectedSizeChange = { selectedSizes[chosenIdx] = it },
                            language = language,
                            // Après un remplacement de note, met à jour la formule sélectionnée en
                            // local — la référence en base est déjà mise à jour côté back.
                            onFormulaChange = { updated -> formulas[chosenIdx] = updated },
                            reference = reference.ifEmpty { null },
                            similarPerfumes = SimilarPerfumesState(
                                cached = similarByIndex[chosenIdx],
                                onFetch = { fetchSimilar(chosenIdx) },
                                loading = similarLoading[chosenIdx] ?: false,
                                error = similarError[chosenIdx] ?: false,
                            ),
                        )

                        // Barre d'outils : imprimer, QR, mail en icône, puis Terminer / Personne suivante
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            var printing by remember { mutableStateOf(false) }
                            RoundIconButton(
                                icon = Icons.Default.Print,
                                contentDescription = strings.get(R.string.quiz_print),
                                loading = printing,
                                onClick = {
                                    printing = true
                                    scope.launch {
                                        printFormula(context, selectedFormula.profile, selectedFormula.sizes.forOption(sizeFor(chosenIdx)), reference)
                                        printing = false
                                    }
                                },
                            )
                            FormulaQrCode(
                                shareUrl = ShareableFormulaCodec.buildShareUrl(
                                    ShareableFormulaCodec.create(selectedFormula.profile, sizeFor(chosenIdx), selectedFormula.sizes),
                                    language,
                                    webAppBaseUrl,
                                ),
                                buttonLabel = strings.get(R.string.recommendations_qr_button),
                                title = strings.get(R.string.recommendations_qr_title),
                                subtitle = strings.get(R.string.recommendations_qr_subtitle),
                                iconOnly = true,
                            )
                            // Mail : nécessite la référence en base — spinner pendant
                            // l'enregistrement, « Réessayer » en cas d'échec.
                            when {
                                reference.isNotEmpty() -> SendFormulaMailButton(reference = reference)
                                referenceError -> RoundIconButton(
                                    icon = Icons.Default.Refresh,
                                    contentDescription = strings.get(R.string.quiz_results_reference_retry),
                                    style = RoundIconStyle.DANGER,
                                    onClick = { handleChoose(chosenIdx) },
                                )
                                else -> RoundIconButton(
                                    icon = Icons.Default.Mail,
                                    contentDescription = strings.get(R.string.send_mail_button),
                                    loading = true,
                                    enabled = false,
                                    onClick = {},
                                )
                            }
                            RobotGoButton()
                            PrimaryPillButton(
                                text = if (participant != null && !isLastParticipant) strings.get(R.string.quiz_results_next_person) else strings.get(R.string.quiz_results_finish),
                                onClick = onFinish ?: onExit,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // Duo : la couleur de la personne n'est plus le fond de page (texte marque peu lisible sur
        // certains pastels) mais un liseré en haut d'écran + la pastille du bandeau.
        if (colorDef != null) {
            Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(8.dp).background(colorDef.text))
        }

        // Référence en haut au centre, sur la ligne du logo — comme en voix.
        if (confirmed && (reference.isNotEmpty() || savingReference)) {
            Box(modifier = Modifier.align(Alignment.TopCenter).height(56.dp), contentAlignment = Alignment.Center) {
                if (savingReference) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = LyloPrimary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        Text(strings.get(R.string.quiz_results_reference_loading), style = MaterialTheme.typography.labelSmall, color = LyloPrimary.copy(alpha = 0.7f))
                    }
                } else {
                    ReferenceHeader(reference)
                }
            }
        }
    }
}

/** Référence + consigne « donnez cette référence au parfumeur » (en-tête des écrans finaux). */
@Composable
fun ReferenceHeader(reference: String) {
    val strings = LocalStringResolver.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            reference,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 1.sp,
            color = LyloPrimary,
        )
        Text(strings.get(R.string.quiz_results_give_reference_to_perfumer), fontSize = 10.sp, color = LyloPrimary.copy(alpha = 0.7f))
    }
}

/** Bandeau de la personne : barre à sa couleur, textes dans des encadrés blancs (lisibles quelle que soit la couleur). */
@Composable
private fun ParticipantBanner(colorDef: ParticipantColor, participant: JourneyParticipant, modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(colorDef.bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(colorDef.text))
            Text(
                strings.get(R.string.quiz_results_choose_your_formula).replace("{name}", strings.get(colorDef.labelRes)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = LyloPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "${participant.index + 1} / ${participant.total}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = LyloPrimary,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
