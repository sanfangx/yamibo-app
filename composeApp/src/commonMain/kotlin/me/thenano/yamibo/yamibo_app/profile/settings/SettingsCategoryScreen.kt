package me.thenano.yamibo.yamibo_app.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalMangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalNovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsChipRow
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsSlider
import me.thenano.yamibo.yamibo_app.profile.settings.components.ThemeSelectorContent
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state
import kotlin.math.roundToInt

private const val PREVIEW_TEXT = "我是YamiboApp的作者TheNano，這是一個第三方個人獨立開發的開源App"

/** 根據 category 顯示對應的設定內容 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsCategoryScreen(category: String) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current

    val title = when (category) {
        "appearance" -> "外觀"
        "novel_reader" -> "小說閱讀器"
        "manga_reader" -> "漫畫閱讀器"
        else -> "設定"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Text("◀", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.brownDeep,
                    scrolledContainerColor = colors.brownDeep
                )
            )
        },
        containerColor = colors.creamBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            when (category) {
                "appearance" -> AppearanceContent()
                "novel_reader" -> NovelReaderContent()
                "manga_reader" -> MangaReaderContent()
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = YamiboTheme.colors
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textDark.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

// ─── Appearance ───

@Composable
private fun AppearanceContent() {
    val appSettingsRepo = LocalAppSettingsRepository.current
    val themeMode = appSettingsRepo.themeMode.state()
    val themeScheme = appSettingsRepo.themeScheme.state()

    ThemeSelectorContent(
        currentMode = themeMode,
        currentSchemeName = themeScheme,
        onModeChange = { appSettingsRepo.themeMode.setValue(it) },
        onSchemeChange = { appSettingsRepo.themeScheme.setValue(it) }
    )
}

// ─── Novel Reader ───

@Composable
private fun NovelReaderContent() {
    val colors = YamiboTheme.colors
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val fontSize = novelSettingsRepo.fontSize.state()
    val lineSpacing = novelSettingsRepo.lineSpacing.state()
    val contentWidthFraction = novelSettingsRepo.contentWidthFraction.state()

    // Preview Area
    SectionLabel("預覽")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.creamSurface)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(contentWidthFraction)
        ) {
            Text(
                text = PREVIEW_TEXT,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * lineSpacing).sp,
                color = colors.textDark
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    // Font Size
    SectionLabel("字體大小")
    SettingsSlider(
        label = "文字大小",
        value = fontSize.toFloat(),
        valueRange = 10f..40f,
        steps = 29,
        valueDisplay = { "${it.toInt()} sp" },
        onValueChange = { novelSettingsRepo.fontSize.setValue(it.toInt()) }
    )

    Spacer(Modifier.height(24.dp))

    // Line Spacing
    SectionLabel("行距")
    SettingsSlider(
        label = "行距比例",
        value = lineSpacing,
        valueRange = 1.0f..3.0f,
        steps = 39,
        valueDisplay = { "${(it * 100f).roundToInt() / 100f}x" },
        onValueChange = { novelSettingsRepo.lineSpacing.setValue(it) }
    )

    Spacer(Modifier.height(24.dp))

    // Content Width Fraction
    SectionLabel("頁寬")
    SettingsSlider(
        label = "內容寬度",
        value = contentWidthFraction,
        valueRange = 0.6f..1.0f,
        steps = 39,
        valueDisplay = { "${(it * 100f).roundToInt()}%" },
        onValueChange = { novelSettingsRepo.contentWidthFraction.setValue(it) }
    )
}

// ─── Manga Reader ───

@Composable
private fun MangaReaderContent() {
    val mangaSettingsRepo = LocalMangaReaderSettingsRepository.current
    val readingMode = mangaSettingsRepo.readingMode.state()
    val touchZone = mangaSettingsRepo.touchZone.state()

    SectionLabel("閱讀模式")
    SettingsChipRow(
        options = MangaReaderSettingsRepository.readingModeOptions,
        selectedValue = readingMode,
        onSelect = { mangaSettingsRepo.readingMode.setValue(it) }
    )

    Spacer(Modifier.height(24.dp))

    SectionLabel("輕觸區域")
    SettingsChipRow(
        options = MangaReaderSettingsRepository.touchZoneOptions,
        selectedValue = touchZone,
        onSelect = { mangaSettingsRepo.touchZone.setValue(it) }
    )
}
