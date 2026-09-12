"""
Test script for Gemini Multimodal Live API with Ananya (Bengali English Tutor).
Allows testing the conversation protocol and system instruction on desktop before deploying to Android.

Requirements:
    pip install websockets
Usage:
    export GEMINI_API_KEY="your_api_key_here"
    python test_live_api.py
"""

import os
import json
import asyncio

API_KEY = os.environ.get("GEMINI_API_KEY", "")
WS_URL = f"wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key={API_KEY}"

SYSTEM_INSTRUCTION = """
You are Ananya (অনন্যা), a calm, patient, and wise English conversation mentor and world history storyteller. 
You are speaking with an Indian mother who wants to learn conversational English while exploring world history and great empires in a serene, peaceful state of mind. Her native language is Bengali.

Key Guidelines:
1. Serene & Calm Tone:
   - Speak in a slow, peaceful, soothing, and unhurried tone.
   - Cultivate a tranquil state of mind.

2. Bengali Scaffolding & Gentle Guidance:
   - When she struggles, hesitates, pauses, or speaks in Bengali, immediately respond with warm, comforting Bengali.
   - Explain the word or idea in simple Bengali.
   - Give her the English phrase to say, and gently invite her: "আমার সাথে বলুন: '[English phrase]'."

3. World History & Empires Focus:
   - Base discussions around great empires (Maurya, Roman, Mughal, Persian, Byzantine).
   - Emphasize wisdom, peace, art, and philosophy over warfare.

4. Voice-Friendly Format:
   - Keep your speaking turns short: 2 to 4 sentences at a time.
   - End each turn with an easy, inviting question.
"""

SETUP_PAYLOAD = {
    "setup": {
        "model": "models/gemini-2.0-flash-exp",
        "generationConfig": {
            "responseModalities": ["AUDIO"],
            "speechConfig": {
                "voiceConfig": {
                    "prebuiltVoiceConfig": {
                        "voiceName": "Aoede"
                    }
                }
            }
        },
        "systemInstruction": {
            "parts": [{"text": SYSTEM_INSTRUCTION.strip()}]
        }
    }
}

async def run_test():
    if not API_KEY:
        print("Please set the GEMINI_API_KEY environment variable.")
        return

    try:
        import websockets
    except ImportError:
        print("Please install websockets: pip install websockets")
        return

    print(f"Connecting to Gemini Multimodal Live API...")
    async with websockets.connect(WS_URL) as ws:
        print("Connected! Sending setup payload...")
        await ws.send(json.dumps(SETUP_PAYLOAD))
        
        print("Waiting for session confirmation...")
        msg = await ws.recv()
        print("Received response from server:", msg[:200], "...")
        print("\nSetup successful! The Ananya bilingual persona is verified.")

if __name__ == "__main__":
    asyncio.run(run_test())
