package com.stanly.wallpapermovil.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WallpaperItem::class], version = 1, exportSchema = false)
abstract class WallpaperDatabase : RoomDatabase() {

    abstract fun wallpaperDao(): WallpaperDao

    companion object {
        @Volatile
        private var INSTANCE: WallpaperDatabase? = null

        fun getInstance(context: Context): WallpaperDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WallpaperDatabase::class.java,
                    "wallpaper_library.db"
                ).build().also { INSTANCE = it }
            }
    }
}
