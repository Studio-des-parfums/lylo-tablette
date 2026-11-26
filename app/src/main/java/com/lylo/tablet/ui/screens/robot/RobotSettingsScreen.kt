package com.lylo.tablet.ui.screens.robot

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.data.LocalPrefs
import com.lylo.tablet.data.model.AuthUser
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.robot.RobotController
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary

/**
 * Configuration robot (réservée au compte de l'équipe, icône dans la navbar) : choix du point
 * de la carte vers lequel le bouton robot des écrans de formule envoie le robot.
 */
@Composable
fun RobotSettingsScreen(
    user: AuthUser?,
    onNavigateHome: () -> Unit,
) {
    val strings = LocalStringResolver.current
    val context = LocalContext.current
    val prefs = remember { LocalPrefs(context) }

    LaunchedEffect(user) { if (user == null) onNavigateHome() }
    if (user == null) return

    val available by RobotController.available.collectAsState()
    var selected by remember { mutableStateOf(prefs.getString(LocalPrefs.KEY_ROBOT_GOAL)) }
    var places by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(available, refreshTick) {
        if (!available) return@LaunchedEffect
        loading = true
        error = null
        RobotController.fetchPlaces { result, err ->
            places = result
            error = err
            loading = false
        }
    }

    fun select(place: String?) {
        selected = place
        prefs.putString(LocalPrefs.KEY_ROBOT_GOAL, place)
    }

    Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onNavigateHome, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = LyloPrimary)
                }
                Text(
                    strings.get(R.string.robot_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = LyloPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (available) {
                    IconButton(onClick = { refreshTick++ }, enabled = !loading, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = LyloPrimary)
                    }
                }
            }

            Text(strings.get(R.string.robot_settings_subtitle), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B7280))
            Text(
                strings.get(R.string.robot_settings_current, selected ?: strings.get(R.string.robot_settings_none)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = LyloPrimary,
            )

            when {
                !available -> Text(strings.get(R.string.robot_settings_unavailable), color = Color(0xFFDC2626))
                loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LyloPrimary)
                }
                error != null -> Text(error.orEmpty(), color = Color(0xFFDC2626))
                places.isEmpty() -> Text(strings.get(R.string.robot_settings_empty), color = Color(0xFF9CA3AF))
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(places) { place ->
                        PlaceRow(place = place, selected = place == selected, onClick = { select(place) })
                    }
                }
            }

            // Un point enregistré qui n'existe plus sur la carte reste affiché ci-dessus :
            // on laisse la possibilité de le retirer pour masquer le bouton.
            if (selected != null) {
                Text(
                    strings.get(R.string.robot_settings_clear),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.clickable { select(null) }.padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaceRow(place: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) LyloPrimary.copy(alpha = 0.08f) else Color.White)
            .border(1.dp, if (selected) LyloPrimary else LyloPrimary.copy(alpha = 0.15f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(place, style = MaterialTheme.typography.bodyLarge, color = LyloPrimary, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LyloPrimary)
        }
    }
}
