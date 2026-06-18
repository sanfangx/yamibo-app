package me.thenano.yamibo.yamibo_app.store.sign

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.util.time.currentLocalDateKey
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

data class SignStatusRecord(
    val dateKey: String,
    val isSigned: Boolean,
    val updatedAt: Long,
    val lastMessage: String?,
)

interface SignStatusStore {
    fun getToday(): SignStatusRecord?
    fun updateToday(isSigned: Boolean, message: String? = null)
}

class DatabaseSignStatusStore(db: Database) : SignStatusStore {
    private val queries = db.signDailyRecordQueries

    override fun getToday(): SignStatusRecord? =
        queries.getByDateKey(currentLocalDateKey()).executeAsOneOrNull()?.let { record ->
            SignStatusRecord(
                dateKey = record.dateKey,
                isSigned = record.isSigned != 0L,
                updatedAt = record.updatedAt,
                lastMessage = record.lastMessage,
            )
        }

    override fun updateToday(isSigned: Boolean, message: String?) {
        queries.upsert(
            dateKey = currentLocalDateKey(),
            isSigned = if (isSigned) 1L else 0L,
            updatedAt = currentTimeMillis(),
            lastMessage = message,
        )
    }
}
