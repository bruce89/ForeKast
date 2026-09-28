package dev.bruze.forekast.data.remote

import dev.bruze.forekast.core.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class InvalidWeatherResponse : IllegalArgumentException("Invalid weather response")

object WeatherMapper {
    fun map(locationId: String, forecast: ForecastDto, dailyResponse: ForecastDto, fetchedAt: Instant): WeatherSnapshot {
        val zone = ZoneId.of(forecast.timezone)
        valid(ZoneId.of(dailyResponse.timezone) == zone)
        val current = forecast.current?.let { c ->
            unit(forecast.current_units, "time", "unixtime")
            unitIf(c.temperature_2m, forecast.current_units, "temperature_2m", "°C")
            unitIf(c.apparent_temperature, forecast.current_units, "apparent_temperature", "°C")
            unitIf(c.relative_humidity_2m, forecast.current_units, "relative_humidity_2m", "%")
            unitIf(c.wind_speed_10m, forecast.current_units, "wind_speed_10m", "km/h")
            unitIf(c.weather_code, forecast.current_units, "weather_code", "wmo code")
            unitIf(c.is_day, forecast.current_units, "is_day", "")
            valid(c.is_day == null || c.is_day in 0..1)
            valid(c.wind_speed_10m == null || c.wind_speed_10m >= 0)
            CurrentWeather(Instant.ofEpochSecond(c.time), finite(c.temperature_2m), finite(c.apparent_temperature),
                percent(c.relative_humidity_2m), finite(c.wind_speed_10m), condition(c.weather_code), c.weather_code, c.is_day?.let { it == 1 })
        }
        val hours = forecast.hourly?.let { h ->
            aligned(h.time, h.temperature_2m, h.precipitation_probability, h.weather_code)
            ordered(h.time)
            unitIf(h.weather_code, forecast.hourly_units, "weather_code", "wmo code")
            unit(forecast.hourly_units, "time", "unixtime")
            unitIf(h.temperature_2m, forecast.hourly_units, "temperature_2m", "°C")
            unitIf(h.precipitation_probability, forecast.hourly_units, "precipitation_probability", "%")
            h.time.indices.map { i -> HourWeather(Instant.ofEpochSecond(h.time[i]), finite(h.temperature_2m?.get(i)),
                percent(h.precipitation_probability?.get(i)), condition(h.weather_code?.get(i)), h.weather_code?.get(i)) }
        }.orEmpty()
        val days = dailyResponse.daily?.let { d ->
            aligned(d.time, d.temperature_2m_min, d.temperature_2m_max, d.precipitation_probability_max, d.weather_code)
            unit(dailyResponse.daily_units, "time", "iso8601")
            unitIf(d.temperature_2m_min, dailyResponse.daily_units, "temperature_2m_min", "°C")
            unitIf(d.temperature_2m_max, dailyResponse.daily_units, "temperature_2m_max", "°C")
            unitIf(d.precipitation_probability_max, dailyResponse.daily_units, "precipitation_probability_max", "%")
            unitIf(d.weather_code, dailyResponse.daily_units, "weather_code", "wmo code")
            val dates = d.time.map(LocalDate::parse)
            ordered(dates)
            dates.indices.map { i ->
                val low = finite(d.temperature_2m_min?.get(i)); val high = finite(d.temperature_2m_max?.get(i))
                valid(low == null || high == null || low <= high)
                DayWeather(dates[i], low, high, percent(d.precipitation_probability_max?.get(i)), condition(d.weather_code?.get(i)), d.weather_code?.get(i))
            }
        }.orEmpty()
        valid(current != null || hours.isNotEmpty() || days.isNotEmpty())
        return WeatherSnapshot(locationId, fetchedAt, current, hours, days, zone)
    }

    fun condition(code: Int?): Condition = when (code) {
        0 -> Condition.Clear
        1, 2 -> Condition.PartlyCloudy
        3 -> Condition.Cloudy
        45, 48 -> Condition.Fog
        51, 53, 55, 56, 57 -> Condition.Drizzle
        61, 63, 65, 66, 67, 80, 81, 82 -> Condition.Rain
        71, 73, 75, 77, 85, 86 -> Condition.Snow
        95, 96, 99 -> Condition.Thunderstorm
        else -> Condition.Unknown
    }
    private fun valid(value: Boolean) { if (!value) throw InvalidWeatherResponse() }
    private fun finite(value: Double?): Double? { valid(value == null || value.isFinite()); return value }
    private fun percent(value: Int?): Int? { valid(value == null || value in 0..100); return value }
    private fun aligned(time: List<*>, vararg arrays: List<*>?) { arrays.forEach { valid(it == null || it.size == time.size) } }
    private fun <T : Comparable<T>> ordered(values: List<T>) { valid(values.zipWithNext().all { (a, b) -> a < b }) }
    private fun unit(units: Map<String, String>, key: String, expected: String) { valid(units[key] == expected) }
    private fun unitIf(value: Any?, units: Map<String, String>, key: String, expected: String) { if (value != null) unit(units, key, expected) }
}
