package dev.bruze.forekast.core.ports

import dev.bruze.forekast.core.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun observe(locationId: String): Flow<WeatherSnapshot?>
    suspend fun refresh(locationId: String): RefreshResult
    suspend fun refreshIfNeeded(locationId: String): RefreshResult = refresh(locationId)
}

sealed interface RefreshResult {
    data object Updated : RefreshResult
    data object Reused : RefreshResult
    data object Failed : RefreshResult
    data class Unavailable(val reason: String, val retryAt: java.time.Instant? = null) : RefreshResult
}
