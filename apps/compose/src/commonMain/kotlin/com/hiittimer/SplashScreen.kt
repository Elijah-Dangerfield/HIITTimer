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
import androidx.navigation.NavGraphBuilder
import com.dangerfield.hiittimer.libraries.core.BuildInfo
import com.dangerfield.hiittimer.libraries.core.Platform
import com.dangerfield.hiittimer.libraries.navigation.AnimationType
import com.dangerfield.hiittimer.libraries.navigation.FeatureEntryPoint
import com.dangerfield.hiittimer.libraries.navigation.Route
import com.dangerfield.hiittimer.libraries.navigation.Router
import com.dangerfield.hiittimer.libraries.navigation.screen
import com.dangerfield.hiittimer.libraries.navigation.serializableType
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import com.dangerfield.hiittimer.libraries.ui.components.CircularProgressIndicator
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.components.text.TypewriterTextEffect
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.ui.tooling.preview.Preview
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.reflect.typeOf

@Serializable
class SplashRoute : Route(
    enter = AnimationType.None,
    exit = AnimationType.None,
    popExit = AnimationType.None,
)

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class SplashScreenEntryPoint : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<SplashRoute>(
            typeMap = mapOf(typeOf<AnimationType>() to serializableType<AnimationType>())
        ) {
            // Placeholder - actual SplashScreen is rendered in App.kt with access to AppViewModel
            // This just registers the route so navigation works
        }
    }
}

/**
 * Splash screen that handles platform-specific behavior:
 * - **iOS**: Shows typewriter animation, then navigates when complete
 * - **Android**: Immediately navigates (native splash already showed)
 * 
 * @param destinationRoute The route to navigate to once splash is complete
 * @param onNavigate Callback to perform the navigation
 */
@Composable
fun SplashScreen(
    destinationRoute: Route?,
    onNavigate: (Route) -> Unit,
) {
    // On Android, navigate immediately - native splash already showed
    if (BuildInfo.platform == Platform.Android) {
        LaunchedEffect(destinationRoute) {
            destinationRoute?.let { onNavigate(it) }
        }
        // Show matching background while navigation happens
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = AppTheme.colors.background.color),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Empty - just background color matching native splash
        }
        return
    }
    
    // iOS: Show the full typewriter splash experience
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTheme.colors.background.color),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var shouldShowLoading by remember { mutableStateOf(false) }
        var isTypewriterComplete by remember { mutableStateOf(false) }
        var hasNavigated by remember { mutableStateOf(false) }

        // After typewriter completes, wait a moment then navigate
        LaunchedEffect(isTypewriterComplete, destinationRoute) {
            if (isTypewriterComplete && destinationRoute != null && !hasNavigated) {
                delay(1000) // Let the text hang for a moment
                hasNavigated = true
                onNavigate(destinationRoute)
            }
        }
        
        // Show loading if we're waiting too long for the destination
        LaunchedEffect(Unit) {
            delay(5000) // Fallback timeout
            if (!hasNavigated) {
                shouldShowLoading = true
            }
        }

        if (shouldShowLoading && !hasNavigated) {
            CircularProgressIndicator()
        } else {
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
}


@Preview
@Composable
private fun PreviewSplashScreen() {
    PreviewContent {
        SplashScreen(
            destinationRoute = null,
            onNavigate = {}
        )
    }
}