package me.thenano.yamibo.yamibo_app.profile.sign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import io.github.littlesurvival.YamiboRoute
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.LocalSignRepository
import me.thenano.yamibo.yamibo_app.profile.LoadingOverlay
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.webview.WebViewTopBar

@Composable
expect fun SignPlatformWebView(
    url: String,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onBack: (() -> Unit) -> Unit,
    onForward: (() -> Unit) -> Unit,
    onReload: (() -> Unit) -> Unit,
    onHtmlAvailable: (url: String, html: String) -> Unit,
)

internal class ISignWebView(
    private val semiAutomatic: Boolean,
    private val onSemiAutoReady: () -> Unit = {},
    private val onResultObserved: () -> Unit = {},
    private val onMaintenanceObserved: () -> Unit = {},
) : Navigatable {
    override val id = buildId("sign-webview", semiAutomatic)

    @Composable
    override fun Content() {
        SignWebViewScreen(
            semiAutomatic = semiAutomatic,
            onSemiAutoReady = onSemiAutoReady,
            onResultObserved = onResultObserved,
            onMaintenanceObserved = onMaintenanceObserved,
        )
    }
}

@Composable
private fun SignWebViewScreen(
    semiAutomatic: Boolean,
    onSemiAutoReady: () -> Unit,
    onResultObserved: () -> Unit,
    onMaintenanceObserved: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val signRepository = LocalSignRepository.current
    val colors = YamiboTheme.colors

    var currentTitle by remember { mutableStateOf("每日簽到") }
    var currentUrl by remember { mutableStateOf(YamiboRoute.Sign.build()) }
    var loading by remember { mutableStateOf(true) }
    var backFunc by remember { mutableStateOf<(() -> Unit)?>(null) }
    var forwardFunc by remember { mutableStateOf<(() -> Unit)?>(null) }
    var reloadFunc by remember { mutableStateOf<(() -> Unit)?>(null) }
    var handledSignPage by remember(semiAutomatic) { mutableStateOf(false) }
    var handledMaintenancePage by remember(semiAutomatic) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.brownDeep)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            WebViewTopBar(
                title = currentTitle,
                url = currentUrl,
                onCloseClick = { navigator.pop() },
                onBackClick = { backFunc?.invoke() },
                onForwardClick = { forwardFunc?.invoke() },
                onRefreshClick = { reloadFunc?.invoke() },
                onOpenBrowserClick = {
                    try {
                        uriHandler.openUri(currentUrl)
                    } catch (_: Exception) {
                    }
                },
                useBackIcon = true,
            )
            Box(modifier = Modifier.weight(1f)) {
                SignPlatformWebView(
                    url = YamiboRoute.Sign.build(),
                    onTitleChanged = { currentTitle = it },
                    onUrlChanged = { currentUrl = it },
                    onLoadingChanged = { loading = it },
                    onBack = { backFunc = it },
                    onForward = { forwardFunc = it },
                    onReload = { reloadFunc = it },
                    onHtmlAvailable = { _, html ->
                        signRepository.cacheObservedHtml(html)
                        if (semiAutomatic && !handledMaintenancePage && isMaintenancePageHtml(html)) {
                            handledMaintenancePage = true
                            onMaintenanceObserved()
                            navigator.pop()
                            return@SignPlatformWebView
                        }
                        if (!handledSignPage && isSignPageHtml(html)) {
                            handledSignPage = true
                            if (semiAutomatic) {
                                onSemiAutoReady()
                                navigator.pop()
                            }
                        }
                        if (isSignResultPageHtml(html)) {
                            onResultObserved()
                        }
                    },
                )
                LoadingOverlay(visible = loading)
            }
        }
    }
}
