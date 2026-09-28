package dev.bruze.forekast.core.ports

import dev.bruze.forekast.core.model.*
import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class Library(val selected: Location?, val favorites: List<Location>)
interface LibraryRepository {
    val library: Flow<Library>
    suspend fun select(location: Location)
    suspend fun toggleFavorite(id: String)
    suspend fun clearCities()
}
data class Preferences(val unit: TemperatureUnit = TemperatureUnit.Celsius, val theme: ThemeMode = ThemeMode.System)
interface PreferencesRepository {
    val preferences: Flow<Preferences>
    suspend fun setUnit(value: TemperatureUnit)
    suspend fun setTheme(value: ThemeMode)
}
interface CooldownStore {
    suspend fun read(host: String): Instant?
    suspend fun write(host: String, until: Instant)
}
class MemoryCooldownStore : CooldownStore {
    private val values = java.util.concurrent.ConcurrentHashMap<String, Instant>()
    override suspend fun read(host: String) = values[host]
    override suspend fun write(host: String, until: Instant) { values[host] = until }
}
