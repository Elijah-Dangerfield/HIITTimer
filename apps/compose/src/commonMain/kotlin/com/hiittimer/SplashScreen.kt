package com.dangerfield.hiittimer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dangerfield.hiittimer.libraries.core.BuildInfo
import com.dangerfield.hiittimer.libraries.core.Platform
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import com.dangerfield.hiittimer.system.AppTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.cd_rounds_logo
import rounds.libraries.resources.generated.resources.logo_rounds_word
import androidx.compose.ui.tooling.preview.Preview

private const val FadeInMillis = 450
private const val HoldMillis = 650
private const val FadeOutMillis = 450

@Composable
fun SplashOverlay(
    onComplete: () -> Unit,
) {
    if (BuildInfo.platform != Platform.iOS) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    val alpha = remember { Animatable(0f) }
    var hasReported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(FadeInMillis, easing = LinearEasing))
        delay(HoldMillis.toLong())
        alpha.animateTo(0f, tween(FadeOutMillis, easing = LinearEasing))
        if (!hasReported) {
            hasReported = true
            onComplete()
        }
    }

    SplashContent(alpha = alpha.value)
}

@Composable
private fun SplashContent(alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background.color)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        0f to Color(0xFF630A53).copy(alpha = 0.22f),
                        0.45f to Color(0xFFC91F77).copy(alpha = 0.14f),
                        1f to Color.Transparent,
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 0.7f,
                    ),
                )
            }
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo_rounds_word),
            contentDescription = stringResource(AppRes.string.cd_rounds_logo),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.66f)
                .padding(horizontal = 24.dp),
        )
    }
}

@Preview
@Composable
private fun PreviewSplashOverlay() {
    PreviewContent {
        SplashContent(alpha = 1f)
    }
}
