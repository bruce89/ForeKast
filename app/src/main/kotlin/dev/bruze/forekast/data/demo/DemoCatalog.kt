package dev.bruze.forekast.data.demo

import dev.bruze.forekast.core.model.Location
import java.text.Normalizer
import java.time.ZoneId

object DemoCatalog {
    val locations = listOf(
        Location("montevideo-uy", "Montevideo", "Montevideo", "Uruguay", ZoneId.of("America/Montevideo")),
        Location("buenos-aires", "Buenos Aires", "Ciudad Autónoma", "Argentina", ZoneId.of("America/Argentina/Buenos_Aires")),
        Location("madrid", "Madrid", "Comunidad de Madrid", "España", ZoneId.of("Europe/Madrid")),
        Location("tokyo", "Tokio", "Tokio", "Japón", ZoneId.of("Asia/Tokyo")),
        Location("montevideo-us", "Montevideo", "Minnesota", "Estados Unidos", ZoneId.of("America/Chicago")),
        Location("ushuaia", "Ushuaia", "Tierra del Fuego", "Argentina", ZoneId.of("America/Argentina/Ushuaia")),
    )
    fun find(id: String): Location = locations.firstOrNull { it.id == id } ?: locations.first()
    fun search(query: String): List<Location> {
        val term = normalize(query.trim())
        if (term.length < 3) return emptyList()
        return locations.filter { normalize("${it.name} ${it.region} ${it.country}").contains(term) }
    }
    private fun normalize(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "").lowercase(java.util.Locale.ROOT)
}
