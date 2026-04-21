package com.dangerfield.hiittimer.features.settings.impl.bugreport

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
import com.dangerfield.hiittimer.libraries.ui.components.SectionCard
import com.dangerfield.hiittimer.libraries.ui.components.SummaryRow
import com.dangerfield.hiittimer.libraries.ui.components.button.Button
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonSize
import com.dangerfield.hiittimer.libraries.ui.components.header.TopBar
import com.dangerfield.hiittimer.libraries.ui.components.text.OutlinedTextField
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.libraries.ui.screenContentPadding
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.bug_report_captured_details
import rounds.libraries.resources.generated.resources.bug_report_error_code
import rounds.libraries.resources.generated.resources.bug_report_report_id
import rounds.libraries.resources.generated.resources.bug_report_subtitle
import rounds.libraries.resources.generated.resources.bug_report_title
import rounds.libraries.resources.generated.resources.char_counter
import rounds.libraries.resources.generated.resources.common_describe_placeholder
import rounds.libraries.resources.generated.resources.common_message_label
import rounds.libraries.resources.generated.resources.common_send
import rounds.libraries.resources.generated.resources.common_sending

private const val BUG_REPORT_CHAR_LIMIT = 180

@Composable
fun BugReportScreen(
    state: BugReportState,
    onAction: (BugReportAction) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val canSubmit = state.message.isNotBlank() && !state.isSubmitting

    Screen(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                title = stringResource(AppRes.string.bug_report_title),
                onNavigateBack = { onAction(BugReportAction.Back) }
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

            if (state.hasContext) {
                SectionCard(title = stringResource(AppRes.string.bug_report_captured_details)) {
                    state.contextMessage?.let {
                        Text(
                            text = it,
                            typography = AppTheme.typography.Body.B500,
                            color = AppTheme.colors.danger
                        )
                    }

                    state.errorCode?.let {
                        SummaryRow(
                            label = stringResource(AppRes.string.bug_report_error_code),
                            value = "$it"
                        )
                    }

                    state.logId?.let {
                        SummaryRow(
                            label = stringResource(AppRes.string.bug_report_report_id),
                            value = it
                        )
                    }
                }

                VerticalSpacerD1000()
            }

            Text(
                text = stringResource(AppRes.string.bug_report_subtitle),
                typography = AppTheme.typography.Body.B700,
                color = AppTheme.colors.textSecondary
            )

            VerticalSpacerD1000()

            OutlinedTextField(
                value = state.message,
                onValueChange = { newValue ->
                    val limited = newValue.take(BUG_REPORT_CHAR_LIMIT)
                    onAction(BugReportAction.MessageChanged(limited))
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
                            onAction(BugReportAction.Submit)
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
                val counterColor = if (messageLength >= BUG_REPORT_CHAR_LIMIT) {
                    AppTheme.colors.danger
                } else {
                    AppTheme.colors.textSecondary
                }

                Text(
                    text = stringResource(AppRes.string.char_counter, messageLength, BUG_REPORT_CHAR_LIMIT),
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
                        onAction(BugReportAction.Submit)
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
private fun BugReportScreenPreview() {
    PreviewContent {
        BugReportScreen(
            state = BugReportState(
                message = "The shortcut sheet would not open.",
                logId = "12356j32345k1",
                errorCode = 1200,
                contextMessage = "Something went wrong while loading."
            ),
            onAction = {}
        )
    }
}

