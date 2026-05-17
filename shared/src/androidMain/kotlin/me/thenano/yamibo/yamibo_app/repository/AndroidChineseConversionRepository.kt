package me.thenano.yamibo.yamibo_app.repository

import com.github.houbb.opencc4j.util.ZhConverterUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.repository.chineseconversion.ChineseConversionMode

class AndroidChineseConversionRepository : ChineseConversionRepository {
    private val modeFlow = MutableStateFlow<ChineseConversionMode?>(null)
    override val currentMode: StateFlow<ChineseConversionMode?> = modeFlow.asStateFlow()

    override fun setConversionMode(mode: ChineseConversionMode?) {
        modeFlow.value = mode
    }

    override suspend fun convert(text: String): String = withContext(Dispatchers.Default) {
        when (modeFlow.value) {
            null -> text
            ChineseConversionMode.Simplified -> ZhConverterUtil.toSimple(text)
            ChineseConversionMode.Traditional -> ZhConverterUtil.toTraditional(text)
        }
    }

    override fun isModeAvailable(mode: ChineseConversionMode): Boolean = true
}
