package com.dangerfield.hiittimer.features.home.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import com.dangerfield.hiittimer.features.home.HomeRoute
import com.dangerfield.hiittimer.features.settings.BugReportRoute
import com.dangerfield.hiittimer.features.settings.FeedbackRoute
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
class HomeFeatureEntryPoint(
    private val homeViewModelFactory: () -> HomeViewModel,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<HomeRoute> {
            val viewModel: HomeViewModel = viewModel { homeViewModelFactory() }
            HomeScreen(
                viewModel = viewModel,
                onNavigateToFeedback = { router.navigate(FeedbackRoute()) },
                onNavigateToBugReport = { router.navigate(BugReportRoute()) },
            )
        }
    }
}
