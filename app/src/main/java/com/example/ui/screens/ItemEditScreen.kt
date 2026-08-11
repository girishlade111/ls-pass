package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoManager
import com.example.data.models.CardData
import com.example.data.models.CustomField
import com.example.data.models.DecryptedVaultItem
import com.example.data.models.FolderEntity
import com.example.data.models.IdentityData
import com.example.data.models.ItemType
import com.example.data.models.LoginData
import com.example.data.models.PasskeyData
import com.example.data.models.SecureNoteData
import com.example.data.models.SshKeyData
import com.example.ui.theme.BitwardenBlue
import com.example.ui.components.AnimatedRefreshIcon
import com.example.ui.components.PasswordStrengthMeter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun isValidUrl(url: String): Boolean {
    if (url.isBlank()) return true
    val trimmed = url.trim().lowercase()
    return trimmed.startsWith("http://") || trimmed.startsWith("https://") || 
           (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.length >= 4)
}

private fun isValidSshPrivateKey(key: String): Boolean {
    if (key.isBlank()) return true
    val trimmed = key.trim()
    return trimmed.startsWith("-----BEGIN") || trimmed.startsWith("ssh-")
}

private fun isValidSshPublicKey(key: String): Boolean {
    if (key.isBlank()) return true
    val trimmed = key.trim()
    return trimmed.startsWith("ssh-") || trimmed.startsWith("ecdsa-") || trimmed.startsWith("-----BEGIN")
}

private fun isValidPasskeyRpId(rpId: String): Boolean {
    if (rpId.isBlank()) return true
    val trimmed = rpId.trim().lowercase()
    return trimmed.contains(".") || trimmed == "localhost" || trimmed.startsWith("http")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    initialItem: DecryptedVaultItem?,
    initialType: ItemType = ItemType.LOGIN,
    folders: List<FolderEntity>,
    onSave: (DecryptedVaultItem) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initialItem?.type ?: initialType) }
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var selectedFolderId by remember { mutableStateOf(initialItem?.folderId) }
    var isFavorite by remember { mutableStateOf(initialItem?.isFavorite ?: false) }
    var isHidden by remember { mutableStateOf(initialItem?.isHidden ?: false) }

    // Type-specific state
    var username by remember { mutableStateOf(initialItem?.loginData?.username ?: "") }
    var password by remember { mutableStateOf(initialItem?.loginData?.password ?: "") }
    var totpSecret by remember { mutableStateOf(initialItem?.loginData?.totpSecret ?: "") }
    val uris = remember { mutableStateListOf(*((initialItem?.loginData?.uris ?: listOf("")).toTypedArray())) }
    var notes by remember {
        mutableStateOf(
            initialItem?.loginData?.notes
                ?: initialItem?.cardData?.notes
                ?: initialItem?.identityData?.notes
                ?: initialItem?.secureNoteData?.notes
                ?: initialItem?.sshKeyData?.notes
                ?: initialItem?.passkeyData?.notes
                ?: ""
        )
    }

    // Card State
    var cardholder by remember { mutableStateOf(initialItem?.cardData?.cardholderName ?: "") }
    var cardNumber by remember { mutableStateOf(initialItem?.cardData?.cardNumber ?: "") }
    var cardBrand by remember { mutableStateOf(initialItem?.cardData?.brand ?: "") }
    var expMonth by remember { mutableStateOf(initialItem?.cardData?.expMonth ?: "") }
    var expYear by remember { mutableStateOf(initialItem?.cardData?.expYear ?: "") }
    var cvv by remember { mutableStateOf(initialItem?.cardData?.code ?: "") }

    // Identity State
    var firstName by remember { mutableStateOf(initialItem?.identityData?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialItem?.identityData?.lastName ?: "") }
    var email by remember { mutableStateOf(initialItem?.identityData?.email ?: "") }
    var phone by remember { mutableStateOf(initialItem?.identityData?.phone ?: "") }
    var ssn by remember { mutableStateOf(initialItem?.identityData?.ssn ?: "") }

    // SSH Key State
    var sshKeyName by remember { mutableStateOf(initialItem?.sshKeyData?.keyName ?: "") }
    var sshPrivateKey by remember { mutableStateOf(initialItem?.sshKeyData?.privateKey ?: "") }
    var sshPublicKey by remember { mutableStateOf(initialItem?.sshKeyData?.publicKey ?: "") }
    var sshFingerprint by remember { mutableStateOf(initialItem?.sshKeyData?.fingerprint ?: "") }

    // Passkey State
    var passkeyRpId by remember { mutableStateOf(initialItem?.passkeyData?.relyingPartyId ?: "") }
    var passkeyUserHandle by remember { mutableStateOf(initialItem?.passkeyData?.userHandle ?: "") }
    var passkeyCredentialId by remember { mutableStateOf(initialItem?.passkeyData?.credentialId ?: "") }
    var passkeyPublicKey by remember { mutableStateOf(initialItem?.passkeyData?.publicKey ?: "") }

    // Custom Fields
    val customFields = remember {
        mutableStateListOf(*(initialItem?.loginData?.customFields ?: emptyList()).toTypedArray())
    }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var folderDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    fun performSave(): Boolean {
        if (name.isBlank()) {
            Toast.makeText(context, "Item name cannot be empty.", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate type-specific constraints
        when (type) {
            ItemType.LOGIN -> {
                for (uri in uris) {
                    if (uri.isNotBlank() && !isValidUrl(uri)) {
                        Toast.makeText(context, "Please fix invalid URI format.", Toast.LENGTH_SHORT).show()
                        return false
                    }
                }
            }
            ItemType.CARD -> {
                if (cardNumber.isNotBlank() && cardNumber.length !in 12..19) {
                    Toast.makeText(context, "Card number must be between 12 and 19 digits.", Toast.LENGTH_SHORT).show()
                    return false
                }
                val mInt = expMonth.toIntOrNull()
                if (expMonth.isNotBlank() && (mInt == null || mInt !in 1..12)) {
                    Toast.makeText(context, "Expiration month must be between 01 and 12.", Toast.LENGTH_SHORT).show()
                    return false
                }
                if (expYear.isNotBlank() && expYear.length !in listOf(2, 4)) {
                    Toast.makeText(context, "Expiration year must be 2 or 4 digits.", Toast.LENGTH_SHORT).show()
                    return false
                }
                if (cvv.isNotBlank() && cvv.length !in 3..4) {
                    Toast.makeText(context, "Security code (CVV) must be 3 or 4 digits.", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            ItemType.IDENTITY -> {
                if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            ItemType.SSH_KEY -> {
                if (sshPrivateKey.isNotBlank() && !isValidSshPrivateKey(sshPrivateKey)) {
                    Toast.makeText(context, "SSH private key must start with -----BEGIN or ssh-.", Toast.LENGTH_SHORT).show()
                    return false
                }
                if (sshPublicKey.isNotBlank() && !isValidSshPublicKey(sshPublicKey)) {
                    Toast.makeText(context, "SSH public key must start with ssh- or ecdsa-.", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            ItemType.PASSKEY -> {
                if (passkeyRpId.isNotBlank() && !isValidPasskeyRpId(passkeyRpId)) {
                    Toast.makeText(context, "Passkey Relying Party ID must be a valid domain or URL.", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            ItemType.SECURE_NOTE -> {}
        }

        val itemToSave = DecryptedVaultItem(
            id = initialItem?.id ?: UUID.randomUUID().toString(),
            type = type,
            name = name,
            folderId = selectedFolderId,
            isFavorite = isFavorite,
            isHidden = isHidden,
            loginData = if (type == ItemType.LOGIN) LoginData(
                username = username,
                password = password,
                totpSecret = totpSecret,
                uris = uris.filter { it.isNotBlank() },
                notes = notes,
                customFields = customFields
            ) else null,
            cardData = if (type == ItemType.CARD) CardData(
                cardholderName = cardholder,
                cardNumber = cardNumber,
                brand = cardBrand,
                expMonth = expMonth,
                expYear = expYear,
                code = cvv,
                notes = notes
            ) else null,
            identityData = if (type == ItemType.IDENTITY) IdentityData(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                ssn = ssn,
                notes = notes
            ) else null,
            secureNoteData = if (type == ItemType.SECURE_NOTE) SecureNoteData(notes = notes) else null,
            sshKeyData = if (type == ItemType.SSH_KEY) SshKeyData(
                keyName = sshKeyName.ifBlank { name },
                privateKey = sshPrivateKey,
                publicKey = sshPublicKey,
                fingerprint = sshFingerprint,
                notes = notes
            ) else null,
            passkeyData = if (type == ItemType.PASSKEY) PasskeyData(
                relyingPartyId = passkeyRpId,
                userHandle = passkeyUserHandle,
                credentialId = passkeyCredentialId,
                publicKey = passkeyPublicKey,
                notes = notes
            ) else null
        )
        onSave(itemToSave)
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialItem == null) "Add item" else "Edit item", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { performSave() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = BitwardenBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Item Type Picker
            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = type.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("What type of item is this?") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BitwardenBlue)
                )
                ExposedDropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false }
                ) {
                    ItemType.entries.forEach { itemType ->
                        DropdownMenuItem(
                            text = { Text(itemType.label) },
                            onClick = {
                                type = itemType
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                isError = name.isBlank(),
                supportingText = if (name.isBlank()) {
                    { Text("Name is required", color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BitwardenBlue)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Folder Picker
            ExposedDropdownMenuBox(
                expanded = folderDropdownExpanded,
                onExpandedChange = { folderDropdownExpanded = !folderDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val folderName = folders.find { it.id == selectedFolderId }?.name ?: "Folder (None)"
                OutlinedTextField(
                    value = folderName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Folder") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BitwardenBlue)
                )
                ExposedDropdownMenu(
                    expanded = folderDropdownExpanded,
                    onDismissRequest = { folderDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            selectedFolderId = null
                            folderDropdownExpanded = false
                        }
                    )
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = {
                                selectedFolderId = folder.id
                                folderDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggles Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Favorite", fontWeight = FontWeight.SemiBold)
                        Switch(checked = isFavorite, onCheckedChange = { isFavorite = it })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hidden Item", fontWeight = FontWeight.SemiBold)
                            Text("Requires master password/biometrics to reveal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isHidden, onCheckedChange = { isHidden = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type-specific fields
            when (type) {
                ItemType.LOGIN -> {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AnimatedRefreshIcon(
                            onRefresh = { password = CryptoManager.generatePassword(16) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PasswordStrengthMeter(
                        password = password,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = totpSecret,
                        onValueChange = { totpSecret = it },
                        label = { Text("Authenticator Key (TOTP)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("URIs", fontWeight = FontWeight.SemiBold)
                    uris.forEachIndexed { index, uriValue ->
                        val isUriInvalid = uriValue.isNotBlank() && !isValidUrl(uriValue)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = uriValue,
                                onValueChange = { uris[index] = it },
                                label = { Text("URI ${index + 1}") },
                                singleLine = true,
                                isError = isUriInvalid,
                                supportingText = if (isUriInvalid) {
                                    { Text("Invalid URL format (e.g. https://example.com)", color = MaterialTheme.colorScheme.error) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                            if (uris.size > 1) {
                                IconButton(onClick = { uris.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove URI")
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { uris.add("") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add URI")
                    }
                }

                ItemType.CARD -> {
                    OutlinedTextField(
                        value = cardholder,
                        onValueChange = { cardholder = it },
                        label = { Text("Cardholder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isCardNumInvalid = cardNumber.isNotBlank() && cardNumber.length !in 12..19
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it.filter { c -> c.isDigit() } },
                        label = { Text("Card Number (Digits only)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isCardNumInvalid,
                        supportingText = if (isCardNumInvalid) {
                            { Text("Must be between 12 and 19 digits", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val mInt = expMonth.toIntOrNull()
                    val isMonthInvalid = expMonth.isNotBlank() && (mInt == null || mInt !in 1..12)
                    val isYearInvalid = expYear.isNotBlank() && expYear.length !in listOf(2, 4)
                    val isCvvInvalid = cvv.isNotBlank() && cvv.length !in 3..4

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = expMonth,
                            onValueChange = { expMonth = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Exp Month") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = isMonthInvalid,
                            supportingText = if (isMonthInvalid) {
                                { Text("01-12", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = expYear,
                            onValueChange = { expYear = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Exp Year") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = isYearInvalid,
                            supportingText = if (isYearInvalid) {
                                { Text("2/4 digits", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { cvv = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Security Code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = isCvvInvalid,
                            supportingText = if (isCvvInvalid) {
                                { Text("3-4 digits", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showDatePickerDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Date via Date Picker")
                    }
                }

                ItemType.IDENTITY -> {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isEmailInvalid = email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = isEmailInvalid,
                        supportingText = if (isEmailInvalid) {
                            { Text("Invalid email address format", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() || c in " +()-" } },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ssn,
                        onValueChange = { ssn = it.filter { c -> c.isDigit() || c == '-' } },
                        label = { Text("SSN / National ID") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ItemType.SSH_KEY -> {
                    val isPrivKeyInvalid = sshPrivateKey.isNotBlank() && !isValidSshPrivateKey(sshPrivateKey)
                    val isPubKeyInvalid = sshPublicKey.isNotBlank() && !isValidSshPublicKey(sshPublicKey)

                    OutlinedTextField(
                        value = sshKeyName,
                        onValueChange = { sshKeyName = it },
                        label = { Text("Key Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sshPrivateKey,
                        onValueChange = { sshPrivateKey = it },
                        label = { Text("Private Key") },
                        minLines = 4,
                        isError = isPrivKeyInvalid,
                        supportingText = if (isPrivKeyInvalid) {
                            { Text("Must start with -----BEGIN or ssh-", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sshPublicKey,
                        onValueChange = { sshPublicKey = it },
                        label = { Text("Public Key") },
                        minLines = 3,
                        isError = isPubKeyInvalid,
                        supportingText = if (isPubKeyInvalid) {
                            { Text("Must start with ssh-rsa, ssh-ed25519, etc.", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sshFingerprint,
                        onValueChange = { sshFingerprint = it },
                        label = { Text("Fingerprint (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ItemType.PASSKEY -> {
                    val isRpIdInvalid = passkeyRpId.isNotBlank() && !isValidPasskeyRpId(passkeyRpId)

                    OutlinedTextField(
                        value = passkeyRpId,
                        onValueChange = { passkeyRpId = it },
                        label = { Text("Relying Party ID / Domain") },
                        singleLine = true,
                        isError = isRpIdInvalid,
                        supportingText = if (isRpIdInvalid) {
                            { Text("Must be a valid domain name (e.g. github.com)", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passkeyUserHandle,
                        onValueChange = { passkeyUserHandle = it },
                        label = { Text("User Handle / Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passkeyCredentialId,
                        onValueChange = { passkeyCredentialId = it },
                        label = { Text("Credential ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passkeyPublicKey,
                        onValueChange = { passkeyPublicKey = it },
                        label = { Text("Public Key / Passkey Data") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ItemType.SECURE_NOTE -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Notes", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { performSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BitwardenBlue)
            ) {
                Text("Save Item", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = selectedMillis
                            expMonth = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
                            expYear = cal.get(Calendar.YEAR).toString()
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
