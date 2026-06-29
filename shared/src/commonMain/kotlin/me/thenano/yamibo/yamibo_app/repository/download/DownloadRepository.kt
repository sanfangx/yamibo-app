package me.thenano.yamibo.yamibo_app.repository.download

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    val queue: StateFlow<List<DownloadQueueEntry>>

    suspend fun isStorageReady(): Boolean
    suspend fun getSummary(): DownloadQueueSummary
    suspend fun getStatus(key: ThreadPageDownloadKey): DownloadStatus
    suspend fun getDownloadedPage(key: ThreadPageDownloadKey): ThreadPage?
    suspend fun getManifest(key: ThreadPageDownloadKey): ThreadPageDownloadManifest?
    suspend fun enqueuePage(tid: ThreadId, title: String, authorId: UserId?, page: Int): Result<Unit>
    suspend fun enqueueThread(tid: ThreadId, title: String, authorId: UserId?): Result<Unit>
    suspend fun enqueueThreadExceptLastPage(tid: ThreadId, title: String, authorId: UserId?): Result<Unit>
    suspend fun refreshPage(tid: ThreadId, title: String, authorId: UserId?, page: Int): YamiboResult<ThreadPage>
    suspend fun markThreadUpdateAvailable(tid: ThreadId, authorId: UserId? = null)
    suspend fun clearPage(key: ThreadPageDownloadKey): Result<Unit>
    suspend fun clearThread(key: ThreadPageDownloadKey): Result<Unit>
    suspend fun retry(key: ThreadPageDownloadKey): Result<Unit>
    fun pauseAll()
    fun resumeAll()
}
