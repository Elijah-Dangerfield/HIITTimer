package com.dangerfield.hiittimer.features.timers.storage.db

import androidx.room.Embedded
import androidx.room.Relation

data class TimerWithBlocks(
    @Embedded val timer: TimerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "timer_id",
    )
    val blocks: List<BlockEntity>,
)
