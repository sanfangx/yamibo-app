package me.thenano.yamibo.yamibo_app.favorite.updates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun FavoriteUpdateStatusCard(
    state: FavoriteUpdateRepository.RunState,
    modifier: Modifier = Modifier,
    onGlobalRefresh: (() -> Unit)? = null,
    onCancel: ((String) -> Unit)? = null,
) {
    val colors = YamiboTheme.colors
    val snapshot = state.snapshotOrNull() ?: return
    val total = snapshot.totalCount.coerceAtLeast(0)
    val processed = (snapshot.completedCount + snapshot.skippedCount + snapshot.failedCount).coerceAtMost(total)
    val progress = if (total > 0) processed.toFloat() / total.toFloat() else 0f
    val boundedProgress = progress.coerceIn(0f, 1f)

    Surface(
        modifier = modifier.heightIn(max = 168.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = colors.creamSurface,
        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title(),
                        color = colors.brownDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = snapshot.currentItem ?: snapshot.phase.name,
                        color = colors.textDark.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "$processed/${snapshot.totalCount}",
                    color = colors.brownDeep,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }

            LinearProgressIndicator(
                progress = { boundedProgress },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = colors.brownDeep,
                trackColor = colors.brownPrimary.copy(alpha = 0.16f),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricText("更新", snapshot.detectedCount)
                MetricText("跳過", snapshot.skippedCount)
                MetricText("錯誤", snapshot.failedCount)
            }

            snapshot.warningMessage?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it.lineSequence().lastOrNull().orEmpty(),
                    color = colors.brownDeep,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            snapshot.errorMessage?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = Color(0xFFB74D42),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state is FavoriteUpdateRepository.RunState.Running && onCancel != null) {
                    Button(
                        onClick = { onCancel(snapshot.runId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8C4B3B),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("取消")
                    }
                }
                if (state !is FavoriteUpdateRepository.RunState.Running && onGlobalRefresh != null) {
                    Button(
                        onClick = onGlobalRefresh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brownPrimary.copy(alpha = 0.18f),
                            contentColor = colors.brownDeep,
                        ),
                    ) {
                        Text("全域刷新")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricText(label: String, value: Int) {
    val colors = YamiboTheme.colors
    Text(
        text = "$label $value",
        color = colors.textDark.copy(alpha = 0.68f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

fun FavoriteUpdateRepository.RunState.snapshotOrNull(): FavoriteUpdateRepository.RunSnapshot? =
    when (this) {
        FavoriteUpdateRepository.RunState.Idle -> null
        is FavoriteUpdateRepository.RunState.Running -> snapshot
        is FavoriteUpdateRepository.RunState.Interrupted -> snapshot
        is FavoriteUpdateRepository.RunState.Failed -> snapshot
        is FavoriteUpdateRepository.RunState.Completed -> snapshot
    }

private fun FavoriteUpdateRepository.RunState.title(): String =
    when (this) {
        FavoriteUpdateRepository.RunState.Idle -> "收藏更新"
        is FavoriteUpdateRepository.RunState.Running -> "正在檢查收藏更新"
        is FavoriteUpdateRepository.RunState.Interrupted -> "收藏更新已中斷"
        is FavoriteUpdateRepository.RunState.Failed -> "收藏更新失敗"
        is FavoriteUpdateRepository.RunState.Completed -> "收藏更新完成"
    }
