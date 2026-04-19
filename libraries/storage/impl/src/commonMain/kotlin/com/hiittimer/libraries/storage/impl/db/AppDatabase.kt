package com.dangerfield.hiittimer.libraries.storage.impl.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.dangerfield.hiittimer.features.timers.storage.db.BlockEntity
import com.dangerfield.hiittimer.features.timers.storage.db.TimerDao
import com.dangerfield.hiittimer.features.timers.storage.db.TimerEntity
import com.dangerfield.hiittimer.libraries.hiittimer.storage.db.SessionDao
import com.dangerfield.hiittimer.libraries.hiittimer.storage.db.SessionEntity
import com.dangerfield.hiittimer.libraries.hiittimer.storage.db.UserDao
import com.dangerfield.hiittimer.libraries.hiittimer.storage.db.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
        TimerEntity::class,
        BlockEntity::class,
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(CoreTypeConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun timerDao(): TimerDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
