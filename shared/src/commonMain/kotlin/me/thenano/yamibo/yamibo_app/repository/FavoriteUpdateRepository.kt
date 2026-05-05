package me.thenano.yamibo.yamibo_app.repository

import kotlinx.coroutines.flow.StateFlow

interface FavoriteUpdateRepository {
    enum class TargetMode {
        NormalThread,
        NovelThread,
        TagManga,
    }

    enum class RunStatus {
        RUNNING,
        INTERRUPTED,
        FAILED,
        COMPLETED,
    }

    enum class RunPhase {
        PREPARING,
        CHECKING,
        INTERRUPTED,
        FAILED,
        COMPLETED,
    }

    data class UpdateEvent(
        val id: Long,
        val targetType: LocalFavoriteRepository.FavoriteTargetType,
        val targetId: Long,
        val authorId: Long?,
        val fid: Int?,
        val forumName: String?,
        val title: String,
        val latestPostTitle: String?,
        val mode: TargetMode,
        val summary: String,
        val detailIds: List<Long>,
        val coverUrl: String?,
        val detectedAt: Long,
        val readAt: Long?,
        val dismissedAt: Long?,
        val ambiguous: Boolean,
    )

    data class FidFilter(
        val fid: Int,
        val forumName: String,
        val enabled: Boolean,
        val itemCount: Int,
    )

    data class RunSnapshot(
        val runId: String,
        val status: RunStatus,
        val phase: RunPhase,
        val startedAt: Long,
        val updatedAt: Long,
        val finishedAt: Long?,
        val totalCount: Int,
        val completedCount: Int,
        val skippedCount: Int,
        val failedCount: Int,
        val detectedCount: Int,
        val currentItem: String?,
        val logMessage: String?,
        val warningMessage: String?,
        val errorMessage: String?,
    )

    sealed interface RunState {
        data object Idle : RunState
        data class Running(val snapshot: RunSnapshot) : RunState
        data class Interrupted(val snapshot: RunSnapshot) : RunState
        data class Failed(val snapshot: RunSnapshot) : RunState
        data class Completed(val snapshot: RunSnapshot) : RunState
    }

    val state: StateFlow<RunState>

    suspend fun startRun(): String
    suspend fun resumeInterruptedRun(): String?
    suspend fun interruptRun(runId: String)
    suspend fun markRunInterrupted(runId: String, reason: String)
    suspend fun getLatestSnapshot(): RunSnapshot?
    suspend fun runUpdate(runId: String)
    suspend fun getActiveEvents(): List<UpdateEvent>
    suspend fun markEventRead(eventId: Long)
    suspend fun dismissEvent(eventId: Long)
    suspend fun getFidFilters(): List<FidFilter>
    suspend fun setFidEnabled(fid: Int, enabled: Boolean)
}
