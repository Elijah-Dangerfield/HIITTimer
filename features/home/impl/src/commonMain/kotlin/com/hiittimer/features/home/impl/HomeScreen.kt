package com.dangerfield.hiittimer.features.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.home_welcome
import rounds.libraries.resources.generated.resources.home_greeting
import rounds.libraries.resources.generated.resources.home_empty_subtitle
import rounds.libraries.resources.generated.resources.home_send_feedback
import rounds.libraries.resources.generated.resources.home_report_bug
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.button.Button
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.VerticalSpacerD500
import com.dangerfield.hiittimer.system.VerticalSpacerD800

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToFeedback: () -> Unit,
    onNavigateToBugReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    Screen(modifier = modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(AppRes.string.home_welcome),
                typography = AppTheme.typography.Heading.H700,
                color = AppTheme.colors.text,
                textAlign = TextAlign.Center,
            )

            VerticalSpacerD800()

            state.userName?.let { userName ->
                Text(
                    text = stringResource(AppRes.string.home_greeting, userName),
                    typography = AppTheme.typography.Body.B600,
                    color = AppTheme.colors.textSecondary,
                )
                VerticalSpacerD500()
            }

            Text(
                text = stringResource(AppRes.string.home_empty_subtitle),
                typography = AppTheme.typography.Body.B500,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            VerticalSpacerD800()

            Button(onClick = onNavigateToFeedback) {
                Text(stringResource(AppRes.string.home_send_feedback))
            }

            VerticalSpacerD500()

            Button(onClick = onNavigateToBugReport) {
                Text(stringResource(AppRes.string.home_report_bug))
            }
        }
    }
}