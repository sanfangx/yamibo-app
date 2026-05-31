package me.thenano.yamibo.yamibo_app.update

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdateDownloadState
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdatePlatform
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdateRelease
import java.io.File
import java.net.URL
import java.security.MessageDigest

class AndroidAppUpdatePlatform(
    private val context: Context,
) : AppUpdatePlatform {
    @Volatile
    private var canceled = false

    override val currentVersionCode: Long =
        context.packageManager.getPackageInfo(context.packageName, 0).let { info ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        }

    override val currentVersionName: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    override val platformKey: String = "android"
    override val supportedAssetTypes: Set<String> = setOf("universal-apk", "apk")

    override suspend fun downloadAndInstall(
        release: AppUpdateRelease,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): AppUpdateDownloadState = withContext(Dispatchers.IO) {
        val asset = release.asset ?: return@withContext AppUpdateDownloadState.Failed(release, "No APK asset")
        canceled = false
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, "yamibo-${release.versionName}.apk")

        runCatching {
            URL(asset.url).openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 60_000
            }.getInputStream().use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    val total = asset.size
                    while (true) {
                        if (canceled) throw CancellationException()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
            asset.sha256?.takeIf { it.isNotBlank() }?.let { expected ->
                val actual = apkFile.sha256()
                if (!actual.equals(expected, ignoreCase = true)) {
                    error("APK sha256 mismatch: expected $expected, actual $actual")
                }
            }
            requestInstall(apkFile, release)
        }.getOrElse { error ->
            if (error is CancellationException) {
                AppUpdateDownloadState.Failed(release, "Download canceled")
            } else {
                AppUpdateDownloadState.Failed(release, error.message ?: "Download failed")
            }
        }
    }

    override fun cancelDownload() {
        canceled = true
    }

    override fun openReleasePage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun requestInstall(apkFile: File, release: AppUpdateRelease): AppUpdateDownloadState {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri(),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            return AppUpdateDownloadState.PermissionRequired(release)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        return AppUpdateDownloadState.Completed(release)
    }
}

private class CancellationException : Exception()

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
