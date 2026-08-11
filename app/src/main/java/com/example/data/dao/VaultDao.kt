package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.models.CollectionEntity
import com.example.data.models.FolderEntity
import com.example.data.models.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    // --- Vault Items ---
    @Query("SELECT * FROM vault_items ORDER BY updatedAt DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getVaultItemById(id: String): VaultItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateVaultItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItemById(id: String)

    @Query("DELETE FROM vault_items")
    suspend fun deleteAllVaultItems()

    // --- Folders ---
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    // --- Collections ---
    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollectionById(id: String)
}
