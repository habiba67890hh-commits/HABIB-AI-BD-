package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var preferredName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .verticalScroll(scrollState)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.habib_logo),
                    contentDescription = "HABIB AI Logo",
                    modifier = Modifier.size(88.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HABIB AI",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "আপনার বুদ্ধিমান বাংলা ও ইংরেজি এআই সঙ্গী",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Mode Tabs (Login / Register / Forgot Password)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TabRow(
                        selectedTabIndex = when (uiState.mode) {
                            AuthScreenMode.LOGIN -> 0
                            AuthScreenMode.REGISTER -> 1
                            AuthScreenMode.FORGOT_PASSWORD -> 2
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = uiState.mode == AuthScreenMode.LOGIN,
                            onClick = { authViewModel.switchMode(AuthScreenMode.LOGIN) },
                            text = { Text("লগইন", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("tab_login")
                        )
                        Tab(
                            selected = uiState.mode == AuthScreenMode.REGISTER,
                            onClick = { authViewModel.switchMode(AuthScreenMode.REGISTER) },
                            text = { Text("নিবন্ধন", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("tab_register")
                        )
                        Tab(
                            selected = uiState.mode == AuthScreenMode.FORGOT_PASSWORD,
                            onClick = { authViewModel.switchMode(AuthScreenMode.FORGOT_PASSWORD) },
                            text = { Text("রিসেট", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("tab_forgot")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Banner
                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Success Banner
                    AnimatedVisibility(visible = uiState.successMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = uiState.successMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Preferred Name field (only for Register)
                    if (uiState.mode == AuthScreenMode.REGISTER) {
                        OutlinedTextField(
                            value = preferredName,
                            onValueChange = { preferredName = it },
                            label = { Text("আপনার নাম (ঐচ্ছিক)") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = "Name")
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("input_name"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("ইমেল ঠিকানা") },
                        placeholder = { Text("example@domain.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_email"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Password Field (For Login, Register, or New Password in Forgot)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                if (uiState.mode == AuthScreenMode.FORGOT_PASSWORD) "নতুন পাসওয়ার্ড"
                                else "পাসওয়ার্ড"
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password")
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "পাসওয়ার্ড লুকান" else "পাসওয়ার্ড দেখুন"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                when (uiState.mode) {
                                    AuthScreenMode.LOGIN -> authViewModel.login(email, password)
                                    AuthScreenMode.REGISTER -> authViewModel.register(email, password, preferredName)
                                    AuthScreenMode.FORGOT_PASSWORD -> authViewModel.resetPassword(email, password)
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .testTag("input_password"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Submit Button
                    Button(
                        onClick = {
                            when (uiState.mode) {
                                AuthScreenMode.LOGIN -> authViewModel.login(email, password)
                                AuthScreenMode.REGISTER -> authViewModel.register(email, password, preferredName)
                                AuthScreenMode.FORGOT_PASSWORD -> authViewModel.resetPassword(email, password)
                            }
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_auth_submit")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when (uiState.mode) {
                                    AuthScreenMode.LOGIN -> "লগইন করুন"
                                    AuthScreenMode.REGISTER -> "নিবন্ধন সম্পন্ন করুন"
                                    AuthScreenMode.FORGOT_PASSWORD -> "পাসওয়ার্ড পরিবর্তন করুন"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ব্যবহারকারী তথ্য ও চ্যাট ইতিহাস সম্পূর্ণ সুরক্ষিত ও বিচ্ছিন্ন থাকে।",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
