package com.dangerfield.hiittimer

import com.dangerfield.hiittimer.libraries.review.RequestReviewIfPossible
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class IosAppComponent(
    private val requestReviewIfPossible: RequestReviewIfPossible,
) : AppComponent {

    @Provides
    fun providesRequestReviewIfPossible(): RequestReviewIfPossible = requestReviewIfPossible
}


@MergeComponent.CreateComponent
expect fun create(
    requestReviewIfPossible: RequestReviewIfPossible,
): IosAppComponent
