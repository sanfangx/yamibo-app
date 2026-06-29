package me.thenano.yamibo.yamibo_app.repository.download

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.littlesurvival.dto.page.PostImage
import me.thenano.yamibo.yamibo_app.repository.ThreadRepository
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

class DownloadRepositoryImpl(
    private val threadRepository: ThreadRepository,
    private val storageProvider: DownloadStorageProvider,
    private val imageFetcher: DownloadImageFetcher,
    private val backgroundController: DownloadBackgroundController = DownloadBackgroundController.None,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    },
) : DownloadRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val workerMutex = Mutex()
    private val queueWriteMutex = Mutex()
    private val knownTitles = mutableMapOf<ThreadPageDownloadKey, String>()
    private val prefetchedPages = mutableMapOf<ThreadPageDownloadKey, ThreadPage>()
    private val _queue = MutableStateFlow<List<DownloadQueueEntry>>(emptyList())
    override val queue: StateFlow<List<DownloadQueueEntry>> = _queue
    private val initialized = CompletableDeferred<Unit>()
    private var paused = false

    init {
        scope.launch {
            try {
                if (!storageProvider.isReady()) return@launch
                val restoredQueue = storageProvider.readQueue().map { entry ->
                    when (entry.status) {
                        DownloadStatus.Downloading -> entry.copy(status = DownloadStatus.Queued)
                        else -> entry
                    }
                }
                val queueKeys = restoredQueue.mapTo(mutableSetOf()) { it.key }
                val restoredDownloads = storageProvider.listManifests()
                    .filterNot { it.key in queueKeys }
                    .map { manifest ->
                        DownloadQueueEntry(
                            key = manifest.key,
                            title = manifest.title,
                            status = DownloadStatus.Downloaded,
                            updatedAt = manifest.downloadedAt,
                        )
                    }
                val restored = (restoredQueue + restoredDownloads)
                    .sortedWith(compareBy<DownloadQueueEntry> { it.key.tid }.thenBy { it.key.page })
                _queue.value = restored
                restored.forEach { knownTitles[it.key] = it.title }
                backgroundController.onQueueChanged(restored)
            } finally {
                initialized.complete(Unit)
            }
            drainQueue()
        }
    }

    override suspend fun isStorageReady(): Boolean =
        storageProvider.isReady()

    override suspend fun getSummary(): DownloadQueueSummary {
        initialized.await()
        val entries = queue.value
        return DownloadQueueSummary(
            queued = entries.count { it.status == DownloadStatus.Queued },
            downloading = entries.count { it.status == DownloadStatus.Downloading },
            downloaded = storageProvider.listManifests().size,
            failed = entries.count { it.status == DownloadStatus.Failed },
            updateAvailable = entries.count { it.status == DownloadStatus.UpdateAvailable },
        )
    }

    override suspend fun getStatus(key: ThreadPageDownloadKey): DownloadStatus {
        initialized.await()
        queue.value.firstOrNull { it.key == key }?.let { return it.status }
        return if (storageProvider.readManifest(key) != null) DownloadStatus.Downloaded else DownloadStatus.NotDownloaded
    }

    override suspend fun getDownloadedPage(key: ThreadPageDownloadKey): ThreadPage? {
        initialized.await()
        val bytes = storageProvider.readThreadPage(key) ?: return null
        val page = runCatching { json.decodeFromString<ThreadPage>(bytes.decodeToString()) }.getOrNull()
            ?: return null
        val manifest = storageProvider.readManifest(key) ?: return page
        val localUris = manifest.images.mapNotNull { image ->
            storageProvider.resolveImageUri(key, image.fileName)?.let { image.sourceUrl to it }
        }.toMap()
        if (localUris.isEmpty()) return page
        return page.copy(
            posts = page.posts.map { post ->
                post.copy(
                    contentHtml = localUris.entries.fold(post.contentHtml) { html, (source, local) ->
                        html.replace(source, local)
                            .replace(source.removePrefix("https://bbs.yamibo.com/"), local)
                    },
                    images = post.images.map { image ->
                        val normalized = normalizeDownloadImageUrl(image.url)
                        PostImage(localUris[normalized] ?: image.url, image.alt)
                    },
                )
            },
        )
    }

    override suspend fun getManifest(key: ThreadPageDownloadKey): ThreadPageDownloadManifest? =
        initialized.await().let { storageProvider.readManifest(key) }

    override suspend fun enqueuePage(tid: ThreadId, title: String, authorId: UserId?, page: Int): Result<Unit> = runCatching {
        initialized.await()
        ensureStorageReady()
        val key = ThreadPageDownloadKey(tid.value, page, authorId?.value)
        knownTitles[key] = title
        upsert(DownloadQueueEntry(key, title, DownloadStatus.Queued, updatedAt = currentTimeMillis()))
        drainQueue()
    }

    override suspend fun enqueueThread(tid: ThreadId, title: String, authorId: UserId?): Result<Unit> = runCatching {
        enqueueThreadPages(tid, title, authorId, includeLastPage = true)
    }

    override suspend fun enqueueThreadExceptLastPage(
        tid: ThreadId,
        title: String,
        authorId: UserId?,
    ): Result<Unit> = runCatching {
        enqueueThreadPages(tid, title, authorId, includeLastPage = false)
    }

    private suspend fun enqueueThreadPages(
        tid: ThreadId,
        title: String,
        authorId: UserId?,
        includeLastPage: Boolean,
    ) {
        initialized.await()
        ensureStorageReady()
        val first = when (val result = threadRepository.fetchThread(tid, authorId, page = 1)) {
            is YamiboResult.Success -> result.value
            else -> error(result.message())
        }
        val totalPages = first.pageNav?.totalPages ?: 1
        val lastPageToQueue = if (includeLastPage) totalPages else totalPages - 1
        if (lastPageToQueue < 1) error("此 Thread 只有一頁，沒有可排除最後頁後下載的內容")
        for (page in 1..lastPageToQueue) {
            val key = ThreadPageDownloadKey(tid.value, page, authorId?.value)
            knownTitles[key] = title
            if (page == 1) prefetchedPages[key] = first
            upsert(DownloadQueueEntry(key, title, DownloadStatus.Queued, updatedAt = currentTimeMillis()))
        }
        drainQueue()
    }

    override suspend fun refreshPage(tid: ThreadId, title: String, authorId: UserId?, page: Int): YamiboResult<ThreadPage> {
        initialized.await()
        if (!storageProvider.isReady()) return YamiboResult.Failure("尚未選擇備份資料夾")
        return when (val result = threadRepository.fetchThread(tid, authorId, page = page)) {
            is YamiboResult.Success -> {
                val key = ThreadPageDownloadKey(tid.value, page, authorId?.value)
                knownTitles[key] = title
                try {
                    persistPage(key, title, result.value)
                    upsert(DownloadQueueEntry(key, title, DownloadStatus.Downloaded, updatedAt = currentTimeMillis()))
                } catch (e: Exception) {
                    upsert(DownloadQueueEntry(key, title, DownloadStatus.Failed, message = e.message, updatedAt = currentTimeMillis()))
                    return YamiboResult.Failure(e.message ?: "下載內容寫入失敗", e)
                }
                result
            }
            else -> result
        }
    }

    override suspend fun markThreadUpdateAvailable(tid: ThreadId, authorId: UserId?) {
        initialized.await()
        storageProvider.listManifests()
            .filter { it.key.tid == tid.value && it.key.authorId == authorId?.value }
            .forEach { manifest ->
                upsert(
                    DownloadQueueEntry(
                        key = manifest.key,
                        title = manifest.title,
                        status = DownloadStatus.UpdateAvailable,
                        message = "下載內容可刷新",
                        updatedAt = currentTimeMillis(),
                    )
                )
            }
    }

    override suspend fun clearPage(key: ThreadPageDownloadKey): Result<Unit> = runCatching {
        initialized.await()
        prefetchedPages.remove(key)
        remove(key)
        storageProvider.deleteThreadPage(key)
    }

    override suspend fun clearThread(key: ThreadPageDownloadKey): Result<Unit> = runCatching {
        initialized.await()
        prefetchedPages.keys.removeAll { it.tid == key.tid && it.authorId == key.authorId }
        _queue.update { entries -> entries.filterNot { it.key.tid == key.tid && it.key.authorId == key.authorId } }
        persistQueue()
        storageProvider.deleteThread(key)
    }

    override suspend fun retry(key: ThreadPageDownloadKey): Result<Unit> = runCatching {
        initialized.await()
        ensureStorageReady()
        val title = knownTitles[key] ?: storageProvider.readManifest(key)?.title ?: key.stableId
        upsert(DownloadQueueEntry(key, title, DownloadStatus.Queued, updatedAt = currentTimeMillis()))
        drainQueue()
    }

    override fun pauseAll() {
        paused = true
        _queue.update { entries ->
            entries.map { if (it.status == DownloadStatus.Queued || it.status == DownloadStatus.Downloading) it.copy(status = DownloadStatus.Paused) else it }
        }
        persistQueue()
    }

    override fun resumeAll() {
        paused = false
        _queue.update { entries ->
            entries.map { if (it.status == DownloadStatus.Paused) it.copy(status = DownloadStatus.Queued) else it }
        }
        persistQueue()
        scope.launch { drainQueue() }
    }

    private fun drainQueue() {
        scope.launch {
            workerMutex.withLock {
                while (!paused) {
                    val next = queue.value.firstOrNull { it.status == DownloadStatus.Queued } ?: break
                    process(next)
                }
            }
        }
    }

    private suspend fun process(entry: DownloadQueueEntry) {
        upsert(entry.copy(status = DownloadStatus.Downloading, message = null, updatedAt = currentTimeMillis()))
        val prefetched = prefetchedPages.remove(entry.key)
        val result = prefetched?.let { YamiboResult.Success(it) }
            ?: threadRepository.fetchThread(
                ThreadId(entry.key.tid),
                entry.key.authorId?.let(::UserId),
                page = entry.key.page,
            )
        when (result) {
            is YamiboResult.Success -> {
                if (queue.value.none {
                        it.key == entry.key && it.status == DownloadStatus.Downloading
                    }
                ) {
                    return
                }
                runCatching { persistPage(entry.key, entry.title, result.value) }
                    .onSuccess {
                        upsert(entry.copy(status = DownloadStatus.Downloaded, progressCurrent = entry.progressTotal, updatedAt = currentTimeMillis()))
                    }
                    .onFailure {
                        upsert(entry.copy(status = DownloadStatus.Failed, message = it.message, updatedAt = currentTimeMillis()))
                    }
            }
            else -> upsert(entry.copy(status = DownloadStatus.Failed, message = result.message(), updatedAt = currentTimeMillis()))
        }
    }

    private suspend fun persistPage(key: ThreadPageDownloadKey, title: String, page: ThreadPage) {
        val imageUrls = page.posts.flatMap { post -> post.images.map { normalizeDownloadImageUrl(it.url) } }.distinct()
        val results = coroutineScope {
            imageUrls.mapIndexed { index, url -> index to url }
                .chunked(3)
                .flatMap { chunk ->
                    chunk.map { (index, url) ->
                        async {
                            val bytes = imageFetcher.fetch(url)
                            val fileName = imageFileName(index, url)
                            PendingDownloadedImage(fileName, bytes) to
                                DownloadedImage(url, fileName, bytes.size.toLong())
                        }
                    }.awaitAll()
                }
        }
        val pendingImages = results.map { it.first }
        val downloadedImages = results.map { it.second }
        val totalPages = page.pageNav?.totalPages ?: 1
        val manifest = ThreadPageDownloadManifest(
            key = key,
            title = title,
            downloadedAt = currentTimeMillis(),
            sourceTotalPages = totalPages,
            pageKind = if (key.page >= totalPages) DownloadPageKind.LastAtDownloadTime else DownloadPageKind.Normal,
            images = downloadedImages,
        )
        storageProvider.writeThreadPage(
            key = key,
            manifestBytes = json.encodeToString(manifest).encodeToByteArray(),
            threadPageBytes = json.encodeToString(page).encodeToByteArray(),
            images = pendingImages,
        )
        threadRepository.setCachedThread(ThreadId(key.tid), key.authorId?.let(::UserId), key.page, page)
    }

    private fun imageFileName(index: Int, url: String): String {
        val ext = url.substringBefore('?').substringAfterLast('.', "").takeIf { it.length in 2..5 } ?: "img"
        return "${index.toString().padStart(4, '0')}.$ext"
    }

    private suspend fun ensureStorageReady() {
        if (!storageProvider.isReady()) error("尚未選擇備份資料夾")
    }

    private fun upsert(entry: DownloadQueueEntry) {
        _queue.update { entries ->
            val without = entries.filterNot { it.key == entry.key }
            (without + entry).sortedWith(compareBy<DownloadQueueEntry> { it.key.tid }.thenBy { it.key.page })
        }
        backgroundController.onQueueChanged(queue.value)
        persistQueue()
    }

    private fun remove(key: ThreadPageDownloadKey) {
        _queue.update { entries -> entries.filterNot { it.key == key } }
        backgroundController.onQueueChanged(queue.value)
        persistQueue()
    }

    private fun persistQueue() {
        scope.launch {
            queueWriteMutex.withLock {
                storageProvider.writeQueue(queue.value)
            }
        }
    }
}
