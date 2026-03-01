package com.stanly.wallpapermovil.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {

    @Query("SELECT * FROM wallpaper_items ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WallpaperItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WallpaperItem): Long

    @Delete
    suspend fun delete(item: WallpaperItem)

    @Query("DELETE FROM wallpaper_items WHERE id = :id")
    suspend fun deleteById(id: Int)
}
