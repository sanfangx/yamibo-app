package me.thenano.yamibo.yamibo_app.profile.settings.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIApplication
import platform.Foundation.NSURL
import platform.darwin.NSObject
import platform.UIKit.UIViewController

private fun UIViewController.topMostViewController(): UIViewController {
    var current = this
    while (current.presentedViewController != null) {
        current = current.presentedViewController!!
    }
    return current
}

class BackupDocumentPickerDelegate(
    private val onPicked: (String) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        val path = url?.path
        if (path != null) {
            val shouldRelease = url.startAccessingSecurityScopedResource()
            try {
                onPicked(path)
            } finally {
                if (shouldRelease) {
                    url.stopAccessingSecurityScopedResource()
                }
            }
        }
    }
}

@Composable
actual fun rememberBackupFileActions(
    onFolderSelected: (String) -> Unit,
    onBackupPicked: (String) -> Unit,
): BackupFileActions {
    return remember(onFolderSelected, onBackupPicked) {
        val folderDelegate = BackupDocumentPickerDelegate(onFolderSelected)
        val fileDelegate = BackupDocumentPickerDelegate(onBackupPicked)
        
        BackupFileActions(
            selectFolder = {
                try {
                    @Suppress("DEPRECATION")
                    val picker = UIDocumentPickerViewController(
                        documentTypes = listOf("public.folder"),
                        inMode = 1L // UIDocumentPickerModeOpen
                    )
                    picker.delegate = folderDelegate
                    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                    if (rootViewController != null) {
                        rootViewController.topMostViewController()
                            .presentViewController(picker, animated = true, completion = null)
                    }
                } catch (e: Exception) {
                    // Ignore or log
                }
            },
            pickBackupFile = {
                try {
                    @Suppress("DEPRECATION")
                    val picker = UIDocumentPickerViewController(
                        documentTypes = listOf("public.item"),
                        inMode = 0L // UIDocumentPickerModeImport
                    )
                    picker.delegate = fileDelegate
                    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                    if (rootViewController != null) {
                        rootViewController.topMostViewController()
                            .presentViewController(picker, animated = true, completion = null)
                    }
                } catch (e: Exception) {
                    // Ignore or log
                }
            }
        )
    }
}
