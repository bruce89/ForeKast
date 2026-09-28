package dev.bruze.forekast.core.insights

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Baseline determinista. No consulta red, almacenamiento ni modelos de IA. */
class RuleBasedWeatherInsightProvider(private val clock: Clock) : WeatherInsightProvider {
    override suspend fun generate(request: InsightRequest): InsightResult {
        val now = clock.instant()
        validate(request, now)
        val spanish = request.locale == "es"
        val zone = ZoneId.of(request.timezone)
        if (request.fetchedAt.isAfter(now) || !request.fetchedAt.plusSeconds(6 * 3600).isAfter(now)) {
            return noInsight(request, now, if (spanish) "Pronóstico desactualizado o con fecha futura." else "Forecast is stale or dated in the future.")
        }
        val complete = request.hours.filter { it.temperatureC != null && it.precipitationProbabilityPct != null }
        val missing = request.hours.size - complete.size
        val result = when (request.mode) {
            InsightMode.DaySummary -> summary(request, now, zone, complete, missing, spanish)
            InsightMode.WalkWindow -> walk(request, now, zone, complete, missing, spanish)
        }
        return result
    }

    private fun summary(request: InsightRequest, now: Instant, zone: ZoneId, hours: List<InsightHour>, missing: Int, es: Boolean): InsightResult {
        if (hours.size < 2) return noInsight(request, now, if (es) "Se necesitan al menos dos horas con temperatura y probabilidad de precipitación." else "At least two hours with temperature and precipitation probability are needed.")
        val low = hours.minBy { it.temperatureC!! }
        val high = hours.maxBy { it.temperatureC!! }
        val rain = hours.maxBy { it.precipitationProbabilityPct!! }
        val insight = Insight(
            id = "day_summary",
            title = if (es) "Próximas horas" else "Coming hours",
            body = if (es) "Entre ${time(hours.first().timestamp, zone)} y ${time(hours.last().timestamp, zone)}, las temperaturas disponibles van de ${number(low.temperatureC!!, es)} a ${number(high.temperatureC!!, es)} °C. La mayor probabilidad de precipitación indicada es ${number(rain.precipitationProbabilityPct!!, es)} % a las ${time(rain.timestamp, zone)}."
                else "From ${time(hours.first().timestamp, zone)} to ${time(hours.last().timestamp, zone)}, available temperatures range from ${number(low.temperatureC!!, es)} to ${number(high.temperatureC!!, es)} °C. The highest indicated precipitation probability is ${number(rain.precipitationProbabilityPct!!, es)} % at ${time(rain.timestamp, zone)}.",
            evidence = listOf(Evidence(low.timestamp, EvidenceField.TemperatureC), Evidence(high.timestamp, EvidenceField.TemperatureC), Evidence(rain.timestamp, EvidenceField.PrecipitationProbabilityPct)).distinct(),
        )
        return ready(request, now, insight, missingLimitation(missing, es))
    }

    private fun walk(request: InsightRequest, now: Instant, zone: ZoneId, hours: List<InsightHour>, missing: Int, es: Boolean): InsightResult {
        val duration = requireNotNull(request.walkDurationMinutes)
        // Una muestra horaria representa una hora; 30 min no tiene resolución propia.
        val sampleCount = if (duration == 120) 2 else 1
        val candidates = hours.windowed(sampleCount).filter { window ->
            window.zipWithNext().all { (a, b) -> b.timestamp == a.timestamp.plusSeconds(3600) }
        }
        if (candidates.isEmpty()) return noInsight(request, now, if (es) "No hay una ventana continua con temperatura y precipitación completas." else "No continuous window has complete temperature and precipitation data.")
        val best = candidates.minWith(compareBy<List<InsightHour>>(
            { window -> window.maxOf { it.precipitationProbabilityPct!! } * 2 + abs(window.map { it.temperatureC!! }.average() - 20.0) * 5 },
            { it.first().timestamp },
        ))
        val maxRain = best.maxBy { it.precipitationProbabilityPct!! }
        val avgTemp = best.map { it.temperatureC!! }.average()
        val evidence = best.flatMap { listOf(Evidence(it.timestamp, EvidenceField.TemperatureC), Evidence(it.timestamp, EvidenceField.PrecipitationProbabilityPct)) }
        val insight = Insight(
            id = "walk_window",
            title = if (es) "Ventana para caminar" else "Walking window",
            body = if (es) "Según las muestras horarias, la ventana de $duration min comienza a las ${time(best.first().timestamp, zone)}. Promedio de temperatura: ${number(avgTemp, es)} °C; mayor probabilidad de precipitación indicada: ${number(maxRain.precipitationProbabilityPct!!, es)} %. Es una preferencia de ranking, no una garantía de buen tiempo."
                else "Based on hourly samples, the $duration min window starts at ${time(best.first().timestamp, zone)}. Average temperature: ${number(avgTemp, es)} °C; highest indicated precipitation probability: ${number(maxRain.precipitationProbabilityPct!!, es)} %. This is a ranking preference, not a weather guarantee.",
            evidence = evidence,
        )
        val limitations = missingLimitation(missing, es) + if (es) "Resolución horaria: la ventana de $duration min es aproximada." else "Hourly resolution: the $duration min window is approximate."
        return ready(request, now, insight, limitations)
    }

    private fun ready(request: InsightRequest, now: Instant, insight: Insight, limitations: List<String>): InsightResult.Ready =
        InsightResult.Ready(payload(request, now, listOf(insight), limitations, minOf(now.plusSeconds(3600), request.fetchedAt.plusSeconds(6 * 3600))))

    private fun noInsight(request: InsightRequest, now: Instant, reason: String): InsightResult.NoInsight =
        InsightResult.NoInsight(payload(request, now, emptyList(), listOf(reason), now.plusSeconds(3600)))

    private fun payload(request: InsightRequest, now: Instant, insights: List<Insight>, limitations: List<String>, expiresAt: Instant) =
        InsightPayload(request.requestId, request.snapshotId, now, expiresAt, InsightMethod.Rules, insights, limitations)

    private fun missingLimitation(count: Int, es: Boolean): List<String> = when {
        count == 0 -> emptyList()
        es -> listOf("Se omitieron $count ${if (count == 1) "hora" else "horas"} con temperatura o precipitación faltante.")
        else -> listOf("Skipped $count ${if (count == 1) "hour" else "hours"} with missing temperature or precipitation data.")
    }

    private fun time(instant: Instant, zone: ZoneId): String = instant.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm XXX", Locale.ROOT))
    private fun number(value: Double, es: Boolean): String = BigDecimal.valueOf(value).setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().let {
        if (es) it.replace('.', ',') else it
    }

    private fun validate(request: InsightRequest, now: Instant) {
        require(request.schemaVersion == "1.0")
        require(request.requestId.isNotBlank() && request.requestId.length <= 80)
        require(request.snapshotId.isNotBlank() && request.snapshotId.length <= 80)
        require(request.locale in setOf("es", "en"))
        require(request.timezone.length <= 80 && runCatching { ZoneId.of(request.timezone) }.isSuccess)
        require(request.hours.size in 1..24)
        require(request.hours.zipWithNext().all { (a, b) -> a.timestamp.isBefore(b.timestamp) })
        require(request.hours.all { hour ->
            hour.timestamp.isAfter(now.minusSeconds(300)) &&
                (hour.temperatureC == null || hour.temperatureC.isFinite()) &&
                (hour.precipitationProbabilityPct == null || (hour.precipitationProbabilityPct.isFinite() && hour.precipitationProbabilityPct in 0.0..100.0))
        })
        require(if (request.mode == InsightMode.WalkWindow) request.walkDurationMinutes in setOf(30, 60, 120) else request.walkDurationMinutes == null)
    }
}
