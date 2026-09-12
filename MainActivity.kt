package com.example.mindfulenglish

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: FloatingActionButton
    private lateinit var btnSettings: ImageButton

    private val PERMISSION_REQUEST_CODE = 201

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getStringExtra(LiveVoiceService.EXTRA_STATE) ?: LiveVoiceService.STATE_IDLE
            updateUiState(state)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnToggle = findViewById(R.id.btnToggleSession)
        btnSettings = findViewById(R.id.btnSettings)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnToggle.setOnClickListener {
            if (hasRequiredPermissions()) {
                toggleSession()
            } else {
                requestPermissions()
            }
        }

        updateUiState(LiveVoiceService.currentState)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(LiveVoiceService.BROADCAST_STATE_CHANGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
        updateUiState(LiveVoiceService.currentState)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(stateReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    private fun toggleSession() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""

        if (apiKey.isBlank()) {
            Toast.makeText(this, "Please enter your Gemini API key in Settings first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        val serviceIntent = Intent(this, LiveVoiceService::class.java)
        if (LiveVoiceService.currentState == LiveVoiceService.STATE_IDLE ||
            LiveVoiceService.currentState == LiveVoiceService.STATE_ERROR) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            serviceIntent.action = LiveVoiceService.ACTION_STOP
            startService(serviceIntent)
        }
    }

    private fun updateUiState(state: String) {
        val colorPrimary = ContextCompat.getColor(this, R.color.sage_green_primary)
        val colorRed = ContextCompat.getColor(this, R.color.stop_red)
        val colorGold = ContextCompat.getColor(this, R.color.warm_gold)

        when (state) {
            LiveVoiceService.STATE_CONNECTING -> {
                tvStatus.text = getString(R.string.status_connecting)
                btnToggle.backgroundTintList = ColorStateList.valueOf(colorGold)
                btnToggle.setImageResource(R.drawable.ic_mic)
            }
            LiveVoiceService.STATE_LISTENING -> {
                tvStatus.text = getString(R.string.status_listening)
                btnToggle.backgroundTintList = ColorStateList.valueOf(colorRed)
                btnToggle.setImageResource(R.drawable.ic_stop)
            }
            LiveVoiceService.STATE_SPEAKING -> {
                tvStatus.text = getString(R.string.status_speaking)
                btnToggle.backgroundTintList = ColorStateList.valueOf(colorRed)
                btnToggle.setImageResource(R.drawable.ic_stop)
            }
            LiveVoiceService.STATE_ERROR -> {
                tvStatus.text = getString(R.string.status_error)
                btnToggle.backgroundTintList = ColorStateList.valueOf(colorPrimary)
                btnToggle.setImageResource(R.drawable.ic_mic)
            }
            else -> { // IDLE
                tvStatus.text = getString(R.string.status_ready)
                btnToggle.backgroundTintList = ColorStateList.valueOf(colorPrimary)
                btnToggle.setImageResource(R.drawable.ic_mic)
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        return micGranted && notifGranted
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                toggleSession()
            } else {
                Toast.makeText(this, "Microphone permission is required to talk to Ananya", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
