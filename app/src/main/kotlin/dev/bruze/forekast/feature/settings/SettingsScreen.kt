package dev.bruze.forekast.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.bruze.forekast.BuildConfig
import dev.bruze.forekast.R
import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.data.demo.DemoScenario
import dev.bruze.forekast.feature.weather.WeatherUiState

@Composable fun SettingsScreen(state: WeatherUiState, onUnit: (TemperatureUnit) -> Unit, onTheme: (ThemeMode) -> Unit,
    onScenario: (DemoScenario) -> Unit, onForecast: () -> Unit, onClearCities: () -> Unit) {
    val uri = LocalUriHandler.current
    var confirmingClear by rememberSaveable { mutableStateOf(false) }
    if (confirmingClear) AlertDialog(
        onDismissRequest = { confirmingClear = false },
        title = { Text(stringResource(R.string.clear_title)) },
        text = { Text(stringResource(R.string.clear_body)) },
        confirmButton = { TextButton(onClick = { confirmingClear = false; onClearCities() }) { Text(stringResource(R.string.clear_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmingClear = false }) { Text(stringResource(R.string.cancel)) } },
    )
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionTitle(stringResource(R.string.temperature_unit))
            ChoiceRow("°C · ${stringResource(R.string.unit_celsius)}", state.unit == TemperatureUnit.Celsius) { onUnit(TemperatureUnit.Celsius) }
            ChoiceRow("°F · ${stringResource(R.string.unit_fahrenheit)}", state.unit == TemperatureUnit.Fahrenheit) { onUnit(TemperatureUnit.Fahrenheit) }
        }
        item {
            SectionTitle(stringResource(R.string.appearance))
            ThemeMode.entries.forEach { theme ->
                val label = when (theme) { ThemeMode.System -> R.string.theme_system; ThemeMode.Light -> R.string.theme_light; ThemeMode.Dark -> R.string.theme_dark }
                ChoiceRow(stringResource(label), state.theme == theme) { onTheme(theme) }
            }
        }
        if (BuildConfig.DEBUG && state.isDemo) item {
            SectionTitle(stringResource(R.string.lab))
            Text(stringResource(R.string.lab_description), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            DemoScenario.entries.forEach { scenario -> ChoiceRow(stringResource(scenario.label()), state.scenario == scenario) { onScenario(scenario) } }
            Button(onClick = onForecast, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.show_forecast)) }
        }
        if (!state.isDemo) item {
            SectionTitle(stringResource(R.string.local_data))
            OutlinedButton(onClick = { confirmingClear = true }, enabled = state.hasSelection || state.favorites.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.clear_title))
            }
        }
        item {
            SectionTitle(stringResource(R.string.about))
            Text(stringResource(if (state.isDemo) R.string.about_body else R.string.live_about), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp)); Text(stringResource(if (state.isDemo) R.string.future_provider else R.string.live_attribution), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { uri.openUri("https://open-meteo.com/") }) { Text(stringResource(R.string.open_provider)) }
            if (!state.isDemo) {
                TextButton(onClick = { uri.openUri("https://creativecommons.org/licenses/by/4.0/") }) { Text("CC BY 4.0") }
                TextButton(onClick = { uri.openUri("https://www.geonames.org/") }) { Text("GeoNames · Ciudades") }
            }
        }
    }
}

private fun DemoScenario.label(): Int = when (this) {
    DemoScenario.Success -> R.string.scenario_success
    DemoScenario.Partial -> R.string.scenario_partial
    DemoScenario.Error -> R.string.scenario_error
    DemoScenario.Stale -> R.string.scenario_stale
    DemoScenario.Loading -> R.string.scenario_loading
}
@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }); Spacer(Modifier.height(10.dp)) }
@Composable private fun ChoiceRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(selected, role = Role.RadioButton, onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null)
        Spacer(Modifier.width(12.dp)); Text(text, modifier = Modifier.weight(1f))
    }
}
