package dev.bruze.forekast.designsystem

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import dev.bruze.forekast.R
import dev.bruze.forekast.core.model.*
import kotlin.math.roundToInt

@StringRes fun Condition.label(): Int = when (this) {
    Condition.Clear -> R.string.condition_clear
    Condition.PartlyCloudy -> R.string.condition_partly
    Condition.Drizzle -> R.string.condition_drizzle
    Condition.Cloudy -> R.string.condition_cloudy
    Condition.Rain -> R.string.condition_rain
    Condition.Snow -> R.string.condition_snow
    Condition.Thunderstorm -> R.string.condition_storm
    Condition.Fog -> R.string.condition_fog
    Condition.Unknown -> R.string.condition_unknown
}
@Composable fun temperature(value: Double?, unit: TemperatureUnit): String =
    value?.let { "${it.inUnit(unit).roundToInt()}°" } ?: stringResource(R.string.missing_symbol)

@Composable fun temperatureDescription(value: Double?, unit: TemperatureUnit): String = value?.let {
    val rounded = it.inUnit(unit).roundToInt()
    pluralStringResource(R.plurals.temperature_accessible, kotlin.math.abs(rounded), rounded,
        stringResource(if (unit == TemperatureUnit.Celsius) R.string.unit_celsius else R.string.unit_fahrenheit))
} ?: stringResource(R.string.data_missing)

@Composable fun probability(value: Int?): String = value?.let { stringResource(R.string.percent, it) }
    ?: stringResource(R.string.missing_symbol)
