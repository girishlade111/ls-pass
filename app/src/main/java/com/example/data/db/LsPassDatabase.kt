package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.VaultDao
import com.example.data.models.CollectionEntity
import com.example.data.models.FolderEntity
import com.example.data.models.VaultItemEntity

@Database(
    entities = [
        VaultItemEntity::class,
        FolderEntity::class,
        CollectionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LsPassDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: LsPassDatabase? = null

        fun getInstance(context: Context): LsPassDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LsPassDatabase::class.java,
                    "ls_pass_vault.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
