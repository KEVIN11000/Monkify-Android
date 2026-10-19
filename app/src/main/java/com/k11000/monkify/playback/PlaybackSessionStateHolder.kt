package com.k11000.monkify.playback

import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.playback.lyrics.LyricLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSessionStateHolder @Inject constructor() {

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _playbackSource = MutableStateFlow(PlaybackSource.adHoc())
    val playbackSource: StateFlow<PlaybackSource> = _playbackSource.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    fun updateCurrentTrack(track: Track?) {
        _currentTrack.value = track
    }

    fun updateQueue(tracks: List<Track>) {
        _queue.value = tracks
    }

    fun updatePlaybackSource(source: PlaybackSource) {
        _playbackSource.value = source
    }

    fun updateLyrics(lyrics: List<LyricLine>) {
        _lyrics.value = lyrics
    }
}
