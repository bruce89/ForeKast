package dev.bruze.forekast.feature.locations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bruze.forekast.R
import androidx.compose.ui.semantics.*
import dev.bruze.forekast.feature.search.LocationRow
import dev.bruze.forekast.feature.weather.WeatherUiState

@Composable fun LocationsScreen(state: WeatherUiState, onSearch: () -> Unit, onSelect: (String) -> Unit, onRemove: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.search)) }
            Spacer(Modifier.height(24.dp)); Text(stringResource(R.string.favorites), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        if (state.favorites.isEmpty()) item { Text(stringResource(R.string.favorites_empty)) }
        items(state.favorites, key = { it }) { id ->
            Column {
                state.knownLocations[id]?.let { LocationRow(it, onSelect, id == state.location.id) }
                TextButton(onClick = { onRemove(id) }) { Text(stringResource(R.string.favorite_remove_city, state.knownLocations[id]?.name ?: id)) }
            }
        }
    }
}
