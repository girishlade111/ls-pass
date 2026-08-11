package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoManager
import com.example.data.models.CollectionEntity
import com.example.data.models.DecryptedVaultItem
import com.example.data.models.EncryptedExportModel
import com.example.data.models.ExportableCollection
import com.example.data.models.ExportableFolder
import com.example.data.models.ExportableItem
import com.example.data.models.ExportableVaultPayload
import com.example.data.models.FolderEntity
import com.example.data.models.IssueSeverity
import com.example.data.models.ItemType
import com.example.data.models.ReusedPasswordIssue
import com.example.data.models.VaultHealthReport
import com.example.data.models.WeakPasswordIssue
import com.example.data.repository.VaultRepository
import com.example.session.VaultSessionManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModel(
    private val repository: VaultRepository,
    private val sessionManager: VaultSessionManager
) : ViewModel() {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _selectedCollectionId = MutableStateFlow<String?>(null)
    val selectedCollectionId: StateFlow<String?> = _selectedCollectionId.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<ItemType?>(null)
    val selectedTypeFilter: StateFlow<ItemType?> = _selectedTypeFilter.asStateFlow()

    private val _showHiddenOnly = MutableStateFlow(false)
    val showHiddenOnly: StateFlow<Boolean> = _showHiddenOnly.asStateFlow()

    private val _isHiddenUnlocked = MutableStateFlow(false)
    val isHiddenUnlocked: StateFlow<Boolean> = _isHiddenUnlocked.asStateFlow()

    val folders: StateFlow<List<FolderEntity>> = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<CollectionEntity>> = repository.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recentlyAccessedIds = MutableStateFlow<List<String>>(emptyList())
    val recentlyAccessedIds: StateFlow<List<String>> = _recentlyAccessedIds.asStateFlow()

    private val allDecryptedItems = sessionManager.activeMasterKeyFlow.flatMapLatest { masterKey ->
        if (masterKey == null) {
            flowOf(emptyList())
        } else {
            repository.getDecryptedItems(masterKey)
        }
    }

    val recentlyAccessedItems: StateFlow<List<DecryptedVaultItem>> = combine(
        allDecryptedItems,
        _recentlyAccessedIds
    ) { items, recentIds ->
        if (items.isEmpty()) return@combine emptyList()

        val itemMap = items.associateBy { it.id }
        val explicitRecents = recentIds.mapNotNull { itemMap[it] }

        // Supplement with items sorted by updatedAt (most recently edited or created)
        val recentlyUpdated = items.sortedByDescending { it.updatedAt }

        (explicitRecents + recentlyUpdated)
            .distinctBy { it.id }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredItems: StateFlow<List<DecryptedVaultItem>> = combine(
        allDecryptedItems,
        _searchQuery,
        _selectedFolderId,
        _selectedCollectionId,
        _selectedTypeFilter,
        _showHiddenOnly
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val items = flows[0] as List<DecryptedVaultItem>
        val query = flows[1] as String
        val folderId = flows[2] as String?
        val collectionId = flows[3] as String?
        val typeFilter = flows[4] as ItemType?
        val showHidden = flows[5] as Boolean

        items.filter { item ->
            // Type filter
            if (typeFilter != null && item.type != typeFilter) {
                return@filter false
            }

            // Hidden filter
            if (showHidden) {
                if (!item.isHidden) return@filter false
            } else {
                if (item.isHidden) return@filter false
            }

            // Folder filter
            if (folderId != null && item.folderId != folderId) {
                return@filter false
            }

            // Collection filter
            if (collectionId != null && !item.collectionIds.contains(collectionId)) {
                return@filter false
            }

            // Search query across all categories (name, username, URI)
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                val nameMatch = item.name.lowercase().contains(q)

                val usernameMatch = (item.loginData?.username?.lowercase()?.contains(q) == true) ||
                        (item.identityData?.email?.lowercase()?.contains(q) == true) ||
                        (item.passkeyData?.userHandle?.lowercase()?.contains(q) == true) ||
                        (item.cardData?.cardholderName?.lowercase()?.contains(q) == true)

                val uriMatch = (item.loginData?.uris?.any { it.lowercase().contains(q) } == true) ||
                        (item.passkeyData?.relyingPartyId?.lowercase()?.contains(q) == true)

                val noteMatch = (item.secureNoteData?.notes?.lowercase()?.contains(q) == true) ||
                        (item.sshKeyData?.keyName?.lowercase()?.contains(q) == true)

                if (!nameMatch && !usernameMatch && !uriMatch && !noteMatch) return@filter false
            }

            true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFolderFilter(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun setCollectionFilter(collectionId: String?) {
        _selectedCollectionId.value = collectionId
    }

    fun setTypeFilter(type: ItemType?) {
        _selectedTypeFilter.value = type
    }

    fun setShowHiddenOnly(showHidden: Boolean) {
        _showHiddenOnly.value = showHidden
    }

    fun unlockHiddenFolder() {
        _isHiddenUnlocked.value = true
        _showHiddenOnly.value = true
    }

    fun lockHiddenFolder() {
        _isHiddenUnlocked.value = false
        _showHiddenOnly.value = false
    }

    suspend fun verifyMasterPassword(password: String): Boolean =
        sessionManager.verifyMasterPassword(password)

    suspend fun verifyPinPasscode(pin: String): Boolean =
        sessionManager.verifyPinPasscode(pin)

    suspend fun setPinPasscode(pin: String) =
        sessionManager.setPinPasscode(pin)

    fun toggleItemHiddenState(item: DecryptedVaultItem) {
        viewModelScope.launch {
            val key = sessionManager.getActiveMasterKey() ?: return@launch
            val updated = item.copy(isHidden = !item.isHidden)
            repository.saveItem(updated, key)
        }
    }

    fun recordItemAccess(itemId: String) {
        if (itemId.isBlank()) return
        val list = _recentlyAccessedIds.value.toMutableList()
        list.remove(itemId)
        list.add(0, itemId)
        _recentlyAccessedIds.value = list.take(10)
    }

    fun saveItem(item: DecryptedVaultItem) {
        viewModelScope.launch {
            val key = sessionManager.getActiveMasterKey() ?: return@launch
            repository.saveItem(item, key)
            recordItemAccess(item.id)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addFolder(FolderEntity(id = UUID.randomUUID().toString(), name = name))
        }
    }

    fun createCollection(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCollection(
                CollectionEntity(id = UUID.randomUUID().toString(), name = name, colorHex = colorHex)
            )
        }
    }

    // --- Offline Vault Health Audit ---
    suspend fun generateHealthReport(): VaultHealthReport {
        val masterKey = sessionManager.getActiveMasterKey()
        val items = repository.getDecryptedItems(masterKey).first()
        val loginItems = items.filter { it.type == ItemType.LOGIN }

        val weakList = mutableListOf<WeakPasswordIssue>()
        val passwordToItemsMap = mutableMapOf<String, MutableList<Pair<String, String>>>()
        var missingTotp = 0

        loginItems.forEach { item ->
            val pass = item.loginData?.password ?: ""
            val user = item.loginData?.username ?: ""

            if (pass.isNotEmpty()) {
                val entropy = CryptoManager.calculateEntropy(pass)
                if (entropy < 45.0) {
                    val severity = if (entropy < 28.0 || pass.length < 8) IssueSeverity.CRITICAL else IssueSeverity.HIGH
                    weakList.add(
                        WeakPasswordIssue(
                            itemId = item.id,
                            itemName = item.name,
                            username = user,
                            entropy = entropy,
                            severity = severity
                        )
                    )
                }

                passwordToItemsMap.getOrPut(pass) { mutableListOf() }.add(Pair(item.id, item.name))
            } else {
                weakList.add(
                    WeakPasswordIssue(
                        itemId = item.id,
                        itemName = item.name,
                        username = user,
                        entropy = 0.0,
                        severity = IssueSeverity.CRITICAL
                    )
                )
            }

            if (item.loginData?.totpSecret.isNull_Blank()) {
                missingTotp++
            }
        }

        val reusedList = passwordToItemsMap
            .filter { it.value.size > 1 }
            .map { (pass, affected) ->
                val severity = if (affected.size >= 3) IssueSeverity.CRITICAL else IssueSeverity.HIGH
                ReusedPasswordIssue(
                    password = pass,
                    affectedItems = affected,
                    severity = severity
                )
            }

        // Calculate 0-100 score
        var score = 100
        if (loginItems.isNotEmpty()) {
            val criticalPenalty = (weakList.count { it.severity == IssueSeverity.CRITICAL } + reusedList.count { it.severity == IssueSeverity.CRITICAL }) * 20
            val highPenalty = (weakList.count { it.severity == IssueSeverity.HIGH } + reusedList.count { it.severity == IssueSeverity.HIGH }) * 12
            score = (100 - criticalPenalty - highPenalty).coerceIn(0, 100)
        }

        return VaultHealthReport(
            totalLogins = loginItems.size,
            weakPasswords = weakList,
            reusedPasswords = reusedList,
            missingTotpCount = missingTotp,
            healthScore = score
        )
    }

    // --- Encrypted Export ---
    suspend fun exportEncryptedVault(exportPassword: String): String {
        val masterKey = sessionManager.getActiveMasterKey() ?: return ""
        val items = repository.getDecryptedItems(masterKey).first()
        val folderList = repository.getAllFolders().first()
        val collectionList = repository.getAllCollections().first()

        val exportItems = items.map { item ->
            ExportableItem(
                id = item.id,
                type = item.type.name,
                name = item.name,
                folderId = item.folderId,
                collectionIds = item.collectionIds,
                isFavorite = item.isFavorite,
                isHidden = item.isHidden,
                contentJson = when (item.type) {
                    ItemType.LOGIN -> moshi.adapter(com.example.data.models.LoginData::class.java).toJson(item.loginData)
                    ItemType.CARD -> moshi.adapter(com.example.data.models.CardData::class.java).toJson(item.cardData)
                    ItemType.IDENTITY -> moshi.adapter(com.example.data.models.IdentityData::class.java).toJson(item.identityData)
                    ItemType.SECURE_NOTE -> moshi.adapter(com.example.data.models.SecureNoteData::class.java).toJson(item.secureNoteData)
                    ItemType.SSH_KEY -> moshi.adapter(com.example.data.models.SshKeyData::class.java).toJson(item.sshKeyData)
                    ItemType.PASSKEY -> moshi.adapter(com.example.data.models.PasskeyData::class.java).toJson(item.passkeyData)
                }
            )
        }

        val exportFolders = folderList.map { ExportableFolder(id = it.id, name = it.name) }
        val exportCollections = collectionList.map { ExportableCollection(id = it.id, name = it.name, colorHex = it.colorHex) }

        val payload = ExportableVaultPayload(
            items = exportItems,
            folders = exportFolders,
            collections = exportCollections
        )

        val payloadAdapter = moshi.adapter(ExportableVaultPayload::class.java)
        val rawPayloadJson = payloadAdapter.toJson(payload)

        val salt = CryptoManager.generateSalt()
        val exportKey = CryptoManager.deriveKey(exportPassword.toCharArray(), salt)

        val encryptedPayload = CryptoManager.encrypt(rawPayloadJson, exportKey)

        val exportContainer = EncryptedExportModel(
            saltBase64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP),
            encryptedVaultJson = encryptedPayload
        )

        return moshi.adapter(EncryptedExportModel::class.java).toJson(exportContainer)
    }

    // --- Import Encrypted Vault or Bitwarden JSON ---
    suspend fun importVaultJson(jsonString: String, passwordIfEncrypted: String?): Boolean {
        val masterKey = sessionManager.getActiveMasterKey() ?: return false
        return try {
            if (jsonString.contains("LSPASS_ENCRYPTED_V1")) {
                val container = moshi.adapter(EncryptedExportModel::class.java).fromJson(jsonString) ?: return false
                val salt = android.util.Base64.decode(container.saltBase64, android.util.Base64.NO_WRAP)
                val importKey = CryptoManager.deriveKey((passwordIfEncrypted ?: "").toCharArray(), salt)
                val decryptedPayloadJson = CryptoManager.decrypt(container.encryptedVaultJson, importKey)

                val payload = moshi.adapter(ExportableVaultPayload::class.java).fromJson(decryptedPayloadJson) ?: return false

                payload.folders.forEach { f ->
                    repository.addFolder(FolderEntity(id = f.id, name = f.name))
                }
                payload.collections.forEach { c ->
                    repository.addCollection(CollectionEntity(id = c.id, name = c.name, colorHex = c.colorHex))
                }

                payload.items.forEach { e ->
                    val type = try { ItemType.valueOf(e.type) } catch (_: Exception) { ItemType.LOGIN }
                    val item = DecryptedVaultItem(
                        id = e.id,
                        type = type,
                        name = e.name,
                        folderId = e.folderId,
                        collectionIds = e.collectionIds,
                        isFavorite = e.isFavorite,
                        isHidden = e.isHidden,
                        loginData = if (type == ItemType.LOGIN) moshi.adapter(com.example.data.models.LoginData::class.java).fromJson(e.contentJson) else null,
                        cardData = if (type == ItemType.CARD) moshi.adapter(com.example.data.models.CardData::class.java).fromJson(e.contentJson) else null,
                        identityData = if (type == ItemType.IDENTITY) moshi.adapter(com.example.data.models.IdentityData::class.java).fromJson(e.contentJson) else null,
                        secureNoteData = if (type == ItemType.SECURE_NOTE) moshi.adapter(com.example.data.models.SecureNoteData::class.java).fromJson(e.contentJson) else null,
                        sshKeyData = if (type == ItemType.SSH_KEY) moshi.adapter(com.example.data.models.SshKeyData::class.java).fromJson(e.contentJson) else null,
                        passkeyData = if (type == ItemType.PASSKEY) moshi.adapter(com.example.data.models.PasskeyData::class.java).fromJson(e.contentJson) else null
                    )
                    repository.saveItem(item, masterKey)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importBitwardenCsv(csvText: String): com.example.data.importer.BitwardenImportResult {
        val masterKey = sessionManager.getActiveMasterKey()
            ?: return com.example.data.importer.BitwardenImportResult(success = false, errorMessage = "Vault is locked.")
        val existingFoldersList = repository.getAllFolders().first()
        return com.example.data.importer.BitwardenCsvImporter.importCsv(
            csvText = csvText,
            repository = repository,
            masterKey = masterKey,
            existingFolders = existingFoldersList
        )
    }

    private fun String?.isNull_Blank(): Boolean = this.isNullOrBlank()
}

class VaultViewModelFactory(
    private val repository: VaultRepository,
    private val sessionManager: VaultSessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VaultViewModel(repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
