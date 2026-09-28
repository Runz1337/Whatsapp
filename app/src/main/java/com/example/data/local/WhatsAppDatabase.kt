package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.models.AccessibilityLogEntity
import com.example.data.models.CallLogEntity
import com.example.data.models.ChannelEntity
import com.example.data.models.ChatEntity
import com.example.data.models.ContactEntity
import com.example.data.models.MessageEntity
import com.example.data.models.StatusEntity

@Database(
    entities = [
        ContactEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        StatusEntity::class,
        CallLogEntity::class,
        ChannelEntity::class,
        AccessibilityLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WhatsAppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun statusDao(): StatusDao
    abstract fun callDao(): CallDao
    abstract fun channelDao(): ChannelDao
    abstract fun contactDao(): ContactDao
    abstract fun accessibilityLogDao(): AccessibilityLogDao

    companion object {
        @Volatile
        private var INSTANCE: WhatsAppDatabase? = null

        fun getInstance(context: Context): WhatsAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhatsAppDatabase::class.java,
                    "whatsapp_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
