package dev.bruze.forekast.data.local

import dev.bruze.forekast.core.model.CachePolicy
import dev.bruze.forekast.core.ports.*
import dev.bruze.forekast.data.remote.*
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/** Room is the sole observable source; no network value bypasses a successful commit. */
class PersistentWeatherRepository(private val api: OpenMeteoClient, private val dao: ForeKastDao,
    private val clock: Clock) : WeatherRepository {
    private val active = ConcurrentHashMap.newKeySet<String>()
    override fun observe(locationId: String) = dao.observeSnapshot(locationId).map { stored ->
        stored?.model()?.takeUnless { CachePolicy.expired(it.fetchedAt, clock.instant()) }
    }
    override suspend fun refresh(locationId: String) = update(locationId, manual = true)
    override suspend fun refreshIfNeeded(locationId: String) = update(locationId, manual = false)
    private suspend fun update(id: String, manual: Boolean): RefreshResult {
        if (!active.add(id)) return RefreshResult.Reused
        try {
            val now = clock.instant()
            dao.purgeExpired(now.minus(Duration.ofDays(7)).toEpochMilli())
            val stored = dao.snapshot(id)
            if (!manual && !CachePolicy.shouldRefresh(stored?.header?.fetchedAt?.let(Instant::ofEpochMilli), now)) return RefreshResult.Reused
            CachePolicy.nextAttempt(dao.attempt(id)?.attemptedAt?.let(Instant::ofEpochMilli), now)?.let {
                return RefreshResult.Unavailable("throttled", it)
            }
            val city = dao.city(id)?.model() ?: return RefreshResult.Unavailable("storage")
            dao.recordAttempt(AttemptEntity(id, now.toEpochMilli()))
            val (forecast, daily) = api.forecast(city)
            val snapshot = WeatherMapper.map(id, forecast, daily, clock.instant())
            dao.replace(snapshot.entity())
            return RefreshResult.Updated
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: RemoteFailure) {
            return RefreshResult.Unavailable(if (failure.kind == RemoteFailure.Kind.RateLimited) "rate_limit" else if (failure.kind == RemoteFailure.Kind.InvalidResponse) "invalid" else "network", failure.retryAt)
        } catch (_: android.database.SQLException) { return RefreshResult.Unavailable("storage") }
        catch (_: java.io.IOException) { return RefreshResult.Unavailable("storage") }
        catch (_: IllegalStateException) { return RefreshResult.Unavailable("storage") }
        catch (_: IllegalArgumentException) { return RefreshResult.Unavailable("invalid") }
        catch (_: java.time.DateTimeException) { return RefreshResult.Unavailable("invalid") }
        finally { active.remove(id) }
    }
}
