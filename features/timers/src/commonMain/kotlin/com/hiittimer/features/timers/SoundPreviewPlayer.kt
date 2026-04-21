package com.dangerfield.hiittimer.features.timers

enum class SoundCueType {
    Short,
    Long,
    Finish,
}

interface SoundPreviewPlayer {
    fun preview(pack: SoundPack, cueType: SoundCueType, volume: Float)
    fun stop()
}

