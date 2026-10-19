package com.k11000.monkify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.k11000.monkify.domain.model.Track

@Entity(tableName = "favorite_tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String?,
    val streamUrl: String,
    val isFavorite: Boolean = true,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    coverUrl = coverUrl,
    streamUrl = localFilePath ?: streamUrl
)

fun Track.toEntity(
    isFavorite: Boolean = true,
    isDownloaded: Boolean = false,
    localFilePath: String? = null,
    playCount: Int = 0,
    lastPlayedAt: Long = 0L
): TrackEntity = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    coverUrl = coverUrl,
    streamUrl = streamUrl,
    isFavorite = isFavorite,
    isDownloaded = isDownloaded,
    localFilePath = localFilePath,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt
)
