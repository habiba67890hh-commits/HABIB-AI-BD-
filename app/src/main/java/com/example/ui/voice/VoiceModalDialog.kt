package com.example.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.voice.VoiceManager
import com.example.voice.VoiceState

@Composable
fun VoiceModalDialog(
    voiceManager: VoiceManager,
    isGenerating: Boolean,
    onSpeechInputRecognized: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val voiceState by voiceManager.voiceState.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            voiceManager.startListening(onSpeechInputRecognized)
        }
    }

    var isContinuousSession by remember { mutableStateOf(voiceManager.isContinuousSession) }

    LaunchedEffect(Unit) {
        if (hasMicPermission) {
            voiceManager.startListening(onSpeechInputRecognized)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Dialog(
        onDismissRequest = {
            voiceManager.stopListening()
            voiceManager.stopSpeaking()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HABIB ভয়েস মোড",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            voiceManager.stopListening()
                            voiceManager.stopSpeaking()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("btn_close_voice")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Microphone & Sound Wave Visualizer
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isListening = voiceState is VoiceState.Listening
                    val isSpeaking = voiceState is VoiceState.Speaking

                    if (isListening || isSpeaking || isGenerating) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.35f,
                            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                            label = "scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        )
                    }

                    // Center Action Button
                    FilledIconButton(
                        onClick = {
                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else if (voiceState is VoiceState.Listening) {
                                voiceManager.stopListening()
                            } else if (voiceState is VoiceState.Speaking) {
                                voiceManager.stopSpeaking()
                            } else {
                                voiceManager.startListening(onSpeechInputRecognized)
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = when (voiceState) {
                                is VoiceState.Listening -> MaterialTheme.colorScheme.primary
                                is VoiceState.Speaking -> MaterialTheme.colorScheme.secondary
                                is VoiceState.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        modifier = Modifier
                            .size(80.dp)
                            .testTag("btn_voice_action")
                    ) {
                        Icon(
                            imageVector = when {
                                voiceState is VoiceState.Speaking -> Icons.AutoMirrored.Filled.VolumeUp
                                voiceState is VoiceState.Listening -> Icons.Default.Mic
                                voiceState is VoiceState.Error -> Icons.Default.MicOff
                                else -> Icons.Default.Mic
                            },
                            contentDescription = "মাইক নিয়ন্ত্রণ",
                            modifier = Modifier.size(36.dp),
                            tint = when (voiceState) {
                                is VoiceState.Listening, is VoiceState.Speaking -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Status Text
                val statusText = when (val state = voiceState) {
                    is VoiceState.Listening -> if (state.partialText.isNotEmpty()) "\"${state.partialText}\"" else "বাংলা বা ইংরেজিতে কথা বলুন..."
                    is VoiceState.Processing -> "কথা বোঝা যাচ্ছে: \"${state.recognizedText}\""
                    is VoiceState.Speaking -> "হাবিব উত্তর বলছে..."
                    is VoiceState.Error -> state.bengaliMessage
                    else -> if (isGenerating) "এআই উত্তর তৈরি করছে..." else "কথা বলতে মাইক বাটনে স্পর্শ করুন"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (voiceState is VoiceState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Continuous Listening Session Switch
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "অবিরাম সেশন (Continuous Voice)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "উত্তর শেষে স্বয়ংক্রিয়ভাবে পুনরায় শুনবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isContinuousSession,
                            onCheckedChange = { checked ->
                                isContinuousSession = checked
                                voiceManager.isContinuousSession = checked
                            },
                            modifier = Modifier.testTag("switch_continuous_voice")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wake-word transparency notice
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "তথ্য",
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "গোপনীয়তা ও ব্যাটারি সুরক্ষায় 'হেই হাবিব' ওয়েক-ওয়ার্ডটি ডিভাইসের সক্রিয় ভয়েস সেশনে কাজ করে। ব্যাকগ্রাউন্ডে কোনো রেকর্ডিং বা অডিও আপলোড হয় না।",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stop Button
                OutlinedButton(
                    onClick = {
                        voiceManager.stopListening()
                        voiceManager.stopSpeaking()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ভয়েস ও কথা থামান")
                }
            }
        }
    }
}
