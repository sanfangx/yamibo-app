package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.store.auth.CookieStore

class IOSThreadRepository(
        private val cookieStore: CookieStore,
        private val yamiboClient: YamiboClient
) : ThreadRepository {

    /** thread page cache — keyed by tid, stores the last fetched page */
    private val cachedThreadPages = mutableMapOf<Int, ThreadPage>()

    override suspend fun fetchThread(
            tid: ThreadId,
            authorId: UserId?,
            page: Int
    ): YamiboResult<ThreadPage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        val result = yamiboClient.fetchThreadById(tid, authorId, page)

        if (result is YamiboResult.Success) {
            cachedThreadPages[tid.value] = result.value
        }
        return result
    }

    override suspend fun addFavorite(tid: ThreadId, formHash: FormHash): YamiboResult<String> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchAddFavorite(tid, formHash)
    }

    override fun getCachedThread(tid: ThreadId): ThreadPage? = cachedThreadPages[tid.value]
}
