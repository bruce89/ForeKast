package dev.bruze.forekast.data.remote

import dev.bruze.forekast.core.model.WeatherSnapshot
import dev.bruze.forekast.core.ports.*
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteWeatherRepository(private val api: OpenMeteoClient, private val locations: LocationDirectory,
    private val clock: Clock) : WeatherRepository {
    private val snapshots = MutableStateFlow<Map<String, WeatherSnapshot>>(emptyMap())
    private val gate = Mutex()
    override fun observe(locationId: String) = snapshots.map { it[locationId] }
    override suspend fun refresh(locationId: String): RefreshResult = gate.withLock {
        val location = locations.find(locationId) ?: return@withLock RefreshResult.Failed
        try {
            val (forecast, daily) = api.forecast(location)
            val snapshot = WeatherMapper.map(locationId, forecast, daily, clock.instant())
            snapshots.update { it + (locationId to snapshot) }
            RefreshResult.Updated
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: RemoteFailure) {
            RefreshResult.Unavailable(if (failure.kind == RemoteFailure.Kind.RateLimited) "rate_limit" else if (failure.kind == RemoteFailure.Kind.InvalidResponse) "invalid" else "network", failure.retryAt)
        } catch (_: IllegalArgumentException) { RefreshResult.Unavailable("invalid") }
        catch (_: java.time.DateTimeException) { RefreshResult.Unavailable("invalid") }
    }
}
