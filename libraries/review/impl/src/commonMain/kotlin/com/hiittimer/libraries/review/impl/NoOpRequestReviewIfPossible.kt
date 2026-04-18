package com.dangerfield.hiittimer.libraries.review.impl

import com.dangerfield.hiittimer.libraries.review.RequestReviewIfPossible
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class NoOpRequestReviewIfPossible : RequestReviewIfPossible {
    override suspend fun invoke() = Unit
}
