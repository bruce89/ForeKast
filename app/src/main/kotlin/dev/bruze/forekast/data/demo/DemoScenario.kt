package dev.bruze.forekast.data.demo

enum class DemoScenario { Success, Partial, Error, Stale, Loading }

/** Control exclusivo del laboratorio; no forma parte del puerto meteorológico. */
interface DemoController {
    fun prepare(locationId: String, scenario: DemoScenario)
}
