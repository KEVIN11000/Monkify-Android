package com.k11000.monkify.data.remote.dto

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class DtoSerializationContractTest {

    private val gson = Gson()

    @Test
    fun `all remote DTO instance fields have SerializedName annotation to prevent R8 field stripping`() {
        val dtoClasses = listOf(
            BackendTrackDto::class.java,
            TrackDto::class.java,
            PaginatedTracksDto::class.java,
            PlaylistDetailDto::class.java,
            PlaylistSummaryDto::class.java,
            AlbumSummaryDto::class.java,
            LoginRequest::class.java,
            LoginResponse::class.java,
            IngestRequest::class.java,
            IngestResponse::class.java,
            LyricsResponse::class.java,
            SupportReportRequestDto::class.java,
            SupportReportResponseDto::class.java,
            CreatePlaylistRequest::class.java,
            AddTrackToPlaylistRequest::class.java,
            ActionResponseDto::class.java,
            GitHubReleaseDto::class.java,
            GitHubAssetDto::class.java,
            OpenMeteoForecastResponse::class.java,
            CurrentDto::class.java,
            DailyDto::class.java
        )

        dtoClasses.forEach { clazz ->
            val instanceFields = clazz.declaredFields.filter { field ->
                !Modifier.isStatic(field.modifiers) && !field.isSynthetic
            }
            assertTrue("DTO ${clazz.simpleName} should have instance fields", instanceFields.isNotEmpty())
            instanceFields.forEach { field ->
                val annotation = field.getAnnotation(SerializedName::class.java)
                assertNotNull(
                    "Field '${field.name}' in ${clazz.simpleName} is missing @SerializedName! R8 will strip it in Release builds.",
                    annotation
                )
            }
        }
    }

    @Test
    fun `BackendTrackDto deserializes from Cloudflare Worker JSON and maps to Domain Track without NPE`() {
        val json = """
            {
                "id": "trk_001",
                "title": "Echoes of Zen",
                "artist": "Monkify Ensemble",
                "album": "Deep Focus",
                "duration_ms": 215000,
                "cover_url": "https://cdn.monkify.workers.dev/covers/trk_001.jpg",
                "stream_url": "/api/v1/stream/trk_001",
                "is_downloaded": true
            }
        """.trimIndent()

        val dto = gson.fromJson(json, BackendTrackDto::class.java)
        assertEquals("trk_001", dto.id)
        assertEquals("Echoes of Zen", dto.title)
        assertEquals("Monkify Ensemble", dto.artist)
        assertEquals("Deep Focus", dto.album)
        assertEquals(215000L, dto.durationMs)

        val domainTrack = dto.toDomain("https://monkify-backend.monkify.workers.dev/api/v1/")
        assertEquals("trk_001", domainTrack.id)
        assertEquals("Echoes of Zen", domainTrack.title)
        assertEquals("Monkify Ensemble", domainTrack.artist)
        assertEquals("Deep Focus", domainTrack.album)
        assertEquals("https://monkify-backend.monkify.workers.dev/api/v1/stream/trk_001", domainTrack.streamUrl)
    }
}
