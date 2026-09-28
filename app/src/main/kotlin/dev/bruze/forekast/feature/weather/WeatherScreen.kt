package dev.bruze.forekast.feature.weather

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import dev.bruze.forekast.R
import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.data.demo.*
import dev.bruze.forekast.designsystem.*
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(state: WeatherUiState, onRefresh: () -> Unit, onFavorite: () -> Unit, onSearch: () -> Unit) {
    val snapshot = state.snapshot
    val current = snapshot?.current
    val density = LocalDensity.current
    val expandedText = density.fontScale >= 1.3f || with(density) { LocalWindowInfo.current.containerSize.width.toDp() < 360.dp }
    val canRefresh = !state.refreshing && state.retryAt?.isAfter(state.now) != true
    PullToRefreshBox(isRefreshing = state.refreshing && snapshot != null, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().testTag("forecast-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Text(stringResource(if (state.isDemo) R.string.demo_badge else R.string.live_badge), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Text(state.location.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Text("${state.location.region}, ${state.location.country}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val favorite = state.location.id in state.favorites
                val atLimit = !favorite && state.favorites.size >= 5
                TextButton(onClick = onFavorite, enabled = !atLimit) {
                    Text(stringResource(if (favorite) R.string.favorite_remove else R.string.favorite_add))
                }
                if (atLimit) Text(stringResource(R.string.favorite_limit), style = MaterialTheme.typography.bodySmall)
            }
            if (snapshot == null) {
                item {
                    if (state.refreshing || !state.contentRead) Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(20.dp)); Text(stringResource(R.string.loading))
                    } else Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(20.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(failureMessage(state.failureReason)))
                            RetryTime(state)
                            Button(onClick = onRefresh, enabled = canRefresh) { Text(stringResource(R.string.retry)) }
                            TextButton(onClick = onSearch) { Text(stringResource(R.string.search)) }
                        }
                    }
                }
            } else {
                val stale = CachePolicy.stale(snapshot.fetchedAt, state.now)
                val formatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("es")).withZone(state.location.zone)
                if (stale || state.failed) item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(if (stale) R.string.saved_forecast else R.string.error_title), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(if (state.failed) failureMessage(state.failureReason) else R.string.saved_body))
                            RetryTime(state)
                            Text(stringResource(R.string.downloaded, formatter.format(snapshot.fetchedAt)), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (current != null) item {
                    if (snapshot.fetchedAt.isAfter(state.now)) Text(stringResource(R.string.unknown_freshness))
                    if (stale) Text(stringResource(R.string.saved_current), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        val description = temperatureDescription(current.temperatureC, state.unit)
                        Text(temperature(current.temperatureC, state.unit), fontSize = if (expandedText) 48.sp else 76.sp, fontWeight = FontWeight.Light,
                            modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = description })
                        if (!expandedText) WeatherGlyph(current.condition, 76.dp, current.isDay)
                    }
                    Text(if (state.unit == TemperatureUnit.Celsius) "°C" else "°F", style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(current.condition.label()), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.feels_like, temperature(current.feelsLikeC, state.unit)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    snapshot.days.firstOrNull()?.let {
                        Text(stringResource(R.string.temperature_range, temperature(it.highC, state.unit), temperature(it.lowC, state.unit)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.current_time, formatter.format(current.validAt)), style = MaterialTheme.typography.bodySmall)
                    if (!stale) Text(stringResource(R.string.downloaded, formatter.format(snapshot.fetchedAt)), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onRefresh, enabled = canRefresh) { Text(stringResource(if (state.refreshing) R.string.updating else R.string.refresh)) }
                }
                if (current != null) item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric(stringResource(R.string.humidity), probability(current.humidityPct), Modifier.fillMaxWidth(if (expandedText) 1f else 0.47f))
                        Metric(stringResource(R.string.wind), current.windKmh?.let { stringResource(R.string.wind_value, it.toInt()) }
                            ?: stringResource(R.string.missing_symbol), Modifier.fillMaxWidth(if (expandedText) 1f else 0.47f))
                    }
                }
                if (current?.temperatureC == null || current.feelsLikeC == null || current.humidityPct == null || current.windKmh == null || snapshot.hours.any { it.temperatureC == null || it.precipitationPct == null } || snapshot.days.any { it.lowC == null || it.highC == null || it.precipitationPct == null }) item {
                    Text(stringResource(R.string.partial_notice), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (current == null) item {
                    Text(stringResource(R.string.no_current))
                    OutlinedButton(onClick = onRefresh, enabled = canRefresh) { Text(stringResource(R.string.refresh)) }
                }
                item {
                    Text(stringResource(R.string.hourly), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(12.dp))
                    val hours = snapshot.hours.filter { !it.time.isBefore(state.now) }.take(24)
                    if (hours.isEmpty()) Text(stringResource(R.string.no_hours))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(hours, key = { it.time.toEpochMilli() }) { HourCard(it, state) }
                    }
                }
                item { Text(stringResource(R.string.daily), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() }) }
                val futureDays = snapshot.days.filter { !it.date.isBefore(state.now.atZone(state.location.zone).toLocalDate()) }
                if (futureDays.isEmpty()) item { Text(stringResource(R.string.no_days)) }
                items(futureDays, key = { it.date.toString() }) { day ->
                    val dayText = if (day.date == state.now.atZone(state.location.zone).toLocalDate()) stringResource(R.string.today)
                        else day.date.format(DateTimeFormatter.ofPattern("EEE d", Locale.forLanguageTag("es")))
                    val condition = stringResource(day.condition.label())
                    val chance = stringResource(R.string.precipitation, probability(day.precipitationPct))
                    val description = stringResource(R.string.day_accessible, dayText, condition,
                        temperatureDescription(day.lowC, state.unit), temperatureDescription(day.highC, state.unit), chance)
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.clearAndSetSemantics { contentDescription = description }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(dayText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                WeatherGlyph(day.condition, 32.dp)
                            }
                            Text(condition, style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.temperature_range, temperature(day.highC, state.unit), temperature(day.lowC, state.unit)))
                            Text(chance, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Text(stringResource(if (state.isDemo) R.string.demo_notice else R.string.live_attribution), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier.semantics(mergeDescendants = true) {}, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable private fun HourCard(hour: HourWeather, state: WeatherUiState) {
    val time = DateTimeFormatter.ofPattern("HH:mm XXX").withZone(state.location.zone).format(hour.time)
    val chance = probability(hour.precipitationPct)
    val description = "$time, ${stringResource(hour.condition.label())}, ${temperatureDescription(hour.temperatureC, state.unit)}, ${stringResource(R.string.precipitation, chance)}"
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.widthIn(min = 78.dp).clearAndSetSemantics { contentDescription = description }) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(time, style = MaterialTheme.typography.labelMedium)
            WeatherGlyph(hour.condition, 32.dp)
            Text(temperature(hour.temperatureC, state.unit), style = MaterialTheme.typography.titleMedium)
            Text(chance, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, name = "Completo")
@Composable private fun WeatherPreview() = ForecastPreview(DemoScenario.Success)
@Preview(showBackground = true, name = "Parcial")
@Composable private fun PartialPreview() = ForecastPreview(DemoScenario.Partial)
@Preview(showBackground = true, name = "Error")
@Composable private fun ErrorPreview() = ForecastPreview(DemoScenario.Error)
@Preview(showBackground = true, name = "Cargando")
@Composable private fun LoadingPreview() = ForecastPreview(DemoScenario.Loading)
@Preview(showBackground = true, name = "Guardado oscuro")
@Composable private fun StalePreview() = ForecastPreview(DemoScenario.Stale, ThemeMode.Dark)

@Composable private fun ForecastPreview(scenario: DemoScenario, theme: ThemeMode = ThemeMode.Light) {
    val clock = Clock.fixed(Instant.parse("2026-09-24T14:00:00Z"), ZoneOffset.UTC)
    val location = DemoCatalog.locations.first()
    val snapshot = if (scenario == DemoScenario.Error || scenario == DemoScenario.Loading) null
        else WeatherFixtures.snapshot(location, clock, scenario == DemoScenario.Partial, scenario == DemoScenario.Stale)
    ForeKastTheme(theme) { Surface { WeatherScreen(WeatherUiState(location, snapshot, scenario == DemoScenario.Loading,
        scenario == DemoScenario.Error || scenario == DemoScenario.Stale, now = clock.instant()), {}, {}, {}) } }
}

private fun failureMessage(reason: String?): Int = when (reason) {
    "rate_limit" -> R.string.rate_limit
    "throttled" -> R.string.refresh_throttled
    "storage" -> R.string.storage_error
    "invalid" -> R.string.invalid_response
    "expired" -> R.string.expired_cache
    else -> R.string.error_body
}

@Composable private fun RetryTime(state: WeatherUiState) {
    state.retryAt?.takeIf { it.isAfter(state.now) }?.let { retry ->
        val time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(state.location.zone).format(retry)
        Text(stringResource(R.string.retry_at, time), style = MaterialTheme.typography.bodyMedium)
    }
}
