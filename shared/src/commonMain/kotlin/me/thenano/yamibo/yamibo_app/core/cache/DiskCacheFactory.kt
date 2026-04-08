package me.thenano.yamibo.yamibo_app.core.cache

import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

class DiskCacheFactory(
    dbFactory: DatabaseFactory,
    val json: Json = Json { ignoreUnknownKeys = true },
    val cacheDirPath: String // Base cache directory from platform
) {
    val database = Database(dbFactory.createDriver())
    val fileSystem = FileSystem.SYSTEM
    val rootCacheDir = cacheDirPath.toPath() / "yamibo_cache"

    /**
     * Create a new DiskCache instance for a specific type <T>
     * @param namespace The separator name to prevent cache key collision
     * @param maxSize Maximum entries before LRU eviction triggers
     * @param expirationMs How many milliseconds an entry lives (null for no time-based expiration, uses only LRU)
     */
    inline fun <reified T : Any> create(
        namespace: String,
        maxSize: Int = 100,
        expirationMs: Long? = null
    ): DiskCache<T> {
        return DiskCacheImpl(
            namespace = namespace,
            maxSize = maxSize,
            expirationMs = expirationMs,
            database = database,
            json = json,
            rootCacheDir = rootCacheDir,
            fileSystem = fileSystem,
            serializer = json.serializersModule.serializer<T>()
        )
    }

    /** Returns total size of the cache directory in bytes */
    suspend fun getTotalCacheSizeBytes(): Long = kotlinx.coroutines.withContext(Dispatchers.IO) {
        calculateSize(rootCacheDir)
    }

    private fun calculateSize(path: okio.Path): Long {
        var size = 0L
        try {
            if (!fileSystem.exists(path)) return 0L
            val meta = fileSystem.metadata(path)
            if (meta.isRegularFile) {
                return meta.size ?: 0L
            } else if (meta.isDirectory) {
                fileSystem.list(path).forEach { child ->
                    size += calculateSize(child)
                }
            }
        } catch(_: Exception) {}
        return size
    }

    /** Clears all disk cache across all namespaces */
    suspend fun clearAllCache() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            database.diskCacheEntryQueries.clearAll()
            if (fileSystem.exists(rootCacheDir)) {
                fileSystem.deleteRecursively(rootCacheDir)
                fileSystem.createDirectories(rootCacheDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class DiskCacheImpl<T : Any>(
    private val namespace: String,
    private val maxSize: Int,
    private val expirationMs: Long?,
    database: Database,
    private val json: Json,
    rootCacheDir: okio.Path,
    private val fileSystem: FileSystem,
    private val serializer: KSerializer<T>
) : DiskCache<T> {

    private val queries = database.diskCacheEntryQueries
    private val scope = CoroutineScope(Dispatchers.IO)
    private val namespaceDir = rootCacheDir / namespace

    init {
        try {
            fileSystem.createDirectories(namespaceDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFilePath(key: String) = namespaceDir / "$key.json"

    override fun set(key: String, value: T) {
        val now = getTimeMillis()
        val serializedValue = try {
            json.encodeToString(serializer, value)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // Write to File System synchronously (to ensure it's available right after set returns)
        val file = getFilePath(key)
        try {
            fileSystem.sink(file).buffer().use { sink ->
                sink.writeUtf8(serializedValue)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // Update DB Metadata
        try {
            queries.upsert(
                namespace = namespace,
                cacheKey = key,
                createdAt = now,
                lastAccessedAt = now
            )
            // Trigger LRU asynchronously
            maintainLRU()
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup file if DB failed
            try { fileSystem.delete(file) } catch (ignore: Exception) {}
        }
    }

    override fun get(key: String): T? {
        val now = getTimeMillis()
        val entry = try {
            queries.get(namespace, key).executeAsOneOrNull()
        } catch (e: Exception) {
            null
        } ?: return null

        // Check Expiration
        if (expirationMs != null && now - entry.createdAt > expirationMs) {
            // Expired
            remove(key)
            return null
        }

        // Update Access Time async
        scope.launch {
            try {
                queries.updateAccessTime(
                    lastAccessedAt = now,
                    namespace = namespace,
                    cacheKey = key
                )
            } catch (e: Exception) {
                // Ignore update failure
            }
        }

        val file = getFilePath(key)
        val jsonString = try {
            if (fileSystem.exists(file)) {
                fileSystem.source(file).buffer().use { source ->
                    source.readUtf8()
                }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (jsonString == null) {
            remove(key) // File missing, clean up DB
            return null
        }

        return try {
            json.decodeFromString(serializer, jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            remove(key)
            null
        }
    }

    override fun remove(key: String) {
        try {
            queries.delete(namespace, key)
            fileSystem.delete(getFilePath(key))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun removeByPrefix(prefix: String) {
        try {
            val matchingKeys = queries.getKeysByPrefix(namespace, "$prefix%").executeAsList()
            queries.deleteByPrefix(namespace, "$prefix%")
            matchingKeys.forEach { key ->
                try {
                    fileSystem.delete(getFilePath(key))
                } catch (ignore: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun clear() {
        try {
            queries.clearNamespace(namespace)
            if (fileSystem.exists(namespaceDir)) {
                fileSystem.deleteRecursively(namespaceDir)
                fileSystem.createDirectories(namespaceDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun maintainLRU() {
        scope.launch {
            try {
                val targets = queries.getLRUTargets(namespace, maxSize.toLong()).executeAsList()
                if (targets.isNotEmpty()) {
                    // Start deleting excess entries from DB and file system
                    targets.forEach { targetKey ->
                        remove(targetKey)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
