package com.dangerfield.hiittimer.features.settings.impl

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.dangerfield.hiittimer.features.settings.BugReportRoute
import com.dangerfield.hiittimer.features.settings.FeedbackRoute
import com.dangerfield.hiittimer.features.settings.SettingsRoute
import com.dangerfield.hiittimer.features.settings.impl.bugreport.BugReportScreen
import com.dangerfield.hiittimer.features.settings.impl.bugreport.BugReportViewModel
import com.dangerfield.hiittimer.features.settings.impl.feedback.FeedbackScreen
import com.dangerfield.hiittimer.features.settings.impl.feedback.FeedbackViewModel
import com.dangerfield.hiittimer.libraries.navigation.FeatureEntryPoint
import com.dangerfield.hiittimer.libraries.navigation.Router
import com.dangerfield.hiittimer.libraries.navigation.screen
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, multibinding = true)
@Inject
class SettingsFeatureEntryPoint(
    private val settingsViewModelFactory: () -> SettingsViewModel,
    private val feedbackViewModelFactory: () -> FeedbackViewModel,
    private val bugReportViewModelFactory: (logId: String?, errorCode: Int?, contextMessage: String?) -> BugReportViewModel,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<SettingsRoute> {
            val vm: SettingsViewModel = viewModel { settingsViewModelFactory() }
            SettingsScreen(
                viewModel = vm,
                onBack = { router.goBack() },
                onOpenUrl = { router.openWebLink(it) },
                onNavigateToFeedback = { router.navigate(FeedbackRoute()) },
                onNavigateToBugReport = { router.navigate(BugReportRoute()) },
            )
        }

        screen<FeedbackRoute> {
            val viewModel: FeedbackViewModel = viewModel { feedbackViewModelFactory() }
            val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
            FeedbackScreen(
                state = state,
                onAction = viewModel::takeAction,
            )
        }

        screen<BugReportRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<BugReportRoute>()
            val viewModel: BugReportViewModel = viewModel {
                bugReportViewModelFactory(route.logId, route.errorCode, route.contextMessage)
            }
            val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
            BugReportScreen(
                state = state,
                onAction = viewModel::takeAction,
            )
        }
    }
}
