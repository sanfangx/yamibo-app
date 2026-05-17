package me.thenano.yamibo.yamibo_app.profile.settings.bound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.LocalNovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.components.rememberConvertedText
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsChipRow
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsSlider
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state

import kotlin.math.roundToInt

private const val PREVIEW_TEXT = "我是YamiboApp的作者TheNano，這是一個第三方個人獨立開發的開源App"

@Composable
fun NovelReaderPreviewSetting() {
    val colors = YamiboTheme.colors
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val fontSize = novelSettingsRepo.fontSize.state()
    val lineSpacing = novelSettingsRepo.lineSpacing.state()
    val contentWidthFraction = novelSettingsRepo.contentWidthFraction.state()
    val previewText = rememberConvertedText(PREVIEW_TEXT)

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
                text = previewText,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * lineSpacing).sp,
                color = colors.textDark
            )
        }
    }
}

@Composable
fun NovelFontSizeSetting() {
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val fontSize = novelSettingsRepo.fontSize.state()

    SettingsSlider(
        label = "文字大小",
        value = fontSize.toFloat(),
        valueRange = 10f..40f,
        steps = 29,
        valueDisplay = { "${it.toInt()} sp" },
        onValueChange = { novelSettingsRepo.fontSize.setValue(it.toInt()) }
    )
}

@Composable
fun NovelLineSpacingSetting() {
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val lineSpacing = novelSettingsRepo.lineSpacing.state()

    SettingsSlider(
        label = "行距比例",
        value = lineSpacing,
        valueRange = 1.0f..3.0f,
        steps = 39,
        valueDisplay = { "${(it * 100f).roundToInt() / 100f}x" },
        onValueChange = { novelSettingsRepo.lineSpacing.setValue(it) }
    )
}

@Composable
fun NovelContentWidthSetting() {
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val contentWidthFraction = novelSettingsRepo.contentWidthFraction.state()

    SettingsSlider(
        label = "內容寬度",
        value = contentWidthFraction,
        valueRange = 0.6f..1.0f,
        steps = 39,
        valueDisplay = { "${(it * 100f).roundToInt()}%" },
        onValueChange = { novelSettingsRepo.contentWidthFraction.setValue(it) }
    )
}

@Composable
fun NovelSystemBarsBackgroundSetting() {
    val colors = YamiboTheme.colors
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val enabled = novelSettingsRepo.keepSystemBarsBackground.state()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { novelSettingsRepo.keepSystemBarsBackground.setValue(!enabled) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "保留系統列背景",
                fontSize = 16.sp,
                color = colors.textDark,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "在狀態列與底部導覽列後方使用閱讀背景，避免透明系統列蓋住內容。",
                fontSize = 13.sp,
                color = colors.textDark.copy(alpha = 0.6f),
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { novelSettingsRepo.keepSystemBarsBackground.setValue(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.brownDeep,
                checkedTrackColor = colors.brownPrimary.copy(alpha = 0.5f),
                uncheckedThumbColor = colors.textDark.copy(alpha = 0.5f),
                uncheckedTrackColor = colors.brownLight.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
fun NovelChineseConversionSetting() {
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val chineseConversion = novelSettingsRepo.chineseConversion.state()

    SettingsChipRow(
        options = NovelReaderSettingsRepository.chineseConversionOptions,
        selectedValue = chineseConversion,
        onSelect = { novelSettingsRepo.chineseConversion.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}
