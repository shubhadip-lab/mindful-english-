package com.example.mindfulenglish

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)

        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val currentKey = prefs.getString("gemini_api_key", "")
        etApiKey.setText(currentKey)

        btnSave.setOnClickListener {
            val key = etApiKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter a valid API key", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putString("gemini_api_key", key).apply()
                Toast.makeText(this, "API Key saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
