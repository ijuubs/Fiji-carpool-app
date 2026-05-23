package com.example.data

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        UserEntity::class,
        CommuteProfileEntity::class,
        RideMatchEntity::class,
        OneOffRequestEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun commuteProfileDao(): CommuteProfileDao
    abstract fun rideMatchDao(): RideMatchDao
    abstract fun oneOffRequestDao(): OneOffRequestDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "veiliu_carpool_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
