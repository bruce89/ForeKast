package dev.bruze.forekast.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bruze.forekast.core.model.Condition
import kotlin.math.cos
import kotlin.math.sin

/** Ilustración decorativa: la etiqueta accesible pertenece al grupo de datos. */
@Composable
fun WeatherGlyph(condition: Condition, size: Dp = 48.dp, isDay: Boolean? = null) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(size)) {
        scale(this.size.width / 64, this.size.height / 64, Offset.Zero) {
            if (condition == Condition.Clear && isDay == false) {
                drawArc(color, 50f, 260f, false, Offset(14f, 10f), androidx.compose.ui.geometry.Size(38f, 42f), style = Stroke(2.5f))
            } else if (condition == Condition.Clear) {
                drawCircle(color, 12f, Offset(32f, 32f), style = Stroke(2.5f))
                repeat(8) { i ->
                    val angle = i * Math.PI / 4
                    drawLine(color, Offset(32 + 20 * cos(angle).toFloat(), 32 + 20 * sin(angle).toFloat()),
                        Offset(32 + 27 * cos(angle).toFloat(), 32 + 27 * sin(angle).toFloat()), 2.5f)
                }
            } else {
                val cloud = Path().apply {
                    moveTo(15f, 43f); cubicTo(0f, 43f, 2f, 23f, 17f, 22f)
                    cubicTo(22f, 5f, 45f, 7f, 49f, 25f); cubicTo(65f, 24f, 64f, 43f, 50f, 43f); close()
                }
                drawPath(cloud, color, style = Stroke(2.5f))
                if (condition == Condition.Drizzle || condition == Condition.Rain || condition == Condition.Thunderstorm) repeat(3) { i ->
                    drawLine(color, Offset(21f + i * 12, 49f), Offset(17f + i * 12, 57f), 2.5f)
                }
                if (condition == Condition.Snow) repeat(3) { i -> drawCircle(color, 2.5f, Offset(20f + i * 12, 53f)) }
                if (condition == Condition.Fog) drawLine(color, Offset(10f, 53f), Offset(55f, 53f), 2.5f)
                if (condition == Condition.Unknown) drawCircle(color, 2.5f, Offset(32f, 53f))
            }
        }
    }
}
