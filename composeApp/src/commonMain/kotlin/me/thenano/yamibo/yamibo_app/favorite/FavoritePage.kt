package me.thenano.yamibo.yamibo_app.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun FavoritePage() {
    val colors = YamiboTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(colors.creamBackground),
        contentAlignment = Alignment.Center
    ) { Text("收藏功能開發中", color = colors.brownDeep) }
}
