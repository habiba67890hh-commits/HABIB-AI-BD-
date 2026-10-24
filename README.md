# HABIB AI — Production-Ready Native Android App

**HABIB AI (হাবিব এআই)** is a production-grade native Android application built with Kotlin and Jetpack Compose. It features rich Bengali and English AI conversation powered by Google's Gemini models (`gemini-3.5-flash`), multimodal image analysis, on-device user profile memory, Bengali speech recognition, female text-to-speech voice playback, live connection diagnostics, and a dark-green interface using the Hind Siliguri typography.

---

## 🌟 Key Features

1. **Bengali & English Intelligence (Gemini 3.5 Flash)**
   - Streaming text responses via Server-Sent Events (SSE).
   - Multimodal image question prompts with on-device camera/gallery picker (`ActivityResultContracts.PickVisualMedia`).
   - Bengali error messages for network drops, invalid API keys, rate limits, and server interruptions.
   - Response controls: copy to clipboard, text-to-speech audio playback, and retry.

2. **Bengali Typography (Hind Siliguri)**
   - Bundled locally in `app/src/main/res/font/hind_siliguri.ttf` (licensed under SIL Open Font License 1.1).
   - Accurate rendering of Bengali যুক্তাক্ষর, কার, ফলা, and punctuation with comfortable line spacing.
   - Fully offline font loading.

3. **Premium Dark-Green UI & Theming**
   - Material Design 3 interface with deep forest black surfaces (`#070E0A`, `#0D1F17`) and emerald green accents (`#10B981`).
   - Dynamic Dark and Light theme toggle.

4. **Secure User Authentication & Profile Memory**
   - Secure PBKDF2 with HMAC-SHA256 password hashing (10,000 rounds) and cryptographically random salt.
   - Registration, login, logout, and password reset flows.
   - User profile memory: preferred name, education level, district/city, and response style.
   - Explicit user permission dialog before committing profile memory.
   - Context injection: User memory is dynamically supplied to Gemini system instructions.
   - User isolation: All chat history and profiles are strictly separated per authenticated user.

5. **Voice Interaction & Voice Mode**
   - Bengali speech recognition (`bn-BD` / `bn-IN`) with runtime microphone permission handling.
   - Female-sounding Bengali TTS playback with pitch tuning fallback.
   - Continuous voice session mode with real-time speech visualizer and Stop controls.
   - Honest on-device wake-word transparency (active session listening without background spyware).

6. **Connection Diagnostics Screen**
   - Real-time diagnostic tests:
     - Internet connectivity check.
     - Gemini API handshake ping with round-trip latency (ms) and HTTP status code.
     - Android SpeechRecognizer availability.
     - Bengali TTS voice availability.
     - Hind Siliguri font status.

---

## 🛠 Project Structure

```
├── .github/workflows/
│   └── build-apk.yml          # GitHub Actions workflow for building debug APK
├── app/
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── ai/            # GeminiClient (streaming SSE, multimodal, errors)
│   │   │   ├── auth/          # PasswordSecurity (PBKDF2), AuthRepository
│   │   │   ├── data/          # Room Database, DAOs, Entities, ChatRepository
│   │   │   ├── ui/            # Compose screens (Auth, Chat, Profile, Settings, Diagnostics)
│   │   │   ├── util/          # ImageUtils for image resizing and Base64 conversion
│   │   │   ├── voice/         # VoiceManager (SpeechRecognizer & TTS)
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── font/          # hind_siliguri.ttf
│   │   │   ├── drawable/      # habib_logo.jpg, adaptive icon vectors
│   │   │   └── mipmap-*/      # Custom launcher icons
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── settings.gradle.kts
├── .env.example               # Gemini API Key configuration template
└── README.md
```

---

## 🚀 Building & Running

### Option 1: AI Studio & Android Studio

1. Open project in **Android Studio Ladybug (or newer)**.
2. In AI Studio, configure `GEMINI_API_KEY` via the **Secrets panel** (or enter a custom key directly in the app's **Settings & Configuration** screen).
3. Build and run on device or emulator:
   ```bash
   ./gradlew assembleDebug
   ```

### Option 2: Automated GitHub Actions APK Build

The workflow in `.github/workflows/build-apk.yml` triggers on push to `main` or manual `workflow_dispatch`:
1. Push this repository to GitHub.
2. Open the **Actions** tab in your GitHub repository.
3. Select **Build Habib AI APK** and click **Run workflow**.
4. Once completed, download `habib-ai-debug-apk` from the workflow artifacts.
5. Transfer the APK to an Android phone and install.

---

## 🔑 External Setup Checklist

- [ ] **Gemini API Key**: Obtain a key from [Google AI Studio](https://aistudio.google.com/).
  - Inject via AI Studio Secrets (`GEMINI_API_KEY`), or
  - Enter into the in-app **Settings & Configuration** screen.
- [ ] **Android Microphone Permission**: When prompted by the app, allow microphone access for Bengali speech recognition.
- [ ] **Google Speech Services & Bengali Voice Data**: For the best voice experience, ensure Google Speech Recognition and TTS are updated on the Android device via Google Play Services.

---

## 📄 License & Attributions

- **Hind Siliguri Font**: SIL Open Font License (OFL) Version 1.1, Copyright (c) 2015 Indian Type Foundry.
- **HABIB AI Codebase**: Released for production use.
