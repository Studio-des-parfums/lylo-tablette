package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.lylo.tablet.R
import com.lylo.tablet.data.remote.SimilarPerfume
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary

/** Cache des parfums ressemblants piloté par le parent (par formule), pour ne jamais relancer la
 * recherche (LLM + web) en revenant sur une formule déjà consultée pendant la session. */
data class SimilarPerfumesState(
    val cached: List<SimilarPerfume>?,
    val onFetch: () -> Unit,
    val loading: Boolean,
    val error: Boolean,
)

/**
 * Portage de src/components/recommendations/SimilarPerfumesButton.tsx : bouton « ? » qui ouvre
 * une bulle avec 2 parfums du commerce ressemblant à la formule. Un reclic referme la bulle.
 */
@Composable
fun SimilarPerfumesButton(state: SimilarPerfumesState, modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current
    var open by remember { mutableStateOf(false) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .shadow(if (open) 0.dp else 1.dp, CircleShape)
                .clip(CircleShape)
                .background(if (open) LyloPrimary else Color.White)
                .border(1.dp, if (open) LyloPrimary else LyloPrimary.copy(alpha = 0.25f), CircleShape)
                .clickable {
                    if (open) {
                        open = false
                    } else {
                        open = true
                        if (state.cached == null && !state.loading) state.onFetch()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = strings.get(R.string.similar_perfumes_button),
                tint = if (open) Color.White else LyloPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
        }

        if (open) {
            val offsetY = with(density) { 36.dp.roundToPx() }
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, offsetY),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = false),
            ) {
                Column(
                    modifier = Modifier
                        .width(224.dp)
                        .shadow(12.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when {
                        state.loading -> Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(color = LyloPrimary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                            Text(
                                strings.get(R.string.similar_perfumes_loading),
                                style = MaterialTheme.typography.labelSmall,
                                color = LyloPrimary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        state.error -> Text(
                            strings.get(R.string.similar_perfumes_error),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEF4444),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                        !state.cached.isNullOrEmpty() -> state.cached.forEach { p ->
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LyloPrimary)) { append(p.name) }
                                    append(" — ${p.brand}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4F443E),
                            )
                        }
                        state.cached != null -> Text(
                            strings.get(R.string.similar_perfumes_error),
                            style = MaterialTheme.typography.labelSmall,
                            color = LyloPrimary.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
