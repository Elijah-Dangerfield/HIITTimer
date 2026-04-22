package com.dangerfield.hiittimer.libraries.core

import com.dangerfield.hiittimer.libraries.core.logging.KLog
import kotlinx.coroutines.TimeoutCancellationException


val Throwable.shouldNotBeCaught: Boolean
    get() = when {
        isThrowableCancellation()
//                || this is VirtualMachineError
//                || this is ThreadDeath
//                || this is InterruptedException
//                || this is LinkageError
                     -> true
        else -> false
    }

private fun Throwable.isThrowableCancellation() =
    this is kotlinx.coroutines.CancellationException && this !is TimeoutCancellationException

/**
 * Used to mark an exception as thrown on purpose.
 */
class CaughtException(e: Throwable? = null, message: String? = e?.message) :
    Exception(message, e)

fun throwIfDebug(throwable: Throwable) {
    KLog.e(throwable)
    if (BuildInfo.isDebug) {
        throw CaughtException(throwable)
    }

}

fun throwIfDebug(lazyMessage: () -> Any) {
    KLog.e(lazyMessage().toString())
    if (BuildInfo.isDebug) {
        throw CaughtException(message = lazyMessage().toString())
    }
}

inline fun checkInDebug(value: Boolean, lazyMessage: () -> Any) {
    if (!value) {
        if (BuildInfo.isDebug) throw CaughtException(message = lazyMessage().toString())
    }
}