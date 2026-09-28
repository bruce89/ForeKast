package dev.bruze.forekast

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.bruze.forekast.core.model.*
import dev.bruze.forekast.core.ports.Library
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveLocalFlowTest {
    @get:Rule(order = 0) val enabled = org.junit.rules.TestRule { statement, _ ->
        object : org.junit.runners.model.Statement() {
            override fun evaluate() {
                Assume.assumeTrue(!BuildConfig.DEMO)
                statement.evaluate()
            }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun firstLaunchAndConfirmedClearReturnToWelcome() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("El tiempo donde vos elijas").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("No necesitamos acceso a tu ubicación.", substring = true).assertExists()
        val container = (compose.activity.application as ForeKastApplication).container
        Assert.assertNull(runBlocking { container.library!!.library.first().selected })
        val city = dev.bruze.forekast.data.demo.DemoCatalog.locations.first().copy(latitude = -34.90, longitude = -56.16)
        runBlocking { container.library!!.select(city) }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Guardar favorita").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("°F · Fahrenheit").performClick()
        compose.onNodeWithText("Borrar ciudades y pronósticos").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        Assert.assertEquals(city.id, runBlocking { container.library!!.library.first().selected?.id })
        compose.onNodeWithText("Borrar ciudades y pronósticos").performScrollTo().performClick()
        compose.onNodeWithText("Borrar").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("El tiempo donde vos elijas").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertEquals(Library(null, emptyList()), runBlocking { container.library!!.library.first() })
        Assert.assertEquals(TemperatureUnit.Fahrenheit, runBlocking { container.preferences!!.preferences.first().unit })
    }
}
