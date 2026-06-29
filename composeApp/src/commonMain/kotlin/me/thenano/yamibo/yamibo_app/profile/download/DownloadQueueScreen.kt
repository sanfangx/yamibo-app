package me.thenano.yamibo.yamibo_app.profile.download

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalDownloadRepository
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboTopBar
import me.thenano.yamibo.yamibo_app.components.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.backup.IBackupSettingsScreen
import me.thenano.yamibo.yamibo_app.repository.download.DownloadQueueEntry
import me.thenano.yamibo.yamibo_app.repository.download.DownloadQueueSummary
import me.thenano.yamibo.yamibo_app.repository.download.DownloadStatus

@Composable
fun DownloadQueueScreen() {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val repository = LocalDownloadRepository.current
    val entries by repository.queue.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf(DownloadQueueSummary()) }
    var folderReady by remember { mutableStateOf(true) }

    LaunchedEffect(entries) {
        summary = repository.getSummary()
        folderReady = repository.isStorageReady()
    }

    Scaffold(
        topBar = {
            YamiboTopBar(
                title = i18n("下載佇列"),
                onBack = navigator::pop,
            )
        },
        snackbarHost = { YamiboSnackbarHost(snackbarHostState) },
        containerColor = colors.creamBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.creamBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DownloadSummaryCard(
                    summary = summary,
                    onPauseAll = repository::pauseAll,
                    onResumeAll = repository::resumeAll,
                )
            }
            if (!folderReady) {
                item {
                    BackupFolderRequiredCard(
                        onOpenSettings = { navigator.navigate(IBackupSettingsScreen()) },
                    )
                }
            }
            if (entries.isEmpty()) {
                item { EmptyDownloadQueueCard() }
            } else {
                items(entries, key = { it.key.stableId }) { entry ->
                    DownloadQueueEntryCard(
                        entry = entry,
                        onRetry = {
                            scope.launch {
                                repository.retry(entry.key)
                                    .onFailure { snackbarHostState.showSnackbar(it.message ?: i18n("重試失敗")) }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadSummaryCard(
    summary: DownloadQueueSummary,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
) {
    val colors = YamiboTheme.colors
    YamiboDownloadCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(YamiboIcons.Download, contentDescription = null, tint = colors.orangeAccent, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(i18n("下載佇列"), color = colors.textStrong, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(
                    i18n("下載中 {}，等待 {}，失敗 {}", summary.downloading, summary.queued, summary.failed),
                    color = colors.textDark.copy(alpha = 0.68f),
                    fontSize = 13.sp,
                )
            }
            SmallQueueButton(i18n("暫停"), onPauseAll)
            Spacer(Modifier.size(8.dp))
            SmallQueueButton(i18n("繼續"), onResumeAll)
        }
        if (summary.active > 0) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (summary.active == 0) 0f else summary.downloading.toFloat() / summary.active.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = colors.orangeAccent,
                trackColor = colors.brownLight.copy(alpha = 0.25f),
            )
        }
    }
}

@Composable
private fun BackupFolderRequiredCard(onOpenSettings: () -> Unit) {
    val colors = YamiboTheme.colors
    YamiboDownloadCard {
        Text(i18n("尚未設定下載資料夾"), color = colors.textStrong, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(i18n("下載內容會存放在設定與收藏備份指定的資料夾。"), color = colors.textDark.copy(alpha = 0.68f), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        SmallQueueButton(i18n("前往設定"), onOpenSettings)
    }
}

@Composable
private fun EmptyDownloadQueueCard() {
    val colors = YamiboTheme.colors
    YamiboDownloadCard {
        Text(i18n("目前沒有下載任務"), color = colors.textStrong, fontWeight = FontWeight.SemiBold)
        Text(i18n("可在閱讀頁使用下載按鈕加入目前頁或完整 Thread。"), color = colors.textDark.copy(alpha = 0.68f), fontSize = 13.sp)
    }
}

@Composable
private fun DownloadQueueEntryCard(
    entry: DownloadQueueEntry,
    onRetry: () -> Unit,
) {
    val colors = YamiboTheme.colors
    val statusColor = when (entry.status) {
        DownloadStatus.Failed -> colors.redAccent
        DownloadStatus.Downloading, DownloadStatus.Queued -> colors.orangeAccent
        else -> colors.brownPrimary
    }
    YamiboDownloadCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (entry.status == DownloadStatus.Downloaded) YamiboIcons.Downloaded else YamiboIcons.Download,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(entry.title, color = colors.textStrong, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${statusLabel(entry.status)} · ${i18n("第 {} 頁", entry.key.page)}${entry.message?.let { " · $it" }.orEmpty()}",
                    color = if (entry.status == DownloadStatus.Failed) colors.redAccent else colors.textDark.copy(alpha = 0.68f),
                    fontSize = 12.sp,
                )
            }
            if (entry.status == DownloadStatus.Failed) {
                SmallQueueButton(i18n("重試"), onRetry)
            }
        }
    }
}

@Composable
private fun YamiboDownloadCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = YamiboTheme.colors
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SmallQueueButton(
    text: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val colors = YamiboTheme.colors
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (destructive) colors.redAccent else colors.brownPrimary,
            containerColor = Color.Transparent,
        ),
    ) {
        Text(text, fontSize = 12.sp)
    }
}

private fun statusLabel(status: DownloadStatus): String = when (status) {
    DownloadStatus.NotDownloaded -> i18n("未下載")
    DownloadStatus.Queued -> i18n("等待中")
    DownloadStatus.Downloading -> i18n("下載中")
    DownloadStatus.Downloaded -> i18n("已下載")
    DownloadStatus.Failed -> i18n("下載失敗")
    DownloadStatus.Paused -> i18n("已暫停")
    DownloadStatus.UpdateAvailable -> i18n("可刷新")
}
