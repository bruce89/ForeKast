package dev.bruze.forekast.data.demo

import dev.bruze.forekast.core.ports.LocationDirectory

object DemoLocationDirectory : LocationDirectory {
    override val initial = DemoCatalog.locations.first()
    override fun find(id: String) = DemoCatalog.locations.firstOrNull { it.id == id }
    override suspend fun search(query: String) = DemoCatalog.search(query)
}
