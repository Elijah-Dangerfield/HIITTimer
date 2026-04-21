package com.dangerfield.hiittimer.libraries.ui.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.dangerfield.hiittimer.system.AppTheme
import com.dangerfield.hiittimer.system.Dimension
import com.dangerfield.hiittimer.system.VerticalSpacerD500
import com.dangerfield.hiittimer.libraries.ui.PreviewContent
import com.dangerfield.hiittimer.libraries.ui.components.button.Button
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonSize
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonStyle
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonType
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.settings_report_bug
import rounds.libraries.resources.generated.resources.shake_dialog_dismiss
import rounds.libraries.resources.generated.resources.shake_dialog_dont_show_again

@Composable
fun ShakeDialog(
    headline: String,
    subtext: String?,
    onDismiss: () -> Unit,
    onReportBug: () -> Unit,
    modifier: Modifier = Modifier,
    state: DialogState = rememberDialogState(),
    onDontShowAgain: (() -> Unit)? = null,
) {
    BasicDialog(
        state = state,
        onDismissRequest = onDismiss,
        modifier = modifier,
        topContent = {
            Text(
                text = headline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (subtext != null) {
                    Spacer(modifier = Modifier.height(Dimension.D300))
                    Text(
                        text = subtext,
                        typography = AppTheme.typography.Body.B600,
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    VerticalSpacerD500()
                }
            }
        },
        bottomContent = {
            Column{
                Button(
                    onClick = {
                        state.dismiss()
                        onReportBug()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.Medium,
                    type = ButtonType.Danger,
                ) {
                    Text(stringResource(AppRes.string.settings_report_bug))
                }

                Spacer(modifier = Modifier.height(Dimension.D500))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.Medium,
                    style = ButtonStyle.Text
                ) {
                    Text(stringResource(AppRes.string.shake_dialog_dismiss))
                }

                if (onDontShowAgain != null) {
                    Button(
                        onClick = {
                            state.dismiss()
                            onDontShowAgain()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        size = ButtonSize.Small,
                        style = ButtonStyle.Text,
                    ) {
                        Text(
                            text = stringResource(AppRes.string.shake_dialog_dont_show_again),
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun ShakeDialogPreview_WithSubtext() {
    PreviewContent {
        ShakeDialog(
            headline = "I felt that.",
            subtext = "Testing the waters?",
            onDismiss = {},
            onReportBug = {},
        )
    }
}

@Preview
@Composable
private fun ShakeDialogPreview_NoSubtext() {
    PreviewContent {
        ShakeDialog(
            headline = "Whoa.",
            subtext = null,
            onDismiss = {},
            onReportBug = {},
        )
    }
}

@Preview
@Composable
private fun ShakeDialogPreview_LongMessage() {
    PreviewContent {
        ShakeDialog(
            headline = "You really like shaking me.",
            subtext = "I've lost count.",
            onDismiss = {},
            onReportBug = {},
        )
    }
}
