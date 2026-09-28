package dev.bruze.forekast

import dev.bruze.forekast.core.insights.*
import dev.bruze.forekast.core.model.Condition
import dev.bruze.forekast.core.model.HourWeather
import dev.bruze.forekast.core.model.WeatherSnapshot
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RuleBasedWeatherInsightProviderTest {
    private val now = Instant.parse("2026-09-24T14:10:00Z")
    private val provider = RuleBasedWeatherInsightProvider(Clock.fixed(now, ZoneId.of("UTC")))
    private val start = Instant.parse("2026-09-24T15:00:00Z")

    private fun request(mode: InsightMode = InsightMode.DaySummary, duration: Int? = null, fetchedAt: Instant = now, hours: List<InsightHour> = listOf(
        InsightHour(start, 16.0, 30.0),
        InsightHour(start.plusSeconds(3600), 22.0, 10.0),
        InsightHour(start.plusSeconds(7200), 19.0, 5.0),
    )) = InsightRequest("request-1", "snapshot-1", fetchedAt, "Europe/Madrid", "es", mode, hours, duration)

    @Test fun summaryUsesRequestEvidenceAndCapsExpiryAtForecastFreshness() = runBlocking {
        val fetched = now.minusSeconds(5 * 3600 + 30 * 60)
        val result = provider.generate(request(fetchedAt = fetched)) as InsightResult.Ready
        assertEquals(InsightMethod.Rules, result.payload.method)
        assertEquals("request-1", result.payload.requestId)
        assertEquals("snapshot-1", result.payload.snapshotId)
        assertEquals(fetched.plusSeconds(6 * 3600), result.payload.expiresAt)
        assertTrue(result.payload.insights.single().body.contains("16 a 22 °C"))
        assertEquals(setOf(Evidence(start, EvidenceField.TemperatureC), Evidence(start.plusSeconds(3600), EvidenceField.TemperatureC), Evidence(start, EvidenceField.PrecipitationProbabilityPct)), result.payload.insights.single().evidence.toSet())
    }

    @Test fun staleAndFutureFetchAbstainWithoutEvidence() = runBlocking {
        for (fetched in listOf(now.minusSeconds(6 * 3600), now.plusSeconds(1))) {
            val result = provider.generate(request(fetchedAt = fetched)) as InsightResult.NoInsight
            assertTrue(result.payload.insights.isEmpty())
            assertTrue(result.payload.limitations.isNotEmpty())
        }
    }

    @Test fun missingHoursAreExplicitAndInsufficientSummaryAbstains() = runBlocking {
        val partial = listOf(InsightHour(start, 16.0, 30.0), InsightHour(start.plusSeconds(3600), null, 20.0), InsightHour(start.plusSeconds(7200), 20.0, 10.0))
        val result = provider.generate(request(hours = partial)) as InsightResult.Ready
        assertTrue(result.payload.limitations.single().contains("1 hora"))
        val absent = provider.generate(request(hours = partial.take(2))) as InsightResult.NoInsight
        assertTrue(absent.payload.insights.isEmpty())
    }

    @Test fun walkRanksContiguousWindowsAndExplainsHourlyResolution() = runBlocking {
        val result = provider.generate(request(InsightMode.WalkWindow, 120)) as InsightResult.Ready
        val insight = result.payload.insights.single()
        assertTrue(insight.body.contains("18:00 +02:00"))
        assertEquals(4, insight.evidence.size)
        assertEquals(setOf(start.plusSeconds(3600), start.plusSeconds(7200)), insight.evidence.map { it.timestamp }.toSet())
        assertTrue(result.payload.limitations.any { it.contains("Resolución horaria") })
    }

    @Test fun walkAbstainsOnGapRatherThanInventingIntermediateHour() = runBlocking {
        val hours = listOf(InsightHour(start, 20.0, 0.0), InsightHour(start.plusSeconds(7200), 20.0, 0.0))
        assertTrue(provider.generate(request(InsightMode.WalkWindow, 120, hours = hours)) is InsightResult.NoInsight)
    }

    @Test fun halfHourWindowDoesNotPretendMinuteResolution() = runBlocking {
        val result = provider.generate(request(InsightMode.WalkWindow, 30)) as InsightResult.Ready
        assertEquals(2, result.payload.insights.single().evidence.size)
        assertTrue(result.payload.limitations.any { it.contains("aproximada") })
    }

    @Test fun ambiguousLocalTimeIncludesOffsetInEvidenceText() = runBlocking {
        val fallBack = Instant.parse("2026-10-25T00:00:00Z")
        val localProvider = RuleBasedWeatherInsightProvider(Clock.fixed(fallBack.minusSeconds(1800), ZoneId.of("UTC")))
        val hours = listOf(InsightHour(fallBack, 16.0, 10.0), InsightHour(fallBack.plusSeconds(3600), 17.0, 20.0))
        val insight = (localProvider.generate(request(hours = hours, fetchedAt = fallBack.minusSeconds(1800))) as InsightResult.Ready).payload.insights.single()
        assertTrue(insight.body.contains("02:00 +02:00"))
        assertTrue(insight.body.contains("02:00 +01:00"))
    }

    @Test fun rejectsInvalidBoundaries() = runBlocking {
        val invalid = listOf(
            request(hours = listOf(InsightHour(start, 20.0, 101.0))),
            request(hours = listOf(InsightHour(start, Double.NaN, 20.0))),
            request(hours = listOf(InsightHour(start, 20.0, 20.0), InsightHour(start, 21.0, 20.0))),
            request(InsightMode.WalkWindow, 45),
        )
        invalid.forEach { candidate ->
            try { provider.generate(candidate); fail("Invalid request accepted") } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun snapshotAdapterKeepsCanonicalUnitsAndOmitsMissingZone() {
        val snapshot = WeatherSnapshot("city", now, null, listOf(
            HourWeather(start.plusSeconds(3600), 20.0, 10, Condition.Clear),
            HourWeather(start, 18.0, null, Condition.Clear),
        ), emptyList(), ZoneId.of("Europe/Madrid"))
        val request = InsightRequestFactory.fromSnapshot(snapshot, "r", InsightMode.DaySummary, now)!!
        assertEquals("city:${now.toEpochMilli()}", request.snapshotId)
        assertEquals(listOf(start, start.plusSeconds(3600)), request.hours.map { it.timestamp })
        assertNull(request.hours.first().precipitationProbabilityPct)
        assertNull(InsightRequestFactory.fromSnapshot(snapshot.copy(zone = null), "r", InsightMode.DaySummary, now))
    }
}
