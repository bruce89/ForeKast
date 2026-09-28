package dev.bruze.forekast.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Location(val id: String, val name: String, val region: String, val country: String, val zone: ZoneId, val latitude: Double? = null, val longitude: Double? = null)
enum class Condition { Clear, PartlyCloudy, Drizzle, Cloudy, Rain, Snow, Thunderstorm, Fog, Unknown }
enum class TemperatureUnit { Celsius, Fahrenheit }
enum class ThemeMode { System, Light, Dark }

data class CurrentWeather(
    val validAt: Instant,
    val temperatureC: Double?,
    val feelsLikeC: Double?,
    val humidityPct: Int?,
    val windKmh: Double?,
    val condition: Condition,
    val code: Int? = null,
    val isDay: Boolean? = null,
)
data class HourWeather(val time: Instant, val temperatureC: Double?, val precipitationPct: Int?, val condition: Condition, val code: Int? = null)
data class DayWeather(val date: LocalDate, val lowC: Double?, val highC: Double?, val precipitationPct: Int?, val condition: Condition, val code: Int? = null)
data class WeatherSnapshot(
    val locationId: String,
    val fetchedAt: Instant,
    val current: CurrentWeather?,
    val hours: List<HourWeather>,
    val days: List<DayWeather>,
    val zone: ZoneId? = null,
)

fun Double.inUnit(unit: TemperatureUnit): Double = when (unit) {
    TemperatureUnit.Celsius -> this
    TemperatureUnit.Fahrenheit -> this * 9.0 / 5.0 + 32.0
}
