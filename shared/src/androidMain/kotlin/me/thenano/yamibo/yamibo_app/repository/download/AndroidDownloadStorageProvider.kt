package me.thenano.yamibo.yamibo_app.repository.download

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.content.Intent
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository

class AndroidDownloadStorageProvider(
    context: Context,
    private val appSettingsRepository: AppSettingsRepository,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : DownloadStorageProvider {
    private val resolver = context.applicationContext.contentResolver

    override suspend fun getSelectedFolderLabel(): String? =
        selectedTreeUri()?.let { queryDisplayName(rootDocumentUri(it)) }

    override suspend fun isReady(): Boolean =
        selectedTreeUri()?.let { uri ->
            resolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission && it.isWritePermission
            }
        } == true

    override suspend fun writeThreadPage(
        key: ThreadPageDownloadKey,
        manifestBytes: ByteArray,
        threadPageBytes: ByteArray,
        images: List<PendingDownloadedImage>,
    ) {
        val treeUri = selectedTreeUri() ?: error("尚未選擇備份資料夾")
        val root = ensureDirectory(treeUri, rootDocumentUri(treeUri), ROOT_DIR)
        val tmpName = "${key.stableId}.tmp"
        deleteChildByName(treeUri, root, tmpName)
        val tmp = ensureDirectory(treeUri, root, tmpName)
        val imagesDir = ensureDirectory(treeUri, tmp, IMAGES_DIR)
        writeFile(treeUri, tmp, "manifest.json", "application/json", manifestBytes)
        writeFile(treeUri, tmp, "thread_page.json", "application/json", threadPageBytes)
        images.forEach { writeFile(treeUri, imagesDir, it.fileName, "application/octet-stream", it.bytes) }
        val previousName = "${key.stableId}.previous"
        deleteChildByName(treeUri, root, previousName)
        val current = findChild(treeUri, root, key.stableId)
        val previous = current?.let { DocumentsContract.renameDocument(resolver, it, previousName) }
        try {
            val renamed = DocumentsContract.renameDocument(resolver, tmp, key.stableId)
                ?: error("儲存提供者不支援完成下載檔案")
            if (renamed == tmp) {
                val actual = findChild(treeUri, root, key.stableId)
                if (actual == null) error("下載資料夾重新命名失敗")
            }
            previous?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
        } catch (error: Throwable) {
            runCatching { DocumentsContract.deleteDocument(resolver, tmp) }
            previous?.let { runCatching { DocumentsContract.renameDocument(resolver, it, key.stableId) } }
            throw error
        }
    }

    override suspend fun readThreadPage(key: ThreadPageDownloadKey): ByteArray? {
        val treeUri = selectedTreeUri() ?: return null
        val pageDir = findChild(treeUri, downloadsRoot(treeUri) ?: return null, key.stableId) ?: return null
        val file = findChild(treeUri, pageDir, "thread_page.json") ?: return null
        return resolver.openInputStream(file)?.use { it.readBytes() }
    }

    override suspend fun resolveImageUri(key: ThreadPageDownloadKey, fileName: String): String? {
        val treeUri = selectedTreeUri() ?: return null
        val pageDir = findChild(treeUri, downloadsRoot(treeUri) ?: return null, key.stableId) ?: return null
        val imagesDir = findChild(treeUri, pageDir, IMAGES_DIR) ?: return null
        return findChild(treeUri, imagesDir, fileName)?.toString()
    }

    override suspend fun readManifest(key: ThreadPageDownloadKey): ThreadPageDownloadManifest? {
        val treeUri = selectedTreeUri() ?: return null
        val pageDir = findChild(treeUri, downloadsRoot(treeUri) ?: return null, key.stableId) ?: return null
        return readManifestFromDir(treeUri, pageDir)
    }

    override suspend fun listManifests(): List<ThreadPageDownloadManifest> {
        val treeUri = selectedTreeUri() ?: return emptyList()
        val root = downloadsRoot(treeUri) ?: return emptyList()
        return listChildren(treeUri, root)
            .mapNotNull { readManifestFromDir(treeUri, it.uri) }
    }

    override suspend fun readQueue(): List<DownloadQueueEntry> {
        val treeUri = selectedTreeUri() ?: return emptyList()
        val root = downloadsRoot(treeUri) ?: return emptyList()
        val file = findChild(treeUri, root, QUEUE_FILE) ?: return emptyList()
        val bytes = resolver.openInputStream(file)?.use { it.readBytes() } ?: return emptyList()
        return runCatching { json.decodeFromString<List<DownloadQueueEntry>>(bytes.decodeToString()) }
            .getOrDefault(emptyList())
    }

    override suspend fun writeQueue(entries: List<DownloadQueueEntry>) {
        val treeUri = selectedTreeUri() ?: return
        val root = ensureDirectory(treeUri, rootDocumentUri(treeUri), ROOT_DIR)
        writeFile(
            treeUri,
            root,
            QUEUE_FILE,
            "application/json",
            json.encodeToString(entries).encodeToByteArray(),
        )
    }

    override suspend fun deleteThreadPage(key: ThreadPageDownloadKey) {
        val treeUri = selectedTreeUri() ?: return
        val root = downloadsRoot(treeUri) ?: return
        deleteChildByName(treeUri, root, key.stableId)
    }

    override suspend fun deleteThread(key: ThreadPageDownloadKey) {
        val treeUri = selectedTreeUri() ?: return
        val root = downloadsRoot(treeUri) ?: return
        listChildren(treeUri, root)
            .filter { it.name.startsWith(key.threadPrefix) }
            .forEach { runCatching { DocumentsContract.deleteDocument(resolver, it.uri) } }
    }

    private fun selectedTreeUri(): Uri? =
        appSettingsRepository.backupFolderUri.getValue().takeIf { it.isNotBlank() }?.let(Uri::parse)

    private fun rootDocumentUri(treeUri: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

    private fun downloadsRoot(treeUri: Uri): Uri? =
        findChild(treeUri, rootDocumentUri(treeUri), ROOT_DIR)

    private fun ensureDirectory(treeUri: Uri, parent: Uri, name: String): Uri =
        findChild(treeUri, parent, name)
            ?: DocumentsContract.createDocument(resolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: error("無法建立資料夾：$name")

    private fun writeFile(treeUri: Uri, parent: Uri, name: String, mimeType: String, bytes: ByteArray) {
        deleteChildByName(treeUri, parent, name)
        val file = DocumentsContract.createDocument(resolver, parent, mimeType, name)
            ?: error("無法建立檔案：$name")
        resolver.openOutputStream(file, "wt")?.use { it.write(bytes) }
            ?: error("無法寫入檔案：$name")
    }

    private fun readManifestFromDir(treeUri: Uri, dir: Uri): ThreadPageDownloadManifest? {
        val file = findChild(treeUri, dir, "manifest.json") ?: return null
        val bytes = resolver.openInputStream(file)?.use { it.readBytes() } ?: return null
        return runCatching { json.decodeFromString<ThreadPageDownloadManifest>(bytes.decodeToString()) }.getOrNull()
    }

    private fun deleteChildByName(treeUri: Uri, parent: Uri, name: String) {
        findChild(treeUri, parent, name)?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
    }

    private fun findChild(treeUri: Uri, parent: Uri, name: String): Uri? =
        listChildren(treeUri, parent).firstOrNull { it.name == name }?.uri

    private fun listChildren(treeUri: Uri, parent: Uri): List<DocumentChild> {
        val docId = DocumentsContract.getDocumentId(parent)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        val items = mutableListOf<DocumentChild>()
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val childId = cursor.getString(idIndex)
                val childName = cursor.getString(nameIndex).orEmpty()
                items += DocumentChild(childName, DocumentsContract.buildDocumentUriUsingTree(treeUri, childId))
            }
        }
        return items
    }

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private data class DocumentChild(val name: String, val uri: Uri)

    private companion object {
        const val ROOT_DIR = "YamiboDownloads"
        const val IMAGES_DIR = "images"
        const val QUEUE_FILE = "queue.json"
    }
}
