package me.thenano.yamibo.yamibo_app.repository.settings

import me.thenano.yamibo.yamibo_app.repository.settings.core.SettingsRegistry
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

enum class ReaderChineseConversionOption(val label: String) {
    DEFAULT("default"),
    SIMPLIFIED("simplified"),
    TRADITIONAL("traditional"),
}

class NovelReaderSettingsRepository(store: SettingsStore) : SettingsRegistry(store, prefix = "novelreadersettings") {

    val fontSize by intSetting(
        name = "font_size",
        description = "novel_reader_font_size",
        default = 16,
        min = 10,
        max = 40,
        interval = 1,
    )

    val lineSpacing by floatSetting(
        name = "line_spacing",
        description = "novel_reader_line_spacing",
        default = 1.5f,
        min = 1.0f,
        max = 3.0f,
        interval = 0.05f,
    )

    val contentWidthFraction by floatSetting(
        name = "content_width",
        description = "novel_reader_content_width",
        default = 1.0f,
        min = 0.6f,
        max = 1.0f,
        interval = 0.01f,
    )

    val keepSystemBarsBackground by boolSetting(
        name = "keep_system_bars_background",
        description = "novel_reader_keep_system_bars_background",
        default = true,
    )

    val chineseConversion by enumSetting(
        name = "chinese_conversion",
        description = "novel_reader_chinese_conversion",
        default = ReaderChineseConversionOption.DEFAULT,
    )

    companion object {
        val chineseConversionOptions = ReaderChineseConversionOption.entries.map { it to it.label }
    }
}

