package com.dangerfield.hiittimer

import androidx.navigation.NavGraphBuilder
import com.dangerfield.hiittimer.features.settings.BugReportRoute
import com.dangerfield.hiittimer.libraries.navigation.FeatureEntryPoint
import com.dangerfield.hiittimer.libraries.navigation.Router
import com.dangerfield.hiittimer.libraries.navigation.ShakeDialogRoute
import com.dangerfield.hiittimer.libraries.navigation.dialog
import com.dangerfield.hiittimer.libraries.ui.components.dialog.ShakeDialog
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.stringResource
import rounds.libraries.resources.generated.resources.Res as AppRes
import rounds.libraries.resources.generated.resources.bug_report_context_shake
import rounds.libraries.resources.generated.resources.shake_dialog_headline
import rounds.libraries.resources.generated.resources.shake_dialog_subtext
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class ShakeDialogEntryPoint(
    private val shakeHandler: ShakeHandler,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        dialog<ShakeDialogRoute> { _, dialogState ->
            val shakeContext = stringResource(AppRes.string.bug_report_context_shake)

            val dismiss: () -> Unit = {
                shakeHandler.onDialogDismissed()
                router.goBack()
            }

            ShakeDialog(
                state = dialogState,
                headline = stringResource(AppRes.string.shake_dialog_headline),
                subtext = stringResource(AppRes.string.shake_dialog_subtext),
                onDismiss = dismiss,
                onReportBug = {
                    shakeHandler.onDialogDismissed()
                    router.goBack()
                    router.navigate(
                        BugReportRoute(contextMessage = shakeContext)
                    )
                },
                onDontShowAgain = {
                    shakeHandler.onDontShowAgain()
                    router.goBack()
                },
            )
        }
    }
}
