package dev.bruze.forekast

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import dev.bruze.forekast.core.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.io.File
import org.junit.*
import org.junit.Assert.*

/** Run the two stages separately with force-stop and network disabled between them. */
class ProcessPersistenceTest {
    @get:Rule(order = 0) val enabled = org.junit.rules.TestRule { statement, _ ->
        object : org.junit.runners.model.Statement() {
            override fun evaluate() {
                Assume.assumeTrue(!BuildConfig.DEMO && InstrumentationRegistry.getArguments().getString("processPersistence") == "true")
                statement.evaluate()
            }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private fun awaitTemperature(unit: String) {
        compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription("grados $unit", substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun seedOnline() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Ciudades").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ciudades").performClick()
        compose.onNodeWithText("Buscar ciudad").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Lisboa")
        val result = hasClickAction() and hasText("Lisboa") and !hasSetTextAction()
        compose.waitUntil(30_000) { compose.onAllNodes(result).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(result)[0].performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Lisboa").fetchSemanticsNodes().isNotEmpty() }
        awaitTemperature("Celsius")
        compose.onNodeWithText("Guardar favorita").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Quitar favorita").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("°F · Fahrenheit").performClick()
        compose.onNodeWithText("Oscuro").performClick()
        compose.onNodeWithText("Volver").performClick(); awaitTemperature("Fahrenheit")
        val container = (compose.activity.application as ForeKastApplication).container
        runBlocking {
            val settings = container.preferences!!.preferences.first { it.theme == ThemeMode.Dark && it.unit == TemperatureUnit.Fahrenheit }
            val city = requireNotNull(container.library!!.library.first().selected)
            val snapshot = container.weatherRepository.observe(city.id).first { it != null }!!
            File(compose.activity.filesDir, "process-proof.txt").writeText(listOf(city.id, snapshot.fetchedAt.toString(), settings.theme.name, android.os.Process.myPid().toString()).joinToString("\n"))
        }
    }
    @Test fun verifyOffline() {
        awaitTemperature("Fahrenheit")
        compose.onNodeWithText("Lisboa").assertIsDisplayed()
        compose.onNodeWithText("Quitar favorita").assertExists()
        val expected = File(compose.activity.filesDir, "process-proof.txt").readLines()
        assertNotEquals(expected[3].toInt(), android.os.Process.myPid())
        val container = (compose.activity.application as ForeKastApplication).container
        runBlocking {
            val library = container.library!!.library.first()
            val snapshot = container.weatherRepository.observe(requireNotNull(library.selected).id).first { it != null }!!
            assertEquals(expected[0], requireNotNull(library.selected).id)
            assertEquals(expected[1], snapshot.fetchedAt.toString())
            assertTrue(library.favorites.any { it.id == requireNotNull(library.selected).id })
            assertEquals(ThemeMode.Dark, container.preferences!!.preferences.first().theme)
        }
        File(compose.activity.getExternalFilesDir(null), "i3-offline.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
