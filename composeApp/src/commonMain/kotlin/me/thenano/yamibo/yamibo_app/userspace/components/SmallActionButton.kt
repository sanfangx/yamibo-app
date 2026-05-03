package me.thenano.yamibo.yamibo_app.userspace.components

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.components.YamiboSmallActionButton
import me.thenano.yamibo.yamibo_app.components.YamiboSmallBadge

@Composable
internal fun SmallActionButton(text: String, onClick: () -> Unit) {
    YamiboSmallActionButton(text = text, onClick = onClick)
}

@Composable
internal fun SmallBadge(text: String) {
    YamiboSmallBadge(text = text)
}
