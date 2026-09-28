package dev.bruze.forekast.data.demo

import dev.bruze.forekast.core.model.*
import java.time.Clock
import java.time.temporal.ChronoUnit

object WeatherFixtures {
    fun snapshot(location: Location, clock: Clock, partial: Boolean = false, stale: Boolean = false): WeatherSnapshot {
        val now = clock.instant()
        val fetched = if (stale) now.minus(7, ChronoUnit.HOURS) else now
        val nextHour = now.truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS)
        val today = now.atZone(location.zone).toLocalDate()
        val base = when (location.id) { "ushuaia" -> 6.0; "tokyo" -> 24.0; else -> 18.0 }
        return WeatherSnapshot(
            locationId = location.id,
            fetchedAt = fetched,
            current = CurrentWeather(fetched, base, if (partial) null else base - 1, if (partial) null else 65,
                14.0, if (stale) Condition.Cloudy else Condition.Clear),
            hours = List(if (partial) 6 else 24) { index ->
                HourWeather(nextHour.plus(index.toLong(), ChronoUnit.HOURS),
                    if (partial && index == 2) null else base + listOf(0, 1, 2, 3, 2, 1)[index % 6],
                    if (partial && index == 2) null else listOf(10, 15, 20, 25, 30, 20)[index % 6],
                    if (index % 6 < 2) Condition.Clear else if (index % 6 < 4) Condition.Cloudy else Condition.Rain)
            },
            days = List(if (partial) 3 else 7) { index ->
                DayWeather(today.plusDays(index.toLong()), base - 6 + index % 3, base + 3 + index % 3,
                    30 + index * 5, if (index % 2 == 0) Condition.Clear else Condition.Rain)
            },
        )
    }
}
