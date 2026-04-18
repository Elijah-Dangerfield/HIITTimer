package com.dangerfield.hiittimer.libraries.ui

interface PhotoSaver {
    suspend fun savePhoto(photoData: ByteArray): String?
}
