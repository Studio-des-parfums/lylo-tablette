package com.lylo.tablet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lylo.tablet.data.remote.ApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Portage de src/hooks/useConnectionTest.ts. */
enum class ConnectionVerdict { GOOD, DEGRADED, POOR }
enum class ConnectionTestStatus { IDLE, PINGING, DOWNLOADING, DONE, ERROR }

data class ConnectionTestResult(
    val downloadMbps: Double,
    val avgLatencyMs: Int,
    val packetLossPercent: Int,
    val verdict: ConnectionVerdict,
)

private const val N_PINGS = 8
private const val PING_INTERVAL_MS = 500L
private const val PING_TIMEOUT_MS = 3000L
private const val SPEEDTEST_TIMEOUT_MS = 10_000L

class ConnectionTestViewModel(application: Application) : AndroidViewModel(application) {

    private val _status = MutableStateFlow(ConnectionTestStatus.IDLE)
    val status: StateFlow<ConnectionTestStatus> = _status.asStateFlow()

    private val _pingProgress = MutableStateFlow(0)
    val pingProgress: StateFlow<Int> = _pingProgress.asStateFlow()

    private val _result = MutableStateFlow<ConnectionTestResult?>(null)
    val result: StateFlow<ConnectionTestResult?> = _result.asStateFlow()

    private var job: Job? = null

    private fun computeVerdict(download: Double, latency: Int, loss: Int): ConnectionVerdict = when {
        download < 1.5 || latency > 300 || loss > 3 -> ConnectionVerdict.POOR
        download < 3 || latency > 150 || loss >= 1 -> ConnectionVerdict.DEGRADED
        else -> ConnectionVerdict.GOOD
    }

    fun run() {
        job?.cancel()
        job = viewModelScope.launch {
            _status.value = ConnectionTestStatus.PINGING
            _pingProgress.value = 0
            _result.value = null

            val latencies = mutableListOf<Long>()
            var failed = 0

            for (i in 0 until N_PINGS) {
                val start = System.currentTimeMillis()
                val ok = withTimeoutOrNull(PING_TIMEOUT_MS) {
                    runCatching { ApiClient.service.ping() }.isSuccess
                } ?: false

                if (ok) latencies.add(System.currentTimeMillis() - start) else failed++
                _pingProgress.value = ((i + 1) * 100) / N_PINGS

                if (i < N_PINGS - 1) delay(PING_INTERVAL_MS)
            }

            if (latencies.isEmpty()) {
                _status.value = ConnectionTestStatus.ERROR
                return@launch
            }

            val avgLatency = (latencies.sum() / latencies.size).toInt()
            val packetLoss = (failed * 100) / N_PINGS

            _status.value = ConnectionTestStatus.DOWNLOADING

            var downloadMbps: Double
            try {
                val start = System.currentTimeMillis()
                val body = withTimeoutOrNull(SPEEDTEST_TIMEOUT_MS) {
                    ApiClient.service.speedtest().body()
                }
                val bytes = body?.bytes()?.size ?: (512 * 1024)
                val durationSec = (System.currentTimeMillis() - start) / 1000.0
                downloadMbps = if (durationSec > 0) {
                    Math.round((bytes * 8 / durationSec / 1_000_000.0) * 100) / 100.0
                } else 0.0
            } catch (e: Exception) {
                downloadMbps = if (avgLatency < 150) 4.0 else if (avgLatency < 300) 2.0 else 1.0
            }

            val verdict = computeVerdict(downloadMbps, avgLatency, packetLoss)
            _result.value = ConnectionTestResult(downloadMbps, avgLatency, packetLoss, verdict)
            _status.value = ConnectionTestStatus.DONE
        }
    }

    fun reset() {
        job?.cancel()
        _status.value = ConnectionTestStatus.IDLE
        _pingProgress.value = 0
        _result.value = null
    }
}
