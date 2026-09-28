package dev.bruze.forekast.core.insights

import java.time.Instant

/** Puerto común para reglas locales y un posible adaptador HTTP futuro. */
interface WeatherInsightProvider {
    suspend fun generate(request: InsightRequest): InsightResult
}

data class InsightRequest(
    val requestId: String,
    val snapshotId: String,
    val fetchedAt: Instant,
    val timezone: String,
    val locale: String,
    val mode: InsightMode,
    val hours: List<InsightHour>,
    val walkDurationMinutes: Int? = null,
    val schemaVersion: String = "1.0",
)

enum class InsightMode { DaySummary, WalkWindow }

data class InsightHour(
    val timestamp: Instant,
    val temperatureC: Double?,
    val precipitationProbabilityPct: Double?,
)

data class Evidence(val timestamp: Instant, val field: EvidenceField)
enum class EvidenceField { TemperatureC, PrecipitationProbabilityPct }
data class Insight(val id: String, val title: String, val body: String, val evidence: List<Evidence>)
enum class InsightMethod { Rules, Llm }

data class InsightPayload(
    val requestId: String,
    val snapshotId: String,
    val generatedAt: Instant,
    val expiresAt: Instant,
    val method: InsightMethod,
    val insights: List<Insight>,
    val limitations: List<String>,
)

sealed interface InsightResult {
    data class Ready(val payload: InsightPayload) : InsightResult
    data class NoInsight(val payload: InsightPayload) : InsightResult
    data object Disabled : InsightResult
    data class Unavailable(val reason: InsightFailure, val retryAt: Instant? = null) : InsightResult
}

enum class InsightFailure { Network, Timeout, RateLimited, InvalidResponse, Unauthorized, Service }

// El adapter mapea enums a nombres wire explícitos: day_summary, walk_window, etc.
// CancellationException se propaga; no se transforma en Unavailable.
