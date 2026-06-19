package me.thenano.yamibo.yamibo_app.components.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import me.thenano.yamibo.yamibo_app.LocalContentCoverRepository
import me.thenano.yamibo.yamibo_app.repository.ContentCoverRepository
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository

@Composable
fun resolvedContentCoverUrl(
    targetType: LocalFavoriteRepository.FavoriteTargetType,
    targetId: Long,
    fallback: String?,
): String? {
    val repository = LocalContentCoverRepository.current
    val key = remember(targetType, targetId) {
        ContentCoverRepository.Key(
            targetType = ContentCoverRepository.TargetType.valueOf(targetType.name),
            targetId = targetId,
        )
    }
    val cover by repository.observeCover(key).collectAsState(null)
    return cover?.resolvedUrl ?: fallback
}

