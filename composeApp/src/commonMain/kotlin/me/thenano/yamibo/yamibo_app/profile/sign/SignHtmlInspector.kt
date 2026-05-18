package me.thenano.yamibo.yamibo_app.profile.sign

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

internal fun isCloudflareChallengeHtml(html: String): Boolean {
    val body = html.lowercase()
    return body.contains("<title>just a moment") ||
        body.contains("cf-chl") ||
        body.contains("challenge-platform") ||
        body.contains("verify you are human") ||
        body.contains("cloudflare") && body.contains("challenge")
}

internal fun isSignPageHtml(html: String): Boolean {
    return html.contains(appString(Res.string.auto_367ee07acc)) ||
        html.contains(appString(Res.string.auto_ceb6b09947)) ||
        html.contains("repairday") ||
        html.contains(appString(Res.string.auto_4f1054038b))
}

internal fun isSignResultPageHtml(html: String): Boolean {
    return html.contains(appString(Res.string.auto_99c74120cc)) && (
        html.contains(appString(Res.string.auto_252f8bcda3)) ||
            html.contains(appString(Res.string.auto_866c9ea11b)) ||
            html.contains(appString(Res.string.auto_cadfe91096)) ||
            html.contains(appString(Res.string.auto_226ba80c13))
        )
}

internal fun isMaintenancePageHtml(html: String): Boolean {
    return html.contains(appString(Res.string.auto_48dd943e55)) ||
        html.contains("""<img class="pic" src="/images/backup01.jpg" alt=appString(Res.string.auto_78838741ff)>""")
}

