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
    suspend fun writeTagMangaChapter(
        key: TagMangaChapterDownloadKey,
        manifestBytes: ByteArray,
        images: List<PendingDownloadedImage>,
    )
    suspend fun resolveTagMangaImageUri(key: TagMangaChapterDownloadKey, fileName: String): String?
    suspend fun resolveTagMangaImageUris(key: TagMangaChapterDownloadKey, fileNames: List<String>): List<String> =
        fileNames.mapNotNull { resolveTagMangaImageUri(key, it) }
    suspend fun readTagMangaManifest(key: TagMangaChapterDownloadKey): TagMangaChapterManifest?
    suspend fun listTagMangaManifests(): List<TagMangaChapterManifest>
    suspend fun readQueue(): List<DownloadQueueEntry>
    suspend fun writeQueue(entries: List<DownloadQueueEntry>)
    suspend fun deleteThreadPage(key: ThreadPageDownloadKey)
    suspend fun deleteThread(key: ThreadPageDownloadKey)
    suspend fun deleteTagMangaChapter(key: TagMangaChapterDownloadKey)
    suspend fun deleteTagManga(tagId: Int)
}

data class PendingDownloadedImage(
    val fileName: String,
    val bytes: ByteArray,
)
