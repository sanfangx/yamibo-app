package me.thenano.yamibo.yamibo_app.favorite.sync

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow
import me.thenano.yamibo.yamibo_app.repository.BackgroundTaskRepository

class AndroidBackgroundTaskRepository(
    context: Context,
) : BackgroundTaskRepository {
    private val appContext = context.applicationContext

    override val runningFavoriteSyncRunIds: StateFlow<Set<String>> =
        FavoriteSyncForegroundService.runningFavoriteSyncRunIds

    override suspend fun startFavoriteSync(runId: String): BackgroundTaskRepository.StartResult {
        if (!AndroidAppForegroundTracker.isForeground()) {
            return BackgroundTaskRepository.StartResult.Rejected(appString(Res.string.auto_8e040c66da))
        }
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
            return BackgroundTaskRepository.StartResult.Rejected(appString(Res.string.auto_2c6e553441))
        }

        return try {
            val intent = FavoriteSyncForegroundService.createStartIntent(appContext, runId)
            ContextCompat.startForegroundService(appContext, intent)
            BackgroundTaskRepository.StartResult.Started
        } catch (_: Throwable) {
            BackgroundTaskRepository.StartResult.Rejected(appString(Res.string.auto_db648abb65))
        }
    }

    override suspend fun cancelFavoriteSync(runId: String) {
        val intent = FavoriteSyncForegroundService.createCancelIntent(appContext, runId)
        ContextCompat.startForegroundService(appContext, intent)
    }
}

