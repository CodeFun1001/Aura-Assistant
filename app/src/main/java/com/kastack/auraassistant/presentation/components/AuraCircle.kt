package com.kastack.auraassistant.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kastack.auraassistant.domain.models.AssistantState

@Composable
fun AuraCircle(
    state: AssistantState,
    amplitude: Float = 0f,
    size: Dp = 220.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_pulse")

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_alpha"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "ring_rotation"
    )

    val (coreColor, glowColor, ringColor) = when (state) {
        is AssistantState.Idle ->
            Triple(Color(0xFF7C3AED), Color(0xFFA78BFA), Color(0xFF5B21B6))
        is AssistantState.Listening ->
            Triple(Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFF0369A1))
        is AssistantState.Typing ->
            Triple(Color(0xFF8B5CF6), Color(0xFFC4B5FD), Color(0xFF6D28D9))
        is AssistantState.Validating ->
            Triple(Color(0xFF6366F1), Color(0xFFA5B4FC), Color(0xFF4338CA))
        is AssistantState.Processing ->
            Triple(Color(0xFFF59E0B), Color(0xFFFDE68A), Color(0xFFD97706))
        is AssistantState.Responding ->
            Triple(Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFF059669))
        is AssistantState.Error ->
            Triple(Color(0xFFEF4444), Color(0xFFFCA5A5), Color(0xFFDC2626))
    }

    val amplitudeScale = if (state is AssistantState.Listening) {
        1f + (amplitude * 0.65f)
    } else 1f

    val animatedAmplitudeScale by animateFloatAsState(
        targetValue = amplitudeScale,
        animationSpec = tween(durationMillis = 80, easing = FastOutLinearInEasing),
        label = "amplitude_scale"
    )

    val effectiveScale = breathScale * animatedAmplitudeScale

    val glowAlpha = (breathAlpha + amplitude * 0.35f).coerceIn(0f, 1f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val baseRadius = this.size.minDimension / 2f

            val coreRadius = baseRadius * 0.58f * effectiveScale
            val glowRadius = baseRadius * 0.78f * effectiveScale
            val outerGlowRadius = baseRadius * 0.95f * effectiveScale

            drawAuraGlow(
                cx = cx, cy = cy,
                radius = outerGlowRadius,
                color = glowColor,
                alpha = glowAlpha * 0.45f
            )

            drawAuraGlow(
                cx = cx, cy = cy,
                radius = glowRadius,
                color = glowColor,
                alpha = glowAlpha * 0.7f
            )

            drawAuraCore(
                cx = cx, cy = cy,
                radius = coreRadius,
                coreColor = coreColor,
                glowColor = glowColor
            )

            if (state is AssistantState.Processing || state is AssistantState.Responding) {
                drawProcessingRing(
                    cx = cx, cy = cy,
                    radius = coreRadius * 1.45f,
                    color = ringColor,
                    rotationDegrees = ringRotation,
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

private fun DrawScope.drawAuraGlow(
    cx: Float, cy: Float,
    radius: Float,
    color: Color,
    alpha: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawAuraCore(
    cx: Float, cy: Float,
    radius: Float,
    coreColor: Color,
    glowColor: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = 0.9f), coreColor),
            center = Offset(cx, cy),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawProcessingRing(
    cx: Float, cy: Float,
    radius: Float,
    color: Color,
    rotationDegrees: Float,
    strokeWidth: Float
) {
    val arcSweep = 90f
    val gap = 30f
    for (i in 0..2) {
        val startAngle = rotationDegrees + i * (360f / 3)
        drawArc(
            color = color.copy(alpha = 0.7f),
            startAngle = startAngle,
            sweepAngle = arcSweep - gap,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )
    }
}