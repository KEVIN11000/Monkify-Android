package com.k11000.monkify.ui.viewmodel

import com.k11000.monkify.domain.model.AppUpdate
import com.k11000.monkify.domain.repository.UpdateRepository
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
}
