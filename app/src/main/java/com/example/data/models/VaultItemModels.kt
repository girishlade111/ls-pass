package com.example.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CustomField(
    val name: String = "",
    val value: String = "",
    val fieldType: String = "text" // "text", "hidden", "boolean"
)

@JsonClass(generateAdapter = true)
data class LoginData(
    val username: String = "",
    val password: String = "",
    val totpSecret: String = "",
    val uris: List<String> = emptyList(),
    val notes: String = "",
    val customFields: List<CustomField> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CardData(
    val cardholderName: String = "",
    val cardNumber: String = "",
    val brand: String = "",
    val expMonth: String = "",
    val expYear: String = "",
    val code: String = "",
    val notes: String = "",
    val customFields: List<CustomField> = emptyList()
)

@JsonClass(generateAdapter = true)
data class IdentityData(
    val title: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val username: String = "",
    val company: String = "",
    val ssn: String = "",
    val passportNumber: String = "",
    val licenseNumber: String = "",
    val email: String = "",
    val phone: String = "",
    val address1: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = "",
    val notes: String = "",
    val customFields: List<CustomField> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SecureNoteData(
    val notes: String = "",
    val customFields: List<CustomField> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SshKeyData(
    val keyName: String = "",
    val privateKey: String = "",
    val publicKey: String = "",
    val fingerprint: String = "",
    val notes: String = "",
    val customFields: List<CustomField> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PasskeyData(
    val relyingPartyId: String = "",
    val userHandle: String = "",
    val credentialId: String = "",
    val publicKey: String = "",
    val notes: String = "",
    val customFields: List<CustomField> = emptyList()
)

/**
 * High-level unencrypted representation of a vault item in memory when vault is unlocked.
 */
data class DecryptedVaultItem(
    val id: String,
    val type: ItemType,
    val name: String,
    val folderId: String? = null,
    val collectionIds: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val loginData: LoginData? = null,
    val cardData: CardData? = null,
    val identityData: IdentityData? = null,
    val secureNoteData: SecureNoteData? = null,
    val sshKeyData: SshKeyData? = null,
    val passkeyData: PasskeyData? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getSubtitle(): String {
        return when (type) {
            ItemType.LOGIN -> loginData?.username?.takeIf { it.isNotBlank() } ?: "No username"
            ItemType.CARD -> {
                val num = cardData?.cardNumber ?: ""
                if (num.length >= 4) "•••• ${num.takeLast(4)}" else cardData?.brand?.takeIf { it.isNotBlank() } ?: "Card"
            }
            ItemType.IDENTITY -> identityData?.email?.takeIf { it.isNotBlank() } ?: identityData?.firstName ?: "Identity"
            ItemType.SECURE_NOTE -> secureNoteData?.notes?.take(30) ?: "Secure Note"
            ItemType.SSH_KEY -> sshKeyData?.fingerprint?.take(16) ?: "SSH Key"
            ItemType.PASSKEY -> passkeyData?.relyingPartyId ?: "Passkey"
        }
    }
}

enum class IssueSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Offline Security Audit Model
 */
data class WeakPasswordIssue(
    val itemId: String,
    val itemName: String,
    val username: String,
    val entropy: Double,
    val severity: IssueSeverity = if (entropy < 28.0) IssueSeverity.CRITICAL else IssueSeverity.HIGH
)

data class ReusedPasswordIssue(
    val password: String,
    val affectedItems: List<Pair<String, String>>, // Item ID to Name
    val severity: IssueSeverity = if (affectedItems.size >= 3) IssueSeverity.CRITICAL else IssueSeverity.HIGH
)

data class VaultHealthReport(
    val totalLogins: Int,
    val weakPasswords: List<WeakPasswordIssue>,
    val reusedPasswords: List<ReusedPasswordIssue>,
    val missingTotpCount: Int = 0,
    val healthScore: Int = 100
) {
    val criticalCount: Int
        get() = weakPasswords.count { it.severity == IssueSeverity.CRITICAL } +
                reusedPasswords.count { it.severity == IssueSeverity.CRITICAL }

    val highRiskCount: Int
        get() = weakPasswords.count { it.severity == IssueSeverity.HIGH } +
                reusedPasswords.count { it.severity == IssueSeverity.HIGH }

    val totalIssuesCount: Int
        get() = weakPasswords.size + reusedPasswords.size
}

/**
 * JSON Import / Export Container
 */
@JsonClass(generateAdapter = true)
data class EncryptedExportModel(
    val format: String = "LSPASS_ENCRYPTED_V1",
    val exportDate: Long = System.currentTimeMillis(),
    val saltBase64: String,
    val encryptedVaultJson: String
)

@JsonClass(generateAdapter = true)
data class ExportableVaultPayload(
    val items: List<ExportableItem>,
    val folders: List<ExportableFolder>,
    val collections: List<ExportableCollection>
)

@JsonClass(generateAdapter = true)
data class ExportableItem(
    val id: String,
    val type: String,
    val name: String,
    val folderId: String?,
    val collectionIds: List<String>,
    val isFavorite: Boolean,
    val isHidden: Boolean,
    val contentJson: String
)

@JsonClass(generateAdapter = true)
data class ExportableFolder(
    val id: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class ExportableCollection(
    val id: String,
    val name: String,
    val colorHex: String
)
