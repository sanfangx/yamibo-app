package me.thenano.yamibo.yamibo_app.repository.settings

import me.thenano.yamibo.yamibo_app.repository.settings.core.SettingsRegistry
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

class NovelReaderSettingsRepository(store: SettingsStore) : SettingsRegistry(store, prefix = "novelreadersettings") {

    val fontSize by intSetting(
        name = "文字大小",
        description = "閱讀器主要的字體大小",
        default = 16,
        min = 10,
        max = 40,
        interval = 1
    )

    val lineSpacing by floatSetting(
        name = "行距",
        description = "閱讀器整體的行距比例",
        default = 1.5f,
        min = 1.0f,
        max = 3.0f,
        interval = 0.05f
    )

    val contentWidthFraction by floatSetting(
        name = "內容寬度",
        description = "內文佔螢幕的寬度比例",
        default = 1.0f,
        min = 0.6f,
        max = 1.0f,
        interval = 0.01f
    )

    val keepSystemBarsBackground by boolSetting(
        name = "保留系統列背景",
        description = "在閱讀器頂部狀態列與底部導覽列後方繪製純色背景，避免透明系統列覆蓋閱讀內容。",
        default = true,
    )
}
