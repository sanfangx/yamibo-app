package me.thenano.yamibo.yamibo_app.webview

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

@Composable
expect fun PlatformWebView(url: String)

class IPlatformWebView(val link: String = "https://bbs.yamibo.com/") : Navigatable {
    override val id: Any = "WebView_$link"

    @Composable
    override fun Content() {
        PlatformWebView(link)
    }
}