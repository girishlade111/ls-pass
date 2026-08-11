package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.TotpGenerator
import com.example.data.models.DecryptedVaultItem
import com.example.data.models.ItemType
import com.example.ui.theme.BitwardenBlue
import kotlinx.coroutines.delay

import com.example.ui.components.AnimatedCopyIcon
import com.example.ui.components.AnimatedVisibilityIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: DecryptedVaultItem,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleHidden: () -> Unit = {},
    onCopySecret: (label: String, text: String) -> Unit
) {
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Live TOTP generator state
    var currentTotpCode by remember { mutableStateOf("") }
    var remainingSeconds by remember { mutableIntStateOf(30) }

    val totpSecret = item.loginData?.totpSecret ?: ""

    LaunchedEffect(totpSecret) {
        if (totpSecret.isNotBlank()) {
            while (true) {
                currentTotpCode = TotpGenerator.generateTotp(totpSecret)
                remainingSeconds = TotpGenerator.getRemainingSeconds()
                delay(1000L)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View item", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (item.isHidden) "Remove from Hidden Folder" else "Move to Hidden Folder") },
                            onClick = {
                                menuExpanded = false
                                onToggleHidden()
                            },
                            leadingIcon = { Icon(if (item.isHidden) Icons.Default.Visibility else Icons.Default.Lock, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEdit,
                containerColor = BitwardenBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Item information Section
            Text(
                text = "Item information",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Name
            DetailField(
                label = "Name",
                value = item.name,
                readOnly = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (item.type) {
                ItemType.LOGIN -> {
                    val login = item.loginData
                    if (login != null) {
                        if (login.username.isNotBlank()) {
                            DetailFieldWithCopy(
                                label = "Username",
                                value = login.username,
                                onCopy = { onCopySecret("Username", login.username) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (login.password.isNotBlank()) {
                            DetailPasswordField(
                                label = "Password",
                                value = login.password,
                                showPassword = showPassword,
                                onToggleShow = { showPassword = !showPassword },
                                onCopy = { onCopySecret("Password", login.password) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (login.totpSecret.isNotBlank()) {
                            DetailTotpField(
                                label = "Verification code (TOTP)",
                                code = currentTotpCode,
                                remainingSeconds = remainingSeconds,
                                onCopy = { onCopySecret("TOTP Code", currentTotpCode) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (login.uris.isNotEmpty()) {
                            Text(
                                text = "URIs",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                            login.uris.forEach { uriStr ->
                                DetailUriField(
                                    uri = uriStr,
                                    onLaunch = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (!uriStr.startsWith("http")) "https://$uriStr" else uriStr))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    onCopy = { onCopySecret("URI", uriStr) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        if (login.notes.isNotBlank()) {
                            SectionHeader("Notes")
                            DetailField(label = "Notes", value = login.notes, readOnly = true, minLines = 3)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (login.customFields.isNotEmpty()) {
                            SectionHeader("Custom fields")
                            login.customFields.forEach { cf ->
                                DetailFieldWithCopy(
                                    label = cf.name,
                                    value = cf.value,
                                    isSecret = cf.fieldType == "hidden",
                                    onCopy = { onCopySecret(cf.name, cf.value) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                ItemType.CARD -> {
                    val card = item.cardData
                    if (card != null) {
                        DetailFieldWithCopy(label = "Cardholder name", value = card.cardholderName, onCopy = { onCopySecret("Cardholder", card.cardholderName) })
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldWithCopy(label = "Number", value = card.cardNumber, isSecret = true, onCopy = { onCopySecret("Card Number", card.cardNumber) })
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailField(label = "Expiration", value = "${card.expMonth} / ${card.expYear}")
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldWithCopy(label = "Security code (CVV)", value = card.code, isSecret = true, onCopy = { onCopySecret("Security Code", card.code) })
                        Spacer(modifier = Modifier.height(12.dp))
                        if (card.notes.isNotBlank()) {
                            SectionHeader("Notes")
                            DetailField(label = "Notes", value = card.notes, minLines = 3)
                        }
                    }
                }

                ItemType.IDENTITY -> {
                    val idData = item.identityData
                    if (idData != null) {
                        DetailFieldWithCopy(label = "Full Name", value = "${idData.firstName} ${idData.middleName} ${idData.lastName}".trim(), onCopy = { onCopySecret("Full Name", "${idData.firstName} ${idData.lastName}") })
                        Spacer(modifier = Modifier.height(12.dp))
                        if (idData.email.isNotBlank()) DetailFieldWithCopy(label = "Email", value = idData.email, onCopy = { onCopySecret("Email", idData.email) })
                        Spacer(modifier = Modifier.height(12.dp))
                        if (idData.phone.isNotBlank()) DetailFieldWithCopy(label = "Phone", value = idData.phone, onCopy = { onCopySecret("Phone", idData.phone) })
                        Spacer(modifier = Modifier.height(12.dp))
                        if (idData.ssn.isNotBlank()) DetailFieldWithCopy(label = "SSN/ID", value = idData.ssn, isSecret = true, onCopy = { onCopySecret("SSN", idData.ssn) })
                        Spacer(modifier = Modifier.height(12.dp))
                        if (idData.passportNumber.isNotBlank()) DetailFieldWithCopy(label = "Passport", value = idData.passportNumber, onCopy = { onCopySecret("Passport", idData.passportNumber) })
                        Spacer(modifier = Modifier.height(12.dp))
                        if (idData.notes.isNotBlank()) {
                            SectionHeader("Notes")
                            DetailField(label = "Notes", value = idData.notes, minLines = 3)
                        }
                    }
                }

                ItemType.SECURE_NOTE -> {
                    val noteData = item.secureNoteData
                    if (noteData != null) {
                        DetailField(label = "Notes", value = noteData.notes, minLines = 5)
                    }
                }

                ItemType.SSH_KEY -> {
                    val ssh = item.sshKeyData
                    if (ssh != null) {
                        DetailFieldWithCopy(label = "Fingerprint", value = ssh.fingerprint, onCopy = { onCopySecret("Fingerprint", ssh.fingerprint) })
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldWithCopy(label = "Public key", value = ssh.publicKey, onCopy = { onCopySecret("Public Key", ssh.publicKey) })
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldWithCopy(label = "Private key", value = ssh.privateKey, isSecret = true, onCopy = { onCopySecret("Private Key", ssh.privateKey) })
                    }
                }

                ItemType.PASSKEY -> {
                    val pk = item.passkeyData
                    if (pk != null) {
                        DetailFieldWithCopy(label = "Relying Party ID", value = pk.relyingPartyId, onCopy = { onCopySecret("RP ID", pk.relyingPartyId) })
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldWithCopy(label = "User Handle", value = pk.userHandle, onCopy = { onCopySecret("User Handle", pk.userHandle) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

@Composable
fun DetailField(
    label: String,
    value: String,
    readOnly: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = readOnly,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun DetailFieldWithCopy(
    label: String,
    value: String,
    isSecret: Boolean = false,
    onCopy: () -> Unit
) {
    var isVisible by remember { mutableStateOf(!isSecret) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.weight(1f),
            trailingIcon = if (isSecret) {
                {
                    AnimatedVisibilityIcon(
                        isVisible = isVisible,
                        onToggle = { isVisible = !isVisible }
                    )
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        AnimatedCopyIcon(
            onCopy = onCopy,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun DetailPasswordField(
    label: String,
    value: String,
    showPassword: Boolean,
    onToggleShow: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.weight(1f),
            trailingIcon = {
                AnimatedVisibilityIcon(
                    isVisible = showPassword,
                    onToggle = onToggleShow
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        AnimatedCopyIcon(
            onCopy = onCopy,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun DetailTotpField(
    label: String,
    code: String,
    remainingSeconds: Int,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = code,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Countdown Circle Badge matching Screenshot 1
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp)
        ) {
            CircularProgressIndicator(
                progress = { remainingSeconds / 30f },
                modifier = Modifier.fillMaxSize(),
                color = BitwardenBlue,
                strokeWidth = 3.dp
            )
            Text(
                text = "$remainingSeconds",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        AnimatedCopyIcon(
            onCopy = onCopy,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun DetailUriField(
    uri: String,
    onLaunch: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uri,
            onValueChange = {},
            label = { Text("URI") },
            readOnly = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(onClick = onLaunch) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Launch,
                    contentDescription = "Launch URI",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        AnimatedCopyIcon(
            onCopy = onCopy,
            modifier = Modifier.size(48.dp)
        )
    }
}
