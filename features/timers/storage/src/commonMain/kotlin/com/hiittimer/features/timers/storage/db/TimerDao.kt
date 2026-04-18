package com.dangerfield.hiittimer.features.timers.storage.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Transaction
    @Query("SELECT * FROM timers ORDER BY sort_order ASC, created_at ASC")
    fun observeAll(): Flow<List<TimerWithBlocks>>

    @Transaction
    @Query("SELECT * FROM timers WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TimerWithBlocks?>

    @Transaction
    @Query("SELECT * FROM timers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TimerWithBlocks?

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM timers")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimer(timer: TimerEntity)

    @Update
    suspend fun updateTimer(timer: TimerEntity)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteTimer(id: String)

    @Transaction
    suspend fun duplicate(timerId: String, newTimerId: String, newName: String, newBlockId: (String) -> String) {
        val source = getById(timerId) ?: return
        val now = source.timer.updatedAt
        val nextSort = maxSortOrder() + 1
        upsertTimer(
            source.timer.copy(
                id = newTimerId,
                name = newName,
                sortOrder = nextSort,
                createdAt = now,
                updatedAt = now,
            )
        )
        source.blocks.forEach { b ->
            insertBlock(b.copy(id = newBlockId(b.id), timerId = newTimerId))
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: BlockEntity)

    @Update
    suspend fun updateBlock(block: BlockEntity)

    @Delete
    suspend fun deleteBlock(block: BlockEntity)

    @Query("DELETE FROM timer_blocks WHERE id = :blockId")
    suspend fun deleteBlockById(blockId: String)

    @Query("SELECT * FROM timer_blocks WHERE timer_id = :timerId ORDER BY sort_order ASC")
    suspend fun blocksForTimer(timerId: String): List<BlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlocks(blocks: List<BlockEntity>)
}
