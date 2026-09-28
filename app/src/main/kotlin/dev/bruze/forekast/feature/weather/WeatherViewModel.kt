package dev.bruze.forekast.feature.weather

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.core.ports.*
import dev.bruze.forekast.data.demo.*
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class WeatherUiState(
    val location: Location = DemoCatalog.locations.first(),
    val snapshot: WeatherSnapshot? = null,
    val refreshing: Boolean = true,
    val failed: Boolean = false,
    val unit: TemperatureUnit = TemperatureUnit.Celsius,
    val theme: ThemeMode = ThemeMode.System,
    val scenario: DemoScenario = DemoScenario.Success,
    val favorites: List<String> = emptyList(),
    val now: Instant = Instant.EPOCH,
    val isDemo: Boolean = true,
    val knownLocations: Map<String, Location> = DemoCatalog.locations.associateBy { it.id },
    val query: String = "",
    val searchResults: List<Location> = emptyList(),
    val searching: Boolean = false,
    val searchFailed: Boolean = false,
    val failureReason: String? = null,
    val retryAt: Instant? = null,
    val initialized: Boolean = true,
    val storageError: Boolean = false,
    val contentRead: Boolean = true,
    val hasSelection: Boolean = true,
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val demo: DemoController,
    private val saved: SavedStateHandle,
    private val clock: Clock,
    private val locations: LocationDirectory = DemoLocationDirectory,
    private val isDemo: Boolean = true,
    private val library: LibraryRepository? = null,
    private val preferences: PreferencesRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState(
        location = locations.find(saved["location"] ?: locations.initial.id) ?: locations.initial,
        unit = enumValueOrDefault(saved["unit"], TemperatureUnit.Celsius),
        theme = enumValueOrDefault(saved["theme"], ThemeMode.System),
        scenario = enumValueOrDefault(saved["scenario"], DemoScenario.Success),
        favorites = saved.get<ArrayList<String>>("favorites")?.filter { locations.find(it) != null } ?: emptyList(),
        isDemo = isDemo,
        initialized = library == null,
        hasSelection = library == null,
        knownLocations = (saved.get<ArrayList<String>>("favorites").orEmpty() + listOf(locations.initial.id, saved.get<String>("location") ?: locations.initial.id)).mapNotNull(locations::find).associateBy { it.id },
        now = clock.instant(),
    ))
    val uiState = _uiState.asStateFlow()
    private var searchGeneration = 0
    private var searchJob: Job? = null
    private var observation: Job? = null
    private var refreshJob: Job? = null
    private var generation = 0

    init {
        if (library == null) loadSelection() else restorePersistentState()
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                val now = clock.instant()
                _uiState.update {
                    if (it.snapshot?.let { snapshot -> CachePolicy.expired(snapshot.fetchedAt, now) } == true)
                        it.copy(now = now, snapshot = null, failed = true, failureReason = "expired")
                    else it.copy(now = now)
                }
            }
        }
    }

    fun selectLocation(id: String) {
        if (!uiState.value.initialized) return
        if (uiState.value.hasSelection && id == uiState.value.location.id) return
        val location = locations.find(id) ?: return
        if (library != null) { persist { library.select(location) }; return }
        saved["location"] = id
        _uiState.update { it.copy(location = location, knownLocations = it.knownLocations + (id to location)) }
        loadSelection()
    }

    fun setScenario(scenario: DemoScenario) {
        if (!isDemo) return
        saved["scenario"] = scenario.name
        _uiState.update { it.copy(scenario = scenario) }
        loadSelection()
    }

    private fun loadSelection() {
        generation++
        refreshJob?.cancel()
        observation?.cancel()
        if (!uiState.value.hasSelection) {
            _uiState.update { it.copy(snapshot = null, refreshing = false, contentRead = true, failed = false, failureReason = null, retryAt = null) }
            return
        }
        val locationId = uiState.value.location.id
        val version = generation
        demo.prepare(locationId, uiState.value.scenario)
        _uiState.update { it.copy(snapshot = null, contentRead = false, failed = false, failureReason = null, retryAt = null, refreshing = true, now = clock.instant()) }
        observation = viewModelScope.launch {
            repository.observe(locationId).catch { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                storageFailure()
            }.collect { snapshot ->
                if (version == generation) _uiState.update { it.copy(snapshot = snapshot, contentRead = true, location = snapshot?.zone?.let { zone -> it.location.copy(zone = zone) } ?: it.location) }
            }
        }
        refreshJob = null
        refresh(manual = false)
    }

    fun onForeground() { if (uiState.value.initialized) refresh(manual = false) }
    fun refresh() = refresh(manual = true)
    private fun refresh(manual: Boolean) {
        if (!uiState.value.initialized || !uiState.value.hasSelection || refreshJob?.isActive == true) return
        val version = generation
        val locationId = uiState.value.location.id
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, failed = false) }
            val result = if (manual) repository.refresh(locationId) else repository.refreshIfNeeded(locationId)
            if (version == generation) _uiState.update {
                it.copy(refreshing = false, failed = result != RefreshResult.Updated && result != RefreshResult.Reused, failureReason = (result as? RefreshResult.Unavailable)?.reason, retryAt = (result as? RefreshResult.Unavailable)?.retryAt, now = clock.instant())
            }
        }
    }

    fun search(query: String) {
        val version = ++searchGeneration
        searchJob?.cancel()
        _uiState.update { it.copy(query = query, searchResults = emptyList(), searching = query.trim().length >= 3, searchFailed = false) }
        if (query.trim().length < 3) return
        searchJob = viewModelScope.launch {
            delay(350)
            try {
                val results = locations.search(query)
                if (version == searchGeneration) _uiState.update { it.copy(searchResults = results, searching = false) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { if (version == searchGeneration) _uiState.update { it.copy(searching = false, searchFailed = true) } }
        }
    }

    fun setUnit(unit: TemperatureUnit) { if (preferences != null) { persist { preferences.setUnit(unit) }; return }; saved["unit"] = unit.name; _uiState.update { it.copy(unit = unit) } }
    fun setTheme(theme: ThemeMode) { if (preferences != null) { persist { preferences.setTheme(theme) }; return }; saved["theme"] = theme.name; _uiState.update { it.copy(theme = theme) } }
    fun toggleFavorite(id: String) {
        if (library != null) { persist { library.toggleFavorite(id) }; return }
        val current = uiState.value.favorites
        val next = if (id in current) current - id else if (current.size < 5) current + id else current
        saved["favorites"] = ArrayList(next)
        _uiState.update { it.copy(favorites = next) }
    }

    private fun restorePersistentState() {
        viewModelScope.launch {
            try {
                val restored = requireNotNull(library).library.first()
                val settings = requireNotNull(preferences).preferences.first()
                applyLibrary(restored)
                _uiState.update { it.copy(unit = settings.unit, theme = settings.theme, initialized = true) }
                loadSelection()
                launch {
                    library.library.catch { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        storageFailure()
                    }.collect { next ->
                        val changed = next.selected?.id != uiState.value.location.takeIf { uiState.value.hasSelection }?.id
                        applyLibrary(next)
                        if (changed) loadSelection()
                    }
                }
                launch {
                    preferences.preferences.catch { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        storageFailure()
                    }.collect { next -> _uiState.update { it.copy(unit = next.unit, theme = next.theme) } }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { storageFailure() }
        }
    }
    private fun applyLibrary(value: Library) {
        val known = value.favorites + listOfNotNull(value.selected)
        locations.remember(known)
        _uiState.update { it.copy(location = value.selected ?: locations.initial, hasSelection = value.selected != null, favorites = value.favorites.map { city -> city.id }, knownLocations = known.associateBy { city -> city.id }) }
    }
    fun clearCities() {
        val durable = library ?: return
        persist {
            generation++
            refreshJob?.cancel()
            observation?.cancel()
            try {
                durable.clearCities()
                search("")
                applyLibrary(Library(null, emptyList()))
                loadSelection()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                // Keep the committed city usable when deletion fails.
                loadSelection()
                throw error
            }
        }
    }
    private val writes = kotlinx.coroutines.sync.Mutex()
    private fun persist(action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.lock()
            try { action(); _uiState.update { it.copy(storageError = false) } }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { storageFailure() }
            finally { writes.unlock() }
        }
    }
    private fun storageFailure() { _uiState.update { it.copy(initialized = true, contentRead = true, storageError = true, refreshing = false, failed = true, failureReason = "storage") } }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default
}
