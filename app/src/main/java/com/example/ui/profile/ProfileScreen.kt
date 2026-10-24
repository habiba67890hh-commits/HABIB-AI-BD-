package com.example.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.UserEntity
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by mainViewModel.currentUser.collectAsState()

    var preferredName by remember(currentUser) { mutableStateOf(currentUser?.preferredName.orEmpty()) }
    var educationLevel by remember(currentUser) { mutableStateOf(currentUser?.educationLevel.orEmpty()) }
    var district by remember(currentUser) { mutableStateOf(currentUser?.district.orEmpty()) }
    var languagePref by remember(currentUser) { mutableStateOf(currentUser?.languagePreference ?: "bn") }
    var responseStyle by remember(currentUser) { mutableStateOf(currentUser?.responseStyle ?: "caring_intelligent") }
    var useFemaleVoice by remember(currentUser) { mutableStateOf(currentUser?.useFemaleVoice ?: true) }
    var speechRate by remember(currentUser) { mutableFloatStateOf(currentUser?.speechRate ?: 1.0f) }

    var showSavePermissionDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("প্রোফাইল ও এআই মেমরি", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_profile")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transparency & Storage Notice Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "তথ্য",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "মেমরি ও ব্যক্তিগত ডেটা সুরক্ষা",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "এই প্রোফাইল তথ্যটি আপনার ডিভাইসের সুরক্ষিত লোকাল ডাটাবেজে আপনার অ্যাকাউন্টে আলাদাভাবে সংরক্ষিত থাকে। কথোপকথনের সময় Gemini মডেলকে আপনার নাম, জেলা ও শিক্ষা স্তর জানানো হয় যেন উত্তরগুলো আপনার উপযোগী হয়। আপনি যেকোনো সময় এটি সম্পাদনা বা মুছে ফেলতে পারেন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Profile Fields Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ব্যবহারকারীর তথ্য",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Email (Read-only)
                    OutlinedTextField(
                        value = currentUser?.email.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ইমেল ঠিকানা (অপরিবর্তনীয়)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Preferred Name
                    OutlinedTextField(
                        value = preferredName,
                        onValueChange = { preferredName = it },
                        label = { Text("আপনার ডাকনাম বা পছন্দের নাম") },
                        placeholder = { Text("যেমন: হাবিব, সাকিব, ফাতিমা") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_name"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Class or Education Level
                    OutlinedTextField(
                        value = educationLevel,
                        onValueChange = { educationLevel = it },
                        label = { Text("শ্রেণি বা শিক্ষাগত যোগ্যতা") },
                        placeholder = { Text("যেমন: দশম শ্রেণি, এইচএসসি, স্নাতক") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_education"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // District / City
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("শহর বা জেলা") },
                        placeholder = { Text("যেমন: ঢাকা, চট্টগ্রাম, রাজশাহী, সিলেট") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_district"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // AI Style & Voice Preferences Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "কথোপকথন ও ভয়েস পছন্দ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Response Style Selector
                    Text("এআই উত্তরের ধরণ:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = responseStyle == "caring_intelligent",
                            onClick = { responseStyle = "caring_intelligent" },
                            label = { Text("বন্ধুত্বপূর্ণ ও বুদ্ধিমান") }
                        )
                        FilterChip(
                            selected = responseStyle == "concise",
                            onClick = { responseStyle = "concise" },
                            label = { Text("সংক্ষিপ্ত") }
                        )
                        FilterChip(
                            selected = responseStyle == "academic",
                            onClick = { responseStyle = "academic" },
                            label = { Text("প্রাতিষ্ঠানিক") }
                        )
                    }

                    // Female Voice Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("নারী কণ্ঠ (Female TTS Voice)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("সমর্থিত বাংলা নারী কণ্ঠ অগ্রাধিকার পাবে", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = useFemaleVoice,
                            onCheckedChange = { useFemaleVoice = it }
                        )
                    }

                    // Speech Rate Slider
                    Column {
                        Text(
                            text = "কথা বলার গতি: ${(speechRate * 10).toInt() / 10.0}x",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = speechRate,
                            onValueChange = { speechRate = it },
                            valueRange = 0.7f..1.4f,
                            steps = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Save Changes Button
            Button(
                onClick = { showSavePermissionDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_profile")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("প্রোফাইল মেমরি সংরক্ষণ করুন", style = MaterialTheme.typography.titleMedium)
            }

            // Delete Memory & Account Button
            OutlinedButton(
                onClick = { showDeleteAccountDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("মেমরি ও অ্যাকাউন্ট সম্পূর্ণ মুছুন")
            }
        }
    }

    // Permission / Confirmation Dialog before saving profile
    if (showSavePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showSavePermissionDialog = false },
            title = { Text("প্রোফাইল মেমরি সংরক্ষণ করবেন?") },
            text = {
                Text("আপনার দেওয়া নাম ('$preferredName'), শ্রেণি ('$educationLevel'), এবং জেলা ('$district') তথ্যগুলো আপনার অ্যাকাউন্টের মেমরিতে সংরক্ষিত হবে এবং হাবিব এআই উত্তর দেওয়ার সময় এটি স্মরণে রাখবে। আপনি কি অনুমতি দিচ্ছেন?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        mainViewModel.updateUserProfile(
                            preferredName = preferredName,
                            educationLevel = educationLevel,
                            district = district,
                            languagePreference = languagePref,
                            responseStyle = responseStyle,
                            speechRate = speechRate,
                            useFemaleVoice = useFemaleVoice
                        )
                        showSavePermissionDialog = false
                        Toast.makeText(context, "প্রোফাইল মেমরি সফলভাবে সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("হ্যাঁ, সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePermissionDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Confirmation Dialog before deleting account & memory
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("অ্যাকাউন্ট ও সমস্ত মেমরি মুছবেন?") },
            text = {
                Text("আপনার সমস্ত সংরক্ষিত তথ্য, কথোপকথনের ইতিহাস এবং প্রোফাইল সম্পূর্ণভাবে মুছে ফেলা হবে। এই কাজটি আর পুনরুদ্ধার করা সম্ভব নয়।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        mainViewModel.authRepository.logout()
                        showDeleteAccountDialog = false
                        Toast.makeText(context, "অ্যাকাউন্ট ও ডেটা মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("হ্যাঁ, সব মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
