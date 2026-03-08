package me.thenano.yamibo.yamibo_app.thread.novel.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Top bar */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThreadTopBar(
    title: String, 
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = YamiboTheme.colors
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) { Text("◀", color = Color.White, fontSize = 20.sp) }
        },
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colors.brownDeep,
                scrolledContainerColor = colors.brownDeep
            )
    )
}
