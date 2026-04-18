package com.dangerfield.hiittimer.features.timers.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.dangerfield.hiittimer.features.settings.SettingsRoute
import com.dangerfield.hiittimer.features.timers.BlockEditRoute
import com.dangerfield.hiittimer.features.timers.RunnerRoute
import com.dangerfield.hiittimer.features.timers.TimerBuilderRoute
import com.dangerfield.hiittimer.features.timers.TimerListRoute
import com.dangerfield.hiittimer.features.timers.impl.blockedit.BlockEditScreen
import com.dangerfield.hiittimer.features.timers.impl.blockedit.BlockEditViewModel
import com.dangerfield.hiittimer.features.timers.impl.builder.TimerBuilderScreen
import com.dangerfield.hiittimer.features.timers.impl.builder.TimerBuilderViewModel
import com.dangerfield.hiittimer.features.timers.impl.list.TimerListScreen
import com.dangerfield.hiittimer.features.timers.impl.list.TimerListViewModel
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerScreen
import com.dangerfield.hiittimer.features.timers.impl.runner.RunnerViewModel
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
class TimersFeatureEntryPoint(
    private val listViewModelFactory: () -> TimerListViewModel,
    private val builderViewModelFactory: (timerId: String) -> TimerBuilderViewModel,
    private val blockEditViewModelFactory: (timerId: String, blockId: String) -> BlockEditViewModel,
    private val runnerViewModelFactory: (timerId: String) -> RunnerViewModel,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<TimerListRoute> {
            val vm: TimerListViewModel = viewModel { listViewModelFactory() }
            TimerListScreen(
                viewModel = vm,
                onOpenRunner = { router.navigate(RunnerRoute(timerId = it)) },
                onOpenBuilder = { router.navigate(TimerBuilderRoute(timerId = it)) },
                onOpenSettings = { router.navigate(SettingsRoute()) },
            )
        }

        screen<TimerBuilderRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TimerBuilderRoute>()
            val timerId = route.timerId ?: return@screen
            val vm: TimerBuilderViewModel = viewModel { builderViewModelFactory(timerId) }
            TimerBuilderScreen(
                viewModel = vm,
                onBack = { router.goBack() },
                onOpenBlock = { t, b -> router.navigate(BlockEditRoute(timerId = t, blockId = b)) },
                onStart = { router.navigate(RunnerRoute(timerId = it)) },
            )
        }

        screen<BlockEditRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<BlockEditRoute>()
            val vm: BlockEditViewModel = viewModel {
                blockEditViewModelFactory(route.timerId, route.blockId)
            }
            BlockEditScreen(viewModel = vm, onBack = { router.goBack() })
        }

        screen<RunnerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<RunnerRoute>()
            val vm: RunnerViewModel = viewModel { runnerViewModelFactory(route.timerId) }
            RunnerScreen(viewModel = vm, onExit = { router.goBack() })
        }
    }
}
