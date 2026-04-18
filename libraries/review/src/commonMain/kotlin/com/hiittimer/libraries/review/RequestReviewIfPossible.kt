@file:OptIn(ExperimentalObjCName::class)

package com.dangerfield.hiittimer.libraries.review

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@ObjCName("RequestReviewIfPossible", exact = true)
interface RequestReviewIfPossible {
    suspend operator fun invoke()
}
