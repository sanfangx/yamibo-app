package me.thenano.yamibo.yamibo_app.thread.reader.components.novel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.profile.settings.components.ThemeSelectorContent
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsSlider
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state
import kotlin.math.roundToInt

@Composable
fun NovelReaderSettingsPanel(
    visible: Boolean,
    novelSettingsRepo: NovelReaderSettingsRepository,
    appSettingsRepo: AppSettingsRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors
    val fontSize = novelSettingsRepo.fontSize.state()
    val lineSpacing = novelSettingsRepo.lineSpacing.state()
    val themeMode = appSettingsRepo.themeMode.state()
    val themeScheme = appSettingsRepo.themeScheme.state()

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            color = colors.brownDeep,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Typography Section
                SectionTitle("文字排版")
                
                // Font Size
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("字體大小", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        IconButton(onClick = { 
                            novelSettingsRepo.fontSize.setValue((fontSize - 1).coerceAtLeast(10))
                        }) {
                            Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "$fontSize",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(onClick = { 
                            novelSettingsRepo.fontSize.setValue((fontSize + 1).coerceAtMost(40))
                        }) {
                            Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Line Spacing
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("行距", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Box(modifier = Modifier.fillMaxWidth(0.6f)) {
                        SettingsSlider(
                            label = "",
                            value = lineSpacing,
                            valueRange = 1.0f..3.0f,
                            steps = 39,
                            valueDisplay = { "${(it * 100f).roundToInt() / 100f}x" },
                            onValueChange = { novelSettingsRepo.lineSpacing.setValue(it) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Theme Section
                ThemeSelectorContent(
                    currentMode = themeMode,
                    currentSchemeName = themeScheme,
                    onModeChange = { appSettingsRepo.themeMode.setValue(it) },
                    onSchemeChange = { appSettingsRepo.themeScheme.setValue(it) }
                )
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
