package dev.bruze.forekast.data.local

import dev.bruze.forekast.core.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun Location.entity() = CityEntity(id, name, region, country, zone.id, latitude, longitude)
fun CityEntity.model() = Location(id, name, region, country, ZoneId.of(zone), latitude, longitude)
private fun condition(value: String) = Condition.entries.firstOrNull { it.name == value } ?: Condition.Unknown
fun WeatherSnapshot.entity(): StoredSnapshot = StoredSnapshot(
    SnapshotEntity(locationId, fetchedAt.toEpochMilli(), zone?.id, current?.let {
        CurrentEntity(it.validAt.toEpochMilli(), it.temperatureC, it.feelsLikeC, it.humidityPct, it.windKmh, it.condition.name, it.code, it.isDay)
    }),
    hours.map { HourEntity(locationId, it.time.toEpochMilli(), it.temperatureC, it.precipitationPct, it.condition.name, it.code) },
    days.map { DayEntity(locationId, it.date.toString(), it.lowC, it.highC, it.precipitationPct, it.condition.name, it.code) },
)
fun StoredSnapshot.model() = WeatherSnapshot(header.locationId, Instant.ofEpochMilli(header.fetchedAt), header.current?.let {
    CurrentWeather(Instant.ofEpochMilli(it.time), it.temperature, it.feelsLike, it.humidity, it.wind, condition(it.condition), it.code, it.isDay)
}, hours.sortedBy { it.time }.map { HourWeather(Instant.ofEpochMilli(it.time), it.temperature, it.precipitation, condition(it.condition), it.code) },
    days.sortedBy { it.date }.map { DayWeather(LocalDate.parse(it.date), it.low, it.high, it.precipitation, condition(it.condition), it.code) }, header.zone?.let(ZoneId::of))
