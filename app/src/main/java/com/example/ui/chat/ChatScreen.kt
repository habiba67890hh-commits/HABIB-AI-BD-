package com.example.ui.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.history.HistoryDrawerSheet
import com.example.ui.voice.VoiceModalDialog
import com.example.voice.VoiceState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    mainViewModel: MainViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentUser by mainViewModel.currentUser.collectAsState()
    val sessions by mainViewModel.sessions.collectAsState()
    val activeSessionId by mainViewModel.currentSessionId.collectAsState()
    val messages by mainViewModel.messages.collectAsState()
    val inputText by mainViewModel.inputText.collectAsState()
    val attachedUri by mainViewModel.attachedImageUri.collectAsState()
    val isGenerating by mainViewModel.isGenerating.collectAsState()
    val voiceState by mainViewModel.voiceManager.voiceState.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showVoiceModal by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Photo picker launcher (zero broad permissions needed)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            mainViewModel.onImageSelected(uri)
        }
    }

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HistoryDrawerSheet(
                sessions = sessions,
                activeSessionId = activeSessionId,
                currentUser = currentUser,
                onSelectSession = { sessionId -> mainViewModel.selectSession(sessionId) },
                onNewChat = { mainViewModel.startNewChat() },
                onDeleteSession = { sessionId -> mainViewModel.deleteSession(sessionId) },
                onClearAll = { mainViewModel.clearAllHistory() },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.habib_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "HABIB AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "অনলাইন • Gemini 3.5 Flash",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("btn_drawer_toggle")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "মেনু")
                        }
                    },
                    actions = {
                        // Voice Mode Action
                        IconButton(
                            onClick = { showVoiceModal = true },
                            modifier = Modifier.testTag("btn_voice_mode_top")
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = "ভয়েস মোড",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Diagnostics Action
                        IconButton(
                            onClick = onNavigateToDiagnostics,
                            modifier = Modifier.testTag("btn_diagnostics_top")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = "সংযোগ পরীক্ষা")
                        }

                        // New Chat Action
                        IconButton(
                            onClick = { mainViewModel.startNewChat() },
                            modifier = Modifier.testTag("btn_new_chat_top")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "নতুন চ্যাট")
                        }

                        // Overflow Menu
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "অন্যান্য")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("প্রোফাইল ও এআই মেমরি") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToProfile()
                                },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("সেটিংস ও কনফিগারেশন") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                // Message List Area
                if (messages.isEmpty()) {
                    // Empty Welcome State
                    EmptyChatGreeting(
                        userName = currentUser?.preferredName.orEmpty(),
                        onSuggestionClicked = { suggestion ->
                            mainViewModel.sendMessage(suggestion)
                        },
                        onOpenVoice = { showVoiceModal = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(messages, key = { it.messageId }) { msg ->
                            val isSpeaking = (voiceState as? VoiceState.Speaking)?.text == msg.text
                            ChatMessageItem(
                                message = msg,
                                isCurrentlySpeaking = isSpeaking,
                                onSpeak = { text -> mainViewModel.speakMessage(text) },
                                onStopSpeaking = { mainViewModel.stopSpeaking() },
                                onRetry = { msgId -> mainViewModel.retryMessage(msgId) }
                            )
                        }
                    }
                }

                // Image Attachment Preview bar (above text field)
                AnimatedVisibility(visible = attachedUri != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = attachedUri,
                                    contentDescription = "নির্বাচিত ছবি",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ছবি সংযুক্ত করা হয়েছে",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "প্রশ্ন লিখুন বা পাঠান বাটনে স্পর্শ করুন",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { mainViewModel.clearAttachedImage() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "ছবি সরান", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Bottom Input Control Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Image Picker Button
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("btn_attach_image")
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "ছবি যুক্ত করুন",
                                tint = if (attachedUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Voice Button
                        IconButton(
                            onClick = { showVoiceModal = true },
                            modifier = Modifier.testTag("btn_mic_input")
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "ভয়েস ইনপুট",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Input TextField
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { mainViewModel.onInputTextChanged(it) },
                            placeholder = {
                                Text(
                                    "বাংলায় বা ইংরেজিতে কিছু জিজ্ঞাসা করুন...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp, max = 120.dp)
                                .testTag("input_chat_prompt"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send Button
                        val canSend = (inputText.isNotBlank() || attachedUri != null) && !isGenerating
                        FilledIconButton(
                            onClick = { mainViewModel.sendMessage() },
                            enabled = canSend,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_send_chat")
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "পাঠান",
                                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Voice Modal Dialog Overlay
    if (showVoiceModal) {
        VoiceModalDialog(
            voiceManager = mainViewModel.voiceManager,
            isGenerating = isGenerating,
            onSpeechInputRecognized = { text ->
                mainViewModel.sendMessage(text)
            },
            onDismiss = { showVoiceModal = false }
        )
    }
}

@Composable
fun EmptyChatGreeting(
    userName: String,
    onSuggestionClicked: (String) -> Unit,
    onOpenVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.habib_logo),
                contentDescription = null,
                modifier = Modifier.size(68.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (userName.isNotBlank()) "স্বাগতম, $userName!" else "স্বাগতম HABIB AI তে!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "আমি আপনার ব্যক্তিগত এআই সহযোগী। যেকোনো প্রশ্ন বাংলায় বা ইংরেজিতে করতে পারেন, ছবি বিশ্লেষণ করতে পারেন অথবা ভয়েসে কথা বলতে পারেন।",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Suggestions
        Text(
            text = "পরামর্শমূলক প্রশ্নসমূহ:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        val suggestions = listOf(
            "আমার পরীক্ষার জন্য একটি সহজ রুটিন বানিয়ে দাও",
            "বাংলা ভাষার উৎপত্তি ও ইতিহাস সংক্ষেপে বলো",
            "একটি সুস্বাদু বাংলাদেশি রান্নার রেসিপি বলো",
            "ইংরেজিতে ভালো কথা বলার ৫টি সহজ উপায় কী?"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSuggestionClicked(prompt) }
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
