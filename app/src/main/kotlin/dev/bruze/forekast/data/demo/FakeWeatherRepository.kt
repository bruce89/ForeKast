package dev.bruze.forekast.data.demo

import dev.bruze.forekast.core.model.WeatherSnapshot
import dev.bruze.forekast.core.ports.RefreshResult
import dev.bruze.forekast.core.ports.WeatherRepository
import java.time.Clock
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeWeatherRepository(private val clock: Clock, private val delayMillis: Long = 650) : WeatherRepository, DemoController {
    /** Diagnostic count for lifecycle experiments; no production telemetry. */
    var refreshCalls: Int = 0
        private set
    private val snapshots = MutableStateFlow<Map<String, WeatherSnapshot>>(emptyMap())
    private val scenarios = mutableMapOf<String, DemoScenario>()

    override fun observe(locationId: String) = snapshots.map { it[locationId] }.distinctUntilChanged()

    override fun prepare(locationId: String, scenario: DemoScenario) {
        scenarios[locationId] = scenario
        snapshots.update { current ->
            if (scenario == DemoScenario.Stale) current + (locationId to WeatherFixtures.snapshot(DemoCatalog.find(locationId), clock, stale = true))
            else current - locationId
        }
    }

    override suspend fun refresh(locationId: String): RefreshResult {
        refreshCalls++
        // Capturar antes de suspender evita cambiar una petición ya iniciada al tocar el laboratorio.
        val scenario = scenarios[locationId] ?: DemoScenario.Success
        if (scenario == DemoScenario.Loading) awaitCancellation()
        delay(delayMillis)
        if (scenario == DemoScenario.Error || scenario == DemoScenario.Stale) return RefreshResult.Failed
        val snapshot = WeatherFixtures.snapshot(DemoCatalog.find(locationId), clock, partial = scenario == DemoScenario.Partial)
        snapshots.update { it + (locationId to snapshot) }
        return RefreshResult.Updated
    }
}
