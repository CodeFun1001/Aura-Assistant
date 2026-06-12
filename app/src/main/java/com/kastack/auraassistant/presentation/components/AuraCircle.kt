package com.kastack.auraassistant.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kastack.auraassistant.domain.models.AssistantState
import kotlin.math.sin

@Composable
fun AuraCircle(
    state: AssistantState,
    amplitude: Float = 0f,
    size: Dp = 220.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_infinite")

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.93f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 2200, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "breath"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f, targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            tween(3200, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "glow_alpha"
    )

    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(3600, easing = LinearEasing)
        ), label = "ring_angle"
    )

    val ringAngleCcw by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(5000, easing = LinearEasing)
        ), label = "ring_ccw"
    )

    val targetAmp = if (state is AssistantState.Listening) amplitude.coerceIn(0f, 1f) else 0f
    val smoothAmp by animateFloatAsState(
        targetValue = targetAmp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "smooth_amp"
    )

    val palette = paletteFor(state)

    val liveListenColor = listeningColor(smoothAmp)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val cx   = this.size.width  / 2f
            val cy   = this.size.height / 2f
            val base = this.size.minDimension / 2f

            val ampScaleBoost = 1f + smoothAmp * 0.42f
            val scale         = breathScale * ampScaleBoost
            val combinedGlow  = (glowAlpha + smoothAmp * 1.1f).coerceIn(0f, 1f)

            val outerR = if (smoothAmp < 0.04f) 0f
            else base * (1.12f + smoothAmp * 1.6f)
            if (outerR > 0f) {
                drawGlowLayer(cx, cy, outerR, liveListenColor, (combinedGlow * 0.22f).coerceIn(0f, 1f))
            }

            val midR = base * 0.76f * scale
            val midColor = if (state is AssistantState.Listening) liveListenColor else palette.glow
            drawGlowLayer(cx, cy, midR, midColor, (combinedGlow * 0.48f).coerceIn(0f, 1f))

            val innerR = base * 0.56f * scale
            drawGlowLayer(cx, cy, innerR, palette.glow, (combinedGlow * 0.78f).coerceIn(0f, 1f))

            val coreR = base * 0.42f * scale
            drawCoreCircle(cx, cy, coreR, palette.core, palette.glow)

            if (state is AssistantState.Processing ||
                state is AssistantState.Responding  ||
                state is AssistantState.Validating) {
                drawSpinningRing(cx, cy, coreR * 1.55f, palette.ring, ringAngle, 2.8.dp.toPx(), arcSweep = 70f)
                drawSpinningRing(cx, cy, coreR * 1.80f, palette.ring.copy(alpha = 0.45f), ringAngleCcw, 1.6.dp.toPx(), arcSweep = 45f)
            }

            if (state is AssistantState.Listening) {
                val pulseR = coreR * 1.22f + smoothAmp * coreR * 0.55f
                drawCircle(
                    color = liveListenColor.copy(alpha = (0.28f + smoothAmp * 0.50f).coerceIn(0f, 1f)),
                    radius = pulseR.coerceAtLeast(4f),
                    center = Offset(cx, cy),
                    style = Stroke(width = (1.5f + smoothAmp * 2.5f).dp.toPx())
                )
                if (smoothAmp > 0.18f) {
                    drawCircle(
                        color = liveListenColor.copy(alpha = (0.14f + smoothAmp * 0.28f).coerceIn(0f, 1f)),
                        radius = (coreR * 2.0f + smoothAmp * coreR * 0.8f).coerceAtLeast(4f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                if (smoothAmp > 0.55f) {
                    drawCircle(
                        color = liveListenColor.copy(alpha = 0.10f),
                        radius = (coreR * 2.8f + smoothAmp * coreR).coerceAtLeast(4f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}

private data class AuraPalette(val core: Color, val glow: Color, val ring: Color)

private fun listeningColor(amplitude: Float): Color = when {
    amplitude < 0.10f -> Color(0xFF3B82F6)
    amplitude < 0.25f -> Color(0xFF06B6D4)
    amplitude < 0.45f -> Color(0xFF8B5CF6)
    amplitude < 0.70f -> Color(0xFFD946EF)
    else              -> Color(0xFFEC4899)
}

@Composable
private fun paletteFor(state: AssistantState): AuraPalette {
    val coreTarget = when (state) {
        is AssistantState.Idle       -> Color(0xFF6D28D9)
        is AssistantState.Listening  -> Color(0xFF0EA5E9)
        is AssistantState.Typing     -> Color(0xFF7C3AED)
        is AssistantState.Validating -> Color(0xFF4F46E5)
        is AssistantState.Processing -> Color(0xFFF59E0B)
        is AssistantState.Responding -> Color(0xFF059669)
        is AssistantState.Error      -> Color(0xFFDC2626)
    }
    val glowTarget = when (state) {
        is AssistantState.Idle       -> Color(0xFFA78BFA)
        is AssistantState.Listening  -> Color(0xFF38BDF8)
        is AssistantState.Typing     -> Color(0xFFC4B5FD)
        is AssistantState.Validating -> Color(0xFF818CF8)
        is AssistantState.Processing -> Color(0xFFFBBF24)
        is AssistantState.Responding -> Color(0xFF34D399)
        is AssistantState.Error      -> Color(0xFFF87171)
    }
    val ringTarget = when (state) {
        is AssistantState.Idle       -> Color(0xFF4C1D95)
        is AssistantState.Listening  -> Color(0xFF0284C7)
        is AssistantState.Typing     -> Color(0xFF5B21B6)
        is AssistantState.Validating -> Color(0xFF3730A3)
        is AssistantState.Processing -> Color(0xFFD97706)
        is AssistantState.Responding -> Color(0xFF047857)
        is AssistantState.Error      -> Color(0xFFB91C1C)
    }

    val spec = tween<Color>(durationMillis = 450)
    val core by animateColorAsState(coreTarget, spec, label = "core")
    val glow by animateColorAsState(glowTarget, spec, label = "glow")
    val ring by animateColorAsState(ringTarget, spec, label = "ring")

    return AuraPalette(core, glow, ring)
}

private fun DrawScope.drawGlowLayer(
    cx: Float, cy: Float, r: Float, color: Color, alpha: Float
) {
    val safeR = r.coerceAtLeast(1f)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to color.copy(alpha = alpha.coerceIn(0f, 1f)),
                0.65f to color.copy(alpha = (alpha * 0.35f).coerceIn(0f, 1f)),
                1.0f to Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = safeR
        ),
        radius = safeR,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawCoreCircle(
    cx: Float, cy: Float, r: Float, core: Color, glow: Color
) {
    val safeR = r.coerceAtLeast(6f)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to glow.copy(alpha = 0.95f),
                0.55f to core.copy(alpha = 0.95f),
                1.0f to core
            ),
            center = Offset(cx, cy),
            radius = safeR
        ),
        radius = safeR,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawSpinningRing(
    cx: Float, cy: Float, r: Float,
    color: Color, angleDeg: Float,
    stroke: Float,
    arcSweep: Float = 65f
) {
    val safeR = r.coerceAtLeast(4f)
    val top   = Offset(cx - safeR, cy - safeR)
    val sz    = Size(safeR * 2f, safeR * 2f)
    repeat(3) { i ->
        val start = angleDeg + i * 120f
        drawArc(
            color = color.copy(alpha = 0.75f),
            startAngle = start,
            sweepAngle = arcSweep,
            useCenter = false,
            topLeft = top,
            size = sz,
            style = Stroke(width = stroke)
        )
    }
}