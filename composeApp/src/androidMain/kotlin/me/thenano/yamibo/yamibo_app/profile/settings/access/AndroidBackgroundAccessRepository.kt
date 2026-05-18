package me.thenano.yamibo.yamibo_app.profile.settings.access

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.net.toUri

class AndroidBackgroundAccessRepository(
    context: Context,
) : BackgroundAccessRepository {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(buildState())

    override val state: StateFlow<BackgroundAccessRepository.SetupState> = _state

    override suspend fun refresh() {
        _state.value = buildState()
    }

    @SuppressLint("QueryPermissionsNeeded")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun runAction(action: BackgroundAccessRepository.SetupAction) {
        val intent = when (action) {
            BackgroundAccessRepository.SetupAction.RequestNotificationPermission -> return
            BackgroundAccessRepository.SetupAction.OpenNotificationSettings -> {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            BackgroundAccessRepository.SetupAction.OpenBatteryOptimizationSettings -> {
                val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${appContext.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (requestIntent.resolveActivity(appContext.packageManager) != null) {
                    requestIntent
                } else {
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            }
            BackgroundAccessRepository.SetupAction.OpenAppSettings -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${appContext.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            BackgroundAccessRepository.SetupAction.OpenDontKillMyApp -> {
                Intent(Intent.ACTION_VIEW, "https://dontkillmyapp.com/".toUri()).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }
        appContext.startActivity(intent)
    }

    private fun buildState(): BackgroundAccessRepository.SetupState {
        val notificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val notificationsEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOptimizationIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(appContext.packageName)
        } else {
            true
        }

        val notificationItem = when {
            !notificationPermissionGranted -> {
                BackgroundAccessRepository.SetupItem(
                    title = appString(Res.string.auto_5cead0090e),
                    subtitle = appString(Res.string.auto_9de49d6a14),
                    status = BackgroundAccessRepository.SetupStatus.Required,
                    actionLabel = appString(Res.string.auto_2808aa1a67),
                    action = BackgroundAccessRepository.SetupAction.RequestNotificationPermission,
                )
            }
            !notificationsEnabled -> {
                BackgroundAccessRepository.SetupItem(
                    title = appString(Res.string.auto_5a9f6672a2),
                    subtitle = appString(Res.string.auto_36d8d41239),
                    status = BackgroundAccessRepository.SetupStatus.Required,
                    actionLabel = appString(Res.string.auto_60233828b6),
                    action = BackgroundAccessRepository.SetupAction.OpenNotificationSettings,
                )
            }
            else -> {
                BackgroundAccessRepository.SetupItem(
                    title = appString(Res.string.auto_5cead0090e),
                    subtitle = appString(Res.string.auto_60372ee1dd),
                    status = BackgroundAccessRepository.SetupStatus.Granted,
                )
            }
        }

        val batteryItem = if (!batteryOptimizationIgnored) {
            BackgroundAccessRepository.SetupItem(
                title = appString(Res.string.auto_7a331081ad),
                subtitle = appString(Res.string.auto_d4d511fe98),
                status = BackgroundAccessRepository.SetupStatus.Recommended,
                actionLabel = appString(Res.string.auto_60233828b6),
                action = BackgroundAccessRepository.SetupAction.OpenBatteryOptimizationSettings,
            )
        } else {
            BackgroundAccessRepository.SetupItem(
                title = appString(Res.string.auto_7a331081ad),
                subtitle = appString(Res.string.auto_08ac566c17),
                status = BackgroundAccessRepository.SetupStatus.Granted,
            )
        }

        val appSettingsItem = BackgroundAccessRepository.SetupItem(
            title = appString(Res.string.auto_18d17b4f1c),
            subtitle = appString(Res.string.auto_2114b8c90d),
            status = BackgroundAccessRepository.SetupStatus.Info,
            actionLabel = appString(Res.string.auto_60233828b6),
            action = BackgroundAccessRepository.SetupAction.OpenAppSettings,
        )
        val dontKillMyAppItem = BackgroundAccessRepository.SetupItem(
            title = appString(Res.string.auto_c96782a112),
            subtitle = appString(Res.string.auto_b2f73ce461),
            status = BackgroundAccessRepository.SetupStatus.Info,
            actionLabel = appString(Res.string.auto_607e7a4f37),
            action = BackgroundAccessRepository.SetupAction.OpenDontKillMyApp,
        )

        val requiredMissingCount = listOf(notificationItem).count {
            it.status == BackgroundAccessRepository.SetupStatus.Required
        }
        val recommendedCount = listOf(batteryItem).count {
            it.status == BackgroundAccessRepository.SetupStatus.Recommended
        }
        val summary = when {
            requiredMissingCount > 0 -> appString(Res.string.background_access_required_missing, requiredMissingCount)
            recommendedCount > 0 -> appString(Res.string.background_access_recommended_missing, recommendedCount)
            else -> appString(Res.string.auto_c3dd2b4e97)
        }

        return BackgroundAccessRepository.SetupState(
            summary = summary,
            items = listOf(notificationItem, batteryItem, appSettingsItem, dontKillMyAppItem),
            platformNote = appString(Res.string.auto_64afbee29e),
        )
    }
}


