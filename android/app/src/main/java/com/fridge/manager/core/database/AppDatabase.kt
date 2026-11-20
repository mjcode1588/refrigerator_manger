package com.fridge.manager.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocationEntity::class, ItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun itemDao(): ItemDao
}
