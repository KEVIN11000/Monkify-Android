package com.k11000.monkify.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSpectrumProcessorTest {

    private lateinit var testScope: TestScope
    private lateinit var processor: AudioSpectrumProcessor

    @Before
    fun setUp() {
        testScope = TestScope()
        processor = AudioSpectrumProcessor(scope = testScope)
    }

    @Test
    fun initial_state_is_empty_with_twenty_eight_bands() {
        val initial = processor.spectrumFlow.value
        assertEquals(AudioSpectrumData.BAND_COUNT, initial.bands.size)
        assertFalse(initial.isRealTime)
        assertEquals(AudioSpectrumData.EMPTY, initial)
    }

    @Test
    fun attachAudioSession_with_zero_or_negative_id_activates_procedural_fallback() {
        processor.attachAudioSession(0)
        assertTrue(processor.isFallbackActive)

        processor.attachAudioSession(-1)
        assertTrue(processor.isFallbackActive)
    }

    @Test
    fun processFftBytes_normalizes_to_twenty_eight_bands_within_bounded_range() {
        val dummyFft = ByteArray(256) { idx -> ((idx * 3) % 127).toByte() }
        processor.processFftBytes(dummyFft)

        val state = processor.spectrumFlow.value
        assertEquals(AudioSpectrumData.BAND_COUNT, state.bands.size)
        assertTrue(state.isRealTime)

        for (band in state.bands) {
            assertTrue("Band $band should be >= 0.05f", band >= 0.05f)
            assertTrue("Band $band should be <= 1.0f", band <= 1.0f)
        }
    }

    @Test
    fun release_resets_state_and_restores_empty_spectrum() {
        val dummyFft = ByteArray(256) { 50.toByte() }
        processor.processFftBytes(dummyFft)
        assertTrue(processor.spectrumFlow.value.isRealTime)

        processor.release()
        assertEquals(AudioSpectrumData.EMPTY, processor.spectrumFlow.value)
    }
}
