package me.thenano.yamibo.yamibo_app.profile.settings.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIApplication
import platform.Foundation.NSURL
import platform.darwin.NSObject
import platform.UniformTypeIdentifiers.UTType
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
                    val picker = UIDocumentPickerViewController(
                        forOpeningContentTypes = listOf(UTType.folderType),
                        asCopy = false
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
                    val picker = UIDocumentPickerViewController(
                        forOpeningContentTypes = listOf(UTType.itemType),
                        asCopy = true
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
