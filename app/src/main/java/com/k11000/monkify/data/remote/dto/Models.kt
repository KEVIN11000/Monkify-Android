package com.k11000.monkify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TrackDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String,
    @SerializedName("duration_ms") val durationMs: Long,
    @SerializedName("release_year") val releaseYear: Int,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("stream_url") val streamUrl: String,
    @SerializedName("has_lyrics") val hasLyrics: Boolean,
    @SerializedName("is_favorite") val isFavorite: Boolean
)

/** Pista tal como la devuelve el Worker de Cloudflare (`GET /api/v1/tracks`, array JSON plano). */
data class BackendTrackDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String?,
    @SerializedName("duration_ms") val durationMs: Long?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("stream_url") val streamUrl: String?,
    @SerializedName("is_downloaded") val isDownloaded: Boolean?
)

data class PaginatedTracksDto(
    @SerializedName("items") val items: List<TrackDto>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("has_next") val hasNext: Boolean
)

data class PlaylistDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("total_tracks") val totalTracks: Int,
    @SerializedName("tracks") val tracks: List<TrackDto>
)

data class PlaylistSummaryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("track_count") val trackCount: Int,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("cover_urls") val coverUrls: List<String> = emptyList()
)

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int
)

data class IngestRequest(
    @SerializedName("url") val url: String
)

data class IngestResponse(
    @SerializedName("task_id") val taskId: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

data class LyricsResponse(
    @SerializedName("track_id") val trackId: String = "",
    @SerializedName("format") val format: String? = "lrc",
    @SerializedName("content") private val _content: String? = null,
    @SerializedName("lyrics") val lyrics: String? = null,
    @SerializedName("synced") val synced: Boolean = false,
    @SerializedName("source") val source: String? = null
) {
    val content: String
        get() = lyrics?.takeIf { it.isNotBlank() } ?: _content.orEmpty()
}

data class SupportReportRequestDto(
    @SerializedName("message") val message: String,
    @SerializedName("diagnostics") val diagnostics: String,
    @SerializedName("app_version") val appVersion: String = "0.4.0",
    @SerializedName("device_model") val deviceModel: String = ""
)

data class SupportReportResponseDto(
    @SerializedName("ok") val ok: Boolean = true,
    @SerializedName("report_id") val reportId: String = "",
    @SerializedName("github_issue_url") val githubIssueUrl: String? = null
)

data class CreatePlaylistRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)

data class AddTrackToPlaylistRequest(
    @SerializedName("track_id") val trackId: String
)

data class ActionResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("playlist_id") val playlistId: String? = null,
    @SerializedName("track_id") val trackId: String? = null,
    @SerializedName("position") val position: Int? = null,
    @SerializedName("deleted_id") val deletedId: String? = null
)
