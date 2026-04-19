package com.dangerfield.hiittimer.features.timers.impl

import com.dangerfield.hiittimer.features.timers.Block
import com.dangerfield.hiittimer.features.timers.Timer
import com.dangerfield.hiittimer.features.timers.storage.db.BlockEntity
import com.dangerfield.hiittimer.features.timers.storage.db.TimerDao
import com.dangerfield.hiittimer.features.timers.storage.db.TimerEntity
import com.dangerfield.hiittimer.features.timers.storage.db.TimerWithBlocks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

interface TimerRepository {
    fun observeAll(): Flow<List<Timer>>
    fun observe(id: String): Flow<Timer?>
    suspend fun create(name: String): Timer
    suspend fun createWithBlocks(name: String, cycleCount: Int, blocks: List<Block>): Timer
    suspend fun updateTimer(timer: Timer)
    suspend fun delete(id: String)
    suspend fun duplicate(id: String): String?
    suspend fun addBlock(timerId: String, block: Block)
    suspend fun updateBlock(timerId: String, block: Block)
    suspend fun deleteBlock(timerId: String, blockId: String)
    suspend fun reorderBlocks(timerId: String, orderedIds: List<String>)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class TimerRepositoryImpl(
    private val dao: TimerDao,
) : TimerRepository {

    override fun observeAll(): Flow<List<Timer>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observe(id: String): Flow<Timer?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun create(name: String): Timer {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = Uuid.random().toString()
        val sort = dao.maxSortOrder() + 1
        val entity = TimerEntity(
            id = id,
            name = name,
            cycleCount = 1,
            sortOrder = sort,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsertTimer(entity)
        val work = Block(Uuid.random().toString(), "Work", 30.seconds, ColorPalette.defaultWorkArgb)
        val rest = Block(Uuid.random().toString(), "Rest", 15.seconds, ColorPalette.defaultRestArgb)
        dao.upsertBlocks(
            listOf(
                work.toEntity(id, sortOrder = 0),
                rest.toEntity(id, sortOrder = 1),
            )
        )
        return Timer(id = id, name = name, cycleCount = 1, blocks = listOf(work, rest))
    }

    override suspend fun createWithBlocks(
        name: String,
        cycleCount: Int,
        blocks: List<Block>,
    ): Timer {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = Uuid.random().toString()
        val sort = dao.maxSortOrder() + 1
        dao.upsertTimer(
            TimerEntity(
                id = id,
                name = name,
                cycleCount = cycleCount.coerceIn(1, 99),
                sortOrder = sort,
                createdAt = now,
                updatedAt = now,
            )
        )
        val entities = blocks.mapIndexed { index, block ->
            block.toEntity(id, sortOrder = index)
        }
        if (entities.isNotEmpty()) dao.upsertBlocks(entities)
        return Timer(id = id, name = name, cycleCount = cycleCount, blocks = blocks)
    }

    override suspend fun updateTimer(timer: Timer) {
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = dao.getById(timer.id)?.timer ?: return
        dao.updateTimer(
            existing.copy(
                name = timer.name,
                cycleCount = timer.cycleCount,
                updatedAt = now,
            )
        )
        val entities = timer.blocks.mapIndexed { index, block ->
            block.toEntity(timer.id, sortOrder = index)
        }
        dao.upsertBlocks(entities)
    }

    override suspend fun delete(id: String) {
        dao.deleteTimer(id)
    }

    override suspend fun duplicate(id: String): String? {
        val source = dao.getById(id) ?: return null
        val newId = Uuid.random().toString()
        dao.duplicate(
            timerId = id,
            newTimerId = newId,
            newName = "${source.timer.name} (copy)",
            newBlockId = { Uuid.random().toString() },
        )
        return newId
    }

    override suspend fun addBlock(timerId: String, block: Block) {
        val existing = dao.blocksForTimer(timerId)
        dao.insertBlock(block.toEntity(timerId, sortOrder = existing.size))
        touchTimer(timerId)
    }

    override suspend fun updateBlock(timerId: String, block: Block) {
        val existing = dao.blocksForTimer(timerId).firstOrNull { it.id == block.id } ?: return
        dao.updateBlock(
            existing.copy(
                name = block.name,
                durationSeconds = block.duration.inWholeSeconds.toInt(),
                colorArgb = block.colorArgb,
                role = block.role.name,
            )
        )
        touchTimer(timerId)
    }

    override suspend fun deleteBlock(timerId: String, blockId: String) {
        dao.deleteBlockById(blockId)
        touchTimer(timerId)
    }

    override suspend fun reorderBlocks(timerId: String, orderedIds: List<String>) {
        val current = dao.blocksForTimer(timerId).associateBy { it.id }
        val reordered = orderedIds.mapIndexedNotNull { index, id ->
            current[id]?.copy(sortOrder = index)
        }
        dao.upsertBlocks(reordered)
        touchTimer(timerId)
    }

    private suspend fun touchTimer(timerId: String) {
        val existing = dao.getById(timerId)?.timer ?: return
        dao.updateTimer(existing.copy(updatedAt = Clock.System.now().toEpochMilliseconds()))
    }

    private fun TimerWithBlocks.toDomain(): Timer = Timer(
        id = timer.id,
        name = timer.name,
        cycleCount = timer.cycleCount,
        blocks = blocks.sortedBy { it.sortOrder }.map { it.toDomain() },
    )

    private fun BlockEntity.toDomain(): Block = Block(
        id = id,
        name = name,
        duration = durationSeconds.seconds,
        colorArgb = colorArgb,
        role = runCatching { com.dangerfield.hiittimer.features.timers.BlockRole.valueOf(role) }
            .getOrDefault(com.dangerfield.hiittimer.features.timers.BlockRole.Cycle),
    )

    private fun Block.toEntity(timerId: String, sortOrder: Int): BlockEntity = BlockEntity(
        id = id,
        timerId = timerId,
        name = name,
        durationSeconds = duration.inWholeSeconds.toInt(),
        colorArgb = colorArgb,
        sortOrder = sortOrder,
        role = role.name,
    )
}
