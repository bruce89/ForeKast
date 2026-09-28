package dev.bruze.forekast

import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.core.ports.*
import dev.bruze.forekast.data.remote.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class OpenMeteoTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC)
    private fun fixture(name: String) = requireNotNull(javaClass.getResource("/open-meteo/$name.json")).readText()
    private fun forecast() = json.decodeFromString<ForecastDto>(fixture("forecast"))
    private fun daily() = json.decodeFromString<ForecastDto>(fixture("daily"))
    private fun map(f: ForecastDto = forecast(), d: ForecastDto = daily()) = WeatherMapper.map("city", f, d, clock.instant())
    private fun kotlinx.coroutines.test.TestScope.client(handler: MockRequestHandler) = HttpClient(MockEngine(MockEngineConfig().apply {
        dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        addHandler(handler)
    })) {
        install(ContentNegotiation) { json(this@OpenMeteoTest.json) }
    }
    private fun invalid(block: () -> Unit) { assertThrows(IllegalArgumentException::class.java, block) }

    @Test fun recordedResponsesPreserveUnitsDatesAndAbsoluteInstants() {
        val f = forecast(); val d = daily(); val result = map(f, d)
        assertEquals(168, result.hours.size)
        assertEquals(7, result.days.size)
        assertEquals(f.current!!.temperature_2m, result.current!!.temperatureC)
        assertEquals(Instant.ofEpochSecond(f.hourly!!.time.first()), result.hours.first().time)
        assertEquals(LocalDate.parse(d.daily!!.time.first()), result.days.first().date)
        assertEquals(ZoneId.of("America/Montevideo"), result.zone)
    }
    @Test fun missingCurrentAndNullMeasurementsRemainPartial() {
        val f = forecast(); val h = f.hourly!!
        val result = map(f.copy(current = null, hourly = h.copy(temperature_2m = List(h.time.size) { null })))
        assertNull(result.current); assertNull(result.hours.first().temperatureC)
    }
    @Test fun rejectsMisalignedArraysInsteadOfTruncating() {
        val f = forecast()
        invalid { map(f.copy(hourly = f.hourly!!.copy(temperature_2m = listOf(2.0)))) }
    }
    @Test fun rejectsBadUnitsProbabilityNonfiniteValuesAndDuplicates() {
        val f = forecast(); val c = f.current!!; val h = f.hourly!!
        invalid { map(f.copy(current_units = f.current_units + ("temperature_2m" to "°F"))) }
        invalid { map(f.copy(current = c.copy(relative_humidity_2m = 101))) }
        invalid { map(f.copy(current = c.copy(temperature_2m = Double.NaN))) }
        invalid { map(f.copy(hourly = h.copy(time = List(h.time.size) { h.time.first() }))) }
        invalid { map(f.copy(current = null, hourly = null), daily().copy(daily = null)) }
    }
    @Test fun wmoGroupsAndUnknownCodesArePreserved() {
        val groups = mapOf(0 to Condition.Clear, 1 to Condition.PartlyCloudy, 2 to Condition.PartlyCloudy, 3 to Condition.Cloudy,
            45 to Condition.Fog, 48 to Condition.Fog, 51 to Condition.Drizzle, 53 to Condition.Drizzle, 55 to Condition.Drizzle,
            56 to Condition.Drizzle, 57 to Condition.Drizzle, 61 to Condition.Rain, 63 to Condition.Rain, 65 to Condition.Rain,
            66 to Condition.Rain, 67 to Condition.Rain, 80 to Condition.Rain, 81 to Condition.Rain, 82 to Condition.Rain,
            71 to Condition.Snow, 73 to Condition.Snow, 75 to Condition.Snow, 77 to Condition.Snow, 85 to Condition.Snow,
            86 to Condition.Snow, 95 to Condition.Thunderstorm, 96 to Condition.Thunderstorm, 99 to Condition.Thunderstorm)
        groups.forEach { (code, expected) -> assertEquals(expected, WeatherMapper.condition(code)) }
        val result = map(forecast().let { it.copy(current = it.current!!.copy(weather_code = 1234)) })
        assertEquals(Condition.Unknown, result.current!!.condition); assertEquals(1234, result.current.code)
    }
    @Test fun fallBackRetainsBothRepeatedHoursAndDailyDates() {
        val zone = ZoneId.of("Europe/Madrid")
        val times = listOf(Instant.parse("2026-10-25T00:00:00Z"), Instant.parse("2026-10-25T01:00:00Z"))
        val f = ForecastDto(zone.id, hourly_units = mapOf("time" to "unixtime"), hourly = HourlyDto(times.map { it.epochSecond }))
        val d = ForecastDto(zone.id, daily_units = mapOf("time" to "iso8601"), daily = DailyDto(listOf("2026-10-24", "2026-10-25", "2026-10-26")))
        val result = map(f, d)
        assertEquals(2, result.hours.map { it.time.atZone(zone).hour }.distinct().single())
        assertEquals(2, result.hours.map { it.time.atZone(zone).offset }.distinct().size)
        assertEquals(d.daily!!.time, result.days.map { it.date.toString() })
    }
    @Test fun springForwardSkipsTheMissingHour() {
        val times = listOf(Instant.parse("2026-03-29T00:00:00Z"), Instant.parse("2026-03-29T01:00:00Z"))
        val f = ForecastDto("Europe/Madrid", hourly_units = mapOf("time" to "unixtime"), hourly = HourlyDto(times.map { it.epochSecond }))
        val result = map(f, ForecastDto("Europe/Madrid"))
        assertEquals(listOf(1, 3), result.hours.map { it.time.atZone(result.zone).hour })
    }
    @Test fun searchEncodesQueryAndHandlesAbsentResults() = runTest {
        var calls = 0
        client { request ->
            calls++; assertEquals("San José & Sol", request.url.parameters["name"])
            respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }.use { http ->
            val directory = RemoteLocationDirectory(OpenMeteoClient(http, clock))
            assertTrue(directory.search("ab").isEmpty()); assertEquals(0, calls)
            assertTrue(directory.search("San José & Sol").isEmpty()); assertEquals(1, calls)
        }
    }
    @Test fun recordedSearchKeepsHomonymsAndCoordinates() = runTest {
        client { respond(fixture("search"), headers = headersOf(HttpHeaders.ContentType, "application/json")) }.use { http ->
            val directory = RemoteLocationDirectory(OpenMeteoClient(http, clock))
            val results = directory.search("Montevideo")
            assertTrue(results.size > 1)
            assertEquals(results.size, results.map { it.id }.distinct().size)
            results.forEach { assertNotNull(it.latitude); assertEquals(it, directory.find(it.id)) }
        }
    }
    @Test fun rateLimitBlocksManualRetryButNotTheOtherHost() = runTest {
        var calls = 0
        client { request ->
            calls++
            if (request.url.host.startsWith("geocoding")) respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            else respond("{}", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "120"))
        }.use { http ->
            val api = OpenMeteoClient(http, clock); val directory = RemoteLocationDirectory(api)
            repeat(2) {
                try { api.forecast(directory.initial); fail() } catch (e: RemoteFailure) {
                    assertEquals(RemoteFailure.Kind.RateLimited, e.kind); assertEquals(clock.instant().plusSeconds(120), e.retryAt)
                }
            }
            assertEquals(1, calls); api.search("Madrid"); assertEquals(2, calls)
        }
    }
    @Test fun transientServerFailureRetriesOnce() = runTest {
        var calls = 0
        client {
            calls++
            if (calls == 1) respond("{}", HttpStatusCode.ServiceUnavailable)
            else respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }.use { http -> OpenMeteoClient(http, clock).search("Madrid"); assertEquals(2, calls) }
    }
    @Test fun malformedJsonDoesNotRetry() = runTest {
        var calls = 0
        client { calls++; respond("broken", headers = headersOf(HttpHeaders.ContentType, "application/json")) }.use { http ->
            try { OpenMeteoClient(http, clock).search("Madrid"); fail() }
            catch (e: RemoteFailure) { assertEquals(RemoteFailure.Kind.InvalidResponse, e.kind) }
            assertEquals(1, calls)
        }
    }
    @Test fun cancellationPropagatesWithoutRetry() = runTest {
        var calls = 0
        client { calls++; throw CancellationException("test") }.use { http ->
            try { OpenMeteoClient(http, clock).search("Madrid"); fail() } catch (_: CancellationException) { }
            assertEquals(1, calls)
        }
    }
    @Test fun repositoryPreservesSnapshotWhenNewResponseIsInvalid() = runTest {
        var corrupt = false
        client { request ->
            val body = if (corrupt) "{}" else fixture(if (request.url.parameters["daily"] != null) "daily" else "forecast")
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }.use { http ->
            val api = OpenMeteoClient(http, clock); val directory = RemoteLocationDirectory(api)
            val repo = RemoteWeatherRepository(api, directory, clock); val id = directory.initial.id
            assertEquals(RefreshResult.Updated, repo.refresh(id))
            val snapshot = repo.observe(id).first(); corrupt = true
            assertTrue(repo.refresh(id) is RefreshResult.Unavailable)
            assertEquals(snapshot, repo.observe(id).first())
        }
    }
    @Test fun transportFailureRetriesOnceAndClientErrorNeverRetries() = runTest {
        var calls = 0
        client { calls++; throw java.io.IOException("offline") }.use { http ->
            try { OpenMeteoClient(http, clock).search("Madrid"); fail() }
            catch (e: RemoteFailure) { assertEquals(RemoteFailure.Kind.Network, e.kind) }
            assertEquals(2, calls)
        }
        calls = 0
        client { calls++; respond("{}", HttpStatusCode.BadRequest) }.use { http ->
            try { OpenMeteoClient(http, clock).search("Madrid"); fail() }
            catch (e: RemoteFailure) { assertEquals(RemoteFailure.Kind.Server, e.kind) }
            assertEquals(1, calls)
        }
    }
    @Test fun totalDeadlineStopsAnUnresponsiveRequest() = runTest {
        var calls = 0
        client { calls++; awaitCancellation() }.use { http ->
            try { OpenMeteoClient(http, clock).search("Madrid"); fail() }
            catch (e: RemoteFailure) { assertEquals(RemoteFailure.Kind.Network, e.kind) }
            assertEquals(1, calls)
        }
    }

    @Test fun retryAfterAcceptsSecondsHttpDateAndSafeFallback() {
        val now = Instant.parse("2026-09-25T12:00:00Z")
        assertEquals(now.plusSeconds(120), retryAfter("120", now))
        assertEquals(now.plusSeconds(120), retryAfter("Fri, 25 Sep 2026 12:02:00 GMT", now))
        assertEquals(now.plusSeconds(900), retryAfter("garbage", now))
        assertEquals(now.plusSeconds(900), retryAfter("-1", now))
    }
}
