package dev.bruze.forekast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bruze.forekast.designsystem.ForeKastTheme
import dev.bruze.forekast.feature.weather.WeatherViewModel
import dev.bruze.forekast.navigation.ForeKastApp
import dev.bruze.forekast.core.model.ThemeMode

class MainActivity : ComponentActivity() {
    private val model: WeatherViewModel by viewModels {
        val container = (application as ForeKastApplication).container
        viewModelFactory { initializer {
            WeatherViewModel(container.weatherRepository, container.demoController, createSavedStateHandle(), container.clock, container.locations, container.isDemo, container.library, container.preferences)
        } }
    }

    override fun onStart() {
        super.onStart()
        model.onForeground()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by model.uiState.collectAsStateWithLifecycle()
            val dark = when (state.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
            }
            SideEffect {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            ForeKastTheme(state.theme) {
                ForeKastApp(state, model::refresh, model::selectLocation, model::toggleFavorite,
                    model::setUnit, model::setTheme, model::setScenario, model::search, model::clearCities)
            }
        }
    }
}
