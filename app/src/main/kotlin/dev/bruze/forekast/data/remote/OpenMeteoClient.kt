package dev.bruze.forekast.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import dev.bruze.forekast.core.model.Location
import dev.bruze.forekast.core.ports.CooldownStore
import dev.bruze.forekast.core.ports.MemoryCooldownStore

class RemoteFailure(val kind: Kind, val retryAt: Instant? = null, cause: Throwable? = null) : Exception(kind.name, cause) {
    enum class Kind { Network, RateLimited, Server, InvalidResponse }
}

fun weatherHttpClient() = HttpClient(OkHttp) {
    expectSuccess = false
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) { connectTimeoutMillis = 5_000; requestTimeoutMillis = 8_000; socketTimeoutMillis = 8_000 }
    engine { config { retryOnConnectionFailure(false) } }
}

/** One retry authority; serialized per host so queued calls respect a received 429. */
class OpenMeteoClient(val client: HttpClient, private val clock: Clock, private val cooldown: CooldownStore = MemoryCooldownStore()) {
    private val forecastGate = Mutex()
    private val searchGate = Mutex()

    suspend fun search(query: String): GeocodingDto = deadline {
        request("geocoding-api.open-meteo.com", "/v1/search", mapOf("name" to query, "count" to "10", "language" to "es", "format" to "json"))
    }
    suspend fun forecast(location: Location): Pair<ForecastDto, ForecastDto> = deadline {
        val coordinates = mapOf("latitude" to requireNotNull(location.latitude).toString(),
            "longitude" to requireNotNull(location.longitude).toString(), "timezone" to location.zone.id,
            "forecast_days" to "7", "temperature_unit" to "celsius", "wind_speed_unit" to "kmh")
        // Absolute instants for hourly data; explicit ISO local dates for daily aggregates.
        val forecast: ForecastDto = request("api.open-meteo.com", "/v1/forecast", coordinates + mapOf(
            "timeformat" to "unixtime", "current" to "temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m",
            "hourly" to "temperature_2m,precipitation_probability,weather_code"))
        val daily: ForecastDto = request("api.open-meteo.com", "/v1/forecast", coordinates + mapOf(
            "timeformat" to "iso8601", "daily" to "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"))
        forecast to daily
    }
    private suspend fun <T : Any> deadline(block: suspend () -> T): T =
        withTimeoutOrNull(20_000) { block() } ?: throw RemoteFailure(RemoteFailure.Kind.Network)

    private suspend inline fun <reified T> request(host: String, path: String, parameters: Map<String, String>): T {
        val gate = if (host == "api.open-meteo.com") forecastGate else searchGate
        return gate.withLock {
            cooldown.read(host)?.let { if (clock.instant().isBefore(it)) throw RemoteFailure(RemoteFailure.Kind.RateLimited, it) }
            var attempts = 0
            while (true) {
                try {
                    val response = client.get("https://$host$path") { parameters.forEach { (key, value) -> parameter(key, value) } }
                    val status = response.status.value
                    if (status == 429) {
                        val until = retryAfter(response.headers["Retry-After"], clock.instant())
                        cooldown.write(host, until)
                        throw RemoteFailure(RemoteFailure.Kind.RateLimited, until)
                    }
                    if (status in listOf(502, 503, 504) && attempts++ == 0) { delay(750); continue }
                    if (status !in 200..299) throw RemoteFailure(RemoteFailure.Kind.Server)
                    return@withLock try { response.body<T>() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: IOException) { throw failure }
                    catch (failure: Exception) { throw RemoteFailure(RemoteFailure.Kind.InvalidResponse, cause = failure) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: IOException) {
                    if (attempts++ == 0) { delay(750); continue }
                    throw RemoteFailure(RemoteFailure.Kind.Network, cause = failure)
                }
            }
            @Suppress("UNREACHABLE_CODE") error("Unreachable")
        }
    }
}

internal fun retryAfter(value: String?, now: Instant): Instant {
    val seconds = value?.toLongOrNull()?.takeIf { it >= 0 }
    val parsed = runCatching {
        if (seconds != null) now.plusSeconds(seconds)
        else value?.let { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
    }.getOrNull()
    return parsed?.takeIf { !it.isBefore(now) } ?: now.plusSeconds(15 * 60)
}
