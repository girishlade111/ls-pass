package com.example.data.importer

import com.example.data.models.CardData
import com.example.data.models.CustomField
import com.example.data.models.DecryptedVaultItem
import com.example.data.models.FolderEntity
import com.example.data.models.IdentityData
import com.example.data.models.ItemType
import com.example.data.models.LoginData
import com.example.data.models.SecureNoteData
import com.example.data.repository.VaultRepository
import java.util.UUID
import javax.crypto.SecretKey

data class BitwardenImportResult(
    val success: Boolean,
    val importedCount: Int = 0,
    val createdFoldersCount: Int = 0,
    val errorMessage: String? = null
)

object BitwardenCsvImporter {

    /**
     * Parses a Bitwarden unencrypted CSV export and saves all items and folders to Room Database via VaultRepository.
     */
    suspend fun importCsv(
        csvText: String,
        repository: VaultRepository,
        masterKey: SecretKey,
        existingFolders: List<FolderEntity>
    ): BitwardenImportResult {
        val cleanCsv = csvText.removePrefix("\uFEFF").trim()
        if (cleanCsv.isBlank()) {
            return BitwardenImportResult(success = false, errorMessage = "CSV content is empty.")
        }

        val rows = parseCsvRows(cleanCsv)
        if (rows.isEmpty()) {
            return BitwardenImportResult(success = false, errorMessage = "No rows found in CSV.")
        }

        val headerRow = rows[0]
        val headerMap = headerRow.mapIndexed { index, name ->
            name.trim().lowercase().removePrefix("\"").removeSuffix("\"") to index
        }.toMap()

        // Verify that this resembles a Bitwarden CSV export
        val isBitwardenHeader = headerMap.containsKey("folder") ||
                headerMap.containsKey("type") ||
                headerMap.containsKey("name") ||
                headerMap.containsKey("login_username") ||
                headerMap.containsKey("login_password")

        if (!isBitwardenHeader && rows.size == 1) {
            return BitwardenImportResult(success = false, errorMessage = "Unrecognized CSV header format.")
        }

        val folderMap = existingFolders.associateBy { it.name.trim().lowercase() }.toMutableMap()
        var createdFoldersCount = 0
        var importedCount = 0

        val dataRows = if (headerMap.containsKey("type") || headerMap.containsKey("name") || headerMap.containsKey("login_username")) {
            rows.drop(1)
        } else {
            rows
        }

        for (row in dataRows) {
            if (row.all { it.isBlank() }) continue

            fun getCol(key: String): String {
                val idx = headerMap[key] ?: return ""
                return row.getOrNull(idx)?.trim() ?: ""
            }

            fun getColAny(vararg keys: String): String {
                for (k in keys) {
                    val valStr = getCol(k)
                    if (valStr.isNotBlank()) return valStr
                }
                return ""
            }

            val folderName = getCol("folder")
            var folderId: String? = null

            if (folderName.isNotBlank()) {
                val folderKey = folderName.trim().lowercase()
                val existingFolder = folderMap[folderKey]
                if (existingFolder != null) {
                    folderId = existingFolder.id
                } else {
                    val newId = UUID.randomUUID().toString()
                    val newFolder = FolderEntity(id = newId, name = folderName.trim())
                    repository.addFolder(newFolder)
                    folderMap[folderKey] = newFolder
                    createdFoldersCount++
                    folderId = newId
                }
            }

            val favoriteStr = getCol("favorite")
            val isFavorite = favoriteStr == "1" || favoriteStr.equals("true", ignoreCase = true)

            val rawType = getCol("type").lowercase()
            val name = getCol("name").ifBlank { getCol("login_uri").ifBlank { "Imported Item" } }
            val notes = getCol("notes")
            val fieldsStr = getCol("fields")

            val loginUri = getColAny("login_uri", "uri")
            val loginUsername = getColAny("login_username", "username")
            val loginPassword = getColAny("login_password", "password")
            val loginTotp = getColAny("login_totp", "totp")

            val customFields = parseCustomFields(fieldsStr)

            val itemType = determineItemType(rawType, loginUsername, loginPassword, loginUri, getCol("card_number"))

            var loginData: LoginData? = null
            var secureNoteData: SecureNoteData? = null
            var cardData: CardData? = null
            var identityData: IdentityData? = null

            when (itemType) {
                ItemType.LOGIN -> {
                    val uris = if (loginUri.isNotBlank()) listOf(loginUri) else emptyList()
                    loginData = LoginData(
                        username = loginUsername,
                        password = loginPassword,
                        totpSecret = loginTotp,
                        uris = uris,
                        notes = notes,
                        customFields = customFields
                    )
                }
                ItemType.SECURE_NOTE -> {
                    secureNoteData = SecureNoteData(
                        notes = notes,
                        customFields = customFields
                    )
                }
                ItemType.CARD -> {
                    cardData = CardData(
                        cardholderName = getColAny("card_cardholder", "cardholder"),
                        brand = getColAny("card_brand", "brand"),
                        cardNumber = getColAny("card_number", "card_num"),
                        expMonth = getColAny("card_expmonth", "exp_month"),
                        expYear = getColAny("card_expyear", "exp_year"),
                        code = getColAny("card_code", "security_code"),
                        notes = notes,
                        customFields = customFields
                    )
                }
                ItemType.IDENTITY -> {
                    identityData = IdentityData(
                        title = getCol("identity_title"),
                        firstName = getCol("identity_first"),
                        middleName = getCol("identity_middle"),
                        lastName = getCol("identity_last"),
                        username = loginUsername,
                        company = getCol("identity_company"),
                        ssn = getCol("identity_ssn"),
                        passportNumber = getCol("identity_passport"),
                        licenseNumber = getCol("identity_license"),
                        email = getCol("identity_email").ifBlank { loginUsername },
                        phone = getCol("identity_phone"),
                        address1 = getCol("identity_address1"),
                        city = getCol("identity_city"),
                        state = getCol("identity_state"),
                        postalCode = getCol("identity_postalcode"),
                        country = getCol("identity_country"),
                        notes = notes,
                        customFields = customFields
                    )
                }
                else -> {
                    loginData = LoginData(
                        username = loginUsername,
                        password = loginPassword,
                        totpSecret = loginTotp,
                        uris = if (loginUri.isNotBlank()) listOf(loginUri) else emptyList(),
                        notes = notes,
                        customFields = customFields
                    )
                }
            }

            val newItem = DecryptedVaultItem(
                id = UUID.randomUUID().toString(),
                type = itemType,
                name = name,
                folderId = folderId,
                collectionIds = emptyList(),
                isFavorite = isFavorite,
                isHidden = false,
                loginData = loginData,
                cardData = cardData,
                identityData = identityData,
                secureNoteData = secureNoteData,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Save item into Room Database (applies masterKey encryption internally)
            repository.saveItem(newItem, masterKey)
            importedCount++
        }

        return BitwardenImportResult(
            success = true,
            importedCount = importedCount,
            createdFoldersCount = createdFoldersCount
        )
    }

    private fun determineItemType(rawType: String, username: String, password: String, uri: String, cardNumber: String): ItemType {
        return when {
            rawType == "login" || rawType == "1" -> ItemType.LOGIN
            rawType == "securenote" || rawType == "note" || rawType == "2" -> ItemType.SECURE_NOTE
            rawType == "card" || rawType == "3" -> ItemType.CARD
            rawType == "identity" || rawType == "4" -> ItemType.IDENTITY
            cardNumber.isNotBlank() -> ItemType.CARD
            username.isNotBlank() || password.isNotBlank() || uri.isNotBlank() -> ItemType.LOGIN
            else -> ItemType.LOGIN
        }
    }

    private fun parseCustomFields(fieldsStr: String): List<CustomField> {
        if (fieldsStr.isBlank()) return emptyList()
        val fields = mutableListOf<CustomField>()
        fieldsStr.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                val colonIndex = trimmed.indexOf(':')
                val equalsIndex = trimmed.indexOf('=')
                val splitIndex = if (colonIndex != -1) colonIndex else equalsIndex
                if (splitIndex != -1) {
                    val key = trimmed.substring(0, splitIndex).trim()
                    val value = trimmed.substring(splitIndex + 1).trim()
                    if (key.isNotEmpty()) {
                        fields.add(CustomField(name = key, value = value, fieldType = "text"))
                    }
                } else {
                    fields.add(CustomField(name = "Field", value = trimmed, fieldType = "text"))
                }
            }
        }
        return fields
    }

    private fun parseCsvRows(csvText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < csvText.length) {
            val c = csvText[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csvText.length && csvText[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                    }
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < csvText.length && csvText[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(ArrayList(currentRow))
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(ArrayList(currentRow))
                        currentRow.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }

        return rows
    }
}
