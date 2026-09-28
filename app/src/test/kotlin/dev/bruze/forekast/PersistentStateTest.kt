package dev.bruze.forekast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.core.ports.*
import dev.bruze.forekast.data.demo.*
import dev.bruze.forekast.feature.weather.WeatherViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.*
import java.time.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PersistentStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private val clock = Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC)
    private val library = object : LibraryRepository {
        override val library = MutableStateFlow(Library(DemoCatalog.find("madrid"), listOf(DemoCatalog.find("tokyo"))))
        override suspend fun clearCities() { library.value = Library(null, emptyList()) }
        override suspend fun select(location: Location) { library.update { it.copy(selected = location) } }
        override suspend fun toggleFavorite(id: String) { library.update { it.copy(favorites = it.favorites.filterNot { c -> c.id == id }) } }
    }
    private class Settings : PreferencesRepository {
        override val preferences = MutableStateFlow(Preferences(TemperatureUnit.Fahrenheit, ThemeMode.Dark))
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        override suspend fun setTheme(value: ThemeMode) {
            gate?.await()
            if (fail) throw java.io.IOException("disk")
            preferences.update { it.copy(theme = value) }
        }
        override suspend fun setUnit(value: TemperatureUnit) { preferences.update { it.copy(unit = value) } }
    }
    private fun model(settings: Settings): Pair<WeatherViewModel, () -> Int> {
        val fake = FakeWeatherRepository(clock)
        var calls = 0
        val repo = object : WeatherRepository by fake {
            override suspend fun refreshIfNeeded(locationId: String): RefreshResult { calls++; return fake.refresh(locationId) }
        }
        val vm = WeatherViewModel(repo, fake, SavedStateHandle(mapOf("location" to "montevideo-uy", "unit" to "Celsius")), clock,
            DemoLocationDirectory, false, library, settings)
        store.put("model", vm)
        return vm to { calls }
    }
    @Test fun emptyLibraryWaitsForAnExplicitSelectionAndClearKeepsPreferences() = runTest {
        try {
            library.library.value = Library(null, emptyList())
            val (vm, calls) = model(Settings()); runCurrent()
            assertTrue(vm.uiState.value.initialized); assertFalse(vm.uiState.value.hasSelection)
            vm.onForeground(); vm.refresh(); runCurrent()
            assertEquals(0, calls()); assertFalse(vm.uiState.value.refreshing)
            // The default catalog city is selectable even though location is the UI fallback.
            vm.selectLocation(DemoCatalog.locations.first().id); runCurrent()
            assertTrue(vm.uiState.value.hasSelection); assertEquals(1, calls())
            vm.clearCities(); runCurrent(); advanceTimeBy(1_000); runCurrent()
            assertFalse(vm.uiState.value.hasSelection); assertNull(vm.uiState.value.snapshot)
            assertFalse(vm.uiState.value.refreshing); assertNull(library.library.value.selected)
            assertEquals(TemperatureUnit.Fahrenheit, vm.uiState.value.unit)
            assertEquals(ThemeMode.Dark, vm.uiState.value.theme)
            vm.onForeground(); runCurrent(); assertEquals(1, calls())
        } finally { store.clear() }
    }
    @Test fun durableStateWinsOverSavedStateAndPreferencesDoNotFetch() = runTest {
        try {
            val (vm, calls) = model(Settings()); runCurrent(); advanceTimeBy(651); runCurrent()
            assertEquals("madrid", vm.uiState.value.location.id)
            assertEquals(listOf("tokyo"), vm.uiState.value.favorites)
            assertEquals(TemperatureUnit.Fahrenheit, vm.uiState.value.unit)
            vm.setUnit(TemperatureUnit.Celsius); vm.toggleFavorite("tokyo"); runCurrent()
            assertEquals(TemperatureUnit.Celsius, vm.uiState.value.unit)
            assertTrue(vm.uiState.value.favorites.isEmpty()); assertEquals(1, calls())
        } finally { store.clear() }
    }
    @Test fun preferenceIsVisibleOnlyAfterCommitAndFailurePreservesIt() = runTest {
        try {
            val settings = Settings(); val (vm, _) = model(settings); runCurrent()
            settings.gate = CompletableDeferred()
            vm.setTheme(ThemeMode.Light); runCurrent()
            assertEquals(ThemeMode.Dark, vm.uiState.value.theme)
            settings.gate!!.complete(Unit); runCurrent()
            assertEquals(ThemeMode.Light, vm.uiState.value.theme)
            settings.fail = true; vm.setTheme(ThemeMode.System); runCurrent()
            assertEquals(ThemeMode.Light, vm.uiState.value.theme); assertTrue(vm.uiState.value.storageError)
        } finally { store.clear() }
    }
}
