package com.example.mindfulenglish

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*
import java.util.concurrent.TimeUnit

class LiveVoiceService : Service() {

    private val TAG = "LiveVoiceService"
    private val CHANNEL_ID = "MindfulEnglishVoiceChannel"
    private val NOTIFICATION_ID = 1001

    private var wakeLock: PowerManager.WakeLock? = null
    private var audioStreamManager: AudioStreamManager? = null
    private var webSocket: WebSocket? = null

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    companion object {
        const val ACTION_START = "com.example.mindfulenglish.ACTION_START"
        const val ACTION_STOP = "com.example.mindfulenglish.ACTION_STOP"
        const val BROADCAST_STATE_CHANGE = "com.example.mindfulenglish.STATE_CHANGE"
        const val EXTRA_STATE = "EXTRA_STATE"

        const val STATE_IDLE = "IDLE"
        const val STATE_CONNECTING = "CONNECTING"
        const val STATE_LISTENING = "LISTENING"
        const val STATE_SPEAKING = "SPEAKING"
        const val STATE_ERROR = "ERROR"

        @Volatile
        var currentState = STATE_IDLE
            private set
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSession()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("Connecting to Ananya...", false))
                startSession()
                return START_STICKY
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MindfulEnglish:LiveVoiceService").apply {
            setReferenceCounted(false)
            acquire(3 * 60 * 60 * 1000L) // 3 hours timeout safety
        }
    }

    private fun startSession() {
        updateState(STATE_CONNECTING)

        val prefs: SharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""

        if (apiKey.isBlank()) {
            updateState(STATE_ERROR)
            updateNotification("Please set your Gemini API Key in Settings")
            return
        }

        audioStreamManager = AudioStreamManager { pcmBytes, length ->
            val chunkJson = GeminiLiveProtocol.buildRealtimeAudioChunk(pcmBytes, length)
            webSocket?.send(chunkJson)
        }
        audioStreamManager?.initAudioTrack()

        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to Gemini Live API WebSocket")
                val setupJson = GeminiLiveProtocol.buildSetupMessage()
                webSocket.send(setupJson)

                audioStreamManager?.startRecording()
                updateState(STATE_LISTENING)
                updateNotification("Ananya is listening... (Screen-off supported)")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val events = GeminiLiveProtocol.parseServerMessage(text)
                for (event in events) {
                    when (event) {
                        is GeminiLiveProtocol.ServerEvent.AudioData -> {
                            updateState(STATE_SPEAKING)
                            audioStreamManager?.playAudio(event.pcmBytes)
                        }
                        is GeminiLiveProtocol.ServerEvent.Interrupted -> {
                            Log.d(TAG, "User interrupted Ananya speaking")
                            audioStreamManager?.stopPlaybackAndClear()
                            updateState(STATE_LISTENING)
                        }
                        is GeminiLiveProtocol.ServerEvent.TurnComplete -> {
                            updateState(STATE_LISTENING)
                        }
                        is GeminiLiveProtocol.ServerEvent.Error -> {
                            Log.e(TAG, "Server error: ${event.message}")
                        }
                        is GeminiLiveProtocol.ServerEvent.TextTranscript -> {
                            Log.d(TAG, "Transcript: ${event.text}")
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                updateState(STATE_ERROR)
                updateNotification("Connection failed. Please retry.")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                updateState(STATE_IDLE)
            }
        })
    }

    private fun updateState(newState: String) {
        currentState = newState
        val intent = Intent(BROADCAST_STATE_CHANGE).apply {
            putExtra(EXTRA_STATE, newState)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mindful English Conversation",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active voice conversation with Ananya"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(statusText: String, isSpeaking: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, LiveVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ananya — English Tutor")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingOpenApp)
            .addAction(R.drawable.ic_stop, "End Conversation", pendingStop)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(statusText, currentState == STATE_SPEAKING))
    }

    private fun stopSession() {
        try {
            audioStreamManager?.stopAll()
            audioStreamManager = null
            webSocket?.close(1000, "User ended session")
            webSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping session", e)
        } finally {
            updateState(STATE_IDLE)
        }
    }

    override fun onDestroy() {
        stopSession()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
