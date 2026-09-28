package dev.bruze.forekast.data.remote

import kotlinx.serialization.Serializable

// Names deliberately mirror the wire contract; domain names live in the mapper.
@Serializable data class ForecastDto(
    val timezone: String,
    val current_units: Map<String, String> = emptyMap(),
    val hourly_units: Map<String, String> = emptyMap(),
    val daily_units: Map<String, String> = emptyMap(),
    val current: CurrentDto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null,
)
@Serializable data class CurrentDto(
    val time: Long,
    val temperature_2m: Double? = null,
    val apparent_temperature: Double? = null,
    val relative_humidity_2m: Int? = null,
    val wind_speed_10m: Double? = null,
    val weather_code: Int? = null,
    val is_day: Int? = null,
)
@Serializable data class HourlyDto(
    val time: List<Long>,
    val temperature_2m: List<Double?>? = null,
    val precipitation_probability: List<Int?>? = null,
    val weather_code: List<Int?>? = null,
)
@Serializable data class DailyDto(
    val time: List<String>,
    val temperature_2m_min: List<Double?>? = null,
    val temperature_2m_max: List<Double?>? = null,
    val precipitation_probability_max: List<Int?>? = null,
    val weather_code: List<Int?>? = null,
)
@Serializable data class GeocodingDto(val results: List<PlaceDto> = emptyList())
@Serializable data class PlaceDto(
    val id: Long, val name: String, val latitude: Double, val longitude: Double,
    val timezone: String? = null, val admin1: String = "", val country: String = "",
)
