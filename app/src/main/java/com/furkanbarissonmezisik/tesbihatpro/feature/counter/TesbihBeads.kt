package com.furkanbarissonmezisik.tesbihatpro.feature.counter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.furkanbarissonmezisik.tesbihatpro.core.ui.BeadStyle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign

private const val BEAD_RADIUS = 0.40f
private const val BOW = 0.24f
private const val FOCUS_GROWTH = 0.18f
private const val EDGE_FADE = 0.6f
private const val IMAME_WIDTH = 1.15f
private const val IMAME_HEIGHT = 1.45f
private const val THREAD_SEGMENTS = 16

private val BeadSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 1500f)

/**
 * Sayı animasyonu beklemez: dizi hedefin en fazla bir boncuk gerisinde kalır, hızlı dokunuşta geride
 * kalanı atlar. Konum yalnız çizim aşamasında okunur, böylece animasyon yeniden kompozisyon yapmaz.
 */
@Composable
fun TesbihBeads(
    position: Int,
    imames: List<Int>,
    lastBead: Int?,
    style: BeadStyle,
    spacing: Dp,
    modifier: Modifier = Modifier,
) {
    val animated = remember { Animatable(position.toFloat()) }
    LaunchedEffect(position) {
        val target = position.toFloat()
        val lag = target - animated.value
        if (abs(lag) > 1f) animated.snapTo(target - sign(lag))
        animated.animateTo(target, BeadSpring)
    }
    val focusColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val background = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier) {
        BeadPainter(this, spacing.toPx(), style, background)
            .draw(animated.value, imames, lastBead, focusColor)
    }
}

private class BeadPainter(
    private val scope: DrawScope,
    private val spacing: Float,
    private val style: BeadStyle,
    private val background: Color,
) {
    private val halfHeight = scope.size.height / 2
    private val centerY = halfHeight

    fun draw(position: Float, imames: List<Int>, lastBead: Int?, focusColor: Color) {
        scope.drawCircle(
            color = focusColor,
            radius = spacing * 0.56f,
            center = Offset(xAt(centerY), centerY),
        )
        drawThread(position, lastBead)

        val beadsPerSide = ceil(halfHeight / spacing).toInt() + 1
        val beads = visibleBeads(position, beadsPerSide, lastBead)
        // İmame diğerlerinden büyük olduğu için en üste çizilir.
        beads.filterNot { it in imames }.forEach { drawBead(it, position) }
        beads.filter { it in imames }.forEach { drawImame(it, position) }
    }

    private fun yOf(bead: Int, position: Float) = centerY + (position - bead) * spacing

    /** Dizinin hafif kavisi: ortada içe, uçlarda dışa doğru. */
    private fun xAt(y: Float): Float {
        val fromCenter = (y - centerY) / halfHeight
        return scope.size.width / 2 + spacing * BOW * (fromCenter * fromCenter - 0.5f)
    }

    /** Boncuklar opak kalır ki ip içlerinden görünmesin; uçlara doğru zemine karışır. */
    private fun faded(color: Color, y: Float): Color {
        val fromCenter = (y - centerY) / halfHeight
        return lerp(color, background, (EDGE_FADE * fromCenter * fromCenter).coerceIn(0f, 1f))
    }

    private fun focusScale(bead: Int, position: Float): Float =
        1f + FOCUS_GROWTH * (1f - abs(position - bead)).coerceAtLeast(0f)

    private fun drawThread(position: Float, lastBead: Int?) {
        val top = lastBead?.let { yOf(it, position).coerceAtLeast(0f) } ?: 0f
        val bottom = scope.size.height
        if (top >= bottom) return
        val path = Path().apply {
            moveTo(xAt(top), top)
            for (segment in 1..THREAD_SEGMENTS) {
                val y = top + (bottom - top) * segment / THREAD_SEGMENTS
                lineTo(xAt(y), y)
            }
        }
        scope.drawPath(
            path = path,
            color = style.threadColor,
            style = Stroke(width = with(scope) { 2.dp.toPx() }, cap = StrokeCap.Round),
        )
    }

    private fun shadedBrush(center: Offset, radius: Float, highlight: Color, base: Color) =
        Brush.radialGradient(
            colors = listOf(faded(highlight, center.y), faded(base, center.y)),
            center = center - Offset(radius * 0.35f, radius * 0.35f),
            radius = radius * 1.6f,
        )

    private fun drawBead(bead: Int, position: Float) {
        val y = yOf(bead, position)
        val center = Offset(xAt(y), y)
        val radius = spacing * BEAD_RADIUS * focusScale(bead, position)
        scope.drawCircle(
            brush = shadedBrush(center, radius, style.highlightColor, style.baseColor),
            radius = radius,
            center = center,
        )
    }

    private fun drawImame(bead: Int, position: Float) {
        val y = yOf(bead, position)
        val center = Offset(xAt(y), y)
        val radius = spacing * BEAD_RADIUS * focusScale(bead, position)
        val size = Size(radius * 2 * IMAME_WIDTH, radius * 2 * IMAME_HEIGHT)
        scope.drawOval(
            brush = shadedBrush(
                center = center,
                radius = size.height / 2,
                highlight = style.imameHighlightColor,
                base = style.imameBaseColor,
            ),
            topLeft = center - Offset(size.width / 2, size.height / 2),
            size = size,
        )
        // İki uçtaki halka imameyi sıradan boncuktan ayırır.
        listOf(-1f, 1f).forEach { end ->
            scope.drawCircle(
                color = faded(style.imameBaseColor, y),
                radius = radius * 0.2f,
                center = center + Offset(0f, end * size.height / 2),
            )
        }
    }
}
