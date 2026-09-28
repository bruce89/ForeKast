package dev.bruze.forekast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.core.ports.*
import dev.bruze.forekast.data.demo.*
import dev.bruze.forekast.feature.weather.WeatherViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val clock = Clock.fixed(Instant.parse("2026-09-24T14:00:00Z"), ZoneOffset.UTC)
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(handle: SavedStateHandle = SavedStateHandle()): Pair<WeatherViewModel, CountingRepository> {
        val fake = FakeWeatherRepository(clock)
        val repo = CountingRepository(fake)
        val model = WeatherViewModel(repo, fake, handle, clock)
        store.put("model", model)
        return model to repo
    }

    private fun modelTest(block: suspend TestScope.() -> Unit) = runTest {
        try { block() } finally { store.clear() }
    }

    @Test fun refreshIsDeduplicatedAndPublishesObservedData() = modelTest {
        val (vm, repo) = model()
        runCurrent(); vm.refresh(); vm.refresh()
        advanceTimeBy(651); runCurrent()
        assertEquals(1, repo.calls)
        assertFalse(vm.uiState.value.refreshing)
        assertEquals(24, vm.uiState.value.snapshot!!.hours.size)
    }

    @Test fun aFailedRefreshPreservesTheSavedSnapshot() = modelTest {
        val (vm, _) = model()
        vm.setScenario(DemoScenario.Stale)
        runCurrent()
        val saved = vm.uiState.value.snapshot
        assertNotNull(saved)
        advanceTimeBy(651); runCurrent()
        assertTrue(vm.uiState.value.failed)
        assertEquals(saved, vm.uiState.value.snapshot)
        assertEquals(clock.instant().minusSeconds(7 * 3600), saved!!.fetchedAt)
    }

    @Test fun selectingAnotherCityCancelsTheOldRequest() = modelTest {
        val (vm, _) = model()
        runCurrent(); advanceTimeBy(300)
        vm.selectLocation("madrid")
        runCurrent(); advanceTimeBy(351); runCurrent()
        assertNull(vm.uiState.value.snapshot)
        advanceTimeBy(300); runCurrent()
        assertEquals("madrid", vm.uiState.value.snapshot!!.locationId)
    }

    @Test fun leavingSustainedLoadingRecoversWithoutAnError() = modelTest {
        val (vm, _) = model()
        vm.setScenario(DemoScenario.Loading); runCurrent()
        assertTrue(vm.uiState.value.refreshing)
        vm.setScenario(DemoScenario.Partial); runCurrent()
        advanceTimeBy(651); runCurrent()
        assertFalse(vm.uiState.value.failed)
        assertFalse(vm.uiState.value.refreshing)
        assertNull(vm.uiState.value.snapshot!!.current!!.feelsLikeC)
        assertNull(vm.uiState.value.snapshot!!.hours[2].temperatureC)
    }

    @Test fun changingUnitsDoesNotFetchAndSavesOnlyThePreferenceKey() = modelTest {
        val handle = SavedStateHandle()
        val (vm, repo) = model(handle)
        runCurrent(); advanceTimeBy(651); runCurrent()
        vm.setUnit(TemperatureUnit.Fahrenheit)
        assertEquals(1, repo.calls)
        assertEquals("Fahrenheit", handle.get<String>("unit"))
        assertEquals(18.0, vm.uiState.value.snapshot!!.current!!.temperatureC!!, 0.0)
        assertEquals(64.4, 18.0.inUnit(TemperatureUnit.Fahrenheit), 0.0001)
    }

    @Test fun favoritesHaveALimitAndAreIndependentOfSelection() = modelTest {
        val (vm, _) = model()
        DemoCatalog.locations.forEach { vm.toggleFavorite(it.id) }
        assertEquals(5, vm.uiState.value.favorites.size)
        val selected = vm.uiState.value.location.id
        vm.toggleFavorite(selected)
        assertFalse(selected in vm.uiState.value.favorites)
        assertEquals(selected, vm.uiState.value.location.id)
    }

    @Test fun restoredKeysReconstructASelectionWithoutSerializingWeather() = modelTest {
        val handle = SavedStateHandle(mapOf("location" to "tokyo", "theme" to "Dark", "unit" to "Fahrenheit"))
        val (vm, _) = model(handle)
        assertEquals("tokyo", vm.uiState.value.location.id)
        assertEquals(ThemeMode.Dark, vm.uiState.value.theme)
        assertNull(vm.uiState.value.snapshot)
        runCurrent(); advanceTimeBy(651); runCurrent()
        assertEquals("tokyo", vm.uiState.value.snapshot!!.locationId)
    }

    @Test fun searchDebouncesCancelsAndRejectsShortQueries() = modelTest {
        var calls = 0
        val directory = object : LocationDirectory by DemoLocationDirectory {
            override suspend fun search(query: String): List<Location> {
                calls++; kotlinx.coroutines.delay(500)
                return DemoCatalog.search(query)
            }
        }
        val fake = FakeWeatherRepository(clock)
        val vm = WeatherViewModel(fake, fake, SavedStateHandle(), clock, directory)
        store.put("model", vm)
        vm.search("Mad"); runCurrent(); advanceTimeBy(200)
        vm.search("Tok"); runCurrent(); advanceTimeBy(349); runCurrent()
        assertEquals(0, calls)
        advanceTimeBy(1); runCurrent(); assertEquals(1, calls)
        vm.search("Mo"); runCurrent(); advanceTimeBy(600); runCurrent()
        assertFalse(vm.uiState.value.searching); assertTrue(vm.uiState.value.searchResults.isEmpty())
        vm.search("Madrid"); runCurrent(); advanceTimeBy(851); runCurrent()
        assertEquals("madrid", vm.uiState.value.searchResults.single().id)
        assertEquals(2, calls)
    }

    @Test fun lateNonCooperativeSearchCannotReplaceTheNewQuery() = modelTest {
        val old = kotlinx.coroutines.CompletableDeferred<List<Location>>()
        val directory = object : LocationDirectory by DemoLocationDirectory {
            override suspend fun search(query: String): List<Location> =
                if (query == "Madrid") kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { old.await() }
                else DemoCatalog.search(query)
        }
        val fake = FakeWeatherRepository(clock)
        val vm = WeatherViewModel(fake, fake, SavedStateHandle(), clock, directory)
        store.put("model", vm)
        vm.search("Madrid"); advanceTimeBy(351); runCurrent()
        vm.search("Tokio"); advanceTimeBy(351); runCurrent()
        assertEquals("tokyo", vm.uiState.value.searchResults.single().id)
        old.complete(DemoCatalog.search("Madrid")); runCurrent()
        assertEquals("Tokio", vm.uiState.value.query)
        assertEquals("tokyo", vm.uiState.value.searchResults.single().id)
        assertFalse(vm.uiState.value.searching)
    }
    @Test fun unresolvableRemoteSelectionFallsBackWithoutInventingCoordinates() = modelTest {
        val fake = FakeWeatherRepository(clock)
        val vm = WeatherViewModel(fake, fake, SavedStateHandle(mapOf("location" to "remote-id", "favorites" to arrayListOf("remote-id"))), clock)
        store.put("model", vm)
        assertEquals(DemoCatalog.locations.first(), vm.uiState.value.location)
        assertTrue(vm.uiState.value.favorites.isEmpty())
    }

    private class CountingRepository(private val delegate: WeatherRepository) : WeatherRepository by delegate {
        var calls = 0
        override suspend fun refreshIfNeeded(locationId: String) = refresh(locationId)
        override suspend fun refresh(locationId: String): RefreshResult { calls++; return delegate.refresh(locationId) }
    }
}
