package com.dangerfield.hiittimer.libraries.ui

import androidx.compose.runtime.MutableState

fun MutableState<Boolean>.toggle() {
    value = !value
}