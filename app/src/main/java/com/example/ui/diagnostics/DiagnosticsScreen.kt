package com.example.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DiagnosticReport
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report by mainViewModel.diagnosticReport.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        mainViewModel.runDiagnostics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সংযোগ পরীক্ষা ও ডায়াগনস্টিক", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_diagnostics")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { mainViewModel.runDiagnostics() },
                        enabled = !report.isRunning,
                        modifier = Modifier.testTag("btn_refresh_diagnostics")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "পুনরায় পরীক্ষা চালান")
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
            // Overall Status Summary Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (report.geminiApiStatus?.isSuccess == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (report.geminiApiStatus?.isSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (report.geminiApiStatus?.isSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (report.geminiApiStatus?.isSuccess == true) "সিস্টেম সম্পূর্ণরূপে সংযুক্ত ও প্রস্তুত" else "সংযোগ ও কনফিগারেশন ত্রুটি শনাক্ত",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (report.geminiApiStatus?.isSuccess == true) "Gemini এআই মডেল, স্পিচ ও ফন্ট স্বাভাবিক কাজ করছে" else "নিচের বিস্তারিত দেখে প্রয়োজনীয় পদক্ষেপ নিন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "বিস্তারিত পরীক্ষা ফলাফল",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Diagnostic Item 1: Internet Connectivity
            DiagnosticItemCard(
                title = "ইন্টারনেট ও নেটওয়ার্ক সংযোগ",
                isSuccess = report.internetReachable,
                details = if (report.internetReachable) "ডিভাইস সক্রিয় নেটওয়ার্কের সাথে সংযুক্ত রয়েছে।" else "ইন্টারনেট সংযোগ বিচ্ছিন্ন! ওয়াইফাই বা মোবাইল ডাটা চালু করুন।"
            )

            // Diagnostic Item 2: Gemini API
            val geminiTest = report.geminiApiStatus
            val isGeminiOk = geminiTest?.isSuccess == true
            DiagnosticItemCard(
                title = "Google Gemini API (gemini-3.5-flash)",
                isSuccess = isGeminiOk,
                details = when {
                    report.isRunning -> "API এর সাথে হ্যান্ডশেক ও প্রতিক্রিয়া পরীক্ষা চলছে..."
                    geminiTest == null -> "পরীক্ষা সম্পন্ন হয়নি"
                    isGeminiOk -> "সফলভাবে সংযুক্ত! লেটেন্সি: ${geminiTest.latencyMs} ms, কোড: ${geminiTest.statusCode}"
                    else -> geminiTest.message
                }
            )

            // Diagnostic Item 3: Speech Recognition
            DiagnosticItemCard(
                title = "অ্যান্ড্রয়েড স্পিচ রিকগনিশন (SpeechRecognizer)",
                isSuccess = report.speechRecognitionAvailable,
                details = if (report.speechRecognitionAvailable) "স্পিচ ইঞ্জিন উপস্থিত ও বাংলা/ইংরেজি শ্রবণ সমর্থিত।" else "ডিভাইসে কোনো ভয়েস রিকগনিশন সার্ভিস পাওয়া যায়নি।"
            )

            // Diagnostic Item 4: Text-To-Speech (TTS)
            DiagnosticItemCard(
                title = "টেক্সট-টু-স্পিচ (TTS) বাংলা ইঞ্জিন",
                isSuccess = report.ttsBengaliSupport.first,
                details = report.ttsBengaliSupport.second
            )

            // Diagnostic Item 5: Bengali Typography (Hind Siliguri)
            DiagnosticItemCard(
                title = "বাংলা টাইপোগ্রাফি (Hind Siliguri Font)",
                isSuccess = report.fontLoaded,
                details = "স্থানীয় ফন্ট ফাইল (hind_siliguri.ttf) সক্রিয় এবং যুক্তাক্ষর সফলভাবে রেন্ডার হচ্ছে।"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Re-run Button
            Button(
                onClick = { mainViewModel.runDiagnostics() },
                enabled = !report.isRunning,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (report.isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("পরীক্ষা চলছে...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("পুনরায় পরীক্ষা চালান")
                }
            }
        }
    }
}

@Composable
fun DiagnosticItemCard(
    title: String,
    isSuccess: Boolean,
    details: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
