package com.furkanbarissonmezisik.tesbihatpro.core.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ZikirEntity::class, DailyCountEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun zikirDao(): ZikirDao
    abstract fun dailyCountDao(): DailyCountDao

    /** 1000 altındaki id'ler hazır zikirlere ayrılır; kullanıcı zikirleri 1000'den başlar. */
    object ReserveIdsForPresets : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT INTO sqlite_sequence (name, seq) VALUES ('zikirs', 999)")
        }
    }

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "tesbihat.db")
                .addCallback(ReserveIdsForPresets)
                .build()
    }
}
