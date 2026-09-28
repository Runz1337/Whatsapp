package com.familyguardian.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.familyguardian.app.data.models.CapturedContactEntity
import com.familyguardian.app.data.models.CapturedMessageEntity
import com.familyguardian.app.data.models.DeviceIdentityEntity
import com.familyguardian.app.data.models.MirroredChatEntity
import com.familyguardian.app.data.models.MirroredMessageEntity
import com.familyguardian.app.data.models.SupervisedEventEntity
import com.familyguardian.app.data.models.SupervisionLinkEntity

/**
 * Local store for observed WhatsApp activity and the supervision mirror.
 *
 * The previous demo-clone tables (chats, messages, statuses, calls, channels,
 * contacts, accessibility logs) were dropped in v5. There is no local send path,
 * so this database only ever holds what was actually observed.
 */
@Database(
    entities = [
        DeviceIdentityEntity::class,
        SupervisionLinkEntity::class,
        SupervisedEventEntity::class,
        MirroredChatEntity::class,
        MirroredMessageEntity::class,
        CapturedContactEntity::class,
        CapturedMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class WhatsAppDatabase : RoomDatabase() {
    abstract fun deviceIdentityDao(): DeviceIdentityDao
    abstract fun supervisionLinkDao(): SupervisionLinkDao
    abstract fun supervisedEventDao(): SupervisedEventDao
    abstract fun mirroredChatDao(): MirroredChatDao
    abstract fun mirroredMessageDao(): MirroredMessageDao
    abstract fun capturedContactDao(): CapturedContactDao
    abstract fun capturedMessageDao(): CapturedMessageDao

    companion object {
        const val DATABASE_NAME = "family_guardian_db"

        @Volatile
        private var INSTANCE: WhatsAppDatabase? = null

        fun getInstance(context: Context): WhatsAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhatsAppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
