package com.dangerfield.hiittimer.libraries.navigation

import com.dangerfield.hiittimer.libraries.core.Catching

fun interface WebLinkLauncher {
    fun open(url: String): Catching<Unit>
}
