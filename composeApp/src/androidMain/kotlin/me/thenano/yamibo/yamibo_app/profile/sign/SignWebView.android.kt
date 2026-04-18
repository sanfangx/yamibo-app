package me.thenano.yamibo.yamibo_app.profile.sign

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import org.json.JSONArray

private const val SignMobileUserAgent =
    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
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
    val authRepository = LocalAuthRepository.current
    val cookies = authRepository.cookieStore.load() ?: ""
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

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

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewInstance = this
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                        onLoadingChanged(true)
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        onLoadingChanged(false)
                        val currentUrl = pageUrl ?: return
                        onUrlChanged(currentUrl)
                        evaluateJavascript("(function(){return document.documentElement.outerHTML;})()") { value ->
                            val html = decodeEvaluatedHtml(value)
                            if (html.isNotBlank()) {
                                onHtmlAvailable(currentUrl, html)
                            }
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        me.thenano.yamibo.yamibo_app.Logger.e("SignWebView", error?.description.toString())
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let(onTitleChanged)
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadsImagesAutomatically = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    javaScriptCanOpenWindowsAutomatically = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = SignMobileUserAgent
                    setSupportZoom(true)
                }

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
                if (cookies.isNotEmpty()) {
                    cookies.split(";").forEach { cookieManager.setCookie(url, it.trim()) }
                    cookieManager.flush()
                }
                loadUrl(url)
            }
        },
        update = { webViewInstance = it },
    )
}

private fun decodeEvaluatedHtml(value: String?): String {
    if (value.isNullOrBlank() || value == "null") return ""
    return runCatching { JSONArray("[$value]").getString(0) }.getOrElse { value }
}
