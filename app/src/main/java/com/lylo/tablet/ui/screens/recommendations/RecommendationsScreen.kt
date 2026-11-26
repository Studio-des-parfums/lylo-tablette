package com.lylo.tablet.ui.screens.recommendations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.data.LocalPrefs
import com.lylo.tablet.data.ShareableFormulaCodec
import com.lylo.tablet.data.model.SizeOption
import com.lylo.tablet.i18n.LanguageStore
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.print.printFormula
import com.lylo.tablet.ui.components.AvatarSection
import com.lylo.tablet.ui.components.BottomBar
import com.lylo.tablet.ui.components.FormulaCard
import com.lylo.tablet.ui.components.FormulaCardVariant
import com.lylo.tablet.ui.components.FormulaPyramidPanel
import com.lylo.tablet.ui.components.FormulaQrCode
import com.lylo.tablet.ui.components.Navbar
import com.lylo.tablet.ui.components.PrimaryPillButton
import com.lylo.tablet.ui.components.RobotGoButton
import com.lylo.tablet.ui.components.RoundIconButton
import com.lylo.tablet.ui.components.SendFormulaMailButton
import com.lylo.tablet.ui.components.TranscriptPanel
import com.lylo.tablet.ui.screens.quiz.ReferenceHeader
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.viewmodel.SessionViewModel
import io.livekit.android.room.Room
import kotlinx.coroutines.launch

/**
 * Portage de src/app/recommendations/page.tsx. Deux vues selon le nombre de formules restantes
 * dans SessionViewModel.formulas :
 * - vue « 2 formules » : avatar + les deux cartes côte à côte, tout tient dans l'écran ;
 * - vue « 1 formule » (formule choisie à l'oral) : référence en haut au centre, agent en haut à
 *   droite, pyramide olfactive (notes remplaçables), puis barre d'outils imprimer / QR / mail /
 *   Terminer.
 * Toutes deux terminées par le BottomBar LiveKit.
 */
@Composable
fun RecommendationsScreen(
    sessionViewModel: SessionViewModel,
    room: Room?,
    avatarFallback: Any,
    avatarEnabled: Boolean,
    webAppBaseUrl: String,
    onReturnHome: () -> Unit,
    onInterrupt: () -> Unit,
    onResumeListen: () -> Unit,
    onResume: () -> Unit,
    onMicToggle: (Boolean) -> Unit,
) {
    val strings = LocalStringResolver.current
    val context = LocalContext.current
    val prefs = remember { LocalPrefs(context) }
    val scope = rememberCoroutineScope()

    val formulas by sessionViewModel.formulas.collectAsState()
    val formulaReference by sessionViewModel.formulaReference.collectAsState()
    val profile by sessionViewModel.profile.collectAsState()
    val sessionData by sessionViewModel.sessionData.collectAsState()
    val agentName by sessionViewModel.agentName.collectAsState()
    val agentState by sessionViewModel.agentState.collectAsState()
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val transcripts by sessionViewModel.transcripts.collectAsState()
    val avatarDisabled by sessionViewModel.avatarDisabled.collectAsState()

    var selectedSize by remember { mutableStateOf(SizeOption.ML30) }
    var printing by remember { mutableStateOf(false) }
    // Levé ici pour que TranscriptPanel soit rendu en overlay plein écran sur le Box racine.
    var transcriptOpen by remember { mutableStateOf(false) }

    val language = remember { LanguageStore.resolveStoredLanguage(prefs) }
    val isSingle = formulas.size == 1
    val selectedFormula = formulas.firstOrNull()

    Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight)) {
        // Décorations de fond
        Box(modifier = Modifier.align(Alignment.TopEnd).fillMaxSize(0.4f).blur(120.dp).background(LyloPrimary.copy(alpha = 0.03f), CircleShape))
        Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxSize(0.4f).blur(120.dp).background(LyloPrimary.copy(alpha = 0.05f), CircleShape))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            Column(modifier = Modifier.fillMaxSize()) {
                Navbar(showActions = false, transparent = true, onLogoClick = onReturnHome)

                if (isSingle && selectedFormula != null) {
                    // ════ VUE 1 FORMULE ════
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FormulaPyramidPanel(
                            formula = selectedFormula,
                            selectedSize = selectedSize,
                            onSelectedSizeChange = { selectedSize = it },
                            language = language,
                            onFormulaChange = { sessionViewModel.replaceSelectedFormula(it) },
                            reference = formulaReference,
                            sessionId = sessionData?.sessionId,
                        )

                        // Imprimer / QR code / Recevoir par mail / Terminer
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RoundIconButton(
                                icon = Icons.Default.Print,
                                contentDescription = strings.get(R.string.recommendations_page_print),
                                loading = printing,
                                onClick = {
                                    printing = true
                                    scope.launch {
                                        printFormula(context, selectedFormula.profile, selectedFormula.sizes.forOption(selectedSize))
                                        printing = false
                                    }
                                },
                            )
                            val shareUrl = remember(selectedFormula, selectedSize, language) {
                                ShareableFormulaCodec.buildShareUrl(
                                    ShareableFormulaCodec.create(selectedFormula.profile, selectedSize, selectedFormula.sizes),
                                    language,
                                    webAppBaseUrl,
                                )
                            }
                            FormulaQrCode(
                                shareUrl = shareUrl,
                                buttonLabel = strings.get(R.string.recommendations_qr_button),
                                title = strings.get(R.string.recommendations_qr_title),
                                subtitle = strings.get(R.string.recommendations_qr_subtitle),
                                iconOnly = true,
                            )
                            // Recevoir par mail : toujours visible ; la référence arrive avec
                            // l'événement formula_selected de l'agent, désactivé en attendant.
                            val ref = formulaReference
                            if (ref != null) {
                                SendFormulaMailButton(reference = ref, initialFirstName = profile["first_name"])
                            } else {
                                RoundIconButton(
                                    icon = Icons.Default.Mail,
                                    contentDescription = strings.get(R.string.send_mail_button),
                                    loading = true,
                                    enabled = false,
                                    onClick = {},
                                )
                            }
                            RobotGoButton()
                            // Terminer : fin de session et retour à l'accueil
                            PrimaryPillButton(
                                text = strings.get(R.string.quiz_results_finish),
                                onClick = onReturnHome,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                } else if (formulas.isNotEmpty()) {
                    // ════ VUE 2 FORMULES ════ tout tient dans l'écran pour garder la barre visible
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (screenHeight > 580.dp) {
                            AvatarSection(name = "", role = "", fallbackImageUrl = avatarFallback, avatarEnabled = avatarEnabled, room = room, avatarDisabled = avatarDisabled, size = 100.dp)
                        }
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 1400.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        ) {
                            formulas.forEach { formula ->
                                FormulaCard(
                                    name = formula.profile,
                                    sizes = formula.sizes,
                                    variant = FormulaCardVariant.COMPARISON,
                                    selectedSize = selectedSize,
                                    onSelectedSizeChange = { selectedSize = it },
                                    modifier = Modifier.weight(1f, fill = false).widthIn(max = 340.dp).fillMaxHeight().heightIn(min = 260.dp, max = 720.dp),
                                )
                            }
                        }
                        Text(
                            strings.get(R.string.recommendations_return_home),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.clickable { onReturnHome() }.padding(4.dp),
                        )
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(strings.get(R.string.recommendations_no_formulas), color = Color(0xFF9CA3AF), style = MaterialTheme.typography.titleMedium)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(vertical = if (isSingle) 12.dp else 8.dp), contentAlignment = Alignment.Center) {
                    if (sessionData != null) {
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
                        )
                    }
                }
            }
        }

        if (isSingle && selectedFormula != null) {
            // En-tête sur la ligne du logo : référence au centre, agent à droite — libère l'axe
            // vertical pour que la barre d'outils reste visible.
            formulaReference?.let { ref ->
                Box(modifier = Modifier.align(Alignment.TopCenter).height(56.dp), contentAlignment = Alignment.Center) {
                    ReferenceHeader(ref)
                }
            }
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("$agentName AI", style = MaterialTheme.typography.labelMedium, fontStyle = FontStyle.Italic, color = LyloPrimary)
                AvatarSection(name = "", role = "", fallbackImageUrl = avatarFallback, avatarEnabled = avatarEnabled, room = room, avatarDisabled = avatarDisabled, size = 56.dp)
            }
        }

        TranscriptPanel(
            open = transcriptOpen,
            transcripts = transcripts,
            agentName = agentName,
            onToggle = { transcriptOpen = !transcriptOpen },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}
