package dev.bruze.forekast

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForeKastFlowTest {
    @get:Rule(order = 0) val enabled = org.junit.rules.TestRule { statement, _ ->
        object : org.junit.runners.model.Statement() {
            override fun evaluate() {
                org.junit.Assume.assumeTrue("Run with -PforekastDemo=true", BuildConfig.DEMO)
                statement.evaluate()
            }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private fun awaitForecast() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Despejado").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun recreationDuringLoadingKeepsViewModelAndDoesNotDuplicateRefresh() {
        awaitForecast()
        val provider = androidx.lifecycle.ViewModelProvider(compose.activity)
        val vm = provider[dev.bruze.forekast.feature.weather.WeatherViewModel::class.java]
        val repo = (compose.activity.application as ForeKastApplication).container.weatherRepository as dev.bruze.forekast.data.demo.FakeWeatherRepository
        compose.runOnIdle { vm.setScenario(dev.bruze.forekast.data.demo.DemoScenario.Loading) }
        compose.waitUntil { vm.uiState.value.refreshing }
        val calls = repo.refreshCalls
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            org.junit.Assert.assertSame(vm, androidx.lifecycle.ViewModelProvider(compose.activity)[dev.bruze.forekast.feature.weather.WeatherViewModel::class.java])
            org.junit.Assert.assertEquals(calls, repo.refreshCalls)
            org.junit.Assert.assertTrue(vm.uiState.value.refreshing)
            vm.setScenario(dev.bruze.forekast.data.demo.DemoScenario.Success)
        }
        awaitForecast()
    }
    @Test fun forecastPassesAutomatedAccessibilityChecks() {
        awaitForecast()
        compose.enableAccessibilityChecks()
        compose.onNodeWithText("Ciudades").performClick()
        compose.onNodeWithText("Buscar ciudad").performClick()
    }
    @Test fun homonymousCitiesKeepDistinctIdentity() {
        awaitForecast()
        compose.onNodeWithText("Ciudades").performClick()
        compose.onNodeWithText("Buscar ciudad").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Montevideo")
        val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[dev.bruze.forekast.feature.weather.WeatherViewModel::class.java]
        compose.waitUntil(10_000) { vm.uiState.value.searchResults.size == 2 }
        org.junit.Assert.assertEquals(setOf("montevideo-uy", "montevideo-us"), vm.uiState.value.searchResults.map { it.id }.toSet())
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag("location-montevideo-us"))
        compose.onNodeWithTag("location-montevideo-us").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Minnesota, Estados Unidos").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Minnesota, Estados Unidos").assertIsDisplayed()
    }
    @Test fun forecastShowsHourlyAndDailyContent() {
        awaitForecast()
        val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[dev.bruze.forekast.feature.weather.WeatherViewModel::class.java]
        org.junit.Assert.assertEquals(24, vm.uiState.value.snapshot!!.hours.size)
        org.junit.Assert.assertEquals(7, vm.uiState.value.snapshot!!.days.size)
        compose.onNodeWithTag("forecast-list").performScrollToNode(hasText("Próximas horas"))
        compose.onNodeWithText("Próximas horas").assertIsDisplayed()
        compose.onNodeWithTag("forecast-list").performScrollToNode(hasContentDescription("Probabilidad de precipitación", substring = true))
        compose.onNodeWithTag("forecast-list").performScrollToNode(hasText("Próximos días"))
        compose.onNodeWithText("Próximos días").assertIsDisplayed()
        compose.onNodeWithTag("forecast-list").performScrollToNode(hasContentDescription("Mínima", substring = true))
        compose.onAllNodes(hasContentDescription("Mínima", substring = true))[0].assertIsDisplayed()
    }
    @Test fun failureWithoutCachedDataOffersRetryAndCityChange() {
        awaitForecast()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Error sin datos"))
        compose.onNodeWithText("Error sin datos").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Ver pronóstico"))
        compose.onNodeWithText("Ver pronóstico").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("No pudimos actualizar").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Reintentar").assertExists()
        compose.onNodeWithText("Buscar ciudad").assertExists()
    }
    @Test fun sixthFavoriteShowsTheLimit() {
        awaitForecast()
        val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[dev.bruze.forekast.feature.weather.WeatherViewModel::class.java]
        dev.bruze.forekast.data.demo.DemoCatalog.locations.take(5).forEach { vm.toggleFavorite(it.id) }
        vm.selectLocation("ushuaia")
        compose.waitUntil(10_000) { vm.uiState.value.location.id == "ushuaia" }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Ya guardaste cinco ciudades", substring = true))
        compose.onNodeWithText("Ya guardaste cinco ciudades", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Guardar favorita").assertIsNotEnabled()
    }
    @Test fun searchesACityAndReturnsToItsForecast() {
        awaitForecast()
        compose.onNodeWithText("Ciudades").performClick()
        compose.onNodeWithText("Buscar ciudad").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Madrid")
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("location-madrid").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("location-madrid").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Comunidad de Madrid, España").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Madrid").assertIsDisplayed()
    }

    @Test fun changesUnitsAndSurvivesActivityRecreation() {
        awaitForecast()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("°F · Fahrenheit").performClick()
        compose.onNodeWithText("Oscuro").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithContentDescription("64 grados Fahrenheit").assertExists()
        File(compose.activity.getExternalFilesDir(null), "forekast-dark.png").outputStream().use { output ->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Test fun errorWithSavedDataKeepsContentVisible() {
        awaitForecast()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Error con datos guardados"))
        compose.onNodeWithText("Error con datos guardados").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Ver pronóstico"))
        compose.onNodeWithText("Ver pronóstico").performClick()
        compose.onNodeWithText("Pronóstico guardado").assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Último dato disponible"))
        compose.onNodeWithText("Último dato disponible").assertIsDisplayed()
    }
}
