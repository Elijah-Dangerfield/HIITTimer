package com.dangerfield.hiittimer.features.timers.impl

import androidx.compose.ui.graphics.Color

object ColorPalette {
    val swatches: List<Long> = listOf(
        0xFFE53935, // red
        0xFFEF6C00, // orange
        0xFFF9A825, // amber
        0xFF43A047, // green
        0xFF00897B, // teal
        0xFF1E88E5, // blue
        0xFF3949AB, // indigo
        0xFF8E24AA, // purple
        0xFFD81B60, // pink
        0xFF546E7A, // blue grey
        0xFF6D4C41, // brown
        0xFF212121, // near-black
    )

    val defaultWorkArgb: Int = 0xFFE53935.toInt()
    val defaultRestArgb: Int = 0xFF1E88E5.toInt()
    val warmupArgb: Int = 0xFFF9A825.toInt()   // amber
    val lowIntensityArgb: Int = 0xFF43A047.toInt() // green

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
