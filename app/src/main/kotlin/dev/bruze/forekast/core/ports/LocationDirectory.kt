package dev.bruze.forekast.core.ports

import dev.bruze.forekast.core.model.Location

interface LocationDirectory {
    val initial: Location
    fun find(id: String): Location?
    fun remember(locations: List<Location>) {}
    suspend fun search(query: String): List<Location>
}
