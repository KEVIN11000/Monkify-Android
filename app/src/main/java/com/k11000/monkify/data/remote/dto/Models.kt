package com.k11000.monkify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TrackDto(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("release_year") val releaseYear: Int,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("stream_url") val streamUrl: String,
    @SerializedName("has_lyrics") val hasLyrics: Boolean,
    @SerializedName("is_favorite") val isFavorite: Boolean
)

data class PaginatedTracksDto(
    val items: List<TrackDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    @SerializedName("has_next") val hasNext: Boolean
)

data class PlaylistDetailDto(
    val id: String,
    val name: String,
    val description: String?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("total_tracks") val totalTracks: Int,
    val tracks: List<TrackDto>
)

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int
)

data class IngestRequest(val url: String)

data class IngestResponse(
    @SerializedName("task_id") val taskId: String,
    val status: String,
    val message: String
)

data class LyricsResponse(
    @SerializedName("track_id") val trackId: String,
    val format: String,
    val content: String
)
