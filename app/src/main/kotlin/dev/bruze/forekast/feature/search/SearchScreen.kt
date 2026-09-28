package dev.bruze.forekast.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import dev.bruze.forekast.R
import dev.bruze.forekast.core.model.Location
import dev.bruze.forekast.feature.weather.WeatherUiState

@Composable fun SearchScreen(state: WeatherUiState, onQuery: (String) -> Unit, onSelect: (String) -> Unit) {
    val query = state.query
    val focus = remember { FocusRequester() }
    val results = state.searchResults
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(results) { if (results.isNotEmpty()) keyboard?.hide() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
        OutlinedTextField(value = query, onValueChange = onQuery, singleLine = true,
            label = { Text(stringResource(R.string.search_hint)) }, modifier = Modifier.fillMaxWidth().focusRequester(focus))
        Text(stringResource(if (state.isDemo) R.string.demo_catalog else R.string.live_catalog), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (query.trim().length < 3) Text(stringResource(R.string.search_help))
        else if (state.searching) CircularProgressIndicator()
        else if (state.searchFailed) {
            Text(stringResource(R.string.search_error))
            TextButton(onClick = { onQuery(query) }) { Text(stringResource(R.string.retry)) }
        }
        else if (results.isEmpty()) Text(stringResource(R.string.search_empty))
        }
        items(results, key = { it.id }) { LocationRow(it, onSelect) }
    }
}

@Composable fun LocationRow(location: Location, onSelect: (String) -> Unit, selected: Boolean = false) {
    Card(Modifier.fillMaxWidth().testTag("location-${location.id}").semantics { this.selected = selected }.clickable(role = Role.Button) { onSelect(location.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(location.name, style = MaterialTheme.typography.titleMedium)
            Text("${location.region} · ${location.country}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (selected) Text(stringResource(R.string.selected_city), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
