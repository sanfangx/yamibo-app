package me.thenano.yamibo.yamibo_app.webview

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import androidx.compose.ui.zIndex

@Composable
fun WebViewTopBar(
    title: String,
    onBackClick: () -> Unit = { },
    onForwardClick: () -> Unit = { },
    onRefreshClick: () -> Unit = { },
) {
    val colors = YamiboTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .zIndex(1f),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.creamSurface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◀",
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { onBackClick() },
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.brownPrimary
                )
                
                Text(
                    text = "▶",
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { onForwardClick() },
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.brownPrimary
                )

                Spacer(Modifier.width(8.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.weight(1f).basicMarquee(
                        initialDelayMillis = 1500
                    ),
                    color = colors.textDark
                )

                Text(
                    text = "⟳",
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { onRefreshClick() },
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.brownPrimary
                )
            }
        }
    }
}
