package com.k11000.monkify.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.BackendTrackDto
import com.k11000.monkify.data.remote.dto.IngestRequest
import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.data.remote.dto.LoginRequest
import com.k11000.monkify.data.remote.dto.LoginResponse
import com.k11000.monkify.data.remote.dto.LyricsResponse
import com.k11000.monkify.data.remote.dto.toDomain
import com.k11000.monkify.data.repository.NetworkTrackRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class NetworkLayerTest {

    private val baseUrl = "https://monkify-backend.monkify.workers.dev/api/v1/"

    private val workerJson = """
        [{"id":"stream1","title":"Stream Song","artist":"Monk","album":null,
          "duration_ms":1000,"cover_url":null,"stream_url":"/stream/stream1","is_downloaded":false},
         {"id":"abs1","title":"Abs","artist":"Monk","album":"LP","duration_ms":null,
          "cover_url":"/covers/abs1.jpg","stream_url":"https://cdn.example.com/abs1.mp3","is_downloaded":true}]
    """.trimIndent()

    private fun parse(): List<BackendTrackDto> =
        Gson().fromJson(workerJson, object : TypeToken<List<BackendTrackDto>>() {}.type)

    @Test
    fun `deserializa el payload del Worker tolerando nulos`() {
        val tracks = parse()
        assertEquals(2, tracks.size)
        assertNull(tracks[0].album)
        assertEquals(1000L, tracks[0].durationMs)
        assertNull(tracks[1].durationMs)
    }

    @Test
    fun `mapper resuelve stream relativo al endpoint real y rellena nulos`() {
        val track = parse()[0].toDomain(baseUrl)
        assertEquals("https://monkify-backend.monkify.workers.dev/api/v1/stream/stream1", track.streamUrl)
        assertEquals("", track.album)
        assertNull(track.coverUrl)
    }

    @Test
    fun `mapper conserva stream absoluto y resuelve cover relativo`() {
        val track = parse()[1].toDomain(baseUrl)
        assertEquals("https://cdn.example.com/abs1.mp3", track.streamUrl)
        assertEquals("https://monkify-backend.monkify.workers.dev/covers/abs1.jpg", track.coverUrl)
        assertEquals(0L, track.durationMs)
    }

    private class FakeApi(private val result: () -> List<BackendTrackDto>) : MonkifyApi {
        override suspend fun getTracks(limit: Int, offset: Int, query: String?) = result()
        override suspend fun login(request: LoginRequest): LoginResponse = error("unused")
        override suspend fun ingest(request: IngestRequest): Response<IngestResponse> = error("unused")
        override suspend fun getLyrics(trackId: String): LyricsResponse = error("unused")
    }

    @Test
    fun `repositorio mapea pistas de la API`() = runBlocking {
        val repo = NetworkTrackRepository(FakeApi { parse() }, baseUrl)
        val tracks = repo.getRecentTracks().first()
        assertEquals(listOf("stream1", "abs1"), tracks.map { it.id })
        assertTrue(tracks[0].streamUrl.endsWith("/api/v1/stream/stream1"))
    }

    @Test
    fun `repositorio emite lista vacia ante fallo de red`() = runBlocking {
        val repo = NetworkTrackRepository(FakeApi { throw java.io.IOException("offline") }, baseUrl)
        assertTrue(repo.getRecentTracks().first().isEmpty())
    }
}
