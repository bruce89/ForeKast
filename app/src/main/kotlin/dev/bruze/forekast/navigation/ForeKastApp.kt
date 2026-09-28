package dev.bruze.forekast.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.bruze.forekast.R
import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.data.demo.DemoScenario
import dev.bruze.forekast.feature.weather.*
import dev.bruze.forekast.feature.search.SearchScreen
import dev.bruze.forekast.feature.settings.SettingsScreen
import dev.bruze.forekast.feature.locations.LocationsScreen
import kotlinx.serialization.Serializable

@Serializable data object Forecast : NavKey
@Serializable data object Search : NavKey
@Serializable data object Locations : NavKey
@Serializable data object Settings : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ForeKastApp(state: WeatherUiState, onRefresh: () -> Unit, onSelect: (String) -> Unit,
    onFavorite: (String) -> Unit, onUnit: (TemperatureUnit) -> Unit, onTheme: (ThemeMode) -> Unit,
    onScenario: (DemoScenario) -> Unit, onQuery: (String) -> Unit, onClearCities: () -> Unit) {
    if (!state.initialized) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val backStack = rememberNavBackStack(Forecast)
    val destination = backStack.lastOrNull() ?: Forecast
    fun showForecast() { while (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    fun select(id: String) { onSelect(id); showForecast() }
    Scaffold(bottomBar = {
        if (state.storageError) Surface(color = MaterialTheme.colorScheme.errorContainer) {
            Text(stringResource(R.string.storage_error), modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }, topBar = {
        Surface {
            Column(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 12.dp)) {
                Text(stringResource(when (destination) {
                    Search -> R.string.search; Locations -> R.string.cities; Settings -> R.string.settings; else -> R.string.app_name
                }), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp).semantics { heading() })
                FlowRow {
                    if (destination != Forecast) TextButton(onClick = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }) { Text(stringResource(R.string.back)) }
                    else {
                        TextButton(onClick = { backStack.add(Locations) }) { Text(stringResource(R.string.cities)) }
                        TextButton(onClick = { backStack.add(Settings) }) { Text(stringResource(R.string.settings)) }
                    }
                }
            }
        }
    }) { padding ->
        NavDisplay(backStack = backStack, modifier = Modifier.padding(padding),
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            entryProvider = entryProvider {
                entry<Forecast> {
                    if (state.hasSelection) WeatherScreen(state, onRefresh, { onFavorite(state.location.id) }, { backStack.add(Search) })
                    else WelcomeScreen { backStack.add(Search) }
                }
                entry<Search> { SearchScreen(state, onQuery, ::select) }
                entry<Locations> { LocationsScreen(state, { backStack.add(Search) }, ::select, onFavorite) }
                entry<Settings> { SettingsScreen(state, onUnit, onTheme, onScenario, ::showForecast, { onClearCities(); showForecast() }) }
            })
    }
}

@Composable private fun WelcomeScreen(onSearch: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() }) }
        item { Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.search)) } }
        item { Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge) }
    }
}
