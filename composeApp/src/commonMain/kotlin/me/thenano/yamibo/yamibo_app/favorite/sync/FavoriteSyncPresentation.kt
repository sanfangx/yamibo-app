package me.thenano.yamibo.yamibo_app.favorite.sync

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncPhase
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncSnapshot
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncState

internal data class SyncProgressUi(
    val progress: Float,
    val label: String,
    val lines: List<Pair<String, String>>,
)

internal fun FavoriteSyncState.snapshotOrNull(): FavoriteSyncSnapshot? {
    return when (this) {
        FavoriteSyncState.Idle -> null
        is FavoriteSyncState.Running -> snapshot
        is FavoriteSyncState.Interrupted -> snapshot
        is FavoriteSyncState.Failed -> snapshot
        is FavoriteSyncState.Completed -> snapshot
    }
}

internal fun FavoriteSyncState.title(): String {
    return when (this) {
        FavoriteSyncState.Idle -> appString(Res.string.auto_0d7fe90a73)
        is FavoriteSyncState.Running -> appString(Res.string.auto_5b2af01686)
        is FavoriteSyncState.Interrupted -> appString(Res.string.auto_416d31ec7e)
        is FavoriteSyncState.Failed -> appString(Res.string.auto_c599384c8a)
        is FavoriteSyncState.Completed -> appString(Res.string.auto_02f667de32)
    }
}

internal fun FavoriteSyncSnapshot.toProgressUi(): SyncProgressUi {
    val safeScannedCount = scannedCount.coerceAtLeast(1)
    val importedProcessed = (importedCount + failedCount).coerceAtMost(safeScannedCount)
    val uploadProgress = when {
        uploadTargetCount <= 0 && phase != FavoriteSyncPhase.PREPARING && phase != FavoriteSyncPhase.FETCHING_REMOTE -> 1f
        uploadTargetCount > 0 -> (uploadedCount.toFloat() / uploadTargetCount.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }
    val fetchProgress = totalPages
        ?.takeIf { it > 0 }
        ?.let { (currentPage.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
        ?: if (currentPage > 0) 0.15f else 0f

    val progress = when (phase) {
        FavoriteSyncPhase.PREPARING -> 0.03f
        FavoriteSyncPhase.FETCHING_REMOTE -> 0.05f + (0.35f * fetchProgress)
        FavoriteSyncPhase.IMPORTING_REMOTE -> 0.40f + (0.40f * (importedProcessed.toFloat() / safeScannedCount.toFloat()))
        FavoriteSyncPhase.UPLOADING_LOCAL -> 0.80f + (0.15f * uploadProgress)
        FavoriteSyncPhase.RECONCILING_REMOTE -> 0.95f
        FavoriteSyncPhase.INTERRUPTED,
        FavoriteSyncPhase.FAILED -> snapshotFrozenProgress()
        FavoriteSyncPhase.COMPLETED -> 1f
    }.coerceIn(0f, 1f)

    return when (phase) {
        FavoriteSyncPhase.PREPARING -> SyncProgressUi(
            progress = progress,
            label = appString(Res.string.auto_945847ea7e),
            lines = listOf(appString(Res.string.auto_bd91f6187b) to appString(Res.string.auto_bd84aa9f93)),
        )

        FavoriteSyncPhase.FETCHING_REMOTE -> SyncProgressUi(
            progress = progress,
            label = appString(Res.string.auto_0cd83a34b9),
            lines = buildList {
                add(appString(Res.string.auto_e8202e1ba6) to if (currentPage <= 0) appString(Res.string.auto_640481ba4a) else appString(Res.string.favorite_sync_page_progress, currentPage.toString(), totalPages?.toString() ?: "?"))
                add(appString(Res.string.auto_5ac0884b60) to appString(Res.string.favorite_sync_scanned_count, scannedCount))
            },
        )

        FavoriteSyncPhase.IMPORTING_REMOTE,
        FavoriteSyncPhase.UPLOADING_LOCAL,
        FavoriteSyncPhase.RECONCILING_REMOTE,
        FavoriteSyncPhase.COMPLETED,
        FavoriteSyncPhase.INTERRUPTED,
        FavoriteSyncPhase.FAILED -> SyncProgressUi(
            progress = progress,
            label = appString(Res.string.auto_4c41b6645f),
            lines = buildList {
                add(appString(Res.string.auto_bdfc325e15) to "$importedCount/${scannedCount.coerceAtLeast(importedCount)}")
                add(appString(Res.string.auto_c219db0ba3) to uploadedCount.toString())
                add(appString(Res.string.auto_c599384c8a) to failedCount.toString())
            },
        )
    }
}

private fun FavoriteSyncSnapshot.snapshotFrozenProgress(): Float {
    return when {
        totalPages != null && totalPages!! > 0 && currentPage > 0 ->
            (0.05f + (0.35f * (currentPage.toFloat() / totalPages!!.toFloat()))).coerceIn(0f, 0.95f)
        scannedCount > 0 -> (0.40f + (0.40f * ((importedCount + failedCount).toFloat() / scannedCount.toFloat()))).coerceIn(0f, 0.95f)
        else -> 0.05f
    }
}


