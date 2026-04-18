package com.dangerfield.hiittimer.features.settings.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import com.dangerfield.hiittimer.features.settings.SettingsRoute
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
) : FeatureEntryPoint {

    override fun NavGraphBuilder.buildNavGraph(router: Router) {
        screen<SettingsRoute> {
            val vm: SettingsViewModel = viewModel { settingsViewModelFactory() }
            SettingsScreen(
                viewModel = vm,
                onBack = { router.goBack() },
                onOpenUrl = { router.openWebLink(it) },
            )
        }
    }
}
