package com.stanly.wallpapermovil.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class WallpaperRepository(context: Context) {

    private val dao = WallpaperDatabase.getInstance(context).wallpaperDao()

    val allWallpapers: Flow<List<WallpaperItem>> = dao.getAll()

    suspend fun add(item: WallpaperItem): Long = dao.insert(item)

    suspend fun delete(item: WallpaperItem) = dao.delete(item)

    suspend fun deleteById(id: Int) = dao.deleteById(id)

    companion object {
        @Volatile
        private var INSTANCE: WallpaperRepository? = null

        fun getInstance(context: Context): WallpaperRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WallpaperRepository(context).also { INSTANCE = it }
            }
    }
}
