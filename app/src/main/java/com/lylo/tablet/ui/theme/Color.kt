package com.lylo.tablet.ui.theme

import androidx.compose.ui.graphics.Color

// ── Palettes par thème — reprises 1:1 de src/app/globals.css ([data-brand="lylo"|"help"]) ──
// Le thème est fixé au build (LYLO_BRAND), comme NEXT_PUBLIC_BRAND côté web.
private val isHelpTheme = com.lylo.tablet.data.Brand.active.theme == com.lylo.tablet.data.ThemeId.HELP

val LyloPrimary = if (isHelpTheme) Color(0xFF071B2A) else Color(0xFF996F56)
val LyloBackgroundLight = Color(0xFFF7F7F6)
val LyloBackgroundDark = if (isHelpTheme) Color(0xFF071B2A) else Color(0xFF1C1816)
val LyloAccentTan = if (isHelpTheme) Color(0xFFD9DEE3) else Color(0xFFD7CDC6)
val LyloSecondary = LyloAccentTan // alias, identique à accent-tan côté web
val LyloTextDark = if (isHelpTheme) Color(0xFF071B2A) else Color(0xFF171412)
val LyloBrandLight = Color(0xFFFDFBF9)

// Couleurs utilitaires additionnelles utilisées dans les composants (rouge/vert d'état,
// stone-* de Tailwind, etc.)
val LyloLuxuryTitle = Color(0xFF333333)
val StoneGray400 = Color(0xFF9CA3AF)
val StoneGray200 = Color(0xFFE5E7EB)
val StoneGray50 = Color(0xFFFAFAF9)
val GreenSuccess = Color(0xFF16A34A)
val GreenSuccessBg = Color(0xFFF0FDF4)
val GreenSuccessBorder = Color(0xFFBBF7D0)
val OrangeWarn = Color(0xFFEA580C)
val OrangeWarnBg = Color(0xFFFFF7ED)
val OrangeWarnBorder = Color(0xFFFED7AA)
val RedError = Color(0xFFEF4444)
val RedErrorBg = Color(0xFFFEF2F2)
val RedErrorBorder = Color(0xFFFECACA)
val AmberGradientStart = Color(0xFFFCD34D)
val AmberGradientEnd = Color(0xFFF59E0B)
val AmberText = Color(0xFF451A03)

// ── Couleurs participants (quiz visuel multi-utilisateurs) — ConfigPanel.PARTICIPANT_COLORS
data class ParticipantColor(val id: String, val labelRes: Int, val bg: Color, val text: Color)

val ParticipantColors = listOf(
    ParticipantColor("rouge", com.lylo.tablet.R.string.configure_color_rouge, Color(0xFFFFADAD), Color(0xFF7A1A1A)),
    ParticipantColor("vert", com.lylo.tablet.R.string.configure_color_vert, Color(0xFFB5EAD7), Color(0xFF1A5C3A)),
    ParticipantColor("bleu", com.lylo.tablet.R.string.configure_color_bleu, Color(0xFFAEC6F8), Color(0xFF1A2E5C)),
    ParticipantColor("jaune", com.lylo.tablet.R.string.configure_color_jaune, Color(0xFFFFEAA7), Color(0xFF5C4A00)),
    ParticipantColor("violet", com.lylo.tablet.R.string.configure_color_violet, Color(0xFFD4BFFF), Color(0xFF3A1A6B)),
    ParticipantColor("orange", com.lylo.tablet.R.string.configure_color_orange, Color(0xFFFFD6A5), Color(0xFF6B3000)),
    ParticipantColor("rose", com.lylo.tablet.R.string.configure_color_rose, Color(0xFFFFCCE0), Color(0xFF6B0038)),
    ParticipantColor("turquoise", com.lylo.tablet.R.string.configure_color_turquoise, Color(0xFFB5F0F0), Color(0xFF0A4A4A)),
)
