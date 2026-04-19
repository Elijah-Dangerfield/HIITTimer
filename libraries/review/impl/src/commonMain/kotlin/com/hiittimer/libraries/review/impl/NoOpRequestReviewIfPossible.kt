package com.dangerfield.hiittimer.libraries.review.impl

import com.dangerfield.hiittimer.libraries.review.RequestReviewIfPossible

class NoOpRequestReviewIfPossible : RequestReviewIfPossible {
    override suspend fun invoke() = Unit
}
