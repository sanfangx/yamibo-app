package me.thenano.yamibo.yamibo_app.repository.download

interface DownloadStorageProvider {
    suspend fun getSelectedFolderLabel(): String?
    suspend fun isReady(): Boolean
    suspend fun writeThreadPage(
        key: ThreadPageDownloadKey,
        manifestBytes: ByteArray,
        threadPageBytes: ByteArray,
        images: List<PendingDownloadedImage>,
    )
    suspend fun readThreadPage(key: ThreadPageDownloadKey): ByteArray?
    suspend fun resolveImageUri(key: ThreadPageDownloadKey, fileName: String): String?
    suspend fun readManifest(key: ThreadPageDownloadKey): ThreadPageDownloadManifest?
    suspend fun listManifests(): List<ThreadPageDownloadManifest>
    suspend fun readQueue(): List<DownloadQueueEntry>
    suspend fun writeQueue(entries: List<DownloadQueueEntry>)
    suspend fun deleteThreadPage(key: ThreadPageDownloadKey)
    suspend fun deleteThread(key: ThreadPageDownloadKey)
}

data class PendingDownloadedImage(
    val fileName: String,
    val bytes: ByteArray,
)
