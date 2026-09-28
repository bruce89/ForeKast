package dev.bruze.forekast.data.local

import dev.bruze.forekast.core.model.Location
import dev.bruze.forekast.core.ports.*
import java.time.Instant
import kotlinx.coroutines.flow.map

class RoomLibraryRepository(private val dao: ForeKastDao) : LibraryRepository {
    override val library = dao.library().map { rows ->
        Library(rows.singleOrNull { it.selected }?.city?.model(), rows.filter { it.city.favoriteOrder != null }
            .sortedBy { it.city.favoriteOrder }.map { it.city.model() })
    }
    override suspend fun clearCities() = dao.clearCities()
    override suspend fun select(location: Location) = dao.select(location.entity())
    override suspend fun toggleFavorite(id: String) = dao.toggleFavorite(id)
}
class RoomCooldownStore(private val dao: ForeKastDao) : CooldownStore {
    override suspend fun read(host: String) = dao.cooldown(host)?.let { Instant.ofEpochMilli(it.until) }
    override suspend fun write(host: String, until: Instant) = dao.writeCooldown(CooldownEntity(host, until.toEpochMilli()))
}
