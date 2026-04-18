package me.thenano.yamibo.yamibo_app.profile.sign

internal fun isSignPageHtml(html: String): Boolean {
    return html.contains("点击打卡") ||
        html.contains("打卡公告") ||
        html.contains("repairday") ||
        html.contains("我的打卡动态")
}

internal fun isSignResultPageHtml(html: String): Boolean {
    return html.contains("提示信息") && (
        html.contains("打卡成功") ||
            html.contains("已经打过卡") ||
            html.contains("补签") ||
            html.contains("返回签详情")
        )
}

internal fun isMaintenancePageHtml(html: String): Boolean {
    return html.contains("<title>百合会每日维护</title>") ||
        html.contains("""<img class="pic" src="/images/backup01.jpg" alt="每日维护">""")
}
