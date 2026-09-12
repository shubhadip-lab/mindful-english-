# Mindful English Tutor (Ananya — অনন্যা)

A dedicated native Android voice companion designed to help an Indian mother learn conversational English through world history and stories of famous empires, fostering a calm, peaceful state of mind.

Built specifically for **Samsung Galaxy & Android devices** with **continuous screen-off background conversation** support using the **Gemini Multimodal Live API**.

---

## Key Features

1. **Bilingual Scaffolding (Bengali & English):**
   - Automatically detects hesitation, pauses, or Bengali speech (e.g., *"কীভাবে বলব..."*).
   - Responds with calm, warm Bengali to clarify meaning.
   - Teaches the English sentence and gently invites her to speak: *"আমার সাথে বলুন: '...'."*
2. **Serene Historical Conversations:**
   - Discussions revolve around great world empires: the Maurya Empire (Ashoka's edicts of peace), the Roman Empire (Marcus Aurelius & Stoic calm), Mughal architecture and gardens, and ancient civilizations.
   - Paced slowly, keeping speaking turns short (2–4 sentences) to avoid overwhelming the listener.
3. **Screen-Off Voice Interaction:**
   - Powered by an Android Foreground Service (`microphone` and `mediaPlayback`).
   - Keeps CPU and audio processing active via `PARTIAL_WAKE_LOCK`.
   - Your mother can tap "Start Conversation", lock her Samsung phone, put it in her pocket or on a table, and continue the conversation hands-free.
4. **Accessible, Elderly-Friendly UI:**
   - Large tactile button with clear visual states (Ready, Connecting, Listening, Speaking).
   - High-contrast serene color palette (Sage Green, Warm Cream, Gold).
   - Lock screen notification with a single-tap "End Conversation" button.

---

## Architecture & Audio Specs

| Component | Technical Details |
|---|---|
| **API** | Gemini Multimodal Live API (`v1alpha.GenerativeService.BidiGenerateContent`) |
| **Model** | `gemini-2.0-flash-exp` |
| **Input Audio** | 16,000 Hz, 16-bit PCM Mono, Little-Endian (Streaming chunks via WebSocket) |
| **Output Audio** | 24,000 Hz, 16-bit PCM Mono, Little-Endian (AudioTrack streaming playback) |
| **Voice Preset** | `Aoede` (warm, articulate, soothing) |
| **Background Service**| Android `ForegroundService` with `WAKE_LOCK` + `VOICE_COMMUNICATION` audio mode |

---

## How to Build & Run on Samsung Phone

### Step 1: Open in Android Studio
1. Unzip the project folder `MindfulEnglishTutor.zip`.
2. Open **Android Studio** (Hedgehog / Iguana / Jellyfish / Koala or newer).
3. Select **File > Open** and choose the `MindfulEnglishTutor` directory.
4. Allow Gradle to sync dependencies.

### Step 2: Set Your Gemini API Key
1. Obtain a free API key from [Google AI Studio](https://aistudio.google.com/).
2. Run the app on the phone.
3. Tap the **Settings** gear icon in the top right corner.
4. Enter your API key and tap **Save**.

### Step 3: Crucial Samsung Phone Settings for Screen-Off Usage
Samsung's One UI aggressively puts background apps to sleep. To guarantee uninterrupted audio when the screen is locked:
1. Open phone **Settings > Apps > Mindful English**.
2. Tap **Battery**, then choose **Unrestricted** (instead of Optimized).
3. Go to **Settings > Battery > Background usage limits > Never auto-sleeping apps**, tap `+`, and add **Mindful English**.

---

## Project Structure
```
MindfulEnglishTutor/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Background service & audio permissions
│   │   ├── java/com/example/mindfulenglish/
│   │   │   ├── MainActivity.kt          # Minimalist accessible UI & permissions
│   │   │   ├── LiveVoiceService.kt      # Foreground Service & screen-off WakeLock
│   │   │   ├── AudioStreamManager.kt    # 16kHz AudioRecord & 24kHz AudioTrack
│   │   │   ├── GeminiLiveProtocol.kt    # WebSocket framing & Bengali system prompt
│   │   │   └── SettingsActivity.kt      # Secure API key storage
│   │   └── res/
│   │       ├── layout/                  # Serene Material3 layouts
│   │       ├── drawable/                # Icons and calm gradients
│   │       └── values/                  # Strings, serene colors, theme
│   └── build.gradle.kts
└── settings.gradle.kts
```
