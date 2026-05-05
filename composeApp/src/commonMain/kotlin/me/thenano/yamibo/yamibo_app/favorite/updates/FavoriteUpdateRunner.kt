package me.thenano.yamibo.yamibo_app.favorite.updates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteUpdateInterval

class FavoriteUpdateRunner(
    private val repository: FavoriteUpdateRepository,
    private val scheduler: FavoriteUpdateScheduler,
) {
    sealed interface LaunchResult {
        data class Started(val runId: String) : LaunchResult
        data class Rejected(val reason: String, val runId: String? = null) : LaunchResult
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateFlow = MutableStateFlow<FavoriteUpdateRepository.RunState>(FavoriteUpdateRepository.RunState.Idle)

    val state: StateFlow<FavoriteUpdateRepository.RunState> = stateFlow.asStateFlow()

    init {
        scope.launch {
            repository.state.collect { stateFlow.value = it }
        }
    }

    suspend fun startManualUpdate(): LaunchResult {
        val running = repository.state.value as? FavoriteUpdateRepository.RunState.Running
        if (running != null) return LaunchResult.Started(running.snapshot.runId)

        val runId = repository.startRun()
        return when (val result = scheduler.startFavoriteUpdate(runId)) {
            FavoriteUpdateScheduler.StartResult.Started -> LaunchResult.Started(runId)
            is FavoriteUpdateScheduler.StartResult.Rejected -> {
                repository.markRunInterrupted(runId, result.reason)
                LaunchResult.Rejected(result.reason, runId)
            }
        }
    }

    suspend fun resumeInterruptedUpdate(): LaunchResult? {
        val runId = repository.resumeInterruptedRun() ?: return null
        return when (val result = scheduler.startFavoriteUpdate(runId)) {
            FavoriteUpdateScheduler.StartResult.Started -> LaunchResult.Started(runId)
            is FavoriteUpdateScheduler.StartResult.Rejected -> {
                repository.markRunInterrupted(runId, result.reason)
                LaunchResult.Rejected(result.reason, runId)
            }
        }
    }

    suspend fun startGlobalRefresh(): LaunchResult = startManualUpdate()

    suspend fun cancelUpdate(runId: String) {
        scheduler.cancelFavoriteUpdate(runId)
        repository.interruptRun(runId)
    }

    suspend fun schedulePeriodicUpdate(interval: FavoriteUpdateInterval) {
        scheduler.schedulePeriodicFavoriteUpdate(interval)
    }
}

interface FavoriteUpdateScheduler {
    sealed interface StartResult {
        data object Started : StartResult
        data class Rejected(val reason: String) : StartResult
    }

    suspend fun startFavoriteUpdate(runId: String): StartResult
    suspend fun cancelFavoriteUpdate(runId: String)
    suspend fun schedulePeriodicFavoriteUpdate(interval: FavoriteUpdateInterval)
}
