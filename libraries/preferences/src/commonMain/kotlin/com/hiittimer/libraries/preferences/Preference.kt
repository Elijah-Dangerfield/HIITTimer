package com.dangerfield.hiittimer.libraries.preferences

abstract class Preference<T : Any> {
    abstract val key: String
    abstract val default: T
}
