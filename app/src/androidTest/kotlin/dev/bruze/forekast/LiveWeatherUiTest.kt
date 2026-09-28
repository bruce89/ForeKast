package dev.bruze.forekast

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*

class LiveWeatherUiTest {
    @get:Rule(order = 0) val enabled = org.junit.rules.TestRule { statement, _ ->
        object : org.junit.runners.model.Statement() {
            override fun evaluate() {
                Assume.assumeTrue(!BuildConfig.DEMO && InstrumentationRegistry.getArguments().getString("liveWeather") == "true")
                statement.evaluate()
            }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Test fun selectsACityOutsideTheDemoCatalogAndShowsRealWeather() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Ciudades").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ciudades").performClick()
        compose.onNodeWithText("Buscar ciudad").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Lisboa")
        val result = hasClickAction() and hasText("Lisboa") and !hasSetTextAction()
        compose.waitUntil(30_000) { compose.onAllNodes(result).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(result)[0].performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Lisboa").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription("grados Celsius", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Lisboa").assertIsDisplayed()
        compose.onNodeWithText("PRONÓSTICO · OPEN-METEO").assertIsDisplayed()
    }
}
