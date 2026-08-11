package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BitwardenBlue

import com.example.ui.components.AnimatedVisibilityIcon
import com.example.ui.components.FluentAppLogoEmblem

@Composable
fun UnlockScreen(
    passwordHint: String,
    biometricEnabled: Boolean,
    onUnlockWithMasterPassword: (password: String) -> Unit,
    onUnlockWithBiometric: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var useMasterPasswordFallback by remember { mutableStateOf(false) }

    // Auto-trigger biometric prompt if enabled and not explicitly using master password fallback
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && !useMasterPasswordFallback) {
            onUnlockWithBiometric()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Fluent 2 App Logo Header
        FluentAppLogoEmblem(
            size = 80.dp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "LS Pass Vault",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Your vault is encrypted & locked",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Biometric Prompt Card if enabled
                if (biometricEnabled && !useMasterPasswordFallback) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Icon",
                        tint = BitwardenBlue,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Touch Biometric Sensor to Unlock",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onUnlockWithBiometric,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("biometric_unlock_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BitwardenBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Authenticate with Biometrics", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fallback to Master Password option
                    OutlinedButton(
                        onClick = { useMasterPasswordFallback = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("use_master_password_fallback_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = BitwardenBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Master Password Instead", color = BitwardenBlue, fontWeight = FontWeight.Medium)
                    }
                } else {
                    // Master Password Unlock Form
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = BitwardenBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter Master Password",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Master Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (password.isNotBlank()) {
                                    onUnlockWithMasterPassword(password)
                                }
                            }
                        ),
                        trailingIcon = {
                            AnimatedVisibilityIcon(
                                isVisible = showPassword,
                                onToggle = { showPassword = !showPassword },
                                modifier = Modifier.testTag("toggle_password_visibility")
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unlock_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BitwardenBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (password.isNotBlank()) {
                                onUnlockWithMasterPassword(password)
                            }
                        },
                        enabled = password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BitwardenBlue)
                    ) {
                        Text("Unlock Vault", fontWeight = FontWeight.Bold)
                    }

                    if (biometricEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { useMasterPasswordFallback = false },
                            modifier = Modifier.testTag("switch_to_biometrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = BitwardenBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch back to Biometrics", color = BitwardenBlue, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        if (passwordHint.isNotBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                onClick = { showHint = !showHint },
                modifier = Modifier.testTag("password_hint_button")
            ) {
                Text(
                    text = if (showHint) "Hint: $passwordHint" else "Need help? Show password hint",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

