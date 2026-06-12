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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE   = 16_000
        private const val CHANNEL_IN    = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING      = AudioFormat.ENCODING_PCM_16BIT
        private const val PCM_MAX       = 32_767f
        private const val POLL_MS       = 60L
    }
    private var audioRecord: AudioRecord? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun amplitudeFlow(): Flow<Float> = callbackFlow {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
        val bufSize = minBuf.coerceAtLeast(4096)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNEL_IN, ENCODING, bufSize
        )
        audioRecord = record

        check(record.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord failed to initialise — check RECORD_AUDIO permission"
        }

        record.startRecording()
        val buffer = ShortArray(bufSize / 2)

        try {
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val normalised = (rms(buffer, read) / PCM_MAX).coerceIn(0f, 1f)
                    trySend(normalised)
                }
                delay(POLL_MS)
            }
        } finally {
            runCatching { record.stop() }
            runCatching { record.release() }
        }

        awaitClose {
            runCatching { record.stop() }
            runCatching { record.release() }
        }
    }.flowOn(Dispatchers.IO)

    private fun rms(buf: ShortArray, n: Int): Float {
        var sum = 0.0
        for (i in 0 until n) sum += buf[i].toLong() * buf[i]
        return sqrt(sum / n).toFloat().coerceIn(0f, PCM_MAX)
    }

    fun stopRecording() {
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
    }
}