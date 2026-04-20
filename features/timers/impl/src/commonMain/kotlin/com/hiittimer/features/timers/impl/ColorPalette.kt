package com.dangerfield.hiittimer.features.timers.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

object ColorPalette {
    val swatches: List<Long> = listOf(
        0xFFFF5252, // red
        0xFFFF9100, // orange
        0xFFFFCA28, // amber
        0xFF66BB6A, // green
        0xFF26A69A, // teal
        0xFF42A5F5, // blue
        0xFF5C6BC0, // indigo
        0xFFAB47BC, // purple
        0xFFEC407A, // pink
        0xFF78909C, // blue grey
        0xFF8D6E63, // brown
        0xFF424242, // near-black
    )

    val defaultWorkArgb: Int = 0xFFFF5252.toInt()
    val defaultRestArgb: Int = 0xFF42A5F5.toInt()
    val warmupArgb: Int = 0xFFFFCA28.toInt()   // amber
    val lowIntensityArgb: Int = 0xFF66BB6A.toInt() // green

    fun onColorFor(argb: Int): Color {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        return if (luminance > 0.55f) Color.Black else Color.White
    }

    fun onColorFor(color: Color): Color {
        val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
        return if (luminance > 0.55f) Color.Black else Color.White
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
private fun ColorPalettePreview() {
    FlowRow(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorPalette.swatches.forEach { colorLong ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(colorLong))
            )
        }
    }
}
