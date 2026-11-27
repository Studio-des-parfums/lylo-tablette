package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.GreenSuccess
import com.lylo.tablet.ui.theme.GreenSuccessBg
import com.lylo.tablet.ui.theme.GreenSuccessBorder
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.OrangeWarn
import com.lylo.tablet.ui.theme.OrangeWarnBg
import com.lylo.tablet.ui.theme.OrangeWarnBorder
import com.lylo.tablet.ui.theme.RedError
import com.lylo.tablet.ui.theme.RedErrorBg
import com.lylo.tablet.ui.theme.RedErrorBorder
import com.lylo.tablet.ui.theme.glassPanel
import com.lylo.tablet.viewmodel.ConnectionTestStatus
import com.lylo.tablet.viewmodel.ConnectionTestViewModel
import com.lylo.tablet.viewmodel.ConnectionVerdict

/** Portage de src/components/preparation/ConnectionTest.tsx. */
@Composable
fun ConnectionTest(modifier: Modifier = Modifier, viewModel: ConnectionTestViewModel = viewModel()) {
    val strings = LocalStringResolver.current
    var open by remember { mutableStateOf(false) }
    val status by viewModel.status.collectAsState()
    val pingProgress by viewModel.pingProgress.collectAsState()
    val result by viewModel.result.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable {
                open = true
                viewModel.run()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = LyloPrimary, modifier = Modifier.size(18.dp))
            Text(strings.get(R.string.preparation_conn_test), style = MaterialTheme.typography.bodyMedium, color = LyloPrimary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LyloPrimary.copy(alpha = 0.4f))
    }

    if (open) {
        LyloDialog(onDismissRequest = { viewModel.reset(); open = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .glassPanel(shape = RoundedCornerShape(20.dp))
                    .padding(20.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.get(R.string.preparation_conn_modal_title), style = MaterialTheme.typography.titleMedium, color = LyloPrimary)
                    IconButton(onClick = { viewModel.reset(); open = false }) {
                        Icon(Icons.Default.Close, contentDescription = strings.get(R.string.preparation_conn_close), tint = LyloPrimary.copy(alpha = 0.5f))
                    }
                }

                when (status) {
                    ConnectionTestStatus.PINGING -> Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.get(R.string.preparation_conn_phase_latency), style = MaterialTheme.typography.labelSmall, color = LyloPrimary.copy(alpha = 0.6f))
                            Text("$pingProgress%", style = MaterialTheme.typography.labelSmall, color = LyloPrimary)
                        }
                        LinearProgressIndicator(
                            progress = { pingProgress / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = LyloPrimary,
                            trackColor = Color(0xFFEDE8E5),
                        )
                    }

                    ConnectionTestStatus.DOWNLOADING -> Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LyloPrimary)
                        Text(strings.get(R.string.preparation_conn_phase_download), color = LyloPrimary)
                    }

                    ConnectionTestStatus.ERROR -> Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RedErrorBg)
                                .border(1.dp, RedErrorBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = RedError)
                            Text(strings.get(R.string.preparation_conn_error), color = RedError)
                        }
                        LyloButton(onClick = { viewModel.run() }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Text(strings.get(R.string.preparation_conn_retry))
                        }
                    }

                    ConnectionTestStatus.DONE -> result?.let { r ->
                        val style = verdictStyle(r.verdict)
                        val bg = style.bg
                        val border = style.border
                        val fg = style.fg
                        val icon = style.icon
                        val verdictTitleRes = when (r.verdict) {
                            ConnectionVerdict.GOOD -> R.string.preparation_conn_verdict_good
                            ConnectionVerdict.DEGRADED -> R.string.preparation_conn_verdict_degraded
                            ConnectionVerdict.POOR -> R.string.preparation_conn_verdict_poor
                        }
                        val verdictSubRes = when (r.verdict) {
                            ConnectionVerdict.GOOD -> R.string.preparation_conn_verdict_sub_good
                            ConnectionVerdict.DEGRADED -> R.string.preparation_conn_verdict_sub_degraded
                            ConnectionVerdict.POOR -> R.string.preparation_conn_verdict_sub_poor
                        }

                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(1.dp, border, RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(icon, contentDescription = null, tint = fg)
                                Column {
                                    Text(strings.get(verdictTitleRes), color = fg, style = MaterialTheme.typography.bodyMedium)
                                    Text(strings.get(verdictSubRes), color = fg, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetricTile(strings.get(R.string.preparation_conn_download), "${r.downloadMbps}", "Mbps", Modifier.weight(1f))
                                MetricTile(strings.get(R.string.preparation_conn_latency), "${r.avgLatencyMs}", "ms", Modifier.weight(1f))
                                MetricTile(strings.get(R.string.preparation_conn_loss), "${r.packetLossPercent}", "%", Modifier.weight(1f))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    strings.get(R.string.preparation_conn_retry),
                                    color = LyloPrimary.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.clickable { viewModel.run() },
                                )
                                LyloButton(onClick = { viewModel.reset(); open = false }) {
                                    Text(strings.get(R.string.preparation_conn_close))
                                }
                            }
                        }
                    }

                    ConnectionTestStatus.IDLE -> Unit
                }
            }
        }
    }
}

private data class VerdictStyle(
    val bg: Color,
    val border: Color,
    val fg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private fun verdictStyle(verdict: ConnectionVerdict): VerdictStyle = when (verdict) {
    ConnectionVerdict.GOOD -> VerdictStyle(GreenSuccessBg, GreenSuccessBorder, GreenSuccess, Icons.Default.CheckCircle)
    ConnectionVerdict.DEGRADED -> VerdictStyle(OrangeWarnBg, OrangeWarnBorder, OrangeWarn, Icons.Default.Warning)
    ConnectionVerdict.POOR -> VerdictStyle(RedErrorBg, RedErrorBorder, RedError, Icons.Default.Error)
}

@Composable
private fun MetricTile(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .border(1.dp, LyloPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LyloPrimary.copy(alpha = 0.55f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = LyloPrimary)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = LyloPrimary.copy(alpha = 0.4f))
    }
}
