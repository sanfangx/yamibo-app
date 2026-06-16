package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import me.thenano.yamibo.yamiboapp.MangaTagReadingHistory
import me.thenano.yamibo.yamiboapp.ReadingHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidReadHistoryRepository(dbFactory: DatabaseFactory) : ReadHistoryRepository {
    private companion object {
        const val COMBINED_HISTORY_BATCH_SIZE = 500L
        const val MAX_HISTORY_ITEMS = 2000L
    }

    private val db = Database(dbFactory.createDriver())
    private val queries = db.readingHistoryQueries
    private val readingTimeQueries = db.readingTimeStatQueries

    private suspend fun <T> databaseIo(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    override suspend fun savePosition(history: ReadHistoryRepository.ThreadReadingHistory) = databaseIo {
        queries.upsert(
            threadId = history.threadId.value.toLong(),
            threadType = history.threadType.name,
            threadName = history.threadName,
            threadCover = history.threadCover,
            forumName = history.forumName,
            forumId = history.forumId?.value?.toLong(),
            authorId = history.authorId?.value?.toLong() ?: 0L,
            page = history.page.toLong(),
            postId = history.postId.value.toLong(),
            postTitle = history.postTitle,
            anchorPostId = history.anchorPostId,
            anchorPostRatio = history.anchorPostRatio?.toDouble(),
            anchorBlockId = history.anchorBlockId,
            anchorBlockType = history.anchorBlockType,
            anchorBlockRatio = history.anchorBlockRatio?.toDouble(),
            globalScrollY = history.globalScrollY?.toLong(),
            viewportHeight = history.viewportHeight?.toLong(),
            firstVisibleItemIndex = history.firstVisibleItemIndex?.toLong(),
            firstVisibleItemOffset = history.firstVisibleItemOffset?.toLong(),
            lastVisitTime = history.lastVisitTime,
            lastUpdatedTime = history.lastUpdatedTime
        )
        queries.trimToLatest(MAX_HISTORY_ITEMS)
        Unit
    }

    override suspend fun getPosition(
        tid: ThreadId,
        threadType: ReadHistoryRepository.ThreadEntryType,
        authorId: UserId?
    ): ReadHistoryRepository.ThreadReadingHistory? = databaseIo {
        queries.getByThreadKey(
            threadId = tid.value.toLong(),
            threadType = threadType.name,
            authorId = authorId?.value?.toLong() ?: 0L
        ).executeAsOneOrNull()?.toHistory()
    }

    override suspend fun getHistoryPage(
        page: Int,
        pageSize: Int
    ): List<ReadHistoryRepository.ThreadReadingHistory> = databaseIo {
        val offset = (page - 1).coerceAtLeast(0) * pageSize
        queries.getPage(pageSize.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toHistory() }
    }

    override suspend fun getHistoryCount(): Long = databaseIo { queries.countAll().executeAsOne() }

    override suspend fun deleteHistory(
        tid: ThreadId,
        threadType: ReadHistoryRepository.ThreadEntryType,
        authorId: UserId?
    ) = databaseIo {
        queries.deleteByThreadKey(
            threadId = tid.value.toLong(),
            threadType = threadType.name,
            authorId = authorId?.value?.toLong() ?: 0L
        )
        Unit
    }

    override suspend fun deleteAll() = databaseIo {
        queries.deleteAll()
        Unit
    }

    override suspend fun searchHistory(
        query: String,
        page: Int,
        pageSize: Int
    ): List<ReadHistoryRepository.ThreadReadingHistory> = databaseIo {
        val offset = (page - 1).coerceAtLeast(0) * pageSize
        queries.searchByName(query, pageSize.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toHistory() }
    }

    override suspend fun searchHistoryCount(query: String): Long = databaseIo { queries.countByName(query).executeAsOne() }

    override suspend fun deleteHistoryBatch(items: List<ReadHistoryRepository.ThreadReadingHistory>) = databaseIo {
        if (items.isEmpty()) return@databaseIo
        db.transaction {
            items.forEach { item ->
                queries.deleteByThreadKey(
                    threadId = item.threadId.value.toLong(),
                    threadType = item.threadType.name,
                    authorId = item.authorId?.value?.toLong() ?: 0L
                )
            }
        }
    }

    private fun ReadingHistory.toHistory(): ReadHistoryRepository.ThreadReadingHistory {
        return ReadHistoryRepository.ThreadReadingHistory(
            threadType = ReadHistoryRepository.ThreadEntryType.fromStorage(threadType),
            threadName = threadName,
            threadId = ThreadId(threadId.toInt()),
            threadCover = threadCover,
            lastUpdatedTime = lastUpdatedTime,
            forumName = forumName,
            forumId = forumId?.toInt()?.let { ForumId(it) },
            authorId = authorId.takeIf { it != 0L }?.toInt()?.let { UserId(it) },
            page = page.toInt(),
            postId = PostId(postId.toInt()),
            postTitle = postTitle,
            anchorPostId = anchorPostId,
            anchorPostRatio = anchorPostRatio?.toFloat(),
            anchorBlockId = anchorBlockId,
            anchorBlockType = anchorBlockType,
            anchorBlockRatio = anchorBlockRatio?.toFloat(),
            globalScrollY = globalScrollY?.toInt(),
            viewportHeight = viewportHeight?.toInt(),
            firstVisibleItemIndex = firstVisibleItemIndex?.toInt(),
            firstVisibleItemOffset = firstVisibleItemOffset?.toInt(),
            lastVisitTime = lastVisitTime
        )
    }

    private val imageQueries = db.imageReadingHistoryQueries

    override suspend fun saveImagePosition(history: ReadHistoryRepository.ImageReadingHistory) = databaseIo {
        imageQueries.upsert(
            postId = history.postId.value.toLong(),
            threadId = history.threadId.value.toLong(),
            pageIndex = history.pageIndex.toLong(),
            totalPages = history.totalPages.toLong(),
            firstVisibleItemIndex = history.firstVisibleItemIndex?.toLong(),
            firstVisibleItemOffset = history.firstVisibleItemOffset?.toLong(),
            lastVisitTime = history.lastVisitTime
        )
        imageQueries.trimToLatest(MAX_HISTORY_ITEMS)
        Unit
    }

    override suspend fun getImagePosition(postId: PostId): ReadHistoryRepository.ImageReadingHistory? = databaseIo {
        imageQueries.getByPostId(postId.value.toLong()).executeAsOneOrNull()?.let {
            ReadHistoryRepository.ImageReadingHistory(
                postId = PostId(it.postId.toInt()),
                threadId = ThreadId(it.threadId.toInt()),
                pageIndex = it.pageIndex.toInt(),
                totalPages = it.totalPages.toInt(),
                firstVisibleItemIndex = it.firstVisibleItemIndex?.toInt(),
                firstVisibleItemOffset = it.firstVisibleItemOffset?.toInt(),
                lastVisitTime = it.lastVisitTime
            )
        }
    }

    private val mangaTagQueries = db.mangaTagReadingHistoryQueries

    override suspend fun saveTagMangaReaderModeHistory(history: ReadHistoryRepository.TagMangaReadingHistory) = databaseIo {
        mangaTagQueries.upsert(
            tagId = history.tagId.value.toLong(),
            tagName = history.tagName,
            tagPage = history.tagPage.toLong(),
            threadId = history.threadId.value.toLong(),
            threadTitle = history.threadTitle,
            threadImagePageIndex = history.threadImagePageIndex.toLong(),
            threadImageTotalPages = history.threadImageTotalPages.toLong(),
            firstVisibleItemIndex = history.firstVisibleItemIndex?.toLong(),
            firstVisibleItemOffset = history.firstVisibleItemOffset?.toLong(),
            lastVisitTime = history.lastVisitTime,
            coverUrl = history.coverUrl
        )
        mangaTagQueries.trimToLatest(MAX_HISTORY_ITEMS)
        Unit
    }

    override suspend fun getTagMangaReaderModeHistoryPosition(tagId: TagId): ReadHistoryRepository.TagMangaReadingHistory? = databaseIo {
        val history = mangaTagQueries.getByTagId(tagId.value.toLong()).executeAsOneOrNull()
        history?.let {
            ReadHistoryRepository.TagMangaReadingHistory(
                tagId = TagId(it.tagId.toInt()),
                tagName = it.tagName,
                tagPage = it.tagPage.toInt(),
                threadId = ThreadId(it.threadId.toInt()),
                threadTitle = it.threadTitle,
                threadImagePageIndex = it.threadImagePageIndex.toInt(),
                threadImageTotalPages = it.threadImageTotalPages.toInt(),
                firstVisibleItemIndex = it.firstVisibleItemIndex?.toInt(),
                firstVisibleItemOffset = it.firstVisibleItemOffset?.toInt(),
                lastVisitTime = it.lastVisitTime,
                coverUrl = it.coverUrl
            )
        }
    }

    override suspend fun deleteMangaTagHistory(tagId: TagId) = databaseIo {
        mangaTagQueries.deleteByTagId(tagId.value.toLong())
        Unit
    }
    private fun MangaTagReadingHistory.toHistory(): ReadHistoryRepository.TagMangaReadingHistory {
        return ReadHistoryRepository.TagMangaReadingHistory(
            tagId = TagId(tagId.toInt()),
            tagName = tagName,
            tagPage = tagPage.toInt(),
            threadId = ThreadId(threadId.toInt()),
            threadTitle = threadTitle,
            threadImagePageIndex = threadImagePageIndex.toInt(),
            threadImageTotalPages = threadImageTotalPages.toInt(),
            firstVisibleItemIndex = firstVisibleItemIndex?.toInt(),
            firstVisibleItemOffset = firstVisibleItemOffset?.toInt(),
            lastVisitTime = lastVisitTime,
            coverUrl = coverUrl
        )
    }

    override suspend fun getCombinedHistoryPage(
        page: Int,
        pageSize: Int
    ): List<ReadHistoryRepository.AnyReadingHistory> = databaseIo {
        val offset = (page - 1).coerceAtLeast(0) * pageSize
        val threads = loadAllThreadHistory()
        val tags = loadAllTagHistory()
        (threads + tags)
            .sortedByDescending { it.lastVisitTime }
            .drop(offset)
            .take(pageSize)
    }

    override suspend fun getCombinedHistoryCount(): Long = databaseIo {
        queries.countAll().executeAsOne() + mangaTagQueries.countAll().executeAsOne()
    }

    override suspend fun getCombinedHistoryPageByFilter(
        filter: ReadHistoryRepository.HistoryFilter,
        page: Int,
        pageSize: Int
    ): List<ReadHistoryRepository.AnyReadingHistory> = databaseIo {
        val offset = (page - 1).coerceAtLeast(0) * pageSize
        when (filter) {
            ReadHistoryRepository.HistoryFilter.All -> {
                val threads = loadAllThreadHistory()
                val tags = loadAllTagHistory()
                (threads + tags)
                    .sortedByDescending { it.lastVisitTime }
                    .drop(offset)
                    .take(pageSize)
            }
            is ReadHistoryRepository.HistoryFilter.Forum -> queries.getPageByForumId(
                filter.forumId.value.toLong(),
                pageSize.toLong(),
                offset.toLong(),
            ).executeAsList().map { it.toHistory() }
            ReadHistoryRepository.HistoryFilter.Tag -> mangaTagQueries.getPage(
                pageSize.toLong(),
                offset.toLong(),
            ).executeAsList().map { it.toHistory() }
        }
    }

    override suspend fun getCombinedHistoryCountByFilter(filter: ReadHistoryRepository.HistoryFilter): Long = databaseIo {
        when (filter) {
            ReadHistoryRepository.HistoryFilter.All -> queries.countAll().executeAsOne() + mangaTagQueries.countAll().executeAsOne()
            is ReadHistoryRepository.HistoryFilter.Forum -> queries.countByForumId(filter.forumId.value.toLong()).executeAsOne()
            ReadHistoryRepository.HistoryFilter.Tag -> mangaTagQueries.countAll().executeAsOne()
        }
    }

    override suspend fun getCombinedHistoryFilterCounts(): List<ReadHistoryRepository.HistoryFilterCount> = databaseIo {
        val forumCounts = queries.getForumCounts().executeAsList().mapNotNull { row ->
            val forumId = row.forumId
            ReadHistoryRepository.HistoryFilterCount(
                filter = ReadHistoryRepository.HistoryFilter.Forum(ForumId(forumId.toInt())),
                label = row.forumName?.takeIf { it.isNotBlank() } ?: "Forum ${forumId}",
                count = row.count,
            )
        }
        val tagCount = mangaTagQueries.countAll().executeAsOne()
        buildList {
            add(
                ReadHistoryRepository.HistoryFilterCount(
                    filter = ReadHistoryRepository.HistoryFilter.All,
                    label = i18n("全部"),
                    count = queries.countAll().executeAsOne() + mangaTagQueries.countAll().executeAsOne(),
                )
            )
            addAll(forumCounts)
            if (tagCount > 0L) {
                add(
                    ReadHistoryRepository.HistoryFilterCount(
                        filter = ReadHistoryRepository.HistoryFilter.Tag,
                        label = "Tag",
                        count = tagCount,
                    )
                )
            }
        }
    }

    override suspend fun searchCombinedHistory(
        query: String,
        page: Int,
        pageSize: Int
    ): List<ReadHistoryRepository.AnyReadingHistory> = databaseIo {
        val offset = (page - 1).coerceAtLeast(0) * pageSize
        val threads = loadAllThreadHistory(query)
        val tags = loadAllTagHistory(query)
        (threads + tags)
            .sortedByDescending { it.lastVisitTime }
            .drop(offset)
            .take(pageSize)
    }

    override suspend fun searchCombinedHistoryCount(query: String): Long = databaseIo {
        queries.countByName(query).executeAsOne() + mangaTagQueries.countByName(query, query).executeAsOne()
    }

    override suspend fun deleteCombinedHistoryBatch(items: List<ReadHistoryRepository.AnyReadingHistory>) = databaseIo {
        val tagIds = items.filterIsInstance<ReadHistoryRepository.TagMangaReadingHistory>().map { it.tagId.value.toLong() }

        items.filterIsInstance<ReadHistoryRepository.ThreadReadingHistory>().forEach { item ->
            queries.deleteByThreadKey(
                threadId = item.threadId.value.toLong(),
                threadType = item.threadType.name,
                authorId = item.authorId?.value?.toLong() ?: 0L
            )
        }
        if (tagIds.isNotEmpty()) {
            mangaTagQueries.deleteByTagIds(tagIds)
        }
    }

    override suspend fun deleteAllCombinedHistory() = databaseIo {
        queries.deleteAll()
        mangaTagQueries.deleteAll()
        Unit
    }

    override suspend fun recordReadingDuration(dateKey: String, durationMillis: Long) = databaseIo {
        if (durationMillis <= 0L) return@databaseIo
        readingTimeQueries.addDuration(dateKey, durationMillis, currentTimeMillis())
    }

    override suspend fun getReadingDurationDays(
        startDateKey: String,
        endDateKey: String,
    ): List<ReadHistoryRepository.ReadingDurationDay> = databaseIo {
        readingTimeQueries.getRange(startDateKey, endDateKey)
            .executeAsList()
            .map {
                ReadHistoryRepository.ReadingDurationDay(
                    dateKey = it.dateKey,
                    durationMillis = it.durationMillis,
                )
            }
    }

    override suspend fun getReadingDurationTotal(startDateKey: String, endDateKey: String): Long = databaseIo {
        readingTimeQueries.getTotalDuration(startDateKey, endDateKey).executeAsOne()
    }

    private fun loadAllThreadHistory(query: String? = null): List<ReadHistoryRepository.ThreadReadingHistory> {
        val histories = mutableListOf<ReadHistoryRepository.ThreadReadingHistory>()
        var offset = 0L

        while (true) {
            val batch = if (query.isNullOrBlank()) {
                queries.getPage(COMBINED_HISTORY_BATCH_SIZE, offset).executeAsList()
            } else {
                queries.searchByName(query, COMBINED_HISTORY_BATCH_SIZE, offset).executeAsList()
            }

            if (batch.isEmpty()) break
            histories += batch.map { it.toHistory() }
            if (batch.size < COMBINED_HISTORY_BATCH_SIZE.toInt()) break
            offset += batch.size
        }

        return histories
    }

    private fun loadAllTagHistory(query: String? = null): List<ReadHistoryRepository.TagMangaReadingHistory> {
        val histories = mutableListOf<ReadHistoryRepository.TagMangaReadingHistory>()
        var offset = 0L

        while (true) {
            val batch = if (query.isNullOrBlank()) {
                mangaTagQueries.getPage(COMBINED_HISTORY_BATCH_SIZE, offset).executeAsList()
            } else {
                mangaTagQueries.searchByName(query, query, COMBINED_HISTORY_BATCH_SIZE, offset).executeAsList()
            }

            if (batch.isEmpty()) break
            histories += batch.map { it.toHistory() }
            if (batch.size < COMBINED_HISTORY_BATCH_SIZE.toInt()) break
            offset += batch.size
        }

        return histories
    }
}
