package com.lylo.tablet.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.viewmodel.MicCalibratorViewModel
import com.lylo.tablet.viewmodel.MicLevelZone
import com.lylo.tablet.viewmodel.MicState

/**
 * Portage de src/components/preparation/MicCalibrator.tsx. Demande RECORD_AUDIO à la volée si
 * pas déjà accordée (le web équivalent déclenche implicitement le prompt navigateur via
 * getUserMedia — ici on passe par le launcher de permission runtime Android standard).
 */
@Composable
fun MicCalibrator(modifier: Modifier = Modifier, viewModel: MicCalibratorViewModel = viewModel()) {
    val strings = LocalStringResolver.current
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }

    val micState by viewModel.micState.collectAsState()
    val level by viewModel.level.collectAsState()
    val levelZone by viewModel.levelZone.collectAsState()
    val sliderValue by viewModel.sliderValue.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startTest()
    }

    fun requestAndStart() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startTest() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    IconButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Default.Tune, contentDescription = strings.get(R.string.preparation_mic_title), tint = LyloPrimary)
    }

    if (open) {
        LyloDialog(onDismissRequest = { viewModel.stopTest(); open = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.get(R.string.preparation_mic_title), style = MaterialTheme.typography.titleMedium, color = LyloPrimary)
                    IconButton(onClick = { viewModel.stopTest(); open = false }) {
                        Icon(Icons.Default.Close, contentDescription = strings.get(R.string.auth_close), tint = LyloPrimary.copy(alpha = 0.5f))
                    }
                }

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.get(R.string.preparation_mic_level), style = MaterialTheme.typography.labelSmall, color = LyloPrimary.copy(alpha = 0.6f))
                        Text("${sliderValue * 2}%", style = MaterialTheme.typography.labelSmall, color = LyloPrimary)
                    }

                    // Barre de niveau (couleur selon la zone)
                    val levelColor = when (levelZone) {
                        MicLevelZone.SILENT -> Color(0xFFD4C9C3)
                        MicLevelZone.GOOD -> Color(0xFF22C55E)
                        MicLevelZone.LOUD -> Color(0xFFFB923C)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFEDE8E5)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (micState == MicState.ACTIVE) level.coerceIn(0f, 1f) else 0f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(levelColor),
                        )
                    }

                    Slider(
                        value = sliderValue.toFloat(),
                        onValueChange = { viewModel.setSlider(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        colors = SliderDefaults.colors(thumbColor = LyloPrimary, activeTrackColor = LyloPrimary),
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.get(R.string.preparation_mic_min), style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0A49E))
                        Text(strings.get(R.string.preparation_mic_max), style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0A49E))
                    }

                    if (micState == MicState.ACTIVE) {
                        val statusRes = when (levelZone) {
                            MicLevelZone.SILENT -> R.string.preparation_mic_silent
                            MicLevelZone.GOOD -> R.string.preparation_mic_good
                            MicLevelZone.LOUD -> R.string.preparation_mic_loud
                        }
                        Text(
                            strings.get(statusRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9C8880),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    when (micState) {
                        MicState.IDLE -> LyloButton(onClick = { requestAndStart() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(strings.get(R.string.preparation_mic_test), modifier = Modifier.padding(start = 8.dp))
                        }

                        MicState.REQUESTING -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LyloPrimary)
                            Text(strings.get(R.string.preparation_mic_requesting), color = LyloPrimary)
                        }

                        MicState.ACTIVE -> LyloButton(
                            onClick = { viewModel.stopTest() },
                            variant = LyloButtonVariant.OUTLINE,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(strings.get(R.string.preparation_mic_stop), modifier = Modifier.padding(start = 8.dp))
                        }

                        MicState.ERROR -> Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF7ED))
                                    .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Default.MicOff, contentDescription = null, tint = Color(0xFFFB923C))
                                Text(strings.get(R.string.preparation_mic_error), color = Color(0xFFC2410C), style = MaterialTheme.typography.bodySmall)
                            }
                            LyloButton(onClick = { requestAndStart() }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                Text(strings.get(R.string.preparation_mic_retry))
                            }
                        }
                    }
                }
            }
        }
    }
}
