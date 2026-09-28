package dev.bruze.forekast

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.core.ports.*
import dev.bruze.forekast.data.local.*
import dev.bruze.forekast.data.remote.*
import dev.bruze.forekast.data.demo.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.time.*
import java.util.UUID
import org.junit.*
import org.junit.Assert.*

class PersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-" + UUID.randomUUID() + ".db"
    private lateinit var db: ForeKastDatabase
    private val clock = Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC)
    private val city = DemoCatalog.locations.first().copy(latitude = -34.90, longitude = -56.16)
    @Before fun setup() { db = open() }
    @After fun cleanup() { db.close(); context.deleteDatabase(name) }
    private fun open() = Room.databaseBuilder(context, ForeKastDatabase::class.java, name).build()
    private fun fixture(name: String) = InstrumentationRegistry.getInstrumentation().context.assets.open("open-meteo/$name.json").bufferedReader().use { it.readText() }
    private fun client(handler: MockRequestHandler) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    private fun snapshot() = WeatherFixtures.snapshot(city, clock).copy(zone = city.zone)

    @Test fun emptyAndClearedLibraryRemainEmptyAfterReopening() = runBlocking {
        val dao = db.dao(); val library = RoomLibraryRepository(dao)
        assertNull(library.library.first().selected)
        library.select(city); library.toggleFavorite(city.id)
        dao.replace(snapshot().entity()); dao.recordAttempt(AttemptEntity(city.id, clock.millis()))
        val until = clock.instant().plusSeconds(900)
        RoomCooldownStore(dao).write("api.open-meteo.com", until)
        library.clearCities()
        assertEquals(Library(null, emptyList()), library.library.first())
        assertNull(dao.city(city.id)); assertNull(dao.snapshot(city.id)); assertNull(dao.attempt(city.id))
        try { dao.replace(snapshot().entity()); fail("Deleted city must not be resurrected") } catch (_: IllegalStateException) { }
        db.close(); db = open()
        assertEquals(Library(null, emptyList()), RoomLibraryRepository(db.dao()).library.first())
        assertEquals(until, RoomCooldownStore(db.dao()).read("api.open-meteo.com"))
    }
    @Test fun invalidResponseDoesNotReplaceCommittedSnapshot() = runBlocking {
        db.dao().select(city.entity()); db.dao().replace(snapshot().entity())
        client { respond("{broken", headers = headersOf(HttpHeaders.ContentType, "application/json")) }.use { http ->
            val repo = PersistentWeatherRepository(OpenMeteoClient(http, clock), db.dao(), clock)
            assertEquals("invalid", (repo.refresh(city.id) as RefreshResult.Unavailable).reason)
            assertEquals(snapshot(), repo.observe(city.id).first())
        }
    }
    @Test fun reopenedDatabaseRestoresSelectionFavoritesAndWholeForecast() = runBlocking {
        val library = RoomLibraryRepository(db.dao())
        library.select(city); library.toggleFavorite(city.id)
        val second = DemoCatalog.locations[2]
        library.select(second)
        db.dao().replace(snapshot().entity())
        db.close(); db = open()
        val restored = RoomLibraryRepository(db.dao()).library.first()
        assertEquals(second, restored.selected)
        assertEquals(listOf(city), restored.favorites)
        assertEquals(snapshot(), db.dao().snapshot(city.id)!!.model())
    }
    @Test fun failedChildInsertRollsBackHeaderAndAllRows() = runBlocking {
        val dao = db.dao(); dao.initialize(city.entity()); dao.replace(snapshot().entity())
        val before = dao.snapshot(city.id)!!
        val replacement = before.copy(header = before.header.copy(fetchedAt = before.header.fetchedAt + 1000), hours = listOf(before.hours.first(), before.hours.first()))
        try { dao.replace(replacement); fail("Expected duplicate primary key") } catch (_: android.database.SQLException) { }
        assertEquals(before.model(), dao.snapshot(city.id)!!.model())
    }
    @Test fun keepsFiveFavoritesAndOnlyTheActiveExtraCity() = runBlocking {
        val library = RoomLibraryRepository(db.dao()); library.select(city)
        DemoCatalog.locations.take(5).forEach { library.select(it); library.toggleFavorite(it.id) }
        val sixth = DemoCatalog.locations.last(); library.select(sixth); library.toggleFavorite(sixth.id)
        assertEquals(5, library.library.first().favorites.size)
        assertEquals(sixth, library.library.first().selected)
        db.dao().replace(WeatherFixtures.snapshot(sixth, clock).entity())
        library.select(city)
        assertNull(db.dao().city(sixth.id)); assertNull(db.dao().snapshot(sixth.id))
        library.toggleFavorite(city.id)
        assertEquals(city, library.library.first().selected)
        assertNotNull(db.dao().city(city.id))
    }
    @Test fun preferencesSurviveClosingAndReopeningTheirStore() = runBlocking {
        val file = File(context.filesDir, UUID.randomUUID().toString() + ".preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            var preferences = DataStorePreferences(PreferenceDataStoreFactory.create(scope = scope) { file })
            preferences.setUnit(TemperatureUnit.Fahrenheit); preferences.setTheme(ThemeMode.Dark)
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            preferences = DataStorePreferences(PreferenceDataStoreFactory.create(scope = scope) { file })
            assertEquals(Preferences(TemperatureUnit.Fahrenheit, ThemeMode.Dark), preferences.preferences.first())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
    @Test fun automaticRefreshReusesDiskAndManualThrottleSurvivesReopen() = runBlocking {
        var calls = 0
        client { request ->
            calls++
            respond(fixture(if (request.url.parameters["daily"] != null) "daily" else "forecast"), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }.use { http ->
            db.dao().initialize(city.entity())
            var repo = PersistentWeatherRepository(OpenMeteoClient(http, clock, RoomCooldownStore(db.dao())), db.dao(), clock)
            assertEquals(RefreshResult.Updated, repo.refresh(city.id)); assertEquals(2, calls)
            db.close(); db = open()
            repo = PersistentWeatherRepository(OpenMeteoClient(http, clock, RoomCooldownStore(db.dao())), db.dao(), clock)
            assertEquals(RefreshResult.Reused, repo.refreshIfNeeded(city.id)); assertEquals(2, calls)
            assertEquals("throttled", (repo.refresh(city.id) as RefreshResult.Unavailable).reason); assertEquals(2, calls)
            assertNotNull(repo.observe(city.id).first())
        }
    }
    @Test fun anotherCityIsNotDroppedWhileARequestIsInFlight() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        var first = true
        client { request ->
            if (first) { first = false; entered.complete(Unit); release.await() }
            respond(fixture(if (request.url.parameters["daily"] != null) "daily" else "forecast"), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }.use { http ->
            val library = RoomLibraryRepository(db.dao())
            library.select(city); library.toggleFavorite(city.id)
            val other = city.copy(id = "second", name = "Second")
            library.select(other)
            val repo = PersistentWeatherRepository(OpenMeteoClient(http, clock), db.dao(), clock)
            val firstRequest = async { repo.refresh(city.id) }
            entered.await()
            assertEquals(RefreshResult.Reused, repo.refresh(city.id))
            val secondRequest = async(start = CoroutineStart.UNDISPATCHED) { repo.refresh(other.id) }
            try { assertFalse(secondRequest.isCompleted) } finally { release.complete(Unit) }
            assertEquals(RefreshResult.Updated, firstRequest.await())
            assertEquals(RefreshResult.Updated, secondRequest.await())
        }
    }

    @Test fun hostCooldownSurvivesDatabaseAndClientRecreation() = runBlocking {
        var calls = 0
        client { calls++; respond("{}", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "900")) }.use { http ->
            var api = OpenMeteoClient(http, clock, RoomCooldownStore(db.dao()))
            try { api.search("Madrid"); fail() } catch (e: RemoteFailure) { assertEquals(RemoteFailure.Kind.RateLimited, e.kind) }
            db.close(); db = open()
            api = OpenMeteoClient(http, clock, RoomCooldownStore(db.dao()))
            try { api.search("Madrid"); fail() } catch (e: RemoteFailure) { assertEquals(clock.instant().plusSeconds(900), e.retryAt) }
            assertEquals(1, calls)
        }
    }
    @Test fun offlineRefreshPreservesOldDataAndExpiredCacheIsRemoved() = runBlocking {
        client { throw java.io.IOException("offline") }.use { http ->
            db.dao().initialize(city.entity()); db.dao().replace(snapshot().entity())
            val later = Clock.offset(clock, Duration.ofHours(7))
            val repo = PersistentWeatherRepository(OpenMeteoClient(http, later), db.dao(), later)
            assertEquals("network", (repo.refreshIfNeeded(city.id) as RefreshResult.Unavailable).reason)
            assertEquals(snapshot(), repo.observe(city.id).first())
            val expiredClock = Clock.offset(clock, Duration.ofDays(7))
            val expired = PersistentWeatherRepository(OpenMeteoClient(http, expiredClock), db.dao(), expiredClock)
            assertNull(expired.observe(city.id).first())
            expired.refreshIfNeeded(city.id)
            assertNull(db.dao().snapshot(city.id))
            assertNotNull(db.dao().city(city.id))
        }
    }
}
