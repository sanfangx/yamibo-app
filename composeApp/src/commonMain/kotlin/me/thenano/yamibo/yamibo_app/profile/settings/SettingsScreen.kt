package me.thenano.yamibo.yamibo_app.profile.settings

import YamiboIcons
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.access.IBackgroundAccessSetupScreen
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsItem
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import org.jetbrains.compose.resources.stringResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen() {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Text(YamiboIcons.Back, color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.brownDeep,
                    scrolledContainerColor = colors.brownDeep,
                ),
            )
        },
        containerColor = colors.creamBackground,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsItem(
                icon = YamiboIcons.Views,
                title = stringResource(Res.string.settings_appearance_title),
                subtitle = stringResource(Res.string.settings_appearance_subtitle),
                onClick = { navigator.navigate(ISettingsCategoryScreen("appearance")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Language,
                title = stringResource(Res.string.settings_language_title),
                subtitle = stringResource(Res.string.settings_language_subtitle),
                onClick = { navigator.navigate(ISettingsCategoryScreen("language")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Book,
                title = stringResource(Res.string.settings_novel_reader_title),
                subtitle = stringResource(Res.string.settings_novel_reader_subtitle),
                onClick = { navigator.navigate(ISettingsCategoryScreen("novel_reader")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Book,
                title = stringResource(Res.string.settings_manga_reader_title),
                subtitle = stringResource(Res.string.settings_manga_reader_subtitle),
                onClick = { navigator.navigate(ISettingsCategoryScreen("manga_reader")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Explore,
                title = stringResource(Res.string.settings_favorite_title),
                subtitle = stringResource(Res.string.settings_favorite_subtitle),
                onClick = { navigator.navigate(ISettingsCategoryScreen("favorite")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Sync,
                title = stringResource(Res.string.settings_background_title),
                subtitle = stringResource(Res.string.settings_background_subtitle),
                onClick = { navigator.navigate(IBackgroundAccessSetupScreen()) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Storage,
                title = stringResource(Res.string.settings_storage_title),
                subtitle = stringResource(Res.string.settings_storage_subtitle),
                onClick = { navigator.navigate(ISettingsCategoryScreen("storage")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.EditOrSign,
                title = stringResource(Res.string.settings_sign_title),
                subtitle = stringResource(Res.string.settings_sign_subtitle),
                onClick = { navigator.navigate(ISettingsCategoryScreen("sign")) },
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    val colors = YamiboTheme.colors
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = colors.brownLight.copy(alpha = 0.15f),
    )
}
