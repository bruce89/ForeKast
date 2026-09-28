package dev.bruze.forekast.core.model

import java.time.Duration
import java.time.Instant

object CachePolicy {
    fun age(fetchedAt: Instant, now: Instant): Duration = Duration.between(fetchedAt, now)
    fun shouldRefresh(fetchedAt: Instant?, now: Instant): Boolean = fetchedAt == null ||
        age(fetchedAt, now).let { it.isNegative || it >= Duration.ofMinutes(30) }
    fun expired(fetchedAt: Instant, now: Instant) = age(fetchedAt, now) >= Duration.ofDays(7)
    fun stale(fetchedAt: Instant, now: Instant) = age(fetchedAt, now).let { it.isNegative || it >= Duration.ofHours(6) }
    fun nextAttempt(lastAttempt: Instant?, now: Instant): Instant? = lastAttempt?.let {
        // A backwards clock jump permits one corrective request; its new timestamp starts the throttle again.
        if (!now.isBefore(it) && now.isBefore(it.plusSeconds(60))) it.plusSeconds(60) else null
    }
}
