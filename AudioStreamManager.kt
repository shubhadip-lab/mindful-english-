package com.example.mindfulenglish

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log

class AudioStreamManager(
    private val onAudioChunkCaptured: (ByteArray, Int) -> Unit
) {
    private val TAG = "AudioStreamManager"

    private val INPUT_SAMPLE_RATE = 16000
    private val OUTPUT_SAMPLE_RATE = 24000

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    @Volatile
    private var isRecording = false
    private var recordingThread: Thread? = null

    fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(8192)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                INPUT_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            // Enable hardware echo cancellation and noise suppression if available
            val sessionId = audioRecord?.audioSessionId ?: 0
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                    enabled = true
                }
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                    enabled = true
                }
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingThread = Thread {
                val buffer = ByteArray(1024)
                while (isRecording) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readBytes > 0) {
                        onAudioChunkCaptured(buffer, readBytes)
                    }
                }
            }.apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord", e)
        }
    }

    fun playAudio(pcmData: ByteArray) {
        audioTrack?.write(pcmData, 0, pcmData.size)
    }

    fun stopPlaybackAndClear() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing AudioTrack", e)
        }
    }

    fun stopAll() {
        isRecording = false
        recordingThread?.interrupt()
        recordingThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        }

        try {
            echoCanceler?.release()
            echoCanceler = null
            noiseSuppressor?.release()
            noiseSuppressor = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio effects", e)
        }

        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        }
    }
}
