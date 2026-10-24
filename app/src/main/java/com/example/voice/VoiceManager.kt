package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    object Idle : VoiceState()
    object Initializing : VoiceState()
    data class Listening(val rmsDb: Float = 0f, val partialText: String = "") : VoiceState()
    data class Processing(val recognizedText: String) : VoiceState()
    data class Speaking(val text: String, val progress: Float = 0f) : VoiceState()
    data class Error(val bengaliMessage: String, val canRetry: Boolean = true) : VoiceState()
}

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {
    companion object {
        private const val TAG = "VoiceManager"
        private const val UTTERANCE_ID = "habib_ai_speech_id"
    }

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    var isContinuousSession = false
    private var onSpeechRecognizedCallback: ((String) -> Unit)? = null

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TTS: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            configureVoice(useFemale = true, speechRate = 1.0f)

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val current = _voiceState.value
                    if (current is VoiceState.Speaking) {
                        _voiceState.value = current.copy(progress = 0.1f)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    _voiceState.value = VoiceState.Idle
                    if (isContinuousSession) {
                        startListening(onSpeechRecognizedCallback ?: {})
                    }
                }

                override fun onError(utteranceId: String?) {
                    _voiceState.value = VoiceState.Idle
                }
            })
        } else {
            Log.w(TAG, "TextToSpeech init failed with status: $status")
        }
    }

    fun configureVoice(useFemale: Boolean = true, speechRate: Float = 1.0f) {
        val tts = textToSpeech ?: return
        if (!isTtsReady) return

        tts.setSpeechRate(speechRate.coerceIn(0.6f, 1.5f))

        // First attempt: Bengali (Bangladesh)
        val bnLocale = Locale.forLanguageTag("bn-BD")
        var langResult = tts.setLanguage(bnLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Second attempt: Bengali (India)
            val bnInLocale = Locale.forLanguageTag("bn-IN")
            langResult = tts.setLanguage(bnInLocale)
        }

        if (langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED) {
            // Find female voice candidate if available in voices
            try {
                val voices = tts.voices
                if (voices != null && voices.isNotEmpty()) {
                    var selectedVoice: Voice? = null
                    if (useFemale) {
                        selectedVoice = voices.firstOrNull { v ->
                            v.locale.language == "bn" && (
                                v.name.contains("female", ignoreCase = true) ||
                                v.name.contains("network-f", ignoreCase = true) ||
                                v.features.any { it.contains("gender=female", ignoreCase = true) }
                            )
                        }
                    }
                    if (selectedVoice != null) {
                        tts.voice = selectedVoice
                        tts.setPitch(1.0f)
                    } else {
                        // Female voice pitch fallback
                        tts.setPitch(if (useFemale) 1.2f else 1.0f)
                    }
                } else {
                    tts.setPitch(if (useFemale) 1.2f else 1.0f)
                }
            } catch (e: Exception) {
                tts.setPitch(if (useFemale) 1.2f else 1.0f)
            }
        } else {
            // Fallback to English / default locale
            tts.language = Locale.ENGLISH
            tts.setPitch(if (useFemale) 1.2f else 1.0f)
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        stopSpeaking()
        onSpeechRecognizedCallback = onResult

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.Error(
                "এই ডিভাইসে স্পিচ রিকগনিশন (Speech Recognition) সমর্থিত নয়। দয়া করে Google স্পিচ সার্ভিস ইনস্টল করুন।",
                canRetry = false
            )
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _voiceState.value = VoiceState.Listening(rmsDb = 0f, partialText = "")
                }

                override fun onBeginningOfSpeech() {
                    _voiceState.value = VoiceState.Listening(rmsDb = 2f, partialText = "")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val current = _voiceState.value
                    if (current is VoiceState.Listening) {
                        _voiceState.value = current.copy(rmsDb = rmsdB.coerceIn(0f, 10f))
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _voiceState.value = VoiceState.Processing(recognizedText = "")
                }

                override fun onError(error: Int) {
                    val bengaliError = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "কথা পরিষ্কার শোনা যায়নি। আবার বলুন।"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো শব্দ শোনা যায়নি।"
                        SpeechRecognizer.ERROR_AUDIO -> "মাইক্রোফোন অডিও রেকর্ডিংয়ে সমস্যা।"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "স্পিচ রিকগনিশনের জন্য নেটওয়ার্ক পাওয়া যায়নি।"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোনের অনুমতি প্রয়োজন।"
                        else -> "স্পিচ রিকগনিশনে সমস্যা হয়েছে (কোড: $error)।"
                    }
                    _voiceState.value = VoiceState.Error(bengaliError)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty().trim()
                    if (text.isNotEmpty()) {
                        _voiceState.value = VoiceState.Processing(text)
                        onResult(text)
                    } else {
                        _voiceState.value = VoiceState.Idle
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    val current = _voiceState.value
                    if (current is VoiceState.Listening) {
                        _voiceState.value = current.copy(partialText = text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.Listening()
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error("মাইক্রোফোন চালু করতে সমস্যা: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer: ${e.message}")
        }
        if (_voiceState.value is VoiceState.Listening) {
            _voiceState.value = VoiceState.Idle
        }
    }

    fun speak(text: String, useFemale: Boolean = true, speechRate: Float = 1.0f) {
        stopListening()
        if (!isTtsReady || textToSpeech == null) {
            _voiceState.value = VoiceState.Error("স্পিচ সিন্থেসাইজার প্রস্তুত নয়")
            return
        }

        configureVoice(useFemale, speechRate)

        // Clean markdown symbols for natural speech playback
        val cleanSpeechText = text
            .replace(Regex("[*#_`>]"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .trim()

        _voiceState.value = VoiceState.Speaking(cleanSpeechText)
        textToSpeech?.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
        if (_voiceState.value is VoiceState.Speaking) {
            _voiceState.value = VoiceState.Idle
        }
    }

    fun checkTtsBengaliSupport(): Pair<Boolean, String> {
        val tts = textToSpeech ?: return Pair(false, "TTS ইঞ্জিন প্রস্তুত নয়")
        val bnBD = Locale.forLanguageTag("bn-BD")
        val resBD = tts.isLanguageAvailable(bnBD)
        if (resBD >= TextToSpeech.LANG_AVAILABLE) {
            val hasFemale = tts.voices?.any {
                it.locale.language == "bn" && it.name.contains("female", ignoreCase = true)
            } == true
            return Pair(true, if (hasFemale) "বাংলা (বাংলাদেশ) ও নারী কণ্ঠ উপলব্ধ" else "বাংলা (বাংলাদেশ) উপলব্ধ (পিচ টিউনিং সক্রিয়)")
        }

        val bnIN = Locale.forLanguageTag("bn-IN")
        val resIN = tts.isLanguageAvailable(bnIN)
        if (resIN >= TextToSpeech.LANG_AVAILABLE) {
            return Pair(true, "বাংলা (ভারত) উপলব্ধ")
        }

        return Pair(false, "ডিভাইসে কোনো বাংলা TTS ভয়েস ডাটা পাওয়া যায়নি (ইংরেজি বিকল্প ব্যবহৃত হবে)")
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
