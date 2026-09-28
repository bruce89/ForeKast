package dev.bruze.forekast.data.remote

import dev.bruze.forekast.core.model.Location
import dev.bruze.forekast.core.ports.LocationDirectory
import java.time.ZoneId

class RemoteLocationDirectory(private val api: OpenMeteoClient) : LocationDirectory {
    override val initial = Location("3441575", "Montevideo", "Montevideo", "Uruguay", ZoneId.of("America/Montevideo"), -34.90328, -56.18816)
    private val known = linkedMapOf(initial.id to initial)
    override fun find(id: String) = known[id]
    override fun remember(locations: List<Location>) { locations.forEach { known[it.id] = it } }
    override suspend fun search(query: String): List<Location> {
        if (query.trim().length < 3) return emptyList()
        val results = api.search(query.trim()).results.take(10).mapNotNull { dto ->
            val zone = runCatching { ZoneId.of(dto.timezone) }.getOrNull() ?: return@mapNotNull null
            if (!dto.latitude.isFinite() || dto.latitude !in -90.0..90.0 || !dto.longitude.isFinite() || dto.longitude !in -180.0..180.0 || dto.name.isBlank()) return@mapNotNull null
            Location(dto.id.toString(), dto.name, dto.admin1, dto.country, zone, dto.latitude, dto.longitude)
        }.distinctBy { it.id }
        results.forEach { known[it.id] = it }
        return results
    }
}
