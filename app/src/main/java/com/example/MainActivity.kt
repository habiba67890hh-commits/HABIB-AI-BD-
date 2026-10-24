package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.GeminiClient
import com.example.auth.AuthRepository
import com.example.data.ChatRepository
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthViewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.diagnostics.DiagnosticsScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.HabibAITheme
import com.example.voice.VoiceManager

enum class AppScreen {
    CHAT,
    PROFILE,
    SETTINGS,
    DIAGNOSTICS
}

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var geminiClient: GeminiClient
    private lateinit var voiceManager: VoiceManager
    private lateinit var mainViewModel: MainViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize core dependencies
        authRepository = AuthRepository(applicationContext)
        geminiClient = GeminiClient(applicationContext)
        chatRepository = ChatRepository(applicationContext, geminiClient)
        voiceManager = VoiceManager(applicationContext)

        mainViewModel = MainViewModel(
            context = applicationContext,
            authRepository = authRepository,
            chatRepository = chatRepository,
            geminiClient = geminiClient,
            voiceManager = voiceManager
        )

        authViewModel = AuthViewModel(authRepository)

        setContent {
            val isDarkMode by mainViewModel.isDarkMode.collectAsState()
            val currentUser by mainViewModel.currentUser.collectAsState()
            var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }

            HabibAITheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUser == null) {
                        AuthScreen(authViewModel = authViewModel)
                    } else {
                        // Back handler for secondary screens
                        if (currentScreen != AppScreen.CHAT) {
                            BackHandler {
                                currentScreen = AppScreen.CHAT
                            }
                        }

                        Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                            when (screen) {
                                AppScreen.CHAT -> {
                                    ChatScreen(
                                        mainViewModel = mainViewModel,
                                        onNavigateToProfile = { currentScreen = AppScreen.PROFILE },
                                        onNavigateToSettings = { currentScreen = AppScreen.SETTINGS },
                                        onNavigateToDiagnostics = { currentScreen = AppScreen.DIAGNOSTICS }
                                    )
                                }
                                AppScreen.PROFILE -> {
                                    ProfileScreen(
                                        mainViewModel = mainViewModel,
                                        onBack = { currentScreen = AppScreen.CHAT }
                                    )
                                }
                                AppScreen.SETTINGS -> {
                                    SettingsScreen(
                                        mainViewModel = mainViewModel,
                                        onBack = { currentScreen = AppScreen.CHAT }
                                    )
                                }
                                AppScreen.DIAGNOSTICS -> {
                                    DiagnosticsScreen(
                                        mainViewModel = mainViewModel,
                                        onBack = { currentScreen = AppScreen.CHAT }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}
