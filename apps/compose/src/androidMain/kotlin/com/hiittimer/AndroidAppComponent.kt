package com.dangerfield.hiittimer

import android.content.Context
import com.dangerfield.hiittimer.libraries.review.RequestReviewIfPossible
import com.dangerfield.hiittimer.libraries.review.impl.NoOpRequestReviewIfPossible
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class AndroidAppComponent(
    private val context: Context
) : AppComponent {

    @Provides
    fun context() = context

    @Provides
    @SingleIn(AppScope::class)
    fun providesRequestReviewIfPossible(): RequestReviewIfPossible = NoOpRequestReviewIfPossible()
}
