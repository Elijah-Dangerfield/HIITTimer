package com.dangerfield.hiittimer.features.settings.impl.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension
import com.dangerfield.hiittimer.system.VerticalSpacerD1000
import com.dangerfield.hiittimer.system.VerticalSpacerD500
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import com.dangerfield.hiittimer.libraries.ui.components.Screen
import com.dangerfield.hiittimer.libraries.ui.components.button.Button
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonSize
import com.dangerfield.hiittimer.libraries.ui.components.header.TopBar
import com.dangerfield.hiittimer.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.screenContentPadding
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.char_counter
import rounds.libraries.resources.generated.resources.common_describe_placeholder
import rounds.libraries.resources.generated.resources.common_message_label
import rounds.libraries.resources.generated.resources.common_send
import rounds.libraries.resources.generated.resources.common_sending
import rounds.libraries.resources.generated.resources.feedback_subtitle
import rounds.libraries.resources.generated.resources.feedback_title

private const val FEEDBACK_CHAR_LIMIT = 200

@Composable
fun FeedbackScreen(
    state: FeedbackState,
    onAction: (FeedbackAction) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val canSubmit = state.message.isNotBlank() && !state.isSubmitting

    Screen(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                title = stringResource(AppRes.string.feedback_title),
                onNavigateBack = { onAction(FeedbackAction.Back) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .screenContentPadding(
                    paddingValues = paddingValues,
                    includeImePadding = true
                ),
            verticalArrangement = Arrangement.Top
        ) {
            VerticalSpacerD1000()

            Text(
                text = stringResource(AppRes.string.feedback_subtitle),
                typography = AppTheme.typography.Body.B700,
                color = AppTheme.colors.textSecondary
            )

            VerticalSpacerD500()

            OutlinedTextField(
                value = state.message,
                onValueChange = { newValue ->
                    val limited = newValue.take(FEEDBACK_CHAR_LIMIT)
                    onAction(FeedbackAction.MessageChanged(limited))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimension.D1900),
                label = { Text(stringResource(AppRes.string.common_message_label)) },
                placeholder = { Text(stringResource(AppRes.string.common_describe_placeholder)) },
                singleLine = false,
                minLines = 6,
                maxLines = 10,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSubmit) {
                            focusManager.clearFocus(force = true)
                            onAction(FeedbackAction.Submit)
                        }
                    }
                )
            )

            VerticalSpacerD500()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val messageLength = state.message.length
                val counterColor = if (messageLength >= FEEDBACK_CHAR_LIMIT) {
                    AppTheme.colors.danger
                } else {
                    AppTheme.colors.textSecondary
                }

                Text(
                    text = stringResource(AppRes.string.char_counter, messageLength, FEEDBACK_CHAR_LIMIT),
                    color = counterColor,
                    typography = AppTheme.typography.Body.B500
                )
            }

            state.errorMessage?.let {
                VerticalSpacerD500()
                Text(
                    text = it,
                    color = AppTheme.colors.danger,
                    textAlign = TextAlign.Start
                )
            }

            VerticalSpacerD1000()

            Button(
                modifier = Modifier.fillMaxWidth(),
                size = ButtonSize.Large,
                enabled = canSubmit,
                onClick = {
                    focusManager.clearFocus(force = true)
                    if (canSubmit) {
                        onAction(FeedbackAction.Submit)
                    }
                }
            ) {
                Text(if (state.isSubmitting) stringResource(AppRes.string.common_sending) else stringResource(AppRes.string.common_send))
            }

            VerticalSpacerD500()
        }
    }
}

@Preview
@Composable
private fun FeedbackScreenPreviewDisabled() {
    PreviewContent {
        FeedbackScreen(
            state = FeedbackState(
                message = ""
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun FeedbackScreenPreview() {
    PreviewContent {
        FeedbackScreen(
            state = FeedbackState(
                message = "I love this app!"
            ),
            onAction = {}
        )
    }
}

