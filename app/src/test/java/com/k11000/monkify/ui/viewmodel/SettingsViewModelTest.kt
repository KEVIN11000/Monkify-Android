package com.k11000.monkify.ui.viewmodel

import com.k11000.monkify.domain.model.AppUpdate
import com.k11000.monkify.domain.repository.UpdateRepository
import com.k11000.monkify.ui.screens.viewmodel.ServerConnectionTestState
import com.k11000.monkify.ui.screens.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeUpdateRepository(
        private val updateHandler: () -> Result<AppUpdate>
    ) : UpdateRepository {
        override suspend fun checkForUpdate(): Result<AppUpdate> = updateHandler()
    }

    @Test
    fun check_for_updates_sets_available_update_when_newer_version_exists() = runTest(testDispatcher) {
        val appUpdate = AppUpdate(
            latestVersionName = "v2.0.0",
            currentVersionName = "1.0",
            releaseNotes = "Nueva versión",
            downloadUrl = "https://github.com/monkify.apk",
            apkSize = 10000000L,
            isUpdateAvailable = true
        )
        val fakeRepo = FakeUpdateRepository { Result.success(appUpdate) }
        val viewModel = SettingsViewModel(fakeRepo)

        viewModel.checkForUpdates(manual = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCheckingUpdate)
        assertNotNull(state.availableUpdate)
        assertEquals("v2.0.0", state.availableUpdate?.latestVersionName)

        viewModel.dismissUpdateDialog()
        assertNull(viewModel.uiState.value.availableUpdate)
    }

    @Test
    fun check_for_updates_displays_up_to_date_message_when_no_update_available() = runTest(testDispatcher) {
        val appUpdate = AppUpdate(
            latestVersionName = "v1.0.0",
            currentVersionName = "1.0",
            releaseNotes = "",
            downloadUrl = "",
            apkSize = 0L,
            isUpdateAvailable = false
        )
        val fakeRepo = FakeUpdateRepository { Result.success(appUpdate) }
        val viewModel = SettingsViewModel(fakeRepo)

        viewModel.checkForUpdates(manual = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCheckingUpdate)
        assertNull(state.availableUpdate)
        assertTrue(state.message?.contains("Monkify está al día") == true)
    }

    @Test
    fun check_for_updates_displays_error_message_on_network_failure() = runTest(testDispatcher) {
        val fakeRepo = FakeUpdateRepository { Result.failure(IOException("Timeout")) }
        val viewModel = SettingsViewModel(fakeRepo)

        viewModel.checkForUpdates(manual = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCheckingUpdate)
        assertNull(state.availableUpdate)
        assertTrue(state.message?.contains("No se pudo verificar actualizaciones") == true)
    }

    private class FakeMonkifyApi(
        private val tracksHandler: () -> List<com.k11000.monkify.data.remote.dto.BackendTrackDto> = { emptyList() },
        private val reportHandler: (com.k11000.monkify.data.remote.dto.SupportReportRequestDto) -> com.k11000.monkify.data.remote.dto.SupportReportResponseDto = {
            com.k11000.monkify.data.remote.dto.SupportReportResponseDto(ok = true, reportId = "rep_default")
        }
    ) : com.k11000.monkify.data.remote.api.MonkifyApi {
        override suspend fun getTracks(limit: Int, offset: Int, query: String?, sort: String?): List<com.k11000.monkify.data.remote.dto.BackendTrackDto> = tracksHandler()
        override suspend fun submitSupportReport(request: com.k11000.monkify.data.remote.dto.SupportReportRequestDto): com.k11000.monkify.data.remote.dto.SupportReportResponseDto = reportHandler(request)
        override suspend fun login(request: com.k11000.monkify.data.remote.dto.LoginRequest): com.k11000.monkify.data.remote.dto.LoginResponse = error("unused")
        override suspend fun ingest(request: com.k11000.monkify.data.remote.dto.IngestRequest): retrofit2.Response<com.k11000.monkify.data.remote.dto.IngestResponse> = error("unused")
        override suspend fun getLyrics(trackId: String): com.k11000.monkify.data.remote.dto.LyricsResponse = error("unused")
        override suspend fun getPlaylists(limit: Int, offset: Int): List<com.k11000.monkify.data.remote.dto.PlaylistSummaryDto> = emptyList()
        override suspend fun getPlaylistTracks(id: String): List<com.k11000.monkify.data.remote.dto.BackendTrackDto> = emptyList()
        override suspend fun createPlaylist(request: com.k11000.monkify.data.remote.dto.CreatePlaylistRequest): com.k11000.monkify.data.remote.dto.PlaylistSummaryDto = error("unused")
        override suspend fun addTrackToPlaylist(playlistId: String, request: com.k11000.monkify.data.remote.dto.AddTrackToPlaylistRequest): com.k11000.monkify.data.remote.dto.ActionResponseDto = error("unused")
        override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): com.k11000.monkify.data.remote.dto.ActionResponseDto = error("unused")
        override suspend fun deletePlaylist(playlistId: String): com.k11000.monkify.data.remote.dto.ActionResponseDto = error("unused")
    }

    @Test
    fun submit_one_click_report_notifies_success_when_api_returns_ok() = runTest(testDispatcher) {
        val fakeRepo = FakeUpdateRepository { Result.failure(IOException()) }
        val fakeApi = FakeMonkifyApi(reportHandler = { req ->
            assertEquals("Problema con audio", req.message)
            assertTrue(req.diagnostics.contains("BUG"))
            com.k11000.monkify.data.remote.dto.SupportReportResponseDto(ok = true, reportId = "rep_123")
        })
        val viewModel = SettingsViewModel(settingsRepository = null, updateRepository = fakeRepo, monkifyApi = fakeApi)

        var callbackSuccess: Boolean? = null
        viewModel.submitOneClickReport("BUG", "Problema con audio", logs = "log data") { success, _ ->
            callbackSuccess = success
        }
        advanceUntilIdle()

        assertTrue(callbackSuccess == true)
        assertFalse(viewModel.uiState.value.isSubmittingReport)
    }

    @Test
    fun set_server_access_token_updates_state_and_verifies_connection_when_api_present() = runTest(testDispatcher) {
        val fakeRepo = FakeUpdateRepository { Result.failure(IOException()) }
        val fakeApi = FakeMonkifyApi(tracksHandler = {
            listOf(
                com.k11000.monkify.data.remote.dto.BackendTrackDto(
                    id = "1",
                    title = "Test",
                    artist = "Artist",
                    album = "Album",
                    durationMs = 1000L,
                    coverUrl = null,
                    streamUrl = null,
                    isDownloaded = false
                )
            )
        })
        val viewModel = SettingsViewModel(settingsRepository = null, updateRepository = fakeRepo, monkifyApi = fakeApi)

        viewModel.setServerAccessToken("  my_secret_token_123  ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("my_secret_token_123", state.serverAccessToken)
        assertTrue(state.connectionTestState is ServerConnectionTestState.Success)

        viewModel.setServerAccessToken("")
        advanceUntilIdle()

        val clearedState = viewModel.uiState.value
        assertEquals("", clearedState.serverAccessToken)
        assertEquals(ServerConnectionTestState.Idle, clearedState.connectionTestState)
        assertTrue(clearedState.message?.contains("eliminada") == true)
    }
}
