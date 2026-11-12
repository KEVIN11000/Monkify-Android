package com.k11000.monkify.playback

import android.content.Context
import android.content.ContextWrapper
import androidx.media3.common.Player
import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.TrackEntity
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
import com.k11000.monkify.data.remote.dto.SupportReportRequestDto
import com.k11000.monkify.data.remote.dto.SupportReportResponseDto
import com.k11000.monkify.data.repository.LocalTrackRepository
import com.k11000.monkify.domain.model.DownloadState
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerControlsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PlayerViewModel
    private lateinit var stateHolder: PlaybackSessionStateHolder

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        stateHolder = PlaybackSessionStateHolder()
        viewModel = PlayerViewModel(
            context = ContextWrapper(null),
            localTrackRepository = LocalTrackRepository(FakeTrackDao()),
            downloadRepository = FakeDownloadRepository(),
            monkifyApi = FakeMonkifyApi(),
            stateHolder = stateHolder
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleRepeatMode en PlayerViewModel avanza por los 3 estados reales de Media3`() {
        assertEquals(Player.REPEAT_MODE_OFF, viewModel.repeatMode.value)

        viewModel.toggleRepeatMode()
        assertEquals(Player.REPEAT_MODE_ALL, viewModel.repeatMode.value)

        viewModel.toggleRepeatMode()
        assertEquals(Player.REPEAT_MODE_ONE, viewModel.repeatMode.value)

        viewModel.toggleRepeatMode()
        assertEquals(Player.REPEAT_MODE_OFF, viewModel.repeatMode.value)
    }

    @Test
    fun `toggleZenCleared y setZenCleared conmutan el StateFlow real en PlayerViewModel`() {
        assertFalse(viewModel.isZenCleared.value)

        viewModel.toggleZenCleared()
        assertTrue(viewModel.isZenCleared.value)

        viewModel.setZenCleared(false)
        assertFalse(viewModel.isZenCleared.value)
    }

    @Test
    fun `toggleShuffle y setShuffleEnabled actualizan isShuffleEnabled en PlayerViewModel`() {
        assertFalse(viewModel.isShuffleEnabled.value)

        viewModel.toggleShuffle()
        assertTrue(viewModel.isShuffleEnabled.value)

        viewModel.setShuffleEnabled(false)
        assertFalse(viewModel.isShuffleEnabled.value)
    }

    @Test
    fun `playQueue reordenar y eliminar elementos actualiza la cola y PlaybackSessionStateHolder`() {
        val tracks = listOf(
            Track("t1", "Track 1", "Artist", "Album 1", 1000L, null, "https://api.monkify.tudominio.com/t1"),
            Track("t2", "Track 2", "Artist", "Album 2", 1000L, null, "https://api.monkify.tudominio.com/t2"),
            Track("t3", "Track 3", "Artist", "Album 3", 1000L, null, "https://api.monkify.tudominio.com/t3")
        )

        viewModel.playQueue(tracks, startIndex = 0)
        assertEquals(3, viewModel.queue.value.size)
        assertEquals("Track 1", viewModel.currentTrackTitle.value)

        // Mover de índice 0 a índice 2 -> [t2, t3, t1]
        viewModel.moveQueueItem(0, 2)
        assertEquals(listOf("t2", "t3", "t1"), viewModel.queue.value.map { it.id })
        assertEquals(listOf("t2", "t3", "t1"), stateHolder.queue.value.map { it.id })

        // Eliminar elemento intermedio (índice 1 -> t3) -> [t2, t1]
        viewModel.removeQueueItem(1)
        assertEquals(listOf("t2", "t1"), viewModel.queue.value.map { it.id })
        assertEquals(listOf("t2", "t1"), stateHolder.queue.value.map { it.id })
    }

    @Test
    fun `seekTo en PlayerViewModel actualiza currentPosition y progress inmediatamente`() {
        viewModel.setDurationForTest(200_000L)

        viewModel.seekTo(50_000L)
        assertEquals(50_000L, viewModel.currentPosition.value)
        assertEquals(0.25f, viewModel.progress.value, 0.001f)

        viewModel.seekTo(100_000L)
        assertEquals(100_000L, viewModel.currentPosition.value)
        assertEquals(0.50f, viewModel.progress.value, 0.001f)
    }

    @Test
    fun `selectDevice en PlayerViewModel marca el dispositivo activo y actualiza activeDeviceName`() {
        val currentDevices = viewModel.devices.value
        assertTrue(currentDevices.isNotEmpty())
        val firstId = currentDevices.first().id

        viewModel.selectDevice(firstId)
        assertTrue(viewModel.devices.value.first { it.id == firstId }.isSelected)
        assertEquals(currentDevices.first().name, viewModel.activeDeviceName.value)
    }

    @Test
    fun `SupportReportRequestDto utiliza BuildConfig VERSION_NAME por defecto`() {
        val dto = SupportReportRequestDto(
            message = "Test",
            diagnostics = "Diag"
        )
        assertEquals(BuildConfig.VERSION_NAME, dto.appVersion)
    }

    private class FakeTrackDao : TrackDao {
        private val tracksFlow = MutableStateFlow<List<TrackEntity>>(emptyList())
        override fun getAllFavoriteTracks(): Flow<List<TrackEntity>> = tracksFlow.map { it.filter { t -> t.isFavorite } }
        override fun getDownloadedTracks(): Flow<List<TrackEntity>> = tracksFlow.map { it.filter { t -> t.isDownloaded } }
        override suspend fun getTrackById(id: String): TrackEntity? = tracksFlow.value.find { it.id == id }
        override fun isFavorite(id: String): Flow<Boolean> = tracksFlow.map { list -> list.any { it.id == id && it.isFavorite } }
        override suspend fun insertTrack(track: TrackEntity) {
            tracksFlow.value = tracksFlow.value.filterNot { it.id == track.id } + track
        }
        override suspend fun deleteTrackById(id: String): Int = 0
        override suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean): Int = 1
        override fun getAllTracksFlow(): Flow<List<TrackEntity>> = tracksFlow
        override suspend fun getPagedTracks(limit: Int, offset: Int): List<TrackEntity> = emptyList()
        override suspend fun searchTracks(query: String, limit: Int, offset: Int): List<TrackEntity> = emptyList()
        override suspend fun insertTracks(tracks: List<TrackEntity>): List<Long> = emptyList()
        override suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String?): Int = 1
        override suspend fun getAllTracksSnapshot(): List<TrackEntity> = tracksFlow.value
        override suspend fun recordTrackPlay(trackId: String, timestamp: Long): Int = 1
        override fun getRecentlyPlayedTracks(limit: Int): Flow<List<TrackEntity>> = flowOf(emptyList())
        override fun getTracksOrderedByPlayCount(): Flow<List<TrackEntity>> = flowOf(emptyList())
        override suspend fun getRecentlyPlayedTracksSnapshot(limit: Int): List<TrackEntity> = emptyList()
        override suspend fun getTracksOrderedByPlayCountSnapshot(limit: Int): List<TrackEntity> = emptyList()
    }

    private class FakeDownloadRepository : DownloadRepository {
        override val downloadingTrackIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        override val downloadedTrackIds: Flow<Set<String>> = flowOf(emptySet())
        override fun getDownloadState(trackId: String): Flow<DownloadState> = flowOf(DownloadState.NotDownloaded)
        override suspend fun downloadTrack(track: Track): Result<String> = Result.success("")
        override suspend fun downloadTracks(tracks: List<Track>, onProgress: (Int, Int) -> Unit): Result<Unit> = Result.success(Unit)
        override suspend fun deleteDownloadedTrack(trackId: String): Result<Unit> = Result.success(Unit)
        override suspend fun toggleDownload(track: Track) {}
        override fun isTrackDownloaded(trackId: String): Boolean = false
        override fun getLocalLyrics(trackId: String): String? = null
        override fun getLocalAudioPath(trackId: String): String? = null
        override fun getTotalDownloadsSizeBytes(): Long = 0L
        override fun downloadPlaylist(playlistId: String, tracks: List<Track>) {}
        override fun cancelPlaylistDownload(playlistId: String) {}
        override fun isPlaylistDownloading(playlistId: String): StateFlow<Boolean> = MutableStateFlow(false)
        override fun getPlaylistDownloadProgress(playlistId: String): StateFlow<Pair<Int, Int>> = MutableStateFlow(0 to 0)
    }

    private class FakeMonkifyApi : MonkifyApi {
        override suspend fun getTracks(limit: Int, offset: Int, query: String?, sort: String?): List<BackendTrackDto> = emptyList()
        override suspend fun submitSupportReport(request: SupportReportRequestDto): SupportReportResponseDto = SupportReportResponseDto(ok = true)
        override suspend fun login(request: LoginRequest): LoginResponse = throw UnsupportedOperationException()
        override suspend fun ingest(request: IngestRequest): Response<IngestResponse> = throw UnsupportedOperationException()
        override suspend fun getLyrics(trackId: String): LyricsResponse = LyricsResponse()
        override suspend fun getPlaylists(limit: Int, offset: Int): List<PlaylistSummaryDto> = emptyList()
        override suspend fun getPlaylistTracks(id: String): List<BackendTrackDto> = emptyList()
        override suspend fun createPlaylist(request: CreatePlaylistRequest): PlaylistSummaryDto = throw UnsupportedOperationException()
        override suspend fun addTrackToPlaylist(playlistId: String, request: AddTrackToPlaylistRequest): ActionResponseDto = ActionResponseDto(true)
        override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): ActionResponseDto = ActionResponseDto(true)
        override suspend fun deletePlaylist(playlistId: String): ActionResponseDto = ActionResponseDto(true)
    }
}
