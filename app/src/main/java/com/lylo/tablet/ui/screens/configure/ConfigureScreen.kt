package com.lylo.tablet.ui.screens.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.components.LyloButton
import com.lylo.tablet.ui.components.Navbar
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.LyloSecondary
import com.lylo.tablet.ui.theme.ParticipantColors
import com.lylo.tablet.ui.theme.cardShadow
import com.lylo.tablet.ui.theme.glassPanel
import com.lylo.tablet.viewmodel.ConfigViewModel

/**
 * Portage de src/app/configure/page.tsx + ConfigPanel.tsx. Écran unique reproduisant : le
 * sélecteur de mode de réponse (voice/click/quiz), puis selon le mode soit le panneau complet
 * (langue/persona/profondeur/mode IA + avatar/email/test connexion/imprimante), soit
 * le panneau visuel simplifié (langue/profondeur + multi-participants avec couleurs).
 */
@Composable
fun ConfigureScreen(
    onNavigateBack: () -> Unit,
    onContinueToQuiz: () -> Unit,
    onContinueToPreparation: () -> Unit,
    onNavigateHome: () -> Unit,
    currentLanguage: com.lylo.tablet.i18n.AppLanguage,
    onLanguageSelect: (com.lylo.tablet.i18n.AppLanguage) -> Unit,
    viewModel: ConfigViewModel = viewModel(),
) {
    val strings = LocalStringResolver.current

    val persona by viewModel.persona.collectAsState()
    val depth by viewModel.depth.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val inputMode by viewModel.inputMode.collectAsState()
    val avatar by viewModel.avatar.collectAsState()
    val participantCount by viewModel.participantCount.collectAsState()
    val selectedColors by viewModel.selectedColors.collectAsState()

    val isQuiz = inputMode == "quiz"
    val colorsValid = viewModel.colorsValid()

    Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight)) {
        // Blobs décoratifs de fond — portage de configure/page.tsx (opacity-5, blur-[100px]).
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 80.dp, top = 80.dp)
                .size(384.dp)
                .blur(100.dp)
                .background(LyloPrimary.copy(alpha = 0.05f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 80.dp, bottom = 80.dp)
                .size(384.dp)
                .blur(100.dp)
                .background(LyloSecondary.copy(alpha = 0.05f), CircleShape),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Navbar(showActions = false, transparent = true, onLogoClick = onNavigateHome)

            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .padding(24.dp)
                        .cardShadow(RoundedCornerShape(16.dp))
                        .verticalScroll(rememberScrollState())
                        .glassPanel(shape = RoundedCornerShape(16.dp))
                        .border(1.dp, LyloPrimary.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // ── Header ──
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            strings.get(R.string.configure_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = LyloPrimary,
                        )
                        Text(
                            strings.get(R.string.configure_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = LyloPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            strings.get(R.string.configure_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LyloPrimary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                        )
                    }

                    // ── Mode de réponse — toujours en premier ──
                    InputModeSelector(inputMode, viewModel::setInputMode)

                    if (!isQuiz) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                LanguagePicker(
                                    current = currentLanguage,
                                    onSelect = onLanguageSelect,
                                )
                                PersonaSelector(persona, viewModel::setPersona)
                                DepthSelector(depth, viewModel::setDepth)
                                ModeSelector(mode, viewModel::setMode)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ToggleRow(
                                    label = if (avatar) strings.get(R.string.configure_avatar_on) else strings.get(R.string.configure_avatar_off),
                                    icon = if (avatar) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    checked = avatar,
                                    onToggle = { viewModel.toggleAvatar() },
                                    title = strings.get(R.string.configure_avatar_title),
                                    hint = strings.get(R.string.configure_avatar_hint),
                                )

                                com.lylo.tablet.ui.components.ConnectionTest()
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            LanguagePicker(current = currentLanguage, onSelect = onLanguageSelect)
                            DepthSelector(depth, viewModel::setDepth)
                        }

                        // ── Mode duo : 1 ou 2 personnes ──
                        Column {
                            Text(strings.get(R.string.configure_duo_title).uppercase(), style = MaterialTheme.typography.labelSmall, color = LyloPrimary.copy(alpha = 0.6f))
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(false, true).forEach { duo ->
                                    val n = if (duo) 2 else 1
                                    val selected = (participantCount == 2) == duo
                                    Text(
                                        text = strings.get(if (duo) R.string.configure_duo_yes else R.string.configure_duo_no),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selected) Color.White else LyloPrimary.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) LyloPrimary else Color.White.copy(alpha = 0.4f))
                                            .border(2.dp, if (selected) LyloPrimary else LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setParticipantCount(n) }
                                            .padding(vertical = 10.dp),
                                    )
                                }
                            }
                        }

                        if (participantCount > 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(strings.get(R.string.configure_participant_color_title), style = MaterialTheme.typography.labelSmall, color = LyloPrimary.copy(alpha = 0.6f))
                                (0 until participantCount).forEach { idx ->
                                    val chosen = selectedColors.getOrNull(idx) ?: ""
                                    Column {
                                        Text(strings.get(R.string.configure_participant_label).replace("{n}", (idx + 1).toString()), style = MaterialTheme.typography.bodySmall, color = LyloPrimary.copy(alpha = 0.5f))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                            items(ParticipantColors) { colorDef ->
                                                val takenByOther = viewModel.usedColorsExcluding(idx).contains(colorDef.id)
                                                val isSelected = chosen == colorDef.id
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(colorDef.bg.copy(alpha = if (takenByOther) 0.3f else 1f))
                                                        .border(2.dp, if (isSelected) colorDef.text else Color.Transparent, RoundedCornerShape(999.dp))
                                                        .clickable(enabled = !takenByOther) { viewModel.selectColor(idx, colorDef.id) }
                                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                                ) {
                                                    Text(strings.get(colorDef.labelRes), color = colorDef.text, style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!colorsValid) {
                                    Text(
                                        strings.get(R.string.configure_participant_color_error),
                                        color = Color(0xFFD97706),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    LyloButton(
                        onClick = {
                            viewModel.persistAndContinue()
                            if (isQuiz) onContinueToQuiz() else onContinueToPreparation()
                        },
                        enabled = viewModel.isFormComplete(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.get(R.string.configure_start))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Text(
                strings.get(R.string.configure_footer),
                style = MaterialTheme.typography.labelSmall,
                color = LyloPrimary.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onToggle: () -> Unit,
    title: String,
    hint: String,
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = LyloPrimary.copy(alpha = 0.6f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = LyloPrimary)
                Text(label, style = MaterialTheme.typography.bodyMedium, color = LyloPrimary)
            }
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (checked) LyloPrimary else LyloPrimary.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .then(if (checked) Modifier.padding(start = 16.dp) else Modifier),
                )
            }
        }
        if (hint.isNotEmpty()) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9C8880), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

