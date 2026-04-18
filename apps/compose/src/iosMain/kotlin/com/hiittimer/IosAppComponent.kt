package com.dangerfield.hiittimer

import com.dangerfield.hiittimer.libraries.hiittimer.PermissionManager
import com.dangerfield.hiittimer.libraries.ui.nativeviews.NativeViewFactory
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class IosAppComponent(
    private val permissionManager: PermissionManager,
    val nativeViewFactory: NativeViewFactory
) : AppComponent {

    @Provides
    fun providePermissionManager(): PermissionManager = permissionManager
}


@MergeComponent.CreateComponent
expect fun create(
    permissionManager: PermissionManager,
    nativeViewFactory: NativeViewFactory
): IosAppComponent
