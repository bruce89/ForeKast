package dev.bruze.forekast.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.core.ports.*
import kotlinx.coroutines.flow.map

val Context.forekastPreferences by preferencesDataStore(name = "forekast_preferences")
class DataStorePreferences(private val store: DataStore<androidx.datastore.preferences.core.Preferences>) : PreferencesRepository {
    private val unit = stringPreferencesKey("temperature_unit")
    private val theme = stringPreferencesKey("theme")
    override val preferences = store.data.map { values ->
        dev.bruze.forekast.core.ports.Preferences(
            TemperatureUnit.entries.firstOrNull { it.name == values[unit] } ?: TemperatureUnit.Celsius,
            ThemeMode.entries.firstOrNull { it.name == values[theme] } ?: ThemeMode.System)
    }
    override suspend fun setUnit(value: TemperatureUnit) { store.edit { it[unit] = value.name } }
    override suspend fun setTheme(value: ThemeMode) { store.edit { it[theme] = value.name } }
}
