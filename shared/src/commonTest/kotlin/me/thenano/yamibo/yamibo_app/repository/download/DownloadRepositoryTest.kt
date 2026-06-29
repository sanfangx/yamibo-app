package me.thenano.yamibo.yamibo_app.repository.download

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.Tags
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.PostImage
import io.github.littlesurvival.dto.page.RatePopoutPage
import io.github.littlesurvival.dto.page.RateResultPopoutPage
import io.github.littlesurvival.dto.page.ThreadInfo
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.page.VotersPopoutScreen
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.thenano.yamibo.yamibo_app.repository.ThreadRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadRepositoryTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun keyIsStableAndSeparatesAuthorMode() {
        assertEquals("thread_42_page_3_author_all", ThreadPageDownloadKey(42, 3).stableId)
        assertEquals("thread_42_page_3_author_7", ThreadPageDownloadKey(42, 3, 7).stableId)
        assertEquals("thread_42_", ThreadPageDownloadKey(42, 3, 7).threadPrefix)
    }

    @Test
    fun downloadedPageUsesLocalImageUrisInImagesAndHtml() = runBlocking {
        val key = ThreadPageDownloadKey(42, 1)
        val source = "https://bbs.yamibo.com/data/attachment/forum/a.jpg"
        val storage = FakeStorage()
        val page = page(42, 1, 1, source)
        storage.seed(
            key,
            ThreadPageDownloadManifest(
                key = key,
                title = "title",
                downloadedAt = 1,
                sourceTotalPages = 1,
                pageKind = DownloadPageKind.LastAtDownloadTime,
                images = listOf(DownloadedImage(source, "0000.jpg", 3)),
            ),
            page,
            mapOf("0000.jpg" to "content://download/0000.jpg"),
        )
        val repository = repository(FakeThreadRepository(), storage)

        val downloaded = assertNotNull(repository.getDownloadedPage(key))

        assertEquals("content://download/0000.jpg", downloaded.posts.single().images.single().url)
        assertTrue(downloaded.posts.single().contentHtml.contains("content://download/0000.jpg"))
    }

    @Test
    fun completeThreadExpandsAllPagesAndMarksLastPage() = runBlocking {
        val storage = FakeStorage()
        val thread = FakeThreadRepository(
            pages = mutableMapOf(
                1 to YamiboResult.Success(page(42, 1, 3)),
                2 to YamiboResult.Success(page(42, 2, 3)),
                3 to YamiboResult.Success(page(42, 3, 3)),
            ),
        )
        val repository = repository(thread, storage)

        repository.enqueueThread(ThreadId(42), "title", null).getOrThrow()
        awaitStatus(repository, ThreadPageDownloadKey(42, 3), DownloadStatus.Downloaded)

        assertEquals(setOf(1, 2, 3), storage.manifests.values.map { it.key.page }.toSet())
        assertEquals(DownloadPageKind.Normal, storage.manifests.getValue(ThreadPageDownloadKey(42, 2)).pageKind)
        assertEquals(
            DownloadPageKind.LastAtDownloadTime,
            storage.manifests.getValue(ThreadPageDownloadKey(42, 3)).pageKind,
        )
        assertEquals(1, thread.fetchCounts[1])
    }

    @Test
    fun threadExceptLastPageQueuesOnlyStablePages() = runBlocking {
        val storage = FakeStorage()
        val thread = FakeThreadRepository(
            pages = mutableMapOf(
                1 to YamiboResult.Success(page(42, 1, 3)),
                2 to YamiboResult.Success(page(42, 2, 3)),
            ),
        )
        val repository = repository(thread, storage)

        repository.enqueueThreadExceptLastPage(ThreadId(42), "title", null).getOrThrow()
        awaitStatus(repository, ThreadPageDownloadKey(42, 2), DownloadStatus.Downloaded)

        assertEquals(setOf(1, 2), storage.manifests.keys.map { it.page }.toSet())
        assertEquals(DownloadStatus.NotDownloaded, repository.getStatus(ThreadPageDownloadKey(42, 3)))
    }

    @Test
    fun threadExceptLastPageRejectsSinglePageThread() = runBlocking {
        val repository = repository(
            FakeThreadRepository(
                pages = mutableMapOf(1 to YamiboResult.Success(page(42, 1, 1))),
            ),
            FakeStorage(),
        )

        val result = repository.enqueueThreadExceptLastPage(ThreadId(42), "title", null)

        assertTrue(result.isFailure)
        Unit
    }

    @Test
    fun restoresDownloadedManifestsWhenQueueFileIsMissing() = runBlocking {
        val key = ThreadPageDownloadKey(42, 1)
        val storage = FakeStorage()
        storage.seed(
            key,
            ThreadPageDownloadManifest(key, "saved", 1, 1, DownloadPageKind.LastAtDownloadTime),
            page(42, 1, 1),
        )

        val repository = repository(FakeThreadRepository(), storage)
        repeat(100) {
            if (repository.queue.value.any { it.key == key }) return@repeat
            delay(10)
        }

        assertEquals(DownloadStatus.Downloaded, repository.getStatus(key))
        assertTrue(repository.queue.value.any { it.key == key && it.status == DownloadStatus.Downloaded })
    }

    @Test
    fun restoresInterruptedQueueAndContinuesDownload() = runBlocking {
        val key = ThreadPageDownloadKey(42, 1)
        val storage = FakeStorage()
        storage.seedQueue(
            listOf(DownloadQueueEntry(key, "restored", DownloadStatus.Downloading)),
        )
        val repository = repository(
            FakeThreadRepository(
                pages = mutableMapOf(1 to YamiboResult.Success(page(42, 1, 1))),
            ),
            storage,
        )

        awaitStatus(repository, key, DownloadStatus.Downloaded)

        assertNotNull(storage.manifests[key])
        Unit
    }

    @Test
    fun clearingInFlightPagePreventsLateWrite() = runBlocking {
        val key = ThreadPageDownloadKey(42, 1)
        val gate = CompletableDeferred<Unit>()
        val storage = FakeStorage()
        val repository = repository(
            FakeThreadRepository(
                pages = mutableMapOf(1 to YamiboResult.Success(page(42, 1, 1))),
                fetchGate = gate,
            ),
            storage,
        )

        repository.enqueuePage(ThreadId(42), "title", null, 1).getOrThrow()
        awaitStatus(repository, key, DownloadStatus.Downloading)
        repository.clearPage(key).getOrThrow()
        gate.complete(Unit)
        delay(100)

        assertNull(storage.manifests[key])
        assertEquals(DownloadStatus.NotDownloaded, repository.getStatus(key))
    }

    @Test
    fun refreshFailureKeepsExistingSnapshot() = runBlocking {
        val key = ThreadPageDownloadKey(42, 1)
        val storage = FakeStorage()
        val oldPage = page(42, 1, 1)
        storage.seed(
            key,
            ThreadPageDownloadManifest(key, "old", 1, 1, DownloadPageKind.LastAtDownloadTime),
            oldPage,
        )
        val thread = FakeThreadRepository(
            pages = mutableMapOf(1 to YamiboResult.Failure("network failed")),
        )
        val repository = repository(thread, storage)

        val result = repository.refreshPage(ThreadId(42), "new", null, 1)

        assertIs<YamiboResult.Failure>(result)
        assertEquals("old", storage.manifests.getValue(key).title)
        assertNotNull(storage.pages[key])
        Unit
    }

    @Test
    fun clearThreadOnlyDeletesMatchingAuthorVariant() = runBlocking {
        val storage = FakeStorage()
        val all = ThreadPageDownloadKey(42, 1)
        val author = ThreadPageDownloadKey(42, 1, 7)
        val other = ThreadPageDownloadKey(43, 1)
        listOf(all, author, other).forEach { key ->
            storage.seed(
                key,
                ThreadPageDownloadManifest(key, "title", 1, 1, DownloadPageKind.LastAtDownloadTime),
                page(key.tid, 1, 1),
            )
        }
        val repository = repository(FakeThreadRepository(), storage)

        repository.clearThread(author).getOrThrow()

        assertNotNull(storage.manifests[all])
        assertNull(storage.manifests[author])
        assertNotNull(storage.manifests[other])
        Unit
    }

    private fun repository(
        threadRepository: ThreadRepository,
        storage: FakeStorage,
    ) = DownloadRepositoryImpl(
        threadRepository = threadRepository,
        storageProvider = storage,
        imageFetcher = object : DownloadImageFetcher({ "" }) {
            override suspend fun fetch(url: String): ByteArray = byteArrayOf(1, 2, 3)
        },
        json = json,
    )

    private suspend fun awaitStatus(
        repository: DownloadRepository,
        key: ThreadPageDownloadKey,
        expected: DownloadStatus,
    ) {
        repeat(200) {
            if (repository.getStatus(key) == expected) return
            delay(10)
        }
        error("Timed out waiting for $key to become $expected. Queue=${repository.queue.value}")
    }

    private fun page(tid: Int, currentPage: Int, totalPages: Int, image: String? = null): ThreadPage {
        val source = image ?: ""
        return ThreadPage(
            thread = ThreadInfo(
                tid = ThreadId(tid),
                title = "title",
                forum = ForumSummary(ForumId(1), "forum", "forum.php?fid=1"),
            ),
            posts = listOf(
                Post(
                    pid = PostId(currentPage),
                    floor = currentPage,
                    title = "",
                    author = User(UserId(7), "author"),
                    timeCreate = TimeInfo("2026-01-01", null, 0),
                    contentHtml = if (source.isBlank()) "" else "<img src=\"$source\">",
                    images = if (source.isBlank()) emptyList() else listOf(PostImage(source)),
                    tags = Tags(),
                    poll = null,
                ),
            ),
            pageNav = PageNav(currentPage = currentPage, totalPages = totalPages),
        )
    }

    private class FakeStorage : DownloadStorageProvider {
        val manifests = mutableMapOf<ThreadPageDownloadKey, ThreadPageDownloadManifest>()
        val pages = mutableMapOf<ThreadPageDownloadKey, ByteArray>()
        private val imageUris = mutableMapOf<Pair<ThreadPageDownloadKey, String>, String>()
        private var queue = emptyList<DownloadQueueEntry>()
        private val json = Json { ignoreUnknownKeys = true }

        fun seed(
            key: ThreadPageDownloadKey,
            manifest: ThreadPageDownloadManifest,
            page: ThreadPage,
            images: Map<String, String> = emptyMap(),
        ) {
            manifests[key] = manifest
            pages[key] = json.encodeToString(page).encodeToByteArray()
            images.forEach { (name, uri) -> imageUris[key to name] = uri }
        }

        fun seedQueue(entries: List<DownloadQueueEntry>) {
            queue = entries
        }

        override suspend fun getSelectedFolderLabel(): String = "test"
        override suspend fun isReady(): Boolean = true
        override suspend fun writeThreadPage(
            key: ThreadPageDownloadKey,
            manifestBytes: ByteArray,
            threadPageBytes: ByteArray,
            images: List<PendingDownloadedImage>,
        ) {
            manifests[key] = json.decodeFromString(manifestBytes.decodeToString())
            pages[key] = threadPageBytes
            images.forEach { image -> imageUris[key to image.fileName] = "content://download/${image.fileName}" }
        }

        override suspend fun readThreadPage(key: ThreadPageDownloadKey): ByteArray? = pages[key]
        override suspend fun resolveImageUri(key: ThreadPageDownloadKey, fileName: String): String? =
            imageUris[key to fileName]
        override suspend fun readManifest(key: ThreadPageDownloadKey): ThreadPageDownloadManifest? = manifests[key]
        override suspend fun listManifests(): List<ThreadPageDownloadManifest> = manifests.values.toList()
        override suspend fun readQueue(): List<DownloadQueueEntry> = queue
        override suspend fun writeQueue(entries: List<DownloadQueueEntry>) {
            queue = entries
        }

        override suspend fun deleteThreadPage(key: ThreadPageDownloadKey) {
            manifests.remove(key)
            pages.remove(key)
            imageUris.keys.removeAll { it.first == key }
        }

        override suspend fun deleteThread(key: ThreadPageDownloadKey) {
            manifests.keys
                .filter { it.tid == key.tid && it.authorId == key.authorId }
                .toList()
                .forEach { deleteThreadPage(it) }
        }
    }

    private class FakeThreadRepository(
        private val pages: MutableMap<Int, YamiboResult<ThreadPage>> = mutableMapOf(),
        private val fetchGate: CompletableDeferred<Unit>? = null,
    ) : ThreadRepository {
        val fetchCounts = mutableMapOf<Int, Int>()

        override suspend fun fetchThread(
            tid: ThreadId,
            authorId: UserId?,
            page: Int,
            reverse: Boolean,
        ): YamiboResult<ThreadPage> {
            fetchCounts[page] = (fetchCounts[page] ?: 0) + 1
            fetchGate?.await()
            return pages[page] ?: YamiboResult.Failure("missing page $page")
        }

        override fun getCachedThread(tid: ThreadId, authorId: UserId?, page: Int): ThreadPage? = null
        override fun setCachedThread(tid: ThreadId, authorId: UserId?, page: Int, threadPage: ThreadPage) = Unit
        override fun clearCachedThread(tid: ThreadId) = Unit
        override suspend fun fetchFindPost(tid: ThreadId, postId: PostId, authorId: UserId?) = error("unused")
        override suspend fun addFavorite(tid: ThreadId, formHash: FormHash) = error("unused")
        override suspend fun votePoll(
            fId: ForumId,
            tId: ThreadId,
            pollOptionIds: List<PollOptionId>,
            formHash: FormHash,
        ) = error("unused")
        override suspend fun fetchRatePopoutPage(tId: ThreadId, pId: PostId): YamiboResult<RatePopoutPage> = error("unused")
        override suspend fun fetchRateResults(tId: ThreadId, pId: PostId): YamiboResult<RateResultPopoutPage> = error("unused")
        override suspend fun fetchVoters(
            tId: ThreadId,
            pollOptionId: PollOptionId?,
            page: Int,
        ): YamiboResult<VotersPopoutScreen> = error("unused")
        override suspend fun ratePost(
            tId: ThreadId,
            pId: PostId,
            score: Int,
            reason: String,
            formHash: FormHash,
            noticeAuthor: Boolean,
        ) = error("unused")
        override suspend fun commentPost(
            tId: ThreadId,
            pId: PostId,
            message: String,
            formHash: FormHash,
        ) = error("unused")
    }
}
