package me.thenano.yamibo.yamibo_app.repository.download

import kotlinx.serialization.Serializable

@Serializable
data class ThreadPageDownloadKey(
    val tid: Int,
    val page: Int,
    val authorId: Int? = null,
) {
    val stableId: String
        get() = "thread_${tid}_page_${page}_author_${authorId ?: "all"}"

    val threadPrefix: String
        get() = "thread_${tid}_"
}

enum class DownloadStatus {
    NotDownloaded,
    Queued,
    Downloading,
    Downloaded,
    Failed,
    Paused,
    UpdateAvailable,
}

enum class DownloadPageKind {
    Normal,
    LastAtDownloadTime,
}

@Serializable
data class DownloadedImage(
    val sourceUrl: String,
    val fileName: String,
    val bytes: Long,
)

@Serializable
data class ThreadPageDownloadManifest(
    val key: ThreadPageDownloadKey,
    val title: String,
    val downloadedAt: Long,
    val sourceTotalPages: Int,
    val pageKind: DownloadPageKind,
    val images: List<DownloadedImage> = emptyList(),
)

@Serializable
data class DownloadQueueEntry(
    val key: ThreadPageDownloadKey,
    val title: String,
    val status: DownloadStatus,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val message: String? = null,
    val updatedAt: Long = 0L,
)

data class DownloadQueueSummary(
    val queued: Int = 0,
    val downloading: Int = 0,
    val downloaded: Int = 0,
    val failed: Int = 0,
    val updateAvailable: Int = 0,
) {
    val active: Int get() = queued + downloading
}
