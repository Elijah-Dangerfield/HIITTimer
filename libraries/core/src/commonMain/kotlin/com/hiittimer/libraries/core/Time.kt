package com.dangerfield.hiittimer.libraries.core

import kotlin.time.Clock
import kotlin.time.Instant

fun Clock.Companion.fixed(instant: Instant) = object : Clock {
    override fun now(): Instant = instant
}
