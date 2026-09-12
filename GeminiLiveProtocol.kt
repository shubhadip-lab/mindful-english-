package com.example.mindfulenglish

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object GeminiLiveProtocol {

    const val DEFAULT_MODEL = "models/gemini-2.0-flash-exp"
    const val VOICE_NAME = "Aoede" // Warm, calm, articulate voice

    val SYSTEM_INSTRUCTION = """
You are Ananya (অনন্যা), a calm, patient, and wise English conversation mentor and world history storyteller. 
You are speaking with an Indian mother who wants to learn conversational English while exploring world history and great empires in a serene, peaceful state of mind. Her native language is Bengali.

Key Guidelines:
1. Serene & Calm Tone:
   - Speak in a slow, peaceful, soothing, and unhurried tone.
   - Reassure her at every step. Cultivate a tranquil state of mind.

2. Bengali Scaffolding & Gentle Guidance:
   - When she struggles, hesitates, pauses, or speaks in Bengali (e.g., "কীভাবে বলব", "এটা বুঝতে পারছি না"), immediately respond with warm, comforting Bengali.
   - Explain the word or idea in simple Bengali.
   - Give her the English phrase to say, and gently invite her: "আমার সাথে বলুন: '[English phrase]'."
   - Keep Bengali explanations warm and natural, then guide her back to English practice.

3. World History & Empires Focus:
   - Base all discussions around the great empires: the Maurya Empire (Ashoka's edicts of peace and dhamma), the Roman Empire (Marcus Aurelius and stoic calm), the Mughal Empire (tranquil Charbagh gardens and art), the Persian Empire, and ancient civilizations.
   - Frame history as lessons of peace, architecture, wisdom, and culture rather than violence.

4. Voice-Friendly Format:
   - Keep your speaking turns short: 2 to 4 sentences at a time.
   - End each turn with an easy, inviting question.
""".trimIndent()

    fun buildSetupMessage(model: String = DEFAULT_MODEL): String {
        return JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", model)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", VOICE_NAME)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", SYSTEM_INSTRUCTION)
                    }))
                })
            })
        }.toString()
    }

    fun buildRealtimeAudioChunk(pcmData: ByteArray, length: Int): String {
        val base64Data = Base64.encodeToString(pcmData, 0, length, Base64.NO_WRAP)
        return JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("mediaChunks", JSONArray().put(JSONObject().apply {
                    put("mimeType", "audio/pcm;rate=16000")
                    put("data", base64Data)
                }))
            })
        }.toString()
    }

    sealed class ServerEvent {
        data class AudioData(val pcmBytes: ByteArray) : ServerEvent()
        object Interrupted : ServerEvent()
        object TurnComplete : ServerEvent()
        data class TextTranscript(val text: String) : ServerEvent()
        data class Error(val message: String) : ServerEvent()
    }

    fun parseServerMessage(jsonStr: String): List<ServerEvent> {
        val events = mutableListOf<ServerEvent>()
        try {
            val root = JSONObject(jsonStr)
            val serverContent = root.optJSONObject("serverContent") ?: return events

            if (serverContent.optBoolean("interrupted", false)) {
                events.add(ServerEvent.Interrupted)
            }

            val modelTurn = serverContent.optJSONObject("modelTurn")
            val parts = modelTurn?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null && inlineData.optString("mimeType").startsWith("audio/pcm")) {
                        val base64Audio = inlineData.getString("data")
                        val pcm = Base64.decode(base64Audio, Base64.DEFAULT)
                        events.add(ServerEvent.AudioData(pcm))
                    }
                    val text = part.optString("text", "")
                    if (text.isNotEmpty()) {
                        events.add(ServerEvent.TextTranscript(text))
                    }
                }
            }

            if (serverContent.optBoolean("turnComplete", false)) {
                events.add(ServerEvent.TurnComplete)
            }
        } catch (e: Exception) {
            events.add(ServerEvent.Error(e.message ?: "JSON parse error"))
        }
        return events
    }
}
