@file:OptIn(ExperimentalObjCName::class)

package com.dangerfield.hiittimer.libraries.hiittimer

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@ObjCName("Permission", exact = true)
sealed class Permission {
    @ObjCName("PermissionAppUsageStats", exact = true)
    data object AppUsageStats : Permission()
    @ObjCName("PermissionNotifications", exact = true)
    data object Notifications : Permission()
    @ObjCName("PermissionCamera", exact = true)
    data object Camera : Permission()
    @ObjCName("PermissionPhotoLibrary", exact = true)
    data object PhotoLibrary : Permission()
    @ObjCName("PermissionMicrophone", exact = true)
    data object Microphone : Permission()
}

@ObjCName("PermissionStatus", exact = true)
enum class PermissionStatus {
    GRANTED, DENIED, NOT_DETERMINED
}

@ObjCName("PermissionResult", exact = true)
sealed class PermissionResult {
    @ObjCName("PermissionResultGranted", exact = true)
    data object Granted : PermissionResult()
    @ObjCName("PermissionResultDenied", exact = true)
    data class Denied(val canRequestAgain: Boolean) : PermissionResult()
}