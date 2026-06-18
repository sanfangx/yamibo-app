package me.thenano.yamibo.yamibo_app.updates

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.message.MessageCenterScreen
import me.thenano.yamibo.yamibo_app.message.MessageCenterTab

@Composable
fun UpdatesPage() {
    MessageCenterScreen(
        initialTab = MessageCenterTab.Updates,
        mainTabTopBar = true,
        updatesOnly = true,
    )
}
