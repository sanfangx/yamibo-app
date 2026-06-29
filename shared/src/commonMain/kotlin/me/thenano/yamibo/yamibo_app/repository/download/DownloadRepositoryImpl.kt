package me.thenano.yamibo.yamibo_app.repository.download

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.page.TagPage
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.TagId
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
import me.thenano.yamibo.yamibo_app.repository.TagRepository
import me.thenano.yamibo.yamibo_app.repository.ThreadRepository
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

class DownloadRepositoryImpl(
    private val threadRepository: ThreadRepository,
    private val tagRepository: TagRepository? = null,
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
    private val knownTitles = mutableMapOf<DownloadTaskKey, String>()
    private val prefetchedPages = mutableMapOf<ThreadPageDownloadKey, ThreadPage>()
    private val tagMangaTasks = mutableMapOf<TagMangaChapterDownloadKey, TagMangaTaskInfo>()
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
                val restoredTagMangaDownloads = storageProvider.listTagMangaManifests()
                    .filterNot { it.key in queueKeys }
                    .map { manifest ->
                        val title = tagMangaQueueTitle(manifest.tagName, manifest.title)
                        DownloadQueueEntry(
                            key = manifest.key,
                            title = title,
                            status = DownloadStatus.Downloaded,
                            progressCurrent = manifest.imageCount,
                            progressTotal = manifest.imageCount,
                            updatedAt = manifest.downloadedAt,
                        )
                    }
                val restored = (restoredQueue + restoredDownloads + restoredTagMangaDownloads)
                    .sortedWith(compareBy<DownloadQueueEntry> { it.key.stableId })
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
            downloaded = storageProvider.listManifests().size + storageProvider.listTagMangaManifests().size,
            failed = entries.count { it.status == DownloadStatus.Failed },
            updateAvailable = entries.count { it.status == DownloadStatus.UpdateAvailable },
        )
    }

    override suspend fun getDownloadedContentSummary(): DownloadedContentSummary {
        initialized.await()
        val threadManifests = storageProvider.listManifests()
        val tagManifests = storageProvider.listTagMangaManifests()
        val allImages = threadManifests.flatMap { it.images } + tagManifests.flatMap { it.images }
        return DownloadedContentSummary(
            totalItems = threadManifests.size + tagManifests.size,
            threadPages = threadManifests.size,
            tagMangaChapters = tagManifests.size,
            imageCount = allImages.size,
            imageBytes = allImages.sumOf { it.bytes },
        )
    }

    override suspend fun getDownloadedContentGroups(): List<DownloadedContentGroup> {
        initialized.await()
        val threadGroups = storageProvider.listManifests()
            .groupBy { manifest -> "thread_${manifest.key.tid}_author_${manifest.key.authorId ?: "all"}" }
            .map { (id, manifests) ->
                val sorted = manifests.sortedBy { it.key.page }
                val items = sorted.map { manifest ->
                    val imageBytes = manifest.images.sumOf { it.bytes }
                    DownloadedContentItem(
                        key = manifest.key,
                        title = manifest.title,
                        detail = "第 ${manifest.key.page} 頁",
                        downloadedAt = manifest.downloadedAt,
                        imageCount = manifest.images.size,
                        imageBytes = imageBytes,
                    )
                }
                DownloadedContentGroup(
                    id = id,
                    title = sorted.firstOrNull()?.title ?: id,
                    type = DownloadedContentGroupType.Thread,
                    itemCount = items.size,
                    imageCount = items.sumOf { it.imageCount },
                    imageBytes = items.sumOf { it.imageBytes },
                    items = items,
                )
            }
        val tagGroups = storageProvider.listTagMangaManifests()
            .groupBy { manifest -> "tag_manga_${manifest.key.tagId}" }
            .map { (id, manifests) ->
                val sorted = manifests.sortedWith(compareBy<TagMangaChapterManifest> { it.tagPage }.thenBy { it.title })
                val tagName = sorted.firstOrNull()?.tagName.orEmpty()
                val items = sorted.map { manifest ->
                    val imageBytes = manifest.images.sumOf { it.bytes }
                    DownloadedContentItem(
                        key = manifest.key,
                        title = manifest.title,
                        detail = "標籤第 ${manifest.tagPage} 頁",
                        downloadedAt = manifest.downloadedAt,
                        imageCount = manifest.images.size,
                        imageBytes = imageBytes,
                    )
                }
                DownloadedContentGroup(
                    id = id,
                    title = if (tagName.isBlank()) id else "#$tagName",
                    type = DownloadedContentGroupType.TagManga,
                    itemCount = items.size,
                    imageCount = items.sumOf { it.imageCount },
                    imageBytes = items.sumOf { it.imageBytes },
                    items = items,
                )
            }
        return (threadGroups + tagGroups).sortedWith(compareBy<DownloadedContentGroup> { it.type.name }.thenBy { it.title })
    }

    override suspend fun getStatus(key: ThreadPageDownloadKey): DownloadStatus {
        initialized.await()
        queue.value.firstOrNull { it.key == key }?.let { return it.status }
        return if (storageProvider.readManifest(key) != null) DownloadStatus.Downloaded else DownloadStatus.NotDownloaded
    }

    override suspend fun getStatus(key: TagMangaChapterDownloadKey): DownloadStatus {
        initialized.await()
        queue.value.firstOrNull { it.key == key }?.let { return it.status }
        return if (storageProvider.readTagMangaManifest(key) != null) DownloadStatus.Downloaded else DownloadStatus.NotDownloaded
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

    override suspend fun getTagMangaChapterImages(key: TagMangaChapterDownloadKey): List<String>? {
        initialized.await()
        val manifest = storageProvider.readTagMangaManifest(key) ?: return null
        return storageProvider.resolveTagMangaImageUris(key, manifest.images.map { it.fileName })
            .takeIf { it.isNotEmpty() }
    }

    override suspend fun getTagMangaManifest(key: TagMangaChapterDownloadKey): TagMangaChapterManifest? =
        initialized.await().let { storageProvider.readTagMangaManifest(key) }

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

    override suspend fun enqueueTagMangaChapter(
        tagId: TagId,
        tagName: String,
        thread: ThreadSummary,
        tagPage: Int,
    ): Result<Unit> = runCatching {
        initialized.await()
        ensureStorageReady()
        val key = tagMangaKey(tagId, thread)
        val title = tagMangaQueueTitle(tagName, thread.title)
        knownTitles[key] = title
        tagMangaTasks[key] = TagMangaTaskInfo(tagName, thread.title, tagPage)
        upsert(DownloadQueueEntry(key, title, DownloadStatus.Queued, updatedAt = currentTimeMillis()))
        drainQueue()
    }

    override suspend fun enqueueTagMangaCurrentPage(
        tagId: TagId,
        tagName: String,
        threads: List<ThreadSummary>,
        tagPage: Int,
    ): Result<Unit> = runCatching {
        initialized.await()
        ensureStorageReady()
        threads.forEach { thread ->
            val key = tagMangaKey(tagId, thread)
            val title = tagMangaQueueTitle(tagName, thread.title)
            knownTitles[key] = title
            tagMangaTasks[key] = TagMangaTaskInfo(tagName, thread.title, tagPage)
            upsert(DownloadQueueEntry(key, title, DownloadStatus.Queued, updatedAt = currentTimeMillis()))
        }
        drainQueue()
    }

    override suspend fun enqueueTagMangaAllPages(tagId: TagId, tagName: String): Result<Unit> = runCatching {
        initialized.await()
        ensureStorageReady()
        val repository = tagRepository ?: error("TagRepository is required for tag manga downloads")
        val first = when (val result = repository.fetchTagPage(tagId, 1)) {
            is YamiboResult.Success -> result.value
            else -> error(result.message())
        }
        queueTagMangaPage(tagId, tagName, first, 1)
        val totalPages = first.pageNav?.totalPages ?: 1
        for (page in 2..totalPages) {
            when (val result = repository.fetchTagPage(tagId, page)) {
                is YamiboResult.Success -> queueTagMangaPage(tagId, tagName, result.value, page)
                else -> error(result.message())
            }
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
                    upsert(DownloadQueueEntry(key, title, DownloadStatus.Downloading, stage = DownloadStage.Preparing, updatedAt = currentTimeMillis()))
                    persistPage(key, title, result.value)
                    upsert(DownloadQueueEntry(key, title, DownloadStatus.Downloaded, updatedAt = currentTimeMillis()))
                } catch (e: Exception) {
                    upsert(DownloadQueueEntry(key, title, DownloadStatus.Failed, message = e.message, updatedAt = currentTimeMillis()))
                    return YamiboResult.Failure(e.message ?: "下載內容寫入失敗", e)
                }
                result
            }
            else -> YamiboResult.Failure(result.message())
        }
    }

    override suspend fun refreshTagMangaChapter(key: TagMangaChapterDownloadKey): YamiboResult<List<String>> {
        initialized.await()
        if (!storageProvider.isReady()) return YamiboResult.Failure("尚未選擇備份資料夾")
        val oldManifest = storageProvider.readTagMangaManifest(key)
        val task = tagMangaTasks[key]
        val tagName = oldManifest?.tagName ?: task?.tagName ?: ""
        val title = oldManifest?.title ?: task?.chapterTitle ?: knownTitles[key] ?: key.stableId
        val tagPage = oldManifest?.tagPage ?: task?.tagPage ?: 1
        val result = threadRepository.fetchThread(ThreadId(key.tid), key.authorId?.let(::UserId), page = 1)
        if (result !is YamiboResult.Success) {
            return YamiboResult.Failure(result.message())
        }
        return try {
            upsert(DownloadQueueEntry(key, tagMangaQueueTitle(tagName, title), DownloadStatus.Downloading, stage = DownloadStage.Preparing, updatedAt = currentTimeMillis()))
            val images = persistTagMangaChapter(key, tagName, title, tagPage, result.value)
            val queueTitle = tagMangaQueueTitle(tagName, title)
            upsert(DownloadQueueEntry(key, queueTitle, DownloadStatus.Downloaded, images.size, images.size, updatedAt = currentTimeMillis()))
            YamiboResult.Success(images)
        } catch (e: Exception) {
            upsert(DownloadQueueEntry(key, tagMangaQueueTitle(tagName, title), DownloadStatus.Failed, message = e.message, updatedAt = currentTimeMillis()))
            YamiboResult.Failure(e.message ?: "下載內容寫入失敗", e)
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
        _queue.update { entries ->
            entries.filterNot {
                val entryKey = it.key as? ThreadPageDownloadKey
                entryKey?.tid == key.tid && entryKey.authorId == key.authorId
            }
        }
        persistQueue()
        storageProvider.deleteThread(key)
    }

    override suspend fun clearTagMangaChapter(key: TagMangaChapterDownloadKey): Result<Unit> = runCatching {
        initialized.await()
        tagMangaTasks.remove(key)
        remove(key)
        storageProvider.deleteTagMangaChapter(key)
    }

    override suspend fun clearTagManga(tagId: TagId): Result<Unit> = runCatching {
        initialized.await()
        tagMangaTasks.keys.removeAll { it.tagId == tagId.value }
        _queue.update { entries -> entries.filterNot { (it.key as? TagMangaChapterDownloadKey)?.tagId == tagId.value } }
        persistQueue()
        storageProvider.deleteTagManga(tagId.value)
    }

    override suspend fun retry(key: DownloadTaskKey): Result<Unit> = runCatching {
        initialized.await()
        ensureStorageReady()
        val title = knownTitles[key] ?: when (key) {
            is ThreadPageDownloadKey -> storageProvider.readManifest(key)?.title
            is TagMangaChapterDownloadKey -> storageProvider.readTagMangaManifest(key)?.let {
                tagMangaQueueTitle(it.tagName, it.title)
            }
        } ?: key.stableId
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
        upsert(entry.copy(status = DownloadStatus.Downloading, stage = DownloadStage.Preparing, progressCurrent = 0, progressTotal = 0, message = null, updatedAt = currentTimeMillis()))
        when (val key = entry.key) {
            is ThreadPageDownloadKey -> processThreadPage(entry, key)
            is TagMangaChapterDownloadKey -> processTagMangaChapter(entry, key)
        }
    }

    private suspend fun processThreadPage(entry: DownloadQueueEntry, key: ThreadPageDownloadKey) {
        val prefetched = prefetchedPages.remove(key)
        upsert(entry.copy(status = DownloadStatus.Downloading, stage = DownloadStage.FetchingContent, progressCurrent = 0, progressTotal = 0, message = null, updatedAt = currentTimeMillis()))
        val result = prefetched?.let { YamiboResult.Success(it) }
            ?: threadRepository.fetchThread(
                ThreadId(key.tid),
                key.authorId?.let(::UserId),
                page = key.page,
            )
        when (result) {
            is YamiboResult.Success -> {
                if (queue.value.none {
                        it.key == key && it.status == DownloadStatus.Downloading
                    }
                ) {
                    return
                }
                runCatching { persistPage(key, entry.title, result.value) }
                    .onSuccess {
                        val current = queue.value.firstOrNull { it.key == key }
                        upsert(entry.copy(status = DownloadStatus.Downloaded, progressCurrent = current?.progressTotal ?: 0, progressTotal = current?.progressTotal ?: 0, stage = null, updatedAt = currentTimeMillis()))
                    }
                    .onFailure {
                        upsert(entry.copy(status = DownloadStatus.Failed, stage = null, message = it.message, updatedAt = currentTimeMillis()))
                    }
            }
            else -> upsert(entry.copy(status = DownloadStatus.Failed, stage = null, message = result.message(), updatedAt = currentTimeMillis()))
        }
    }

    private suspend fun processTagMangaChapter(entry: DownloadQueueEntry, key: TagMangaChapterDownloadKey) {
        val task = tagMangaTasks[key]
        val oldManifest = storageProvider.readTagMangaManifest(key)
        val tagName = oldManifest?.tagName ?: task?.tagName ?: ""
        val title = oldManifest?.title ?: task?.chapterTitle ?: entry.title
        val tagPage = oldManifest?.tagPage ?: task?.tagPage ?: 1
        upsert(entry.copy(status = DownloadStatus.Downloading, stage = DownloadStage.FetchingContent, progressCurrent = 0, progressTotal = 0, message = null, updatedAt = currentTimeMillis()))
        when (val result = threadRepository.fetchThread(ThreadId(key.tid), key.authorId?.let(::UserId), page = 1)) {
            is YamiboResult.Success -> {
                if (queue.value.none { it.key == key && it.status == DownloadStatus.Downloading }) return
                runCatching { persistTagMangaChapter(key, tagName, title, tagPage, result.value) }
                    .onSuccess { images ->
                        upsert(entry.copy(status = DownloadStatus.Downloaded, progressCurrent = images.size, progressTotal = images.size, stage = null, updatedAt = currentTimeMillis()))
                    }
                    .onFailure {
                        upsert(entry.copy(status = DownloadStatus.Failed, stage = null, message = it.message, updatedAt = currentTimeMillis()))
                    }
            }
            else -> upsert(entry.copy(status = DownloadStatus.Failed, stage = null, message = result.message(), updatedAt = currentTimeMillis()))
        }
    }

    private suspend fun persistPage(key: ThreadPageDownloadKey, title: String, page: ThreadPage) {
        updateStage(key, DownloadStage.DownloadingText)
        val imageUrls = page.posts.flatMap { post -> post.images.map { normalizeDownloadImageUrl(it.url) } }.distinct()
        val results = downloadImages(key, imageUrls, startIndex = 0)
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
        updateStage(key, DownloadStage.Saving, downloadedImages.size, downloadedImages.size)
        storageProvider.writeThreadPage(
            key = key,
            manifestBytes = json.encodeToString(manifest).encodeToByteArray(),
            threadPageBytes = json.encodeToString(page).encodeToByteArray(),
            images = pendingImages,
        )
        threadRepository.setCachedThread(ThreadId(key.tid), key.authorId?.let(::UserId), key.page, page)
    }

    private suspend fun persistTagMangaChapter(
        key: TagMangaChapterDownloadKey,
        tagName: String,
        title: String,
        tagPage: Int,
        page: ThreadPage,
    ): List<String> {
        val targetAuthorId = key.authorId ?: page.posts.firstOrNull()?.author?.uid?.value
        val imageUrls = page.posts
            .filter { targetAuthorId == null || it.author.uid.value == targetAuthorId }
            .take(2)
            .flatMap { post -> post.images.map { normalizeDownloadImageUrl(it.url) } }
            .distinct()
        if (imageUrls.isEmpty()) error("此章節沒有可下載圖片")
        val results = downloadImages(key, imageUrls, startIndex = 1)
        val pendingImages = results.map { it.first }
        val downloadedImages = results.map { it.second }
        val manifest = TagMangaChapterManifest(
            key = key,
            tagName = tagName,
            title = title,
            tagPage = tagPage,
            imageCount = downloadedImages.size,
            downloadedAt = currentTimeMillis(),
            sourceUpdatedAt = null,
            images = downloadedImages,
        )
        updateStage(key, DownloadStage.Saving, downloadedImages.size, downloadedImages.size)
        storageProvider.writeTagMangaChapter(
            key = key,
            manifestBytes = json.encodeToString(manifest).encodeToByteArray(),
            images = pendingImages,
        )
        return downloadedImages.mapNotNull { storageProvider.resolveTagMangaImageUri(key, it.fileName) }
            .ifEmpty { downloadedImages.map { it.sourceUrl } }
    }

    private suspend fun queueTagMangaPage(tagId: TagId, tagName: String, page: TagPage, pageNumber: Int) {
        page.threadSummaries.forEach { thread ->
            val key = tagMangaKey(tagId, thread)
            val title = tagMangaQueueTitle(tagName, thread.title)
            knownTitles[key] = title
            tagMangaTasks[key] = TagMangaTaskInfo(tagName, thread.title, pageNumber)
            upsert(DownloadQueueEntry(key, title, DownloadStatus.Queued, updatedAt = currentTimeMillis()))
        }
    }

    private suspend fun downloadImages(
        key: DownloadTaskKey,
        imageUrls: List<String>,
        startIndex: Int,
    ): List<Pair<PendingDownloadedImage, DownloadedImage>> {
        updateStage(key, DownloadStage.DownloadingImages, 0, imageUrls.size)
        var completed = 0
        val results = mutableListOf<Pair<PendingDownloadedImage, DownloadedImage>>()
        coroutineScope {
            imageUrls.mapIndexed { index, url -> index to url }
                .chunked(3)
                .forEach { chunk ->
                    val chunkResults = chunk.map { (index, url) ->
                        async {
                            val bytes = imageFetcher.fetch(url)
                            val fileName = imageFileName(index + startIndex, url)
                            PendingDownloadedImage(fileName, bytes) to DownloadedImage(url, fileName, bytes.size.toLong())
                        }
                    }.awaitAll()
                    results += chunkResults
                    completed += chunkResults.size
                    updateStage(key, DownloadStage.DownloadingImages, completed, imageUrls.size)
                }
        }
        return results
    }

    private fun imageFileName(index: Int, url: String): String {
        val ext = url.substringBefore('?').substringAfterLast('.', "").takeIf { it.length in 2..5 } ?: "img"
        return "${index.toString().padStart(4, '0')}.$ext"
    }

    private fun tagMangaKey(tagId: TagId, thread: ThreadSummary): TagMangaChapterDownloadKey =
        TagMangaChapterDownloadKey(tagId.value, thread.tid.value, thread.author?.uid?.value)

    private fun tagMangaQueueTitle(tagName: String, title: String): String =
        if (tagName.isBlank()) title else "#$tagName / $title"

    private fun updateStage(
        key: DownloadTaskKey,
        stage: DownloadStage,
        progressCurrent: Int = 0,
        progressTotal: Int = 0,
    ) {
        val current = queue.value.firstOrNull { it.key == key } ?: return
        upsert(
            current.copy(
                status = DownloadStatus.Downloading,
                stage = stage,
                progressCurrent = progressCurrent,
                progressTotal = progressTotal,
                message = null,
                updatedAt = currentTimeMillis(),
            )
        )
    }

    private suspend fun ensureStorageReady() {
        if (!storageProvider.isReady()) error("尚未選擇備份資料夾")
    }

    private fun upsert(entry: DownloadQueueEntry) {
        _queue.update { entries ->
            val without = entries.filterNot { it.key == entry.key }
            (without + entry).sortedWith(compareBy<DownloadQueueEntry> { it.key.stableId })
        }
        backgroundController.onQueueChanged(queue.value)
        persistQueue()
    }

    private fun remove(key: DownloadTaskKey) {
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

    private data class TagMangaTaskInfo(
        val tagName: String,
        val chapterTitle: String,
        val tagPage: Int,
    )
}
