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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import java.util.UUID

import com.example.ui.components.AnimatedRefreshIcon
import com.example.ui.components.PasswordStrengthMeter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    initialItem: DecryptedVaultItem?,
    initialType: ItemType = ItemType.LOGIN,
    folders: List<FolderEntity>,
    onSave: (DecryptedVaultItem) -> Unit,
    onCancel: () -> Unit
) {
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

    // Custom Fields
    val customFields = remember {
        mutableStateListOf(*(initialItem?.loginData?.customFields ?: emptyList()).toTypedArray())
    }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var folderDropdownExpanded by remember { mutableStateOf(false) }

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
                    IconButton(
                        onClick = {
                            if (name.isNotBlank()) {
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
                                    sshKeyData = if (type == ItemType.SSH_KEY) SshKeyData(notes = notes) else null,
                                    passkeyData = if (type == ItemType.PASSKEY) PasskeyData(notes = notes) else null
                                )
                                onSave(itemToSave)
                            }
                        }
                    ) {
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
                    OutlinedTextField(value = cardholder, onValueChange = { cardholder = it }, label = { Text("Cardholder Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = cardNumber, onValueChange = { cardNumber = it }, label = { Text("Card Number") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = expMonth, onValueChange = { expMonth = it }, label = { Text("Exp Month") }, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(value = expYear, onValueChange = { expYear = it }, label = { Text("Exp Year") }, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(value = cvv, onValueChange = { cvv = it }, label = { Text("Security Code") }, modifier = Modifier.weight(1f))
                    }
                }

                ItemType.IDENTITY -> {
                    OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = ssn, onValueChange = { ssn = it }, label = { Text("SSN / National ID") }, modifier = Modifier.fillMaxWidth())
                }

                ItemType.SECURE_NOTE, ItemType.SSH_KEY, ItemType.PASSKEY -> {
                    // Uses general notes field below
                }
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
                onClick = {
                    if (name.isNotBlank()) {
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
                            sshKeyData = if (type == ItemType.SSH_KEY) SshKeyData(notes = notes) else null,
                            passkeyData = if (type == ItemType.PASSKEY) PasskeyData(notes = notes) else null
                        )
                        onSave(itemToSave)
                    }
                },
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
}
