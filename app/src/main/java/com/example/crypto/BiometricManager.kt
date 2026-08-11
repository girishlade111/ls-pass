package com.example.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNSUPPORTED
}

/**
 * BiometricManager class integrating with Android BiometricPrompt to handle
 * hardware-backed authentication and unlock the Master Key from Android Keystore.
 */
class BiometricManager(private val context: Context) {

    companion object {
        const val BIOMETRIC_KEY_ALIAS = "ls_pass_biometric_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        @Volatile
        private var INSTANCE: BiometricManager? = null

        fun getInstance(context: Context): BiometricManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BiometricManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val androidBiometricManager = AndroidBiometricManager.from(context)

    /**
     * Checks if biometric hardware authentication is available and enrolled.
     */
    fun canAuthenticate(): Boolean {
        val authenticators = AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
                AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK
        return androidBiometricManager.canAuthenticate(authenticators) == AndroidBiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Returns detailed hardware/enrollment status of biometrics on the device.
     */
    fun getBiometricStatus(): BiometricStatus {
        val authenticators = AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
                AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (androidBiometricManager.canAuthenticate(authenticators)) {
            AndroidBiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            AndroidBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    /**
     * Retrieves or generates a hardware-backed SecretKey stored in Android KeyStore.
     */
    fun getOrCreateHardwareKey(alias: String = BIOMETRIC_KEY_ALIAS): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(alias, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Deletes the hardware-backed key from Android KeyStore.
     */
    fun removeHardwareKey(alias: String = BIOMETRIC_KEY_ALIAS) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (_: Exception) {}
    }

    /**
     * Encrypts the Master Key bytes using the hardware-backed key from Keystore.
     */
    fun encryptMasterKey(masterKey: SecretKey): String {
        val hardwareKey = getOrCreateHardwareKey()
        val masterKeyBase64 = Base64.encodeToString(masterKey.encoded, Base64.NO_WRAP)
        return CryptoManager.encrypt(masterKeyBase64, hardwareKey)
    }

    /**
     * Decrypts and restores the Master Key using the hardware-backed key from Keystore.
     */
    fun decryptMasterKey(encryptedMasterKeyBase64: String): SecretKey? {
        if (encryptedMasterKeyBase64.isBlank()) return null
        return try {
            val hardwareKey = getOrCreateHardwareKey()
            val decryptedBase64 = CryptoManager.decrypt(encryptedMasterKeyBase64, hardwareKey)
            if (decryptedBase64.isEmpty()) return null
            val rawKeyBytes = Base64.decode(decryptedBase64, Base64.NO_WRAP)
            SecretKeySpec(rawKeyBytes, "AES")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Triggers BiometricPrompt authentication.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "LS Pass Biometric Unlock",
        subtitle: String = "Authenticate to access your encrypted vault",
        negativeButtonText: String = "Use Master Password",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
