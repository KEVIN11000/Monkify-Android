package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.api.ServerStatusMonitor
import com.k11000.monkify.data.remote.dto.AlbumSummaryDto
import com.k11000.monkify.data.repository.SettingsRepository
import com.k11000.monkify.domain.model.CurrentWeather
import com.k11000.monkify.domain.model.SyncStatus
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.model.WeatherCondition
import com.k11000.monkify.domain.model.ZenDayPart
import com.k11000.monkify.domain.model.ZenPhraseCatalog
import com.k11000.monkify.domain.repository.DownloadRepository
import com.k11000.monkify.domain.repository.SyncScheduler
import com.k11000.monkify.domain.repository.TrackRepository
import com.k11000.monkify.domain.repository.WeatherRepository
import com.k11000.monkify.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class HomeFilter(val displayName: String) {
    ALL("Todo"),
    MUSIC("Música"),
    ALBUMS("Álbumes"),
    PODCASTS("Podcasts")
}

data class GreetingInfo(
    val title: String,
    val phrase: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val downloadRepository: DownloadRepository? = null,
    private val syncScheduler: SyncScheduler? = null,
    private val monkifyApi: MonkifyApi? = null,
    private val weatherRepository: WeatherRepository? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(HomeFilter.ALL)
    val selectedFilter: StateFlow<HomeFilter> = _selectedFilter.asStateFlow()

    fun setFilter(filter: HomeFilter) {
        _selectedFilter.value = filter
    }

    val downloadedTrackIds: StateFlow<Set<String>> = downloadRepository?.downloadedTrackIds
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
        ?: MutableStateFlow(emptySet())

    val downloadingTrackIds: StateFlow<Set<String>> = downloadRepository?.downloadingTrackIds
        ?: MutableStateFlow(emptySet())

    val syncStatus: StateFlow<SyncStatus> = syncScheduler?.syncStatus
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus.IDLE)
        ?: MutableStateFlow(SyncStatus.IDLE)

    val lastSyncTimestamp: StateFlow<Long> = syncScheduler?.lastSyncTimestamp
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
        ?: MutableStateFlow(0L)

    fun requestSync() {
        syncScheduler?.scheduleImmediateSync()
    }

    fun toggleDownload(track: Track) {
        viewModelScope.launch {
            downloadRepository?.toggleDownload(track)
        }
    }

    val currentWeather: StateFlow<CurrentWeather?> = weatherRepository?.getCurrentWeatherFlow()
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, null)
        ?: MutableStateFlow(null)

    private val _phraseIndexOffset = MutableStateFlow(0)

    val greeting: StateFlow<GreetingInfo> = combine(
        currentWeather,
        _phraseIndexOffset
    ) { weather, offset ->
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val condition = weather?.condition ?: WeatherCondition.SUNNY
        val dayPart = weather?.resolveDayPart() ?: ZenDayPart.fromHour(hour)
        val title = ZenPhraseCatalog.resolveTitle(dayPart)
        val phrase = ZenPhraseCatalog.resolvePhrase(condition, dayPart, offset)
        GreetingInfo(title = title, phrase = phrase)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = run {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val dayPart = ZenDayPart.fromHour(hour)
            GreetingInfo(
                title = ZenPhraseCatalog.resolveTitle(dayPart),
                phrase = ZenPhraseCatalog.resolvePhrase(WeatherCondition.SUNNY, dayPart, 0)
            )
        }
    )

    fun cycleNextPhrase() {
        _phraseIndexOffset.value += 1
    }

    fun getDynamicGreeting(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): GreetingInfo {
        return when (hour) {
            in 0..4 -> GreetingInfo(title = "Buenas noches", phrase = "La noche es joven y la música no duerme")
            in 5..6 -> GreetingInfo(title = "Buenos días", phrase = "Comienza el día con buen ritmo")
            in 7..11 -> GreetingInfo(title = "Buenos días", phrase = "Que la música impulse tu día")
            in 12..13 -> GreetingInfo(title = "Buenas tardes", phrase = "Pausa para disfrutar de tus canciones")
            in 14..17 -> GreetingInfo(title = "Buenas tardes", phrase = "Buena música para seguir adelante")
            in 18..19 -> GreetingInfo(title = "Buenas tardes", phrase = "El día cae, el ritmo sigue")
            in 20..23 -> GreetingInfo(title = "Buenas noches", phrase = "Relájate y disfruta del sonido")
            else -> GreetingInfo(title = "Buenas noches", phrase = "Relájate y disfruta del sonido")
        }
    }

    fun updateGreeting(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        cycleNextPhrase()
    }

    private val _latestUploadedTracks = MutableStateFlow<List<Track>>(emptyList())
    val latestUploadedTracks: StateFlow<List<Track>> = _latestUploadedTracks.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumSummaryDto>>(emptyList())
    val albums: StateFlow<List<AlbumSummaryDto>> = _albums.asStateFlow()

    private val _isLoadingAlbums = MutableStateFlow(false)
    val isLoadingAlbums: StateFlow<Boolean> = _isLoadingAlbums.asStateFlow()

    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

    private val _madeForYouTracks = MutableStateFlow<List<Track>>(emptyList())
    val madeForYouTracks: StateFlow<List<Track>> = _madeForYouTracks.asStateFlow()

    private val _feedTracks = MutableStateFlow<List<Track>>(emptyList())
    val feedTracks: StateFlow<List<Track>> = _feedTracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    init {
        observeRecentAndMadeForYou()
        observeTokenChanges()
        loadData()
        viewModelScope.launch {
            weatherRepository?.refreshWeatherIfNeeded(force = false)
        }
    }

    private fun observeTokenChanges() {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.serverAccessToken
                    .drop(1)
                    .distinctUntilChanged()
                    .collect { newToken ->
                        if (newToken.isNotBlank()) {
                            AppLogger.i("HomeViewModel", "Token de servidor actualizado: recargando catálogo automáticamente")
                            refresh()
                        }
                    }
            }
        }
    }

    fun refresh() {
        _hasMore.value = true
        _phraseIndexOffset.value += 1
        loadData()
        syncScheduler?.scheduleImmediateSync()
        viewModelScope.launch {
            weatherRepository?.refreshWeatherIfNeeded(force = true)
        }
    }

    private fun observeRecentAndMadeForYou() {
        viewModelScope.launch {
            trackRepository.getRecentTracks().collect { tracks ->
                if (tracks.isNotEmpty()) {
                    _recentTracks.value = tracks.take(6)
                }
            }
        }
        viewModelScope.launch {
            trackRepository.getMadeForYouTracks().collect { tracks ->
                if (tracks.isNotEmpty()) {
                    _madeForYouTracks.value = tracks
                }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Cargar las 6 canciones más recientemente subidas al servidor
                try {
                    val latest = trackRepository.getLatestUploadedTracks(limit = 6)
                    if (latest.isNotEmpty()) {
                        _latestUploadedTracks.value = latest
                    }
                } catch (e: Exception) {
                    AppLogger.w("HomeViewModel", "Fallo al cargar novedades recientes: ${e.message}")
                }

                // 2. Cargar álbumes para el filtro Álbumes
                loadAlbums()

                // 3. Catálogo base
                val tracks = trackRepository.getTracks(limit = PAGE_SIZE, offset = 0)
                if (tracks.isNotEmpty()) {
                    _feedTracks.value = tracks
                    if (_recentTracks.value.isEmpty()) {
                        _recentTracks.value = tracks.take(6)
                    }
                    if (_madeForYouTracks.value.isEmpty()) {
                        _madeForYouTracks.value = if (tracks.size > 6) tracks.drop(6).take(10) else tracks.reversed()
                    }
                    _hasMore.value = tracks.size >= PAGE_SIZE
                }
            } catch (e: Exception) {
                AppLogger.e("HomeViewModel", "Error en loadData()", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            _isLoadingAlbums.value = true
            try {
                val remote = monkifyApi?.getAlbums(limit = 50, offset = 0) ?: emptyList()
                if (remote.isNotEmpty()) {
                    _albums.value = remote
                }
            } catch (e: Exception) {
                AppLogger.w("HomeViewModel", "Error al cargar álbumes: ${e.message}")
                ServerStatusMonitor.shared.reportException("Carga de álbumes", e)
            } finally {
                _isLoadingAlbums.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_hasMore.value || _isLoading.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val currentOffset = _feedTracks.value.size
                val newTracks = trackRepository.getTracks(limit = PAGE_SIZE, offset = currentOffset)
                if (newTracks.isEmpty()) {
                    _hasMore.value = false
                } else {
                    _feedTracks.value = _feedTracks.value + newTracks
                    if (newTracks.size < PAGE_SIZE) {
                        _hasMore.value = false
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("HomeViewModel", "Error en loadMore(): ${e.message}")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
