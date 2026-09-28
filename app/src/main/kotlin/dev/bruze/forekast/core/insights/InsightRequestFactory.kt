package dev.bruze.forekast.core.insights

import dev.bruze.forekast.core.model.WeatherSnapshot
import java.time.Instant

/** Traduce el modelo meteorológico a la entrada mínima del motor; no hace I/O. */
object InsightRequestFactory {
    fun fromSnapshot(
        snapshot: WeatherSnapshot,
        requestId: String,
        mode: InsightMode,
        now: Instant,
        locale: String = "es",
        walkDurationMinutes: Int? = null,
    ): InsightRequest? {
        val zone = snapshot.zone ?: return null
        val hours = snapshot.hours.asSequence()
            .filter { it.time.isAfter(now.minusSeconds(300)) }
            .sortedBy { it.time }
            .distinctBy { it.time }
            .take(24)
            .map { InsightHour(it.time, it.temperatureC, it.precipitationPct?.toDouble()) }
            .toList()
        if (hours.isEmpty()) return null
        return InsightRequest(
            requestId = requestId,
            snapshotId = "${snapshot.locationId}:${snapshot.fetchedAt.toEpochMilli()}",
            fetchedAt = snapshot.fetchedAt,
            timezone = zone.id,
            locale = locale,
            mode = mode,
            hours = hours,
            walkDurationMinutes = walkDurationMinutes,
        )
    }
}
