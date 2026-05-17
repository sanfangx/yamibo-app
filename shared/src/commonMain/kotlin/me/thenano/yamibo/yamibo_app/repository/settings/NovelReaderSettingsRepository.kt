package me.thenano.yamibo.yamibo_app.repository.settings

import me.thenano.yamibo.yamibo_app.repository.settings.core.SettingsRegistry
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

enum class ReaderChineseConversionOption(val label: String) {
    DEFAULT("默認"),
    SIMPLIFIED("簡體"),
    TRADITIONAL("繁體"),
}

class NovelReaderSettingsRepository(store: SettingsStore) : SettingsRegistry(store, prefix = "novelreadersettings") {

    val fontSize by intSetting(
        name = "字體大小",
        description = "調整小說閱讀頁的字體大小",
        default = 16,
        min = 10,
        max = 40,
        interval = 1,
    )

    val lineSpacing by floatSetting(
        name = "行距",
        description = "調整小說閱讀頁的行距倍率",
        default = 1.5f,
        min = 1.0f,
        max = 3.0f,
        interval = 0.05f,
    )

    val contentWidthFraction by floatSetting(
        name = "內容寬度",
        description = "調整小說閱讀頁的內容寬度比例",
        default = 1.0f,
        min = 0.6f,
        max = 1.0f,
        interval = 0.01f,
    )

    val keepSystemBarsBackground by boolSetting(
        name = "保留系統欄背景",
        description = "閱讀時讓狀態列與導覽列使用閱讀背景色",
        default = true,
    )

    val chineseConversion by enumSetting(
        name = "簡繁轉換",
        description = "閱讀文字的簡繁轉換方式",
        default = ReaderChineseConversionOption.DEFAULT,
    )

    companion object {
        val chineseConversionOptions = ReaderChineseConversionOption.entries.map { it to it.label }
    }
}
