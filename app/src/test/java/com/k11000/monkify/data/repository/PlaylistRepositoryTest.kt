package com.k11000.monkify.data.repository

import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.ActionResponseDto
import com.k11000.monkify.data.remote.dto.AddTrackToPlaylistRequest
import com.k11000.monkify.data.remote.dto.BackendTrackDto
import com.k11000.monkify.data.remote.dto.CreatePlaylistRequest
import com.k11000.monkify.data.remote.dto.IngestRequest
import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.data.remote.dto.LoginRequest
import com.k11000.monkify.data.remote.dto.LoginResponse
import com.k11000.monkify.data.remote.dto.LyricsResponse
import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class PlaylistRepositoryTest {

    private class FakeMonkifyApi : MonkifyApi {
        var shouldThrow = false
        var createPlaylistHandler: ((CreatePlaylistRequest) -> PlaylistSummaryDto)? = null
        var addTrackHandler: ((String, AddTrackToPlaylistRequest) -> ActionResponseDto)? = null
        var removeTrackHandler: ((String, String) -> ActionResponseDto)? = null
        var deletePlaylistHandler: ((String) -> ActionResponseDto)? = null

        override suspend fun getTracks(limit: Int, offset: Int, query: String?): List<BackendTrackDto> = emptyList()
        override suspend fun login(request: LoginRequest): LoginResponse = error("unused")
        override suspend fun ingest(request: IngestRequest): Response<IngestResponse> = error("unused")
        override suspend fun getLyrics(trackId: String): LyricsResponse = error("unused")

        override suspend fun getPlaylists(limit: Int, offset: Int): List<PlaylistSummaryDto> {
            if (shouldThrow) throw IOException("Network error")
            return listOf(
                PlaylistSummaryDto("pl_1", "Chill Vibes", 5),
                PlaylistSummaryDto("pl_2", "Gym Beats", 12)
            )
        }

        override suspend fun getPlaylistTracks(id: String): List<BackendTrackDto> {
            if (shouldThrow) throw IOException("Network error")
            return listOf(
                BackendTrackDto("t1", "Track 1", "Artist 1", "Album 1", 180000L, null, "/api/v1/stream/t1", false)
            )
        }

        override suspend fun createPlaylist(request: CreatePlaylistRequest): PlaylistSummaryDto {
            if (shouldThrow) throw IOException("Failed to connect")
            return createPlaylistHandler?.invoke(request)
                ?: PlaylistSummaryDto("user_pl_1", request.name, 0)
        }

        override suspend fun addTrackToPlaylist(playlistId: String, request: AddTrackToPlaylistRequest): ActionResponseDto {
            if (shouldThrow) throw IOException("Failed to connect")
            return addTrackHandler?.invoke(playlistId, request)
                ?: ActionResponseDto(true, "added", playlistId, request.trackId, 1)
        }

        override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): ActionResponseDto {
            if (shouldThrow) throw IOException("Failed to connect")
            return removeTrackHandler?.invoke(playlistId, trackId)
                ?: ActionResponseDto(true, "removed", playlistId, trackId)
        }

        override suspend fun deletePlaylist(playlistId: String): ActionResponseDto {
            if (shouldThrow) throw IOException("Failed to connect")
            return deletePlaylistHandler?.invoke(playlistId)
                ?: ActionResponseDto(true, "deleted", deletedId = playlistId)
        }
    }

    @Test
    fun `getPlaylists retorna lista de playlists exitosamente`() = runTest {
        val api = FakeMonkifyApi()
        val repo = PlaylistRepositoryImpl(api)

        val result = repo.getPlaylists()
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(2, list.size)
        assertEquals("Chill Vibes", list[0].name)
    }

    @Test
    fun `getPlaylists retorna failure ante excepcion de red`() = runTest {
        val api = FakeMonkifyApi().apply { shouldThrow = true }
        val repo = PlaylistRepositoryImpl(api)

        val result = repo.getPlaylists()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `getPlaylistTracks mapea pistas correctamente a modelo de dominio`() = runTest {
        val api = FakeMonkifyApi()
        val repo = PlaylistRepositoryImpl(api)

        val result = repo.getPlaylistTracks("pl_1")
        assertTrue(result.isSuccess)
        val tracks = result.getOrThrow()
        assertEquals(1, tracks.size)
        assertEquals("Track 1", tracks[0].title)
        assertTrue(tracks[0].streamUrl.contains("/api/v1/stream/t1"))
    }

    @Test
    fun `createPlaylist crea playlist exitosamente`() = runTest {
        val api = FakeMonkifyApi()
        val repo = PlaylistRepositoryImpl(api)

        val result = repo.createPlaylist("Mi Lista de Estudio")
        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertEquals("Mi Lista de Estudio", created.name)
        assertEquals(0, created.trackCount)
    }

    @Test
    fun `createPlaylist falla si el nombre esta en blanco sin llamar a la api`() = runTest {
        val api = FakeMonkifyApi()
        val repo = PlaylistRepositoryImpl(api)

        val result = repo.createPlaylist("   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `addTrackToPlaylist retorna exito cuando la api responde exitosamente`() = runTest {
        val api = FakeMonkifyApi()
        val repo = PlaylistRepositoryImpl(api)

        val result = repo.addTrackToPlaylist("pl_1", "t1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `addTrackToPlaylist retorna failure cuando la api responde success false`() = runTest {
        val api = FakeMonkifyApi().apply {
            addTrackHandler = { plId, req ->
                ActionResponseDto(false, "Track already exists", plId, req.trackId)
            }
        }
        val repo = PlaylistRepositoryImpl(api)

        val result = repo.addTrackToPlaylist("pl_1", "t1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Track already exists") == true)
    }

    @Test
    fun `removeTrackFromPlaylist retorna exito al remover pista`() = runTest {
        val api = FakeMonkifyApi()
        val repo = PlaylistRepositoryImpl(api)

        val result = repo.removeTrackFromPlaylist("pl_1", "t1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deletePlaylist retorna exito al eliminar playlist`() = runTest {
        val api = FakeMonkifyApi()
        val repo = PlaylistRepositoryImpl(api)

        val result = repo.deletePlaylist("pl_1")
        assertTrue(result.isSuccess)
    }
}
