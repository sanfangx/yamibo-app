package me.thenano.yamibo.yamibo_app.favorite

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSString
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.dataUsingEncoding
import platform.Foundation.writeToURL
import platform.Foundation.NSUTF8StringEncoding
import platform.darwin.NSObject
import platform.UIKit.UIViewController

private fun UIViewController.topMostViewController(): UIViewController {
    var current = this
    while (current.presentedViewController != null) {
        current = current.presentedViewController!!
    }
    return current
}

class FavoriteDocumentPickerDelegate(
    private val onImportPicked: (String) -> Unit,
    private val onImportFailed: (String) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) {
            onImportFailed("未選擇檔案")
            return
        }
        
        val shouldRelease = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url)
            if (data != null) {
                val nsString = NSString(data = data, encoding = NSUTF8StringEncoding)
                onImportPicked(nsString.toString())
            } else {
                onImportFailed("無法讀取檔案內容")
            }
        } catch (e: Exception) {
            onImportFailed(e.message ?: "讀取檔案失敗")
        } finally {
            if (shouldRelease) {
                url.stopAccessingSecurityScopedResource()
            }
        }
    }
    
    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onImportFailed("已取消導入")
    }
}

@Composable
actual fun rememberFavoriteShareFileActions(
    onExported: (String) -> Unit,
    onExportFailed: (String) -> Unit,
    onImportPicked: (String) -> Unit,
    onImportFailed: (String) -> Unit,
): FavoriteShareFileActions {
    return remember(onExported, onExportFailed, onImportPicked, onImportFailed) {
        val delegate = FavoriteDocumentPickerDelegate(onImportPicked, onImportFailed)
        
        val exportOrShare = { fileName: String, jsonText: String, isShare: Boolean ->
            try {
                val tempDir = NSTemporaryDirectory()
                val filePath = tempDir + fileName
                val fileUrl = NSURL.fileURLWithPath(filePath)
                
                @Suppress("CAST_NEVER_SUCCEEDS")
                val nsString = jsonText as NSString
                val data = nsString.dataUsingEncoding(NSUTF8StringEncoding)
                
                if (data != null && data.writeToURL(fileUrl, true)) {
                    val activityViewController = UIActivityViewController(
                        activityItems = listOf(fileUrl),
                        applicationActivities = null
                    )
                    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                    if (rootViewController != null) {
                        val topViewController = rootViewController.topMostViewController()
                        
                        activityViewController.popoverPresentationController?.apply {
                            sourceView = topViewController.view
                            sourceRect = topViewController.view.bounds
                            permittedArrowDirections = 0u
                        }
                        
                        topViewController.presentViewController(activityViewController, animated = true, completion = null)
                        onExported(if (isShare) "分享成功" else "導出成功")
                    } else {
                        onExportFailed("未找到根視窗")
                    }
                } else {
                    onExportFailed("寫入暫存檔案失敗")
                }
            } catch (e: Exception) {
                onExportFailed(e.message ?: "操作失敗")
            }
        }

        FavoriteShareFileActions(
            exportJson = { fileName, jsonText -> exportOrShare(fileName, jsonText, false) },
            shareJson = { fileName, jsonText -> exportOrShare(fileName, jsonText, true) },
            pickJson = {
                try {
                    @Suppress("DEPRECATION")
                    val picker = UIDocumentPickerViewController(
                        documentTypes = listOf("public.json"),
                        inMode = 0L // UIDocumentPickerModeImport
                    )
                    picker.delegate = delegate
                    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                    if (rootViewController != null) {
                        rootViewController.topMostViewController()
                            .presentViewController(picker, animated = true, completion = null)
                    } else {
                        onImportFailed("未找到根視窗")
                    }
                } catch (e: Exception) {
                    onImportFailed(e.message ?: "啟動檔案選取器失敗")
                }
            }
        )
    }
}
