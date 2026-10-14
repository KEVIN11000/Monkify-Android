package com.k11000.monkify.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.k11000.monkify.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM favorite_tracks WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getAllFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM favorite_tracks WHERE isDownloaded = 1 ORDER BY addedAt DESC")
    fun getDownloadedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC")
    fun getAllTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPagedTracks(limit: Int, offset: Int): List<TrackEntity>

    @Query("SELECT * FROM favorite_tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY addedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun searchTracks(query: String, limit: Int, offset: Int): List<TrackEntity>

    @Query("SELECT * FROM favorite_tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :id AND isFavorite = 1)")
    fun isFavorite(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<TrackEntity>): List<Long>

    @Query("DELETE FROM favorite_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String): Int

    @Query("UPDATE favorite_tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean): Int

    @Query("UPDATE favorite_tracks SET isDownloaded = :isDownloaded, localFilePath = :localPath WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String?): Int
}
