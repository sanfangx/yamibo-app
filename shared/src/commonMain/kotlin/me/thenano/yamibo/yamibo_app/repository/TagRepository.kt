package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.Tags
import io.github.littlesurvival.dto.page.TagPage
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId

interface TagRepository {
    data class TagCacheKey(val tagId: Int, val page: Int) {
        fun toCacheKey(): String = "${tagId}_${page}"

        companion object {
            fun keyPrefix(tagId: Int): String = "${tagId}_"
        }
    }

    suspend fun fetchTagPage(tagId: TagId, page: Int = 1): YamiboResult<TagPage>
    suspend fun fetchExtractTags(tid: ThreadId): YamiboResult<Tags>

    suspend fun getCachedTagPage(tagId: TagId, page: Int = 1): TagPage?
    suspend fun setCachedTagPage(tagId: TagId, page: Int, tagPage: TagPage)
    suspend fun clearCachedTagPage(tagId: TagId)
}
