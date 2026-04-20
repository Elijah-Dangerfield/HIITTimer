package com.dangerfield.hiittimer.libraries.inappmessages.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialog
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogHost
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogImage
import com.dangerfield.hiittimer.libraries.inappmessages.InAppMessageDialogResult
import hiittimer.libraries.inappmessages.impl.generated.resources.Res
import hiittimer.libraries.inappmessages.impl.generated.resources.headshot_placeholder
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonGhost
import com.dangerfield.hiittimer.libraries.ui.components.button.ButtonPrimary
import com.dangerfield.hiittimer.libraries.ui.components.dialog.Dialog
import com.dangerfield.hiittimer.libraries.ui.components.text.Text
import com.dangerfield.hiittimer.system.AppTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun InAppMessageOverlay(host: InAppMessageDialogHost) {
    val active by host.active.collectAsStateWithLifecycle()
    val current = active ?: return

    Dialog(
        onDismissRequest = { current.onResult(InAppMessageDialogResult.Dismissed) },
    ) {
        DialogBody(
            dialog = current.dialog,
            onPositive = { current.onResult(InAppMessageDialogResult.Positive) },
            onNegative = { current.onResult(InAppMessageDialogResult.Negative) },
        )
    }
}

@Composable
private fun DialogBody(
    dialog: InAppMessageDialog,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        dialog.image?.let { image ->
            Image(
                painter = painterResource(image.resource()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(16.dp))
        }
        Text(
            text = dialog.title,
            typography = AppTheme.typography.Heading.H700,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = dialog.body,
            typography = AppTheme.typography.Body.B500,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        ButtonPrimary(
            onClick = onPositive,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(dialog.positiveLabel)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            ButtonGhost(onClick = onNegative) {
                Text(dialog.negativeLabel)
            }
        }
    }
}

private fun InAppMessageDialogImage.resource() = when (this) {
    InAppMessageDialogImage.CreatorHeadshot -> Res.drawable.headshot_placeholder
}
