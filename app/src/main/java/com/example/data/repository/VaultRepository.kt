package com.example.data.repository

import com.example.crypto.CryptoManager
import com.example.data.dao.VaultDao
import com.example.data.models.CardData
import com.example.data.models.CollectionEntity
import com.example.data.models.DecryptedVaultItem
import com.example.data.models.FolderEntity
import com.example.data.models.IdentityData
import com.example.data.models.ItemType
import com.example.data.models.LoginData
import com.example.data.models.PasskeyData
import com.example.data.models.SecureNoteData
import com.example.data.models.SshKeyData
import com.example.data.models.VaultItemEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.crypto.SecretKey

class VaultRepository(private val vaultDao: VaultDao) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val loginAdapter = moshi.adapter(LoginData::class.java)
    private val cardAdapter = moshi.adapter(CardData::class.java)
    private val identityAdapter = moshi.adapter(IdentityData::class.java)
    private val noteAdapter = moshi.adapter(SecureNoteData::class.java)
    private val sshAdapter = moshi.adapter(SshKeyData::class.java)
    private val passkeyAdapter = moshi.adapter(PasskeyData::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
    )

    fun getDecryptedItems(masterKey: SecretKey?): Flow<List<DecryptedVaultItem>> {
        return vaultDao.getAllVaultItems().map { entities ->
            entities.mapNotNull { entity ->
                decryptEntity(entity, masterKey)
            }
        }
    }

    suspend fun getItemById(id: String, masterKey: SecretKey?): DecryptedVaultItem? {
        val entity = vaultDao.getVaultItemById(id) ?: return null
        return decryptEntity(entity, masterKey)
    }

    private fun decryptEntity(entity: VaultItemEntity, masterKey: SecretKey?): DecryptedVaultItem? {
        if (masterKey == null) return null
        val type = try { ItemType.valueOf(entity.type) } catch (_: Exception) { ItemType.LOGIN }
        val name = try { CryptoManager.decrypt(entity.encryptedName, masterKey) } catch (_: Exception) { "Encrypted Item" }
        val decryptedContent = try { CryptoManager.decrypt(entity.encryptedContent, masterKey) } catch (_: Exception) { "" }

        val collectionIds = try {
            stringListAdapter.fromJson(entity.collectionIdsJson) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        var loginData: LoginData? = null
        var cardData: CardData? = null
        var identityData: IdentityData? = null
        var noteData: SecureNoteData? = null
        var sshData: SshKeyData? = null
        var passkeyData: PasskeyData? = null

        if (decryptedContent.isNotEmpty()) {
            try {
                when (type) {
                    ItemType.LOGIN -> loginData = loginAdapter.fromJson(decryptedContent)
                    ItemType.CARD -> cardData = cardAdapter.fromJson(decryptedContent)
                    ItemType.IDENTITY -> identityData = identityAdapter.fromJson(decryptedContent)
                    ItemType.SECURE_NOTE -> noteData = noteAdapter.fromJson(decryptedContent)
                    ItemType.SSH_KEY -> sshData = sshAdapter.fromJson(decryptedContent)
                    ItemType.PASSKEY -> passkeyData = passkeyAdapter.fromJson(decryptedContent)
                }
            } catch (_: Exception) {}
        }

        return DecryptedVaultItem(
            id = entity.id,
            type = type,
            name = name,
            folderId = entity.folderId,
            collectionIds = collectionIds,
            isFavorite = entity.isFavorite,
            isHidden = entity.isHidden,
            loginData = loginData,
            cardData = cardData,
            identityData = identityData,
            secureNoteData = noteData,
            sshKeyData = sshData,
            passkeyData = passkeyData,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    suspend fun saveItem(item: DecryptedVaultItem, masterKey: SecretKey) {
        val encryptedName = CryptoManager.encrypt(item.name, masterKey)

        val rawContent = when (item.type) {
            ItemType.LOGIN -> loginAdapter.toJson(item.loginData ?: LoginData())
            ItemType.CARD -> cardAdapter.toJson(item.cardData ?: CardData())
            ItemType.IDENTITY -> identityAdapter.toJson(item.identityData ?: IdentityData())
            ItemType.SECURE_NOTE -> noteAdapter.toJson(item.secureNoteData ?: SecureNoteData())
            ItemType.SSH_KEY -> sshAdapter.toJson(item.sshKeyData ?: SshKeyData())
            ItemType.PASSKEY -> passkeyAdapter.toJson(item.passkeyData ?: PasskeyData())
        }

        val encryptedContent = CryptoManager.encrypt(rawContent, masterKey)
        val collectionsJson = stringListAdapter.toJson(item.collectionIds)

        val entity = VaultItemEntity(
            id = item.id,
            type = item.type.name,
            encryptedName = encryptedName,
            folderId = item.folderId,
            collectionIdsJson = collectionsJson,
            isFavorite = item.isFavorite,
            isHidden = item.isHidden,
            encryptedContent = encryptedContent,
            createdAt = item.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        vaultDao.insertOrUpdateVaultItem(entity)
    }

    suspend fun deleteItem(id: String) {
        vaultDao.deleteVaultItemById(id)
    }

    suspend fun deleteAllItems() {
        vaultDao.deleteAllVaultItems()
    }

    // --- Folders & Collections ---
    fun getAllFolders(): Flow<List<FolderEntity>> = vaultDao.getAllFolders()
    suspend fun addFolder(folder: FolderEntity) = vaultDao.insertFolder(folder)
    suspend fun deleteFolder(id: String) = vaultDao.deleteFolderById(id)

    fun getAllCollections(): Flow<List<CollectionEntity>> = vaultDao.getAllCollections()
    suspend fun addCollection(collection: CollectionEntity) = vaultDao.insertCollection(collection)
    suspend fun deleteCollection(id: String) = vaultDao.deleteCollectionById(id)
}
