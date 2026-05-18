package me.thenano.yamibo.yamibo_app.i18n

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.repository.settings.AppThemeMode
import me.thenano.yamibo.yamibo_app.repository.settings.AppThemeScheme
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteGridMode
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteSortMode
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteUpdateInterval
import me.thenano.yamibo.yamibo_app.repository.settings.ReaderChineseConversionOption
import me.thenano.yamibo.yamibo_app.repository.settings.ReadingMode
import me.thenano.yamibo.yamibo_app.repository.settings.SignInMode
import me.thenano.yamibo.yamibo_app.repository.settings.TouchZoneLayout
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

fun AppThemeMode.localizedLabel(): String = when (this) {
    AppThemeMode.SYSTEM -> appString(Res.string.settings_theme_mode_system)
    AppThemeMode.LIGHT -> appString(Res.string.settings_theme_mode_light)
    AppThemeMode.DARK -> appString(Res.string.settings_theme_mode_dark)
}

fun AppThemeScheme.localizedLabel(): String = when (this) {
    AppThemeScheme.DEFAULT -> appString(Res.string.settings_theme_scheme_default)
    AppThemeScheme.DEFAULT_DARK -> appString(Res.string.settings_theme_scheme_default_dark)
    AppThemeScheme.CLASSIC_BLACK -> appString(Res.string.settings_theme_scheme_classic_black)
    AppThemeScheme.CLASSIC_WHITE -> appString(Res.string.settings_theme_scheme_classic_white)
    else -> label
}

fun FavoriteGridMode.localizedLabel(): String = when (this) {
    FavoriteGridMode.FIXED_GRID -> appString(Res.string.favorite_grid_fixed)
    FavoriteGridMode.STAGGERED -> appString(Res.string.favorite_grid_staggered)
    FavoriteGridMode.ROW_CARD -> appString(Res.string.favorite_grid_row_card)
    FavoriteGridMode.ROW_CARD_TEXT -> appString(Res.string.favorite_grid_row_card_text)
}

fun FavoriteSortMode.localizedLabel(): String = when (this) {
    FavoriteSortMode.DEFAULT -> appString(Res.string.favorite_sort_default)
    FavoriteSortMode.UPDATED_AT -> appString(Res.string.favorite_sort_updated_at)
    FavoriteSortMode.FAVORITED_ORDER -> appString(Res.string.favorite_sort_favorited_order)
    FavoriteSortMode.NAME -> appString(Res.string.favorite_sort_name)
    FavoriteSortMode.FORUM_NAME -> appString(Res.string.favorite_sort_forum_name)
    FavoriteSortMode.LAST_READ -> appString(Res.string.favorite_sort_last_read)
}

fun FavoriteUpdateInterval.localizedLabel(): String = when (this) {
    FavoriteUpdateInterval.MANUAL -> appString(Res.string.favorite_update_interval_manual)
    FavoriteUpdateInterval.HOURS_6 -> appString(Res.string.favorite_update_interval_6h)
    FavoriteUpdateInterval.HOURS_12 -> appString(Res.string.favorite_update_interval_12h)
    FavoriteUpdateInterval.HOURS_24 -> appString(Res.string.favorite_update_interval_24h)
    FavoriteUpdateInterval.DAYS_3 -> appString(Res.string.favorite_update_interval_3d)
    FavoriteUpdateInterval.DAYS_7 -> appString(Res.string.favorite_update_interval_7d)
    FavoriteUpdateInterval.SMART -> appString(Res.string.favorite_update_interval_smart)
}

fun SignInMode.localizedLabel(): String = when (this) {
    SignInMode.SEMI_AUTOMATIC -> appString(Res.string.sign_mode_semi_automatic)
    SignInMode.FULL_MANUAL -> appString(Res.string.sign_mode_full_manual)
}

fun ReadingMode.localizedLabel(): String = when (this) {
    ReadingMode.SINGLE_LTR -> appString(Res.string.manga_reading_mode_single_ltr)
    ReadingMode.SINGLE_RTL -> appString(Res.string.manga_reading_mode_single_rtl)
    ReadingMode.SINGLE_TTB -> appString(Res.string.manga_reading_mode_single_ttb)
    ReadingMode.SCROLL_CONTINUOUS -> appString(Res.string.manga_reading_mode_scroll_continuous)
    ReadingMode.SCROLL_GAP -> appString(Res.string.manga_reading_mode_scroll_gap)
}

fun TouchZoneLayout.localizedLabel(): String = when (this) {
    TouchZoneLayout.L_SHAPE -> appString(Res.string.manga_touch_zone_l_shape)
    TouchZoneLayout.KINDLE -> appString(Res.string.manga_touch_zone_kindle)
    TouchZoneLayout.EDGE -> appString(Res.string.manga_touch_zone_edge)
    TouchZoneLayout.LEFT_RIGHT -> appString(Res.string.manga_touch_zone_left_right)
    TouchZoneLayout.DISABLED -> appString(Res.string.common_disabled)
}

fun ReaderChineseConversionOption.localizedLabel(): String = when (this) {
    ReaderChineseConversionOption.DEFAULT -> appString(Res.string.chinese_conversion_default)
    ReaderChineseConversionOption.SIMPLIFIED -> appString(Res.string.chinese_conversion_simplified)
    ReaderChineseConversionOption.TRADITIONAL -> appString(Res.string.chinese_conversion_traditional)
}
