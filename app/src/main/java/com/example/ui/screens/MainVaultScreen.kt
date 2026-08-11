package com.example.ui.screens

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DecryptedVaultItem
import com.example.data.models.FolderEntity
import com.example.data.models.ItemType
import com.example.session.AutoLockOption
import com.example.session.ClipboardClearOption
import com.example.ui.theme.BitwardenBlue
import com.example.ui.viewmodel.GeneratorType
import com.example.ui.viewmodel.GeneratorViewModel
import com.example.ui.viewmodel.VaultViewModel

import com.example.ui.components.AnimatedCopyIcon
import com.example.ui.components.AnimatedFavoriteIcon
import com.example.ui.components.AnimatedRefreshIcon
import com.example.ui.components.AnimatedTabIcon
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.components.FluentAppLogoEmblem
import com.example.ui.components.PasswordStrengthMeter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check

enum class MainTab(val label: String, val icon: ImageVector) {
    VAULTS("Vaults", Icons.Default.Lock),
    SEND("Send", Icons.AutoMirrored.Filled.Send),
    GENERATOR("Generator", Icons.Default.Refresh),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVaultScreen(
    vaultViewModel: VaultViewModel,
    generatorViewModel: GeneratorViewModel,
    items: List<DecryptedVaultItem>,
    recentlyAccessedItems: List<DecryptedVaultItem> = emptyList(),
    folders: List<FolderEntity>,
    searchQuery: String,
    autoLockOption: AutoLockOption,
    clipboardClearOption: ClipboardClearOption,
    biometricEnabled: Boolean,
    onTriggerBiometric: (onSuccess: () -> Unit) -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onSelectItem: (DecryptedVaultItem) -> Unit,
    onRecordItemAccess: (String) -> Unit = {},
    onAddNewItem: (ItemType) -> Unit = {},
    onOpenVaultHealth: () -> Unit,
    onLockVault: () -> Unit,
    onCopySecret: (label: String, text: String) -> Unit,
    onSetAutoLockOption: (AutoLockOption) -> Unit,
    onSetClipboardClearOption: (ClipboardClearOption) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onExportVault: suspend (password: String) -> String,
    onImportVault: suspend (json: String, password: String?) -> Boolean,
    onImportCsv: (csvText: String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(MainTab.VAULTS) }
    var isSearching by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showTypeBottomSheet by remember { mutableStateOf(false) }
    val selectedTypeFilter by vaultViewModel.selectedTypeFilter.collectAsState()
    val showHiddenOnly by vaultViewModel.showHiddenOnly.collectAsState()
    val isHiddenUnlocked by vaultViewModel.isHiddenUnlocked.collectAsState()
    var showHiddenAuthDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearching || selectedTypeFilter != null || showHiddenOnly || selectedTab != MainTab.VAULTS) {
        when {
            isSearching -> {
                isSearching = false
                onSearchQueryChange("")
            }
            selectedTypeFilter != null -> {
                vaultViewModel.setTypeFilter(null)
            }
            showHiddenOnly -> {
                vaultViewModel.lockHiddenFolder()
            }
            selectedTab != MainTab.VAULTS -> {
                selectedTab = MainTab.VAULTS
            }
        }
    }

    Scaffold(
        topBar = {
            if (selectedTab == MainTab.VAULTS) {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = { Text("Search vault...") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (searchQuery.isNotEmpty()) {
                                                onSearchQueryChange("")
                                            } else {
                                                isSearching = false
                                            }
                                        },
                                        modifier = Modifier.testTag("vault_search_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vault_search_input")
                            )
                        } else {
                            Text(if (showHiddenOnly) "Hidden Folder" else "Vault", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        FluentAppLogoEmblem(
                            size = 32.dp,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (isSearching) {
                                    isSearching = false
                                    onSearchQueryChange("")
                                } else {
                                    isSearching = true
                                }
                            },
                            modifier = Modifier.testTag("vault_search_toggle_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Filter")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            } else {
                TopAppBar(
                    title = { Text(selectedTab.label, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            AnimatedTabIcon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                isSelected = selectedTab == tab
                            )
                        },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BitwardenBlue,
                            selectedTextColor = BitwardenBlue
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == MainTab.VAULTS) {
                FloatingActionButton(
                    onClick = { showTypeBottomSheet = true },
                    containerColor = BitwardenBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_item_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                MainTab.VAULTS -> VaultsTabContent(
                    items = items,
                    recentlyAccessedItems = recentlyAccessedItems,
                    searchQuery = searchQuery,
                    selectedTypeFilter = selectedTypeFilter,
                    showHiddenOnly = showHiddenOnly,
                    onSelectTypeFilter = { type -> vaultViewModel.setTypeFilter(type) },
                    onOpenHiddenFolder = {
                        if (isHiddenUnlocked) {
                            vaultViewModel.unlockHiddenFolder()
                        } else {
                            showHiddenAuthDialog = true
                        }
                    },
                    onLockHiddenFolder = { vaultViewModel.lockHiddenFolder() },
                    onToggleItemHiddenState = { item -> vaultViewModel.toggleItemHiddenState(item) },
                    onSelectItem = onSelectItem,
                    onCopySecret = onCopySecret,
                    onRecordItemAccess = onRecordItemAccess
                )
                MainTab.SEND -> SendTabContent(onCopySecret = onCopySecret)
                MainTab.GENERATOR -> GeneratorTabContent(viewModel = generatorViewModel, onCopySecret = onCopySecret)
                MainTab.SETTINGS -> SettingsTabContent(
                    autoLockOption = autoLockOption,
                    clipboardClearOption = clipboardClearOption,
                    biometricEnabled = biometricEnabled,
                    onOpenVaultHealth = onOpenVaultHealth,
                    onLockVault = onLockVault,
                    onSetAutoLockOption = onSetAutoLockOption,
                    onSetClipboardClearOption = onSetClipboardClearOption,
                    onSetBiometricEnabled = onSetBiometricEnabled,
                    onExportVault = onExportVault,
                    onImportVault = onImportVault,
                    onImportCsv = onImportCsv
                )
            }
        }
    }

    if (showTypeBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTypeBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("item_type_bottom_sheet")
        ) {
            ItemTypeSelectionSheetContent(
                onSelectType = { selectedType ->
                    showTypeBottomSheet = false
                    onAddNewItem(selectedType)
                },
                onDismiss = { showTypeBottomSheet = false }
            )
        }
    }

    if (showHiddenAuthDialog) {
        HiddenFolderAuthDialog(
            biometricEnabled = biometricEnabled,
            onVerifyMasterPassword = { pwd -> vaultViewModel.verifyMasterPassword(pwd) },
            onVerifyPin = { pin -> vaultViewModel.verifyPinPasscode(pin) },
            onSetPin = { pin -> vaultViewModel.setPinPasscode(pin) },
            onTriggerBiometric = onTriggerBiometric,
            onSuccess = {
                showHiddenAuthDialog = false
                vaultViewModel.unlockHiddenFolder()
            },
            onDismiss = { showHiddenAuthDialog = false }
        )
    }
}

private data class ItemTypeOption(
    val type: ItemType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color,
    val testTag: String
)

@Composable
fun ItemTypeSelectionSheetContent(
    onSelectType: (ItemType) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Add item",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select item type to store in vault",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_type_bottom_sheet")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val typeOptions = listOf(
            ItemTypeOption(
                type = ItemType.LOGIN,
                title = "Login",
                subtitle = "Usernames, passwords, TOTP & URLs",
                icon = Icons.Default.Language,
                iconColor = BitwardenBlue,
                bgColor = BitwardenBlue.copy(alpha = 0.15f),
                testTag = "add_type_login"
            ),
            ItemTypeOption(
                type = ItemType.CARD,
                title = "Card",
                subtitle = "Credit cards, debit cards & payment cards",
                icon = Icons.Default.CreditCard,
                iconColor = Color(0xFF2E7D32),
                bgColor = Color(0xFF2E7D32).copy(alpha = 0.15f),
                testTag = "add_type_card"
            ),
            ItemTypeOption(
                type = ItemType.IDENTITY,
                title = "Identity",
                subtitle = "Personal profiles, addresses, passports & SSNs",
                icon = Icons.Default.Person,
                iconColor = Color(0xFF6A1B9A),
                bgColor = Color(0xFF6A1B9A).copy(alpha = 0.15f),
                testTag = "add_type_identity"
            ),
            ItemTypeOption(
                type = ItemType.SECURE_NOTE,
                title = "Secure Note",
                subtitle = "Encrypted plain text notes & secrets",
                icon = Icons.Default.Description,
                iconColor = Color(0xFFE65100),
                bgColor = Color(0xFFE65100).copy(alpha = 0.15f),
                testTag = "add_type_secure_note"
            ),
            ItemTypeOption(
                type = ItemType.SSH_KEY,
                title = "SSH Key",
                subtitle = "Public & private SSH key pairs",
                icon = Icons.Default.Key,
                iconColor = Color(0xFF37474F),
                bgColor = Color(0xFF37474F).copy(alpha = 0.15f),
                testTag = "add_type_ssh_key"
            ),
            ItemTypeOption(
                type = ItemType.PASSKEY,
                title = "Passkey",
                subtitle = "FIDO2 / WebAuthn passwordless credentials",
                icon = Icons.Default.VpnKey,
                iconColor = Color(0xFF00838F),
                bgColor = Color(0xFF00838F).copy(alpha = 0.15f),
                testTag = "add_type_passkey"
            )
        )

        typeOptions.forEach { option ->
            ItemTypeRow(option = option, onClick = { onSelectType(option.type) })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ItemTypeRow(
    option: ItemTypeOption,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(option.testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = option.bgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.title,
                        tint = option.iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = option.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VaultsTabContent(
    items: List<DecryptedVaultItem>,
    recentlyAccessedItems: List<DecryptedVaultItem> = emptyList(),
    searchQuery: String = "",
    selectedTypeFilter: ItemType? = null,
    showHiddenOnly: Boolean = false,
    onSelectTypeFilter: (ItemType?) -> Unit = {},
    onOpenHiddenFolder: () -> Unit = {},
    onLockHiddenFolder: () -> Unit = {},
    onToggleItemHiddenState: (DecryptedVaultItem) -> Unit = {},
    onSelectItem: (DecryptedVaultItem) -> Unit,
    onCopySecret: (label: String, text: String) -> Unit,
    onRecordItemAccess: (String) -> Unit = {}
) {
    val isSearching = searchQuery.isNotBlank()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (showHiddenOnly) {
            item {
                Surface(
                    color = Color(0xFFD84315).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFD84315),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hidden Folder Active (${items.size} items)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315),
                                fontSize = 13.sp
                            )
                        }
                        TextButton(onClick = onLockHiddenFolder) {
                            Text("Lock Hidden Folder", color = Color(0xFFD84315), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (selectedTypeFilter != null) {
            item {
                Surface(
                    color = BitwardenBlue.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = BitwardenBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Filtered by: ${selectedTypeFilter.label}",
                                fontWeight = FontWeight.Bold,
                                color = BitwardenBlue,
                                fontSize = 13.sp
                            )
                        }
                        TextButton(onClick = { onSelectTypeFilter(null) }) {
                            Text("Show All Items", color = BitwardenBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (isSearching) {
            item {
                SectionHeaderLabel("SEARCH RESULTS (${items.size})")
            }

            if (items.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Text(
                            text = "No items matching \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(items) { item ->
                    VaultItemRow(
                        item = item,
                        onClick = {
                            onRecordItemAccess(item.id)
                            onSelectItem(item)
                        },
                        onCopySecret = { label, text ->
                            onRecordItemAccess(item.id)
                            onCopySecret(label, text)
                        },
                        onToggleItemHiddenState = onToggleItemHiddenState
                    )
                }
            }
        } else {
            val favorites = items.filter { it.isFavorite }
            val loginCount = items.count { it.type == ItemType.LOGIN }
            val cardCount = items.count { it.type == ItemType.CARD }
            val identityCount = items.count { it.type == ItemType.IDENTITY }
            val noteCount = items.count { it.type == ItemType.SECURE_NOTE }
            val sshCount = items.count { it.type == ItemType.SSH_KEY }
            val passkeyCount = items.count { it.type == ItemType.PASSKEY }

            item {
                Text(
                    text = if (showHiddenOnly) "Vault: Hidden Folder" else if (selectedTypeFilter != null) "Vault: ${selectedTypeFilter.label}" else "Vault: All",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (recentlyAccessedItems.isNotEmpty()) {
                item {
                    SectionHeaderLabel(
                        title = "RECENTLY ACCESSED (${recentlyAccessedItems.size})",
                        icon = Icons.Default.History
                    )
                }
                items(recentlyAccessedItems) { item ->
                    VaultItemRow(
                        item = item,
                        onClick = {
                            onRecordItemAccess(item.id)
                            onSelectItem(item)
                        },
                        onCopySecret = { label, text ->
                            onRecordItemAccess(item.id)
                            onCopySecret(label, text)
                        },
                        onToggleItemHiddenState = onToggleItemHiddenState,
                        testTag = "recently_accessed_item_${item.id}"
                    )
                }
            }

            if (favorites.isNotEmpty()) {
                item {
                    SectionHeaderLabel("FAVORITES")
                }
                items(favorites) { item ->
                    VaultItemRow(
                        item = item,
                        onClick = {
                            onRecordItemAccess(item.id)
                            onSelectItem(item)
                        },
                        onCopySecret = { label, text ->
                            onRecordItemAccess(item.id)
                            onCopySecret(label, text)
                        },
                        onToggleItemHiddenState = onToggleItemHiddenState
                    )
                }
            }

            item {
                SectionHeaderLabel("SECURITY & HIDDEN")
            }

            item {
                val hiddenCount = items.count { it.isHidden }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (showHiddenOnly) Color(0xFFD84315).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onOpenHiddenFolder() }
                        .testTag("hidden_folder_row")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFD84315).copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD84315), modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hidden Folder", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Protected items (Master Pwd, PIN, Face/Bio)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD84315).copy(alpha = 0.2f)
                        ) {
                            Text(
                                "$hiddenCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                SectionHeaderLabel("TYPES (${items.size})")
            }

            item {
                TypeRowItem(
                    label = "Login",
                    count = loginCount,
                    icon = Icons.Default.Language,
                    isSelected = (selectedTypeFilter == ItemType.LOGIN),
                    onClick = { onSelectTypeFilter(if (selectedTypeFilter == ItemType.LOGIN) null else ItemType.LOGIN) },
                    testTag = "type_filter_login"
                )
                TypeRowItem(
                    label = "Card",
                    count = cardCount,
                    icon = Icons.Default.CreditCard,
                    isSelected = (selectedTypeFilter == ItemType.CARD),
                    onClick = { onSelectTypeFilter(if (selectedTypeFilter == ItemType.CARD) null else ItemType.CARD) },
                    testTag = "type_filter_card"
                )
                TypeRowItem(
                    label = "Identity",
                    count = identityCount,
                    icon = Icons.Default.Person,
                    isSelected = (selectedTypeFilter == ItemType.IDENTITY),
                    onClick = { onSelectTypeFilter(if (selectedTypeFilter == ItemType.IDENTITY) null else ItemType.IDENTITY) },
                    testTag = "type_filter_identity"
                )
                TypeRowItem(
                    label = "Secure Note",
                    count = noteCount,
                    icon = Icons.Default.Description,
                    isSelected = (selectedTypeFilter == ItemType.SECURE_NOTE),
                    onClick = { onSelectTypeFilter(if (selectedTypeFilter == ItemType.SECURE_NOTE) null else ItemType.SECURE_NOTE) },
                    testTag = "type_filter_secure_note"
                )
                TypeRowItem(
                    label = "SSH Key",
                    count = sshCount,
                    icon = Icons.Default.Key,
                    isSelected = (selectedTypeFilter == ItemType.SSH_KEY),
                    onClick = { onSelectTypeFilter(if (selectedTypeFilter == ItemType.SSH_KEY) null else ItemType.SSH_KEY) },
                    testTag = "type_filter_ssh_key"
                )
                TypeRowItem(
                    label = "Passkey",
                    count = passkeyCount,
                    icon = Icons.Default.VpnKey,
                    isSelected = (selectedTypeFilter == ItemType.PASSKEY),
                    onClick = { onSelectTypeFilter(if (selectedTypeFilter == ItemType.PASSKEY) null else ItemType.PASSKEY) },
                    testTag = "type_filter_passkey"
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeaderLabel(if (selectedTypeFilter != null) "FILTERED ITEMS (${items.size})" else "ALL ITEMS")
            }

            if (items.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Text(
                            text = "No items found in vault.\nTap '+' to create your first item.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(items) { item ->
                    VaultItemRow(
                        item = item,
                        onClick = {
                            onRecordItemAccess(item.id)
                            onSelectItem(item)
                        },
                        onCopySecret = { label, text ->
                            onRecordItemAccess(item.id)
                            onCopySecret(label, text)
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SectionHeaderLabel(title: String, icon: ImageVector? = null) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BitwardenBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VaultItemRow(
    item: DecryptedVaultItem,
    onClick: () -> Unit,
    onCopySecret: (label: String, text: String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "vault_item_row_${item.id}"
) {
    val iconBgColor = when (item.type) {
        ItemType.LOGIN -> BitwardenBlue.copy(alpha = 0.15f)
        ItemType.CARD -> Color(0xFF2E7D32).copy(alpha = 0.15f)
        ItemType.IDENTITY -> Color(0xFF6A1B9A).copy(alpha = 0.15f)
        ItemType.SECURE_NOTE -> Color(0xFFE65100).copy(alpha = 0.15f)
        ItemType.SSH_KEY -> Color(0xFF37474F).copy(alpha = 0.15f)
        ItemType.PASSKEY -> Color(0xFF00838F).copy(alpha = 0.15f)
    }

    val iconTint = when (item.type) {
        ItemType.LOGIN -> BitwardenBlue
        ItemType.CARD -> Color(0xFF2E7D32)
        ItemType.IDENTITY -> Color(0xFF6A1B9A)
        ItemType.SECURE_NOTE -> Color(0xFFE65100)
        ItemType.SSH_KEY -> Color(0xFF37474F)
        ItemType.PASSKEY -> Color(0xFF00838F)
    }

    val iconVector = when (item.type) {
        ItemType.LOGIN -> Icons.Default.Language
        ItemType.CARD -> Icons.Default.CreditCard
        ItemType.IDENTITY -> Icons.Default.Person
        ItemType.SECURE_NOTE -> Icons.Default.Description
        ItemType.SSH_KEY -> Icons.Default.Key
        ItemType.PASSKEY -> Icons.Default.VpnKey
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Styled Icon Container
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconBgColor,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = item.type.name,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and Subtitle Labels
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.isFavorite) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.getSubtitle(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Quick-Copy Button Pattern on Right Side
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (item.type) {
                ItemType.LOGIN -> {
                    val username = item.loginData?.username ?: ""
                    val password = item.loginData?.password ?: ""

                    if (username.isNotBlank()) {
                        IconButton(
                            onClick = { onCopySecret("Username", username) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_username_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Copy Username",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (password.isNotBlank()) {
                        IconButton(
                            onClick = { onCopySecret("Password", password) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_password_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Copy Password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                ItemType.CARD -> {
                    val cardNumber = item.cardData?.cardNumber ?: ""
                    if (cardNumber.isNotBlank()) {
                        AnimatedCopyIcon(
                            onCopy = { onCopySecret("Card Number", cardNumber) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_card_${item.id}")
                        )
                    }
                }
                ItemType.IDENTITY -> {
                    val identityContact = item.identityData?.email?.takeIf { it.isNotBlank() }
                        ?: item.identityData?.username?.takeIf { it.isNotBlank() }
                        ?: ""
                    if (identityContact.isNotBlank()) {
                        AnimatedCopyIcon(
                            onCopy = { onCopySecret("Identity Contact", identityContact) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_identity_${item.id}")
                        )
                    }
                }
                ItemType.SSH_KEY -> {
                    val keyToCopy = item.sshKeyData?.publicKey?.takeIf { it.isNotBlank() }
                        ?: item.sshKeyData?.privateKey?.takeIf { it.isNotBlank() }
                        ?: ""
                    if (keyToCopy.isNotBlank()) {
                        IconButton(
                            onClick = { onCopySecret("SSH Key", keyToCopy) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_ssh_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy SSH Key",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                ItemType.SECURE_NOTE -> {
                    val noteText = item.secureNoteData?.notes ?: ""
                    if (noteText.isNotBlank()) {
                        IconButton(
                            onClick = { onCopySecret("Note", noteText) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_note_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Note",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                ItemType.PASSKEY -> {
                    val passkeyInfo = item.passkeyData?.relyingPartyId ?: ""
                    if (passkeyInfo.isNotBlank()) {
                        IconButton(
                            onClick = { onCopySecret("Passkey RP", passkeyInfo) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("copy_passkey_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Passkey Info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("item_details_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TypeRowItem(
    label: String,
    count: Int,
    icon: ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    testTag: String = ""
) {
    Surface(
        color = if (isSelected) BitwardenBlue.copy(alpha = 0.15f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) BitwardenBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                color = if (isSelected) BitwardenBlue else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = BitwardenBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                "$count",
                color = if (isSelected) BitwardenBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}

// --- Send Tab Content ---
@Composable
fun SendTabContent(
    onCopySecret: (label: String, text: String) -> Unit
) {
    var sendTitle by remember { mutableStateOf("") }
    var sendText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Local Send Payload", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Create ephemeral local encrypted text payloads securely stored in vault.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = sendTitle,
            onValueChange = { sendTitle = it },
            label = { Text("Send Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = sendText,
            onValueChange = { sendText = it },
            label = { Text("Secret Text / Note Content") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (sendText.isNotBlank()) {
                    onCopySecret("Send Text", sendText)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BitwardenBlue)
        ) {
            Text("Copy Local Send Payload", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- Generator Tab Content (Matching Screenshot 2) ---
@Composable
fun GeneratorTabContent(
    viewModel: GeneratorViewModel,
    onCopySecret: (label: String, text: String) -> Unit
) {
    val genType by viewModel.generatorType.collectAsState()
    val secret by viewModel.generatedSecret.collectAsState()
    val length by viewModel.length.collectAsState()
    val includeUpper by viewModel.includeUpper.collectAsState()
    val includeLower by viewModel.includeLower.collectAsState()
    val includeNumbers by viewModel.includeNumbers.collectAsState()
    val includeSpecial by viewModel.includeSpecial.collectAsState()
    val avoidAmbiguous by viewModel.avoidAmbiguous.collectAsState()
    val wordCount by viewModel.wordCount.collectAsState()
    val capitalize by viewModel.capitalize.collectAsState()
    val includeNumPassphrase by viewModel.includeNumberInPassphrase.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Output Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = secret,
                onValueChange = {},
                readOnly = true,
                label = { Text("Generated Result") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BitwardenBlue)
            )

            Spacer(modifier = Modifier.width(8.dp))

            AnimatedCopyIcon(
                onCopy = { onCopySecret("Generated Secret", secret) },
                modifier = Modifier.testTag("generator_copy_button")
            )

            AnimatedRefreshIcon(
                onRefresh = { viewModel.regenerate() },
                modifier = Modifier.testTag("generator_refresh_button")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PasswordStrengthMeter(
            password = secret,
            modifier = Modifier.fillMaxWidth(),
            showRequirements = (genType == GeneratorType.PASSWORD)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Options", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Type Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("What would you like to generate?")
            Button(
                onClick = {
                    val nextType = if (genType == GeneratorType.PASSWORD) GeneratorType.PASSPHRASE else GeneratorType.PASSWORD
                    viewModel.setGeneratorType(nextType)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BitwardenBlue)
            ) {
                Text(genType.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (genType == GeneratorType.PASSWORD) {
            Text("Length: $length", fontWeight = FontWeight.SemiBold)
            Slider(
                value = length.toFloat(),
                onValueChange = { viewModel.setLength(it.toInt()) },
                valueRange = 5f..128f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchRow("A－Z (Uppercase)", includeUpper) { viewModel.setIncludeUpper(it) }
            SwitchRow("a－z (Lowercase)", includeLower) { viewModel.setIncludeLower(it) }
            SwitchRow("0－9 (Numbers)", includeNumbers) { viewModel.setIncludeNumbers(it) }
            SwitchRow("!@#$%^&* (Special)", includeSpecial) { viewModel.setIncludeSpecial(it) }
            SwitchRow("Avoid ambiguous characters", avoidAmbiguous) { viewModel.setAvoidAmbiguous(it) }
        } else {
            Text("Words: $wordCount", fontWeight = FontWeight.SemiBold)
            Slider(
                value = wordCount.toFloat(),
                onValueChange = { viewModel.setWordCount(it.toInt()) },
                valueRange = 3f..20f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            SwitchRow("Capitalize words", capitalize) { viewModel.setCapitalize(it) }
            SwitchRow("Include number", includeNumPassphrase) { viewModel.setIncludeNumberInPassphrase(it) }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// --- Settings Tab Content ---
@Composable
fun SettingsTabContent(
    autoLockOption: AutoLockOption,
    clipboardClearOption: ClipboardClearOption,
    biometricEnabled: Boolean,
    onOpenVaultHealth: () -> Unit,
    onLockVault: () -> Unit,
    onSetAutoLockOption: (AutoLockOption) -> Unit,
    onSetClipboardClearOption: (ClipboardClearOption) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onExportVault: suspend (password: String) -> String,
    onImportVault: suspend (json: String, password: String?) -> Boolean,
    onImportCsv: (csvText: String) -> Unit = {},
    onCopySecret: (label: String, text: String) -> Unit = { _, _ -> }
) {
    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    var showImportBackupDialog by remember { mutableStateOf(false) }
    var importBackupText by remember { mutableStateOf("") }
    var importBackupPassword by remember { mutableStateOf("") }

    var showImportCsvDialog by remember { mutableStateOf(false) }
    var csvTextState by remember { mutableStateOf("") }

    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showClipboardDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // SAF File Creator for Exporting Encrypted Backup File
    val exportBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            pendingExportJson?.let { payload ->
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(payload.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Encrypted backup file saved successfully!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        pendingExportJson = null
    }

    // SAF File Picker for Restoring Encrypted Backup File
    val importBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: ""
                if (content.isNotBlank()) {
                    importBackupText = content
                    showImportBackupDialog = true
                } else {
                    Toast.makeText(context, "Selected backup file is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: ""
                if (content.isNotBlank()) {
                    csvTextState = content
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Vault Health Audit
        Card(
            onClick = onOpenVaultHealth,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = BitwardenBlue, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Vault Health Audit", fontWeight = FontWeight.Bold)
                    Text("Check for weak & reused passwords offline", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("SECURITY & AUTO-LOCK", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // Comprehensive Security Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("security_settings_section")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto-lock Inactivity Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAutoLockDialog = true }
                        .padding(vertical = 6.dp)
                        .testTag("auto_lock_timeout_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = BitwardenBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Auto-Lock Timer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Lock state after period of inactivity", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = autoLockOption.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Biometric Unlock Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = BitwardenBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Biometric Unlock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Use fingerprint or face ID to unlock", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = onSetBiometricEnabled,
                        modifier = Modifier.testTag("biometric_unlock_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Clipboard Auto-Clear Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showClipboardDialog = true }
                        .padding(vertical = 6.dp)
                        .testTag("clipboard_clear_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = BitwardenBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Clipboard Auto-Clear", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Wipe copied passwords from clipboard", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = clipboardClearOption.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lock Vault Now Button
                OutlinedButton(
                    onClick = onLockVault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lock_vault_now_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock Vault Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("DATA & BACKUP", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // Backup & Restore Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = BitwardenBlue, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Encrypted Database Backup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Export or restore a portable AES-256 encrypted database file.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_backup_file_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BitwardenBlue)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Backup", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { importBackupFileLauncher.launch("*/*") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_backup_file_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore File", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showImportCsvDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_bitwarden_csv_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Bitwarden CSV", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLockVault,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lock Vault Now", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = BitwardenBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Encrypted Vault Backup")
                }
            },
            text = {
                Column {
                    Text(
                        "Set a backup password to encrypt your vault file (PBKDF2 + AES-256-GCM). " +
                        "You will need this password to restore your backup on any device.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Backup Encryption Password") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_password_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PasswordStrengthMeter(
                        password = exportPassword,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exportPassword.isNotBlank()) {
                            coroutineScope.launch {
                                val json = onExportVault(exportPassword)
                                if (json.isNotBlank()) {
                                    pendingExportJson = json
                                    showExportDialog = false
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    exportBackupFileLauncher.launch("ls_pass_backup_$timeStamp.json")
                                } else {
                                    Toast.makeText(context, "Failed to generate export payload.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = exportPassword.isNotBlank(),
                    modifier = Modifier.testTag("confirm_export_button")
                ) {
                    Text("Save Backup File")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            if (exportPassword.isNotBlank()) {
                                coroutineScope.launch {
                                    val json = onExportVault(exportPassword)
                                    if (json.isNotBlank()) {
                                        showExportDialog = false
                                        onCopySecret("Exported Vault JSON", json)
                                        Toast.makeText(context, "Encrypted payload copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = exportPassword.isNotBlank()
                    ) {
                        Text("Copy Payload")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Restore Backup Dialog
    if (showImportBackupDialog) {
        AlertDialog(
            onDismissRequest = { showImportBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = BitwardenBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Encrypted Backup")
                }
            },
            text = {
                Column {
                    Text(
                        "Enter the password used when this backup file was exported to decrypt and restore your vault items, folders, and collections.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importBackupPassword,
                        onValueChange = { importBackupPassword = it },
                        label = { Text("Backup Decryption Password") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importBackupPassword.isNotBlank() && importBackupText.isNotBlank()) {
                            coroutineScope.launch {
                                val success = onImportVault(importBackupText, importBackupPassword)
                                showImportBackupDialog = false
                                if (success) {
                                    Toast.makeText(context, "Vault successfully restored from encrypted backup file!", Toast.LENGTH_LONG).show()
                                    importBackupText = ""
                                    importBackupPassword = ""
                                } else {
                                    Toast.makeText(context, "Decryption failed. Please check your backup password.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = importBackupPassword.isNotBlank(),
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Decrypt & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportBackupDialog = false
                    importBackupText = ""
                    importBackupPassword = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportCsvDialog) {
        AlertDialog(
            onDismissRequest = { showImportCsvDialog = false },
            title = { Text("Import Bitwarden CSV") },
            text = {
                Column {
                    Text("Select a Bitwarden-exported CSV file or paste the CSV data directly below:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { csvPickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select CSV File")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = csvTextState,
                        onValueChange = { csvTextState = it },
                        label = { Text("CSV Data") },
                        placeholder = { Text("folder,favorite,type,name,notes,fields,...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_csv_text_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (csvTextState.isNotBlank()) {
                            val textToImport = csvTextState
                            showImportCsvDialog = false
                            csvTextState = ""
                            onImportCsv(textToImport)
                        }
                    },
                    enabled = csvTextState.isNotBlank(),
                    modifier = Modifier.testTag("confirm_import_csv_button")
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportCsvDialog = false
                    csvTextState = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Auto-Lock Inactivity Selection Dialog
    if (showAutoLockDialog) {
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = BitwardenBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-Lock Timer")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Configure the inactivity duration before the app locks and requires master password re-authentication.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AutoLockOption.values().forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSetAutoLockOption(option)
                                    showAutoLockDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("auto_lock_dialog_option_${option.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == autoLockOption),
                                onClick = {
                                    onSetAutoLockOption(option)
                                    showAutoLockDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (option == autoLockOption) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoLockDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Clipboard Clear Timeout Dialog
    if (showClipboardDialog) {
        AlertDialog(
            onDismissRequest = { showClipboardDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = BitwardenBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clipboard Clear Timeout")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Choose how quickly copied secrets are automatically wiped from system clipboard.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ClipboardClearOption.values().forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSetClipboardClearOption(option)
                                    showClipboardDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == clipboardClearOption),
                                onClick = {
                                    onSetClipboardClearOption(option)
                                    showClipboardDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (option == clipboardClearOption) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClipboardDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
