package com.dangerfield.hiittimer.features.timers.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.dangerfield.hiittimer.features.timers.Block

@Composable
internal fun BlockStrip(blocks: List<Block>) {
    val totalSeconds = blocks.sumOf { it.duration.inWholeSeconds.toInt() }.coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        blocks.forEach { block ->
            val weight = (block.duration.inWholeSeconds.toFloat() / totalSeconds.toFloat())
                .coerceAtLeast(0.02f)
            val base = Color(block.colorArgb)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            0f to lerp(base, Color.White, 0.18f),
                            1f to lerp(base, Color.Black, 0.22f),
                        ),
                    ),
            )
        }
    }
}
