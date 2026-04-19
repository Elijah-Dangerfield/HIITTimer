package com.dangerfield.hiittimer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.hiittimer.libraries.core.BuildInfo
import com.dangerfield.hiittimer.libraries.core.Platform
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.components.text.TypewriterTextEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview

/**
 * iOS-only splash overlay. Plays a typewriter animation once per process,
 * then calls [onComplete] so the caller can stop rendering it.
 *
 * On Android the native splash is used, so [onComplete] is invoked
 * immediately without animation.
 */
@Composable
fun SplashOverlay(
    onComplete: () -> Unit,
) {
    if (BuildInfo.platform != Platform.iOS) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.background.color),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var isTypewriterComplete by remember { mutableStateOf(false) }
        var hasReported by remember { mutableStateOf(false) }

        LaunchedEffect(isTypewriterComplete) {
            if (isTypewriterComplete && !hasReported) {
                delay(1000)
                hasReported = true
                onComplete()
            }
        }

        TypewriterTextEffect(
            text = "HIIT Timer",
            minDelayInMillis = 50,
            maxDelayInMillis = 150,
            minCharacterChunk = 1,
            maxCharacterChunk = 2,
            onEffectCompleted = { isTypewriterComplete = true }
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = it,
                typography = AppTheme.typography.Brand.B1300,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun PreviewSplashOverlay() {
    PreviewContent {
        SplashOverlay(onComplete = {})
    }
}
