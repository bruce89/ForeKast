package dev.bruze.forekast

import dev.bruze.forekast.data.demo.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class FixturesTest {
    @Test fun fixtureDatesBelongToTheCityNotTheDevice() {
        val clock = Clock.fixed(Instant.parse("2026-09-24T23:30:00Z"), ZoneOffset.UTC)
        val tokyo = WeatherFixtures.snapshot(DemoCatalog.find("tokyo"), clock)
        assertEquals("2026-09-25", tokyo.days.first().date.toString())
        assertEquals(Instant.parse("2026-09-25T00:00:00Z"), tokyo.hours.first().time)
        assertEquals(24, tokyo.hours.map { it.time }.distinct().size)
    }
    @Test fun localSearchDistinguishesNamesAndNormalizesAccents() {
        assertEquals(2, DemoCatalog.search("Montevideo").size)
        assertEquals("ushuaia", DemoCatalog.search("ushuaía").single().id)
        assertTrue(DemoCatalog.search("mo").isEmpty())
    }
}
