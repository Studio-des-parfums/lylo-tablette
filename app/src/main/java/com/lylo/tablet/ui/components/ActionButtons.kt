package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lylo.tablet.ui.theme.LyloPrimary

enum class RoundIconStyle { SURFACE, FILLED, DANGER }

/**
 * Bouton rond 36 dp de la barre d'outils des écrans finaux (imprimer / QR / mail / réessayer) —
 * équivalent des `size-9 rounded-full` côté web : SURFACE = fond clair bordé, FILLED = fond primary,
 * DANGER = bordure rouge (référence indisponible → réessayer).
 */
@Composable
fun RoundIconButton(
    icon: ImageVector?,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RoundIconStyle = RoundIconStyle.SURFACE,
    enabled: Boolean = true,
    loading: Boolean = false,
    iconSize: Int = 18,
) {
    val (bg, fg, border) = when (style) {
        RoundIconStyle.SURFACE -> Triple(Color.White.copy(alpha = 0.9f), LyloPrimary, LyloPrimary.copy(alpha = 0.25f))
        RoundIconStyle.FILLED -> Triple(LyloPrimary, Color.White, LyloPrimary)
        RoundIconStyle.DANGER -> Triple(Color.White, Color(0xFFDC2626), Color(0xFFFCA5A5))
    }
    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(if (style == RoundIconStyle.FILLED) 6.dp else 2.dp, CircleShape, ambientColor = LyloPrimary.copy(alpha = 0.2f), spotColor = LyloPrimary.copy(alpha = 0.2f))
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .alpha(if (enabled || loading) 1f else 0.5f)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = contentDescription, tint = fg, modifier = Modifier.size(iconSize.dp))
        }
    }
}

/** Bouton pilule primary « Terminer → » / « Personne suivante → » / « Continuer → ». */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(999.dp), ambientColor = LyloPrimary.copy(alpha = 0.2f), spotColor = LyloPrimary.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(999.dp))
            .background(LyloPrimary)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
