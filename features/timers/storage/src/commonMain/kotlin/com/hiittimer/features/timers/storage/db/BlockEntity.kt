package com.dangerfield.hiittimer.features.timers.storage.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timer_blocks",
    foreignKeys = [
        ForeignKey(
            entity = TimerEntity::class,
            parentColumns = ["id"],
            childColumns = ["timer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("timer_id")],
)
data class BlockEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "timer_id") val timerId: String,
    val name: String,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int,
    @ColumnInfo(name = "color_argb") val colorArgb: Int,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "role", defaultValue = "Cycle") val role: String = "Cycle",
)
