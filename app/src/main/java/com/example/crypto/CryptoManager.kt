package com.example.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Interface defining vault encryption/decryption contract for sensitive data.
 */
interface VaultCryptoInterface {
    fun encrypt(plainText: String, secretKey: SecretKey): String
    fun decrypt(encryptedBase64: String, secretKey: SecretKey): String
    fun encryptBytes(data: ByteArray, secretKey: SecretKey): ByteArray
    fun decryptBytes(encryptedData: ByteArray, secretKey: SecretKey): ByteArray
}

/**
 * Secure crypto module providing AES-256-GCM encryption and Argon2id key derivation
 * for vault data security.
 */
object CryptoManager : VaultCryptoInterface {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val BIOMETRIC_KEY_ALIAS = "ls_pass_biometric_key"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    // Default Argon2id parameters as per security architecture specifications
    const val ARGON2_ITERATIONS = 3
    const val ARGON2_MEMORY_KB = 65536 // 64 MB
    const val ARGON2_PARALLELISM = 4

    private val secureRandom = SecureRandom()

    fun generateSalt(size: Int = 16): ByteArray {
        val salt = ByteArray(size)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit AES SecretKey from password and salt using Argon2id.
     */
    fun deriveKeyArgon2id(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = ARGON2_ITERATIONS,
        memoryKb: Int = ARGON2_MEMORY_KB,
        parallelism: Int = ARGON2_PARALLELISM,
        keyLengthBits: Int = KEY_LENGTH_BITS
    ): SecretKey {
        val outputLengthBytes = keyLengthBits / 8
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(iterations)
            .withMemoryAsKB(memoryKb)
            .withParallelism(parallelism)
            .withSalt(salt)

        val generator = Argon2BytesGenerator()
        generator.init(builder.build())

        val passwordBytes = String(password).toByteArray(Charsets.UTF_8)
        val resultKeyBytes = ByteArray(outputLengthBytes)
        generator.generateBytes(passwordBytes, resultKeyBytes, 0, resultKeyBytes.size)

        return SecretKeySpec(resultKeyBytes, "AES")
    }

    /**
     * Primary key derivation function using Argon2id.
     */
    fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        return deriveKeyArgon2id(password, salt)
    }

    /**
     * Legacy/Fallback PBKDF2 Key Derivation function.
     */
    fun deriveKeyPbkdf2(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext string using AES-256-GCM with a random 12-byte IV.
     * Returns Base64-encoded string: IV (12 bytes) + Ciphertext + GCM Tag (16 bytes).
     */
    override fun encrypt(plainText: String, secretKey: SecretKey): String {
        if (plainText.isEmpty()) return ""
        val encryptedBytes = encryptBytes(plainText.toByteArray(Charsets.UTF_8), secretKey)
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    /**
     * Decrypts Base64-encoded string (IV + Ciphertext + GCM Tag) using AES-256-GCM.
     */
    override fun decrypt(encryptedBase64: String, secretKey: SecretKey): String {
        if (encryptedBase64.isEmpty()) return ""
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val decryptedBytes = decryptBytes(combined, secretKey)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Encrypts raw bytes using AES-256-GCM with a random 12-byte IV.
     */
    override fun encryptBytes(data: ByteArray, secretKey: SecretKey): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherText = cipher.doFinal(data)
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return combined
    }

    /**
     * Decrypts combined IV + Ciphertext bytes using AES-256-GCM.
     */
    override fun decryptBytes(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
        if (encryptedData.size < GCM_IV_LENGTH) return ByteArray(0)

        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val cipherText = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(cipherText)
    }

    // --- Android KeyStore for Biometric Protected Vault Key Backup ---

    fun getOrCreateBiometricMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    fun removeBiometricMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
            }
        } catch (_: Exception) {}
    }

    // --- Generator Helpers ---

    private val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val NUMBERS = "0123456789"
    private val SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    private val AMBIGUOUS = "iI1lLo0O8"

    private val WORD_LIST = listOf(
        "anchor", "beacon", "castle", "dragon", "echo", "falcon", "glacier", "harbor",
        "island", "jungle", "kingdom", "lantern", "mountain", "nebula", "ocean", "phoenix",
        "quartz", "river", "summit", "thunder", "umbrella", "valley", "wisdom", "xenon",
        "yacht", "zephyr", "amber", "breeze", "crystal", "delta", "ember", "forest",
        "granite", "horizon", "infinity", "javelin", "knight", "lunar", "meadow", "oasis",
        "polaris", "quantum", "radiance", "solstice", "titan", "universe", "vortex", "willow"
    )

    fun generatePassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeNumbers: Boolean = true,
        includeSpecial: Boolean = true,
        minNumbers: Int = 1,
        minSpecial: Int = 1,
        avoidAmbiguous: Boolean = false
    ): String {
        var lowerChars = LOWERCASE
        var upperChars = UPPERCASE
        var numChars = NUMBERS
        var specChars = SPECIAL

        if (avoidAmbiguous) {
            lowerChars = lowerChars.filterNot { AMBIGUOUS.contains(it) }
            upperChars = upperChars.filterNot { AMBIGUOUS.contains(it) }
            numChars = numChars.filterNot { AMBIGUOUS.contains(it) }
            specChars = specChars.filterNot { AMBIGUOUS.contains(it) }
        }

        val poolBuilder = StringBuilder()
        if (includeLower) poolBuilder.append(lowerChars)
        if (includeUpper) poolBuilder.append(upperChars)
        if (includeNumbers) poolBuilder.append(numChars)
        if (includeSpecial) poolBuilder.append(specChars)

        val pool = poolBuilder.toString()
        if (pool.isEmpty()) return ""

        val resultChars = mutableListOf<Char>()

        // Guarantee min numbers
        if (includeNumbers && numChars.isNotEmpty()) {
            for (i in 0 until minNumbers.coerceAtMost(length / 2)) {
                resultChars.add(numChars[secureRandom.nextInt(numChars.length)])
            }
        }

        // Guarantee min special
        if (includeSpecial && specChars.isNotEmpty()) {
            for (i in 0 until minSpecial.coerceAtMost(length / 2)) {
                resultChars.add(specChars[secureRandom.nextInt(specChars.length)])
            }
        }

        // Fill remaining
        while (resultChars.size < length) {
            resultChars.add(pool[secureRandom.nextInt(pool.length)])
        }

        resultChars.shuffle(secureRandom)
        return resultChars.joinToString("")
    }

    fun generatePassphrase(
        wordCount: Int = 4,
        separator: String = "-",
        capitalize: Boolean = true,
        includeNumber: Boolean = true
    ): String {
        val chosenWords = mutableListOf<String>()
        for (i in 0 until wordCount) {
            var word = WORD_LIST[secureRandom.nextInt(WORD_LIST.size)]
            if (capitalize) {
                word = word.replaceFirstChar { it.uppercase() }
            }
            chosenWords.add(word)
        }

        if (includeNumber) {
            val randomIndex = secureRandom.nextInt(chosenWords.size)
            chosenWords[randomIndex] = chosenWords[randomIndex] + secureRandom.nextInt(10)
        }

        return chosenWords.joinToString(separator)
    }

    fun calculateEntropy(password: String): Double {
        if (password.isEmpty()) return 0.0
        var poolSize = 0
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32
        if (poolSize == 0) poolSize = 26

        return password.length * (Math.log(poolSize.toDouble()) / Math.log(2.0))
    }
}
