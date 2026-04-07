package me.thenano.yamibo.yamibo_app.repository.settings

import me.thenano.yamibo.yamibo_app.repository.settings.core.SettingsRegistry
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

class AppSettingsRepository(store: SettingsStore) : SettingsRegistry(store, prefix = "appsettings") {

    val themeMode by stringSetting(
        name = "顏色主題",
        default = "SYSTEM"
    )

    val themeScheme by stringSetting(
        name = "配色風格",
        default = "百合會"
    )

    val isMangaMode by boolSetting(
        name = "漫畫模式",
        default = false
    )
}
