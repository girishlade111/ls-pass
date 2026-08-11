package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.data.db.LsPassDatabase
import com.example.data.models.DecryptedVaultItem
import com.example.data.models.ItemType
import com.example.data.repository.VaultRepository
import com.example.session.AutoLockOption
import com.example.session.ClipboardClearOption
import com.example.session.VaultAuthState
import com.example.session.VaultSessionManager
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.ItemEditScreen
import com.example.ui.screens.MainVaultScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.UnlockScreen
import com.example.ui.screens.VaultHealthScreen
import com.example.ui.theme.LsPassTheme
import com.example.ui.viewmodel.GeneratorViewModel
import com.example.ui.viewmodel.VaultViewModel
import com.example.ui.viewmodel.VaultViewModelFactory
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

sealed class Screen {
    object Main : Screen()
    data class Detail(val item: DecryptedVaultItem) : Screen()
    data class Edit(val item: DecryptedVaultItem?, val initialType: ItemType = ItemType.LOGIN) : Screen()
    object Health : Screen()
}

class MainActivity : FragmentActivity() {

    private lateinit var sessionManager: VaultSessionManager
    private lateinit var vaultViewModel: VaultViewModel
    private var currentAutoLockOption: AutoLockOption = AutoLockOption.MIN_5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = LsPassDatabase.getInstance(applicationContext)
        val repository = VaultRepository(db.vaultDao())
        sessionManager = VaultSessionManager(applicationContext)

        lifecycleScope.launch {
            sessionManager.autoLockOptionFlow.collect { option ->
                currentAutoLockOption = option
            }
        }

        val factory = VaultViewModelFactory(repository, sessionManager)
        vaultViewModel = ViewModelProvider(this, factory)[VaultViewModel::class.java]

        setContent {
            LsPassTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                    sessionManager.onUserInteraction()
                                }
                            }
                        }
                ) {
                    LsPassApp(
                        sessionManager = sessionManager,
                        vaultViewModel = vaultViewModel,
                        onTriggerBiometric = { onSuccess -> triggerBiometricPrompt(onSuccess) }
                    )
                }
            }
        }
    }

    private fun triggerBiometricPrompt(onSuccess: () -> Unit) {
        val biometricManager = com.example.crypto.BiometricManager.getInstance(this)
        biometricManager.authenticate(
            activity = this,
            title = "LS Pass Biometric Unlock",
            subtitle = "Authenticate to access your encrypted vault",
            negativeButtonText = "Use Master Password",
            onSuccess = {
                runOnUiThread { onSuccess() }
            },
            onError = { _, errString ->
                runOnUiThread {
                    Toast.makeText(applicationContext, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        if (::sessionManager.isInitialized) {
            sessionManager.onAppBackground(currentAutoLockOption)
        }
    }
}

@Composable
fun LsPassApp(
    sessionManager: VaultSessionManager,
    vaultViewModel: VaultViewModel,
    onTriggerBiometric: (onSuccess: () -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authState by sessionManager.authState.collectAsState()
    val passwordHint by sessionManager.passwordHintFlow.collectAsState(initial = "")
    val autoLockOption by sessionManager.autoLockOptionFlow.collectAsState(initial = AutoLockOption.MIN_5)
    val clipboardClearOption by sessionManager.clipboardClearOptionFlow.collectAsState(initial = ClipboardClearOption.SEC_30)
    val biometricEnabled by sessionManager.biometricEnabledFlow.collectAsState(initial = false)

    // Inactivity ticker loop
    LaunchedEffect(authState, autoLockOption) {
        if (authState == VaultAuthState.UNLOCKED) {
            while (true) {
                delay(3000L)
                sessionManager.checkAutoLock(autoLockOption)
            }
        }
    }

    val filteredItems by vaultViewModel.filteredItems.collectAsState()
    val recentlyAccessedItems by vaultViewModel.recentlyAccessedItems.collectAsState()
    val folders by vaultViewModel.folders.collectAsState()
    val searchQuery by vaultViewModel.searchQuery.collectAsState()

    val generatorViewModel: GeneratorViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

    when (authState) {
        VaultAuthState.NOT_SETUP -> {
            SetupScreen(
                onSetupVault = { masterPassword, hint, enableBio ->
                    scope.launch {
                        sessionManager.setupVault(masterPassword, hint, enableBio)
                    }
                }
            )
        }

        VaultAuthState.LOCKED -> {
            UnlockScreen(
                passwordHint = passwordHint,
                biometricEnabled = biometricEnabled,
                onUnlockWithMasterPassword = { pwd ->
                    scope.launch {
                        val success = sessionManager.unlockWithMasterPassword(pwd)
                        if (!success) {
                            Toast.makeText(context, "Invalid master password", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onUnlockWithBiometric = {
                    onTriggerBiometric {
                        scope.launch {
                            sessionManager.unlockWithBiometric()
                        }
                    }
                }
            )
        }

        VaultAuthState.UNLOCKED -> {
            BackHandler(enabled = currentScreen != Screen.Main) {
                currentScreen = Screen.Main
            }

            when (val screen = currentScreen) {
                is Screen.Main -> {
                    MainVaultScreen(
                        vaultViewModel = vaultViewModel,
                        generatorViewModel = generatorViewModel,
                        items = filteredItems,
                        recentlyAccessedItems = recentlyAccessedItems,
                        folders = folders,
                        searchQuery = searchQuery,
                        autoLockOption = autoLockOption,
                        clipboardClearOption = clipboardClearOption,
                        biometricEnabled = biometricEnabled,
                        onTriggerBiometric = onTriggerBiometric,
                        onSearchQueryChange = { vaultViewModel.updateSearchQuery(it) },
                        onSelectItem = { item ->
                            vaultViewModel.recordItemAccess(item.id)
                            currentScreen = Screen.Detail(item)
                        },
                        onRecordItemAccess = { id -> vaultViewModel.recordItemAccess(id) },
                        onAddNewItem = { itemType -> currentScreen = Screen.Edit(null, initialType = itemType) },
                        onOpenVaultHealth = { currentScreen = Screen.Health },
                        onLockVault = { sessionManager.lockVault() },
                        onCopySecret = { label, text ->
                            sessionManager.copyToClipboardAndScheduleClear(label, text, clipboardClearOption)
                        },
                        onSetAutoLockOption = { option ->
                            scope.launch { sessionManager.setAutoLockOption(option) }
                        },
                        onSetClipboardClearOption = { option ->
                            scope.launch { sessionManager.setClipboardClearOption(option) }
                        },
                        onSetBiometricEnabled = { enabled ->
                            scope.launch { sessionManager.setBiometricEnabled(enabled) }
                        },
                        onExportVault = { exportPwd ->
                            vaultViewModel.exportEncryptedVault(exportPwd)
                        },
                        onImportVault = { json, pwd ->
                            vaultViewModel.importVaultJson(json, pwd)
                        },
                        onImportCsv = { csvText ->
                            scope.launch {
                                val result = vaultViewModel.importBitwardenCsv(csvText)
                                if (result.success) {
                                    Toast.makeText(
                                        context,
                                        "Imported ${result.importedCount} items (${result.createdFoldersCount} new folders)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Import failed: ${result.errorMessage ?: "Invalid CSV format"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }

                is Screen.Detail -> {
                    ItemDetailScreen(
                        item = screen.item,
                        onBack = { currentScreen = Screen.Main },
                        onEdit = {
                            vaultViewModel.recordItemAccess(screen.item.id)
                            currentScreen = Screen.Edit(screen.item)
                        },
                        onDelete = {
                            vaultViewModel.deleteItem(screen.item.id)
                            currentScreen = Screen.Main
                        },
                        onToggleHidden = {
                            vaultViewModel.toggleItemHiddenState(screen.item)
                            currentScreen = Screen.Main
                        },
                        onCopySecret = { label, text ->
                            vaultViewModel.recordItemAccess(screen.item.id)
                            sessionManager.copyToClipboardAndScheduleClear(label, text, clipboardClearOption)
                        }
                    )
                }

                is Screen.Edit -> {
                    ItemEditScreen(
                        initialItem = screen.item,
                        initialType = screen.initialType,
                        folders = folders,
                        onSave = { itemToSave ->
                            vaultViewModel.saveItem(itemToSave)
                            currentScreen = Screen.Main
                        },
                        onCancel = { currentScreen = Screen.Main }
                    )
                }

                is Screen.Health -> {
                    VaultHealthScreen(
                        onGenerateReport = { vaultViewModel.generateHealthReport() },
                        onBack = { currentScreen = Screen.Main }
                    )
                }
            }
        }
    }
}
