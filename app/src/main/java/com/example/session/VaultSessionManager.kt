package com.example.session

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.crypto.BiometricManager
import com.example.crypto.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.crypto.SecretKey

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ls_pass_preferences")

enum class VaultAuthState {
    NOT_SETUP,
    LOCKED,
    UNLOCKED
}

enum class AutoLockOption(val displayName: String, val timeoutMs: Long) {
    IMMEDIATE("Immediate", 0L),
    MIN_1("1 minute", 60_000L),
    MIN_5("5 minutes (Default)", 300_000L),
    MIN_15("15 minutes", 900_000L),
    MIN_30("30 minutes", 1_800_000L),
    ON_BACKGROUND("On app background", -1L),
    NEVER("Never", -2L)
}

enum class ClipboardClearOption(val displayName: String, val seconds: Int) {
    SEC_10("10 seconds", 10),
    SEC_20("20 seconds", 20),
    SEC_30("30 seconds (Default)", 30),
    MIN_1("1 minute", 60),
    NEVER("Never", 0)
}

class VaultSessionManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val KEY_IS_SETUP = booleanPreferencesKey("is_vault_setup")
    private val KEY_SALT = stringPreferencesKey("salt_base64")
    private val KEY_VERIFICATION_TOKEN = stringPreferencesKey("verification_token_encrypted")
    private val KEY_PASSWORD_HINT = stringPreferencesKey("password_hint")
    private val KEY_AUTO_LOCK_OPTION = stringPreferencesKey("auto_lock_option")
    private val KEY_CLIPBOARD_CLEAR_OPTION = stringPreferencesKey("clipboard_clear_option")
    private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    private val KEY_PIN_PASSCODE = stringPreferencesKey("pin_passcode_hash")

    private val _authState = MutableStateFlow(VaultAuthState.LOCKED)
    val authState: StateFlow<VaultAuthState> = _authState.asStateFlow()

    private val _activeMasterKeyFlow = MutableStateFlow<SecretKey?>(null)
    val activeMasterKeyFlow: StateFlow<SecretKey?> = _activeMasterKeyFlow.asStateFlow()

    private var activeMasterKey: SecretKey? = null
    private var lastActivityTimeMs: Long = System.currentTimeMillis()
    private var clipboardClearJob: Job? = null

    private val biometricManager = BiometricManager.getInstance(context)

    val isSetupFlow = context.dataStore.data.map { prefs -> prefs[KEY_IS_SETUP] ?: false }
    val passwordHintFlow = context.dataStore.data.map { prefs -> prefs[KEY_PASSWORD_HINT] ?: "" }
    val pinPasscodeFlow = context.dataStore.data.map { prefs -> prefs[KEY_PIN_PASSCODE] ?: "" }
    val autoLockOptionFlow = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_AUTO_LOCK_OPTION] ?: AutoLockOption.MIN_5.name
        try { AutoLockOption.valueOf(name) } catch (_: Exception) { AutoLockOption.MIN_5 }
    }
    val clipboardClearOptionFlow = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_CLIPBOARD_CLEAR_OPTION] ?: ClipboardClearOption.SEC_30.name
        try { ClipboardClearOption.valueOf(name) } catch (_: Exception) { ClipboardClearOption.SEC_30 }
    }
    val biometricEnabledFlow = context.dataStore.data.map { prefs -> prefs[KEY_BIOMETRIC_ENABLED] ?: false }

    init {
        scope.launch {
            val isSetup = context.dataStore.data.map { it[KEY_IS_SETUP] ?: false }.first()
            if (!isSetup) {
                _authState.value = VaultAuthState.NOT_SETUP
            } else {
                _authState.value = VaultAuthState.LOCKED
            }
        }
    }

    companion object {
        @Volatile
        private var sharedActiveMasterKey: SecretKey? = null

        fun getSharedMasterKey(): SecretKey? = sharedActiveMasterKey
        fun setSharedMasterKey(key: SecretKey?) {
            sharedActiveMasterKey = key
        }
    }

    fun getActiveMasterKey(): SecretKey? = activeMasterKey ?: sharedActiveMasterKey

    private fun updateActiveMasterKey(key: SecretKey?) {
        activeMasterKey = key
        setSharedMasterKey(key)
        _activeMasterKeyFlow.value = key
    }

    suspend fun setupVault(masterPassword: String, hint: String, enableBiometric: Boolean): Boolean {
        if (masterPassword.isBlank()) return false

        val salt = CryptoManager.generateSalt()
        val derivedKey = CryptoManager.deriveKey(masterPassword.toCharArray(), salt)

        // Store verification string encrypted with derivedKey
        val verificationText = "LS_PASS_VALID_VAULT_TOKEN"
        val encryptedVerification = CryptoManager.encrypt(verificationText, derivedKey)

        context.dataStore.edit { prefs ->
            prefs[KEY_IS_SETUP] = true
            prefs[KEY_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
            prefs[KEY_VERIFICATION_TOKEN] = encryptedVerification
            prefs[KEY_PASSWORD_HINT] = hint
            prefs[KEY_BIOMETRIC_ENABLED] = enableBiometric
        }

        if (enableBiometric) {
            try {
                val encryptedKeyBytes = biometricManager.encryptMasterKey(derivedKey)
                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey("encrypted_master_key_for_bio")] = encryptedKeyBytes
                }
            } catch (_: Exception) {}
        }

        updateActiveMasterKey(derivedKey)
        lastActivityTimeMs = System.currentTimeMillis()
        _authState.value = VaultAuthState.UNLOCKED
        return true
    }

    suspend fun unlockWithMasterPassword(masterPassword: String): Boolean {
        val prefs = context.dataStore.data.first()
        val saltBase64 = prefs[KEY_SALT] ?: return false
        val encryptedVerification = prefs[KEY_VERIFICATION_TOKEN] ?: return false

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val derivedKey = CryptoManager.deriveKey(masterPassword.toCharArray(), salt)

        val decryptedVerification = try {
            CryptoManager.decrypt(encryptedVerification, derivedKey)
        } catch (e: Exception) {
            ""
        }

        if (decryptedVerification == "LS_PASS_VALID_VAULT_TOKEN") {
            updateActiveMasterKey(derivedKey)
            lastActivityTimeMs = System.currentTimeMillis()
            _authState.value = VaultAuthState.UNLOCKED

            // If biometric is enabled, refresh backup
            if (prefs[KEY_BIOMETRIC_ENABLED] == true) {
                try {
                    val encryptedKeyBytes = biometricManager.encryptMasterKey(derivedKey)
                    context.dataStore.edit { p ->
                        p[stringPreferencesKey("encrypted_master_key_for_bio")] = encryptedKeyBytes
                    }
                } catch (_: Exception) {}
            }
            return true
        } else {
            return false
        }
    }

    suspend fun unlockWithBiometric(): Boolean {
        val prefs = context.dataStore.data.first()
        val bioEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false
        if (!bioEnabled) return false

        val encryptedKeyBytes = prefs[stringPreferencesKey("encrypted_master_key_for_bio")] ?: return false
        val masterKey = biometricManager.decryptMasterKey(encryptedKeyBytes) ?: return false

        updateActiveMasterKey(masterKey)
        lastActivityTimeMs = System.currentTimeMillis()
        _authState.value = VaultAuthState.UNLOCKED
        return true
    }

    suspend fun setPinPasscode(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIN_PASSCODE] = pin.trim()
        }
    }

    suspend fun verifyPinPasscode(pin: String): Boolean {
        val savedPin = context.dataStore.data.map { it[KEY_PIN_PASSCODE] ?: "" }.first()
        return savedPin.isNotBlank() && savedPin == pin.trim()
    }

    suspend fun verifyMasterPassword(masterPassword: String): Boolean {
        val prefs = context.dataStore.data.first()
        val saltBase64 = prefs[KEY_SALT] ?: return false
        val encryptedVerification = prefs[KEY_VERIFICATION_TOKEN] ?: return false

        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val derivedKey = CryptoManager.deriveKey(masterPassword.toCharArray(), salt)

        val decryptedVerification = try {
            CryptoManager.decrypt(encryptedVerification, derivedKey)
        } catch (e: Exception) {
            ""
        }
        return decryptedVerification == "LS_PASS_VALID_VAULT_TOKEN"
    }

    fun lockVault() {
        updateActiveMasterKey(null)
        _authState.value = VaultAuthState.LOCKED
    }

    fun onUserInteraction() {
        lastActivityTimeMs = System.currentTimeMillis()
    }

    fun checkAutoLock(autoLockOption: AutoLockOption) {
        if (_authState.value != VaultAuthState.UNLOCKED) return
        if (autoLockOption == AutoLockOption.NEVER) return
        if (autoLockOption == AutoLockOption.IMMEDIATE) return
        if (autoLockOption.timeoutMs > 0) {
            val elapsed = System.currentTimeMillis() - lastActivityTimeMs
            if (elapsed >= autoLockOption.timeoutMs) {
                lockVault()
            }
        }
    }

    fun onAppBackground(autoLockOption: AutoLockOption) {
        if (_authState.value != VaultAuthState.UNLOCKED) return
        if (autoLockOption == AutoLockOption.IMMEDIATE || autoLockOption == AutoLockOption.ON_BACKGROUND) {
            lockVault()
        } else if (autoLockOption.timeoutMs > 0) {
            val elapsed = System.currentTimeMillis() - lastActivityTimeMs
            if (elapsed >= autoLockOption.timeoutMs) {
                lockVault()
            }
        }
    }

    fun copyToClipboardAndScheduleClear(label: String, secretText: String, clearOption: ClipboardClearOption) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, secretText)
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()

        clipboardClearJob?.cancel()
        if (clearOption.seconds > 0) {
            clipboardClearJob = scope.launch {
                delay(clearOption.seconds * 1000L)
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                Toast.makeText(context, "Clipboard cleared for security", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun setAutoLockOption(option: AutoLockOption) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_LOCK_OPTION] = option.name
        }
    }

    suspend fun setClipboardClearOption(option: ClipboardClearOption) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CLIPBOARD_CLEAR_OPTION] = option.name
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
        if (!enabled) {
            biometricManager.removeHardwareKey()
        } else {
            activeMasterKey?.let { masterKey ->
                try {
                    val encryptedKeyBytes = biometricManager.encryptMasterKey(masterKey)
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey("encrypted_master_key_for_bio")] = encryptedKeyBytes
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
