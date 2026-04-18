package me.thenano.yamibo.yamibo_app.profile.sign

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

private const val SignMobileUserAgent =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"

private class SignNavigationDelegate(
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onUrlChanged: (String) -> Unit,
    private val onTitleChanged: (String) -> Unit,
    private val onHtmlAvailable: (String, String) -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        onLoadingChanged(true)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onLoadingChanged(false)
        val currentUrl = webView.URL?.absoluteString ?: return
        onUrlChanged(currentUrl)
        webView.title?.let(onTitleChanged)
        webView.evaluateJavaScript("document.documentElement.outerHTML") { result, _ ->
            val html = result as? String ?: return@evaluateJavaScript
            onHtmlAvailable(currentUrl, html)
        }
    }

    override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: platform.Foundation.NSError,
    ) {
        onLoadingChanged(false)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun SignPlatformWebView(
    url: String,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onBack: (() -> Unit) -> Unit,
    onForward: (() -> Unit) -> Unit,
    onReload: (() -> Unit) -> Unit,
    onHtmlAvailable: (url: String, html: String) -> Unit,
) {
    val navigator = LocalNavigator.current
    var webViewInstance by remember { mutableStateOf<WKWebView?>(null) }
    val delegate = remember {
        SignNavigationDelegate(
            onLoadingChanged = onLoadingChanged,
            onUrlChanged = onUrlChanged,
            onTitleChanged = onTitleChanged,
            onHtmlAvailable = onHtmlAvailable,
        )
    }

    LaunchedEffect(webViewInstance) {
        onBack { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() }
        onForward { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() }
        onReload { webViewInstance?.reload() }
    }

    DisposableEffect(webViewInstance) {
        val handler: () -> Boolean = {
            if (webViewInstance?.canGoBack() == true) {
                webViewInstance?.goBack()
                true
            } else {
                false
            }
        }
        navigator.backHandlers.add(handler)
        onDispose { navigator.backHandlers.remove(handler) }
    }

    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            WKWebView().apply {
                webViewInstance = this
                navigationDelegate = delegate
                customUserAgent = SignMobileUserAgent
                loadRequest(NSURLRequest(NSURL(string = url)))
            }
        },
        update = { webViewInstance = it },
    )
}
