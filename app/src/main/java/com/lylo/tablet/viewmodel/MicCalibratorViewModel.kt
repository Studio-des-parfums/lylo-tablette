package com.lylo.tablet.viewmodel

import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * Portage de src/components/preparation/MicCalibrator.tsx : au lieu de Web Audio API
 * (AudioContext + AnalyserNode + GainNode), on utilise AudioRecord pour lire le buffer PCM brut
 * du micro et calculer une amplitude moyenne (RMS-like) en continu, appliquant un gain logiciel
 * réglable par un slider — équivalent fonctionnel du pipeline source → gainNode → analyser du web.
 *
 * IMPORTANT : nécessite la permission RECORD_AUDIO déjà accordée avant d'appeler [startTest]
 * (voir ui/screens PermissionGate / MainActivity).
 */
enum class MicState { IDLE, REQUESTING, ACTIVE, ERROR }
enum class MicLevelZone { SILENT, GOOD, LOUD }

class MicCalibratorViewModel(application: Application) : AndroidViewModel(application) {

    private val _micState = MutableStateFlow(MicState.IDLE)
    val micState: StateFlow<MicState> = _micState.asStateFlow()

    private val _level = MutableStateFlow(0f) // 0..1
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _levelZone = MutableStateFlow(MicLevelZone.SILENT)
    val levelZone: StateFlow<MicLevelZone> = _levelZone.asStateFlow()

    // Slider 0..100, 50 = gain unitaire (1.0x) — miroir de sliderToGain() côté web.
    private val _sliderValue = MutableStateFlow(50)
    val sliderValue: StateFlow<Int> = _sliderValue.asStateFlow()

    val gainPercent: StateFlow<Int> = _sliderValue // exposé tel quel : 50 = 100%, cohérent avec l'échelle web

    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null

    fun setSlider(value: Int) {
        _sliderValue.value = value.coerceIn(0, 100)
    }

    @Suppress("MissingPermission") // Permission vérifiée par l'appelant avant startTest()
    fun startTest() {
        stopInternal()
        _micState.value = MicState.REQUESTING

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) {
            _micState.value = MicState.ERROR
            return
        }

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2,
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                _micState.value = MicState.ERROR
                return
            }
            audioRecord = record
            record.startRecording()
            _micState.value = MicState.ACTIVE

            recordJob = viewModelScope.launch {
                val buffer = ShortArray(minBufferSize)
                while (isActive && audioRecord != null) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val gain = _sliderValue.value / 50f
                        var sum = 0.0
                        for (i in 0 until read) {
                            val sample = buffer[i] * gain
                            sum += abs(sample.toDouble())
                        }
                        val avgAmplitude = sum / read
                        // Normalise approximativement sur [0,1] (16-bit PCM ~ max 32767)
                        val normalized = min(1f, (avgAmplitude / 4000.0).toFloat())
                        _level.value = normalized
                        _levelZone.value = when {
                            normalized < 0.05f -> MicLevelZone.SILENT
                            normalized < 0.65f -> MicLevelZone.GOOD
                            else -> MicLevelZone.LOUD
                        }
                    }
                    kotlinx.coroutines.delay(50)
                }
            }
        } catch (e: SecurityException) {
            _micState.value = MicState.ERROR
        } catch (e: Exception) {
            _micState.value = MicState.ERROR
        }
    }

    fun stopTest() {
        stopInternal()
        _micState.value = MicState.IDLE
    }

    private fun stopInternal() {
        recordJob?.cancel()
        recordJob = null
        audioRecord?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        audioRecord = null
        _level.value = 0f
        _levelZone.value = MicLevelZone.SILENT
    }

    override fun onCleared() {
        super.onCleared()
        stopInternal()
    }
}
