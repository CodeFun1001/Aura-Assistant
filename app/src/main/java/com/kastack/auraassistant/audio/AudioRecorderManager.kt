package com.kastack.auraassistant.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val AMPLITUDE_MAX = 32_767f
        private const val EMIT_INTERVAL_MS = 80L
    }

    private var audioRecord: AudioRecord? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun amplitudeFlow(): Flow<Float> = callbackFlow {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        ).coerceAtLeast(4096)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            minBufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            close(IllegalStateException("AudioRecord failed to initialize"))
            return@callbackFlow
        }

        audioRecord = record
        record.startRecording()

        val buffer = ShortArray(minBufferSize / 2)

        try {
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val rms = rms(buffer, read)
                    val normalized = (rms / AMPLITUDE_MAX).coerceIn(0f, 1f)
                    trySend(normalized)
                }
                kotlinx.coroutines.delay(EMIT_INTERVAL_MS)
            }
        } finally {
            record.stop()
            record.release()
            audioRecord = null
        }

        awaitClose {
            record.stop()
            record.release()
            audioRecord = null
        }
    }.flowOn(Dispatchers.IO)

    fun stopRecording() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun rms(buffer: ShortArray, readCount: Int): Float {
        if (readCount == 0) return 0f
        var sum = 0.0
        for (i in 0 until readCount) sum += (buffer[i] * buffer[i]).toDouble()
        return sqrt(sum / readCount).toFloat()
    }
}