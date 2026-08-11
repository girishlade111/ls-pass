package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val type: String, // LOGIN, CARD, IDENTITY, SECURE_NOTE, SSH_KEY, PASSKEY
    val encryptedName: String,
    val folderId: String? = null,
    val collectionIdsJson: String = "[]",
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val encryptedContent: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
