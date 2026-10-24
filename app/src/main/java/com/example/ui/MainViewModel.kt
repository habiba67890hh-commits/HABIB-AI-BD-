package com.example.ui

import android.content.Context
import android.net.Uri
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ConnectionDiagnosticResult
import com.example.ai.GeminiClient
import com.example.ai.GeminiResult
import com.example.auth.AuthRepository
import com.example.data.ChatRepository
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.local.entities.UserEntity
import com.example.util.ImageUtils
import com.example.voice.VoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiagnosticReport(
    val internetReachable: Boolean = false,
    val geminiApiStatus: ConnectionDiagnosticResult? = null,
    val speechRecognitionAvailable: Boolean = false,
    val ttsBengaliSupport: Pair<Boolean, String> = Pair(false, "পরীক্ষা চলছে..."),
    val fontLoaded: Boolean = true,
    val isRunning: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val context: Context,
    val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val geminiClient: GeminiClient,
    val voiceManager: VoiceManager
) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    // Sessions for current user
    val sessions: StateFlow<List<ChatSessionEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            chatRepository.getSessionsForUser(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Messages for current active session
    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { sId ->
        val user = currentUser.value
        if (sId != null && user != null) {
            chatRepository.getMessagesForSession(sId, user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Input message state
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // Attached image
    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    val attachedImageUri: StateFlow<Uri?> = _attachedImageUri.asStateFlow()

    private val _attachedImageBase64 = MutableStateFlow<String?>(null)
    val attachedImageBase64: StateFlow<String?> = _attachedImageBase64.asStateFlow()

    // Sending/Generation state
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Diagnostics report
    private val _diagnosticReport = MutableStateFlow(DiagnosticReport())
    val diagnosticReport: StateFlow<DiagnosticReport> = _diagnosticReport.asStateFlow()

    // Settings
    private val prefs = context.getSharedPreferences("habib_settings", Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", "").orEmpty())
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    init {
        // Auto-select latest session or create new
        viewModelScope.launch {
            sessions.collect { list ->
                if (_currentSessionId.value == null && list.isNotEmpty()) {
                    _currentSessionId.value = list.first().sessionId
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun onImageSelected(uri: Uri?) {
        _attachedImageUri.value = uri
        if (uri != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val converted = ImageUtils.uriToBase64(context, uri)
                _attachedImageBase64.value = converted?.first
            }
        } else {
            _attachedImageBase64.value = null
        }
    }

    fun clearAttachedImage() {
        _attachedImageUri.value = null
        _attachedImageBase64.value = null
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
    }

    fun startNewChat() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val newSession = chatRepository.createNewSession(user.id)
            _currentSessionId.value = newSession.sessionId
            clearAttachedImage()
            _inputText.value = ""
        }
    }

    fun deleteSession(sessionId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId, user.id)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun clearAllHistory() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            chatRepository.deleteAllUserSessions(user.id)
            _currentSessionId.value = null
        }
    }

    fun sendMessage(promptText: String = _inputText.value) {
        val user = currentUser.value ?: return
        val textToSend = promptText.trim()
        val imageBase64 = _attachedImageBase64.value

        if (textToSend.isEmpty() && imageBase64 == null) return

        _inputText.value = ""
        clearAttachedImage()
        _isGenerating.value = true

        viewModelScope.launch {
            var activeSessionId = _currentSessionId.value
            if (activeSessionId == null) {
                val newSession = chatRepository.createNewSession(user.id)
                activeSessionId = newSession.sessionId
                _currentSessionId.value = activeSessionId
            }

            val result = chatRepository.sendMessage(
                sessionId = activeSessionId,
                userId = user.id,
                prompt = textToSend,
                imageBase64 = imageBase64,
                userProfile = user,
                onChunk = { /* UI observes Room table updates */ }
            )

            _isGenerating.value = false

            // If voice state is active or continuous session is on, speak the response
            if (result is GeminiResult.Success && voiceManager.isContinuousSession) {
                voiceManager.speak(result.text, useFemale = user.useFemaleVoice, speechRate = user.speechRate)
            }
        }
    }

    fun retryMessage(messageId: String) {
        val user = currentUser.value ?: return
        val sessionId = _currentSessionId.value ?: return

        _isGenerating.value = true
        viewModelScope.launch {
            chatRepository.retryMessage(
                sessionId = sessionId,
                userId = user.id,
                failedMessageId = messageId,
                userProfile = user,
                onChunk = { }
            )
            _isGenerating.value = false
        }
    }

    fun speakMessage(text: String) {
        val user = currentUser.value
        voiceManager.speak(
            text = text,
            useFemale = user?.useFemaleVoice ?: true,
            speechRate = user?.speechRate ?: 1.0f
        )
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun saveCustomApiKey(key: String) {
        val trimmed = key.trim()
        _customApiKey.value = trimmed
        prefs.edit().putString("custom_gemini_api_key", trimmed).apply()
    }

    fun updateUserProfile(
        preferredName: String,
        educationLevel: String,
        district: String,
        languagePreference: String,
        responseStyle: String,
        speechRate: Float,
        useFemaleVoice: Boolean
    ) {
        viewModelScope.launch {
            authRepository.updateProfile(
                preferredName = preferredName,
                educationLevel = educationLevel,
                district = district,
                languagePreference = languagePreference,
                responseStyle = responseStyle,
                speechRate = speechRate,
                useFemaleVoice = useFemaleVoice
            )
        }
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _diagnosticReport.value = _diagnosticReport.value.copy(isRunning = true)

            // Test 1: Speech recognition
            val speechAvail = SpeechRecognizer.isRecognitionAvailable(context)

            // Test 2: TTS
            val ttsSupport = voiceManager.checkTtsBengaliSupport()

            // Test 3: Gemini API handshake
            val geminiTest = geminiClient.testConnection(_customApiKey.value)

            // Test 4: Network reachable
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(network)
            val hasInternet = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            _diagnosticReport.value = DiagnosticReport(
                internetReachable = hasInternet,
                geminiApiStatus = geminiTest,
                speechRecognitionAvailable = speechAvail,
                ttsBengaliSupport = ttsSupport,
                fontLoaded = true,
                isRunning = false
            )
        }
    }
}
