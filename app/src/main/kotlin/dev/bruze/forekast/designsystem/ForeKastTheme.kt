package dev.bruze.forekast.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.bruze.forekast.core.model.ThemeMode

private val Light = lightColorScheme(
    primary = Color(0xFF215E73), onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EDF4), onPrimaryContainer = Color(0xFF123D4E),
    background = Color(0xFFF5F8FA), surface = Color(0xFFF5F8FA),
    surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFE7EFF3),
    onBackground = Color(0xFF172B36), onSurface = Color(0xFF172B36),
    onSurfaceVariant = Color(0xFF506571),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFA0D3E7), onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF214C5D), onPrimaryContainer = Color(0xFFD8EDF4),
    background = Color(0xFF0F1C23), surface = Color(0xFF0F1C23),
    surfaceContainer = Color(0xFF1B2D37), surfaceContainerHigh = Color(0xFF263C48),
    onBackground = Color(0xFFE2EDF3), onSurface = Color(0xFFE2EDF3),
    onSurfaceVariant = Color(0xFFB1C6D1),
)

@Composable
fun ForeKastTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.System -> isSystemInDarkTheme(); ThemeMode.Dark -> true; ThemeMode.Light -> false }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography(), content = content)
}
