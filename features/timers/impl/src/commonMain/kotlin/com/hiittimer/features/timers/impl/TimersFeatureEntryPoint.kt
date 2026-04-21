package com.dangerfield.hiittimer.features.timers.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.dangerfield.hiittimer.features.settings.SettingsRoute
import com.dangerfield.hiittimer.features.timers.BlockEditRoute
import com.dangerfield.hiittimer.features.timers.RunnerRoute
import com.dangerfield.hiittimer.features.timers.TimerDetailRoute
import com.dangerfield.hiittimer.features.timers.TimerListRoute
import com.dangerfield.hiittimer.features.timers.TimerPresetRoute
import com.dangerfield.hiittimer.features.timers.impl.blockedit.BlockEditScreen
import com.dangerfield.hiittimer.features.timers.impl.blockedit.BlockEditViewModel
import com.dangerfield.hiittimer.features.timers.impl.detail.TimerDetailScreen
import com.dangerfield.hiittimer.features.timers.impl.detail.TimerDetailViewModel
import com.dangerfield.hiittimer.features.timers.impl.list.TimerListScreen
import com.dangerfield.hiittimer.features.timers.impl.list.TimerListViewModel
import com.dangerfield.hiittimer.features.timers.impl.preset.TimerPresetScreen
import com.dangerfield.hiittimer.features.timers.impl.preset.TimerPresetViewModel
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
    private val presetViewModelFactory: () -> TimerPresetViewModel,
    private val detailViewModelFactory: (timerId: String) -> TimerDetailViewModel,
    private val blockEditViewModelFactory: (timerId: String, blockId: String) -> BlockEditViewModel,
    private val runnerViewModelFactory: (timerId: String) -> RunnerViewModel,
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<TimerListRoute> {
            val vm: TimerListViewModel = viewModel { listViewModelFactory() }
            TimerListScreen(
                viewModel = vm,
                onOpenDetail = { id, isNew -> router.navigate(TimerDetailRoute(timerId = id, isNew = isNew)) },
                onStart = { router.navigate(RunnerRoute(timerId = it)) },
                onOpenSettings = { router.navigate(SettingsRoute()) },
                onCreateNew = { router.navigate(TimerPresetRoute) },
            )
        }

        screen<TimerPresetRoute> {
            val vm: TimerPresetViewModel = viewModel { presetViewModelFactory() }
            TimerPresetScreen(
                viewModel = vm,
                onOpenTimer = {
                    router.popBackTo(TimerPresetRoute, inclusive = true)
                    router.navigate(TimerDetailRoute(timerId = it, isNew = true))
                },
                onBack = { router.goBack() },
            )
        }

        screen<TimerDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TimerDetailRoute>()
            val vm: TimerDetailViewModel = viewModel { detailViewModelFactory(route.timerId) }
            TimerDetailScreen(
                viewModel = vm,
                isNew = route.isNew,
                onBack = { router.goBack() },
                onStart = { router.navigate(RunnerRoute(timerId = it)) },
                onOpenBlock = { t, b -> router.navigate(BlockEditRoute(timerId = t, blockId = b)) },
                onOpenDuplicate = { router.navigate(TimerDetailRoute(timerId = it)) },
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
            RunnerScreen(
                viewModel = vm,
                onExit = { router.goBack() },
                onSupportClick = { router.openWebLink(TIP_JAR_URL) },
            )
        }
    }

    companion object {
        private const val TIP_JAR_URL = "https://buymeacoffee.com/elidangerfield"
    }
}
