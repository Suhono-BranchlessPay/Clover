package com.branchlesspay.auditshield.clover

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson

class SqliteAnchorQueue(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION,
), AnchorQueueRepository {

    companion object {
        private const val DB_NAME = "bp_anchor_queue.db"
        private const val DB_VERSION = 1
        private val gson = Gson()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE anchor_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                reference_id TEXT NOT NULL,
                event_type TEXT NOT NULL,
                payload_json TEXT NOT NULL,
                retry_count INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'PENDING',
                anchor_id TEXT,
                verify_url TEXT,
                error TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_anchor_queue_status ON anchor_queue(status)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS anchor_queue")
        onCreate(db)
    }

    override fun enqueue(referenceId: String, eventType: String, payload: Map<String, Any>): Long {
        val values = ContentValues().apply {
            put("reference_id", referenceId)
            put("event_type", eventType)
            put("payload_json", gson.toJson(payload))
            put("retry_count", 0)
            put("status", QueueStatus.PENDING.name)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert("anchor_queue", null, values)
    }

    override fun listPending(maxRetries: Int): List<QueuedAnchor> {
        val db = readableDatabase
        val cursor = db.query(
            "anchor_queue",
            null,
            "status = ? AND retry_count < ?",
            arrayOf(QueueStatus.PENDING.name, maxRetries.toString()),
            null,
            null,
            "created_at ASC",
        )
        cursor.use {
            val items = mutableListOf<QueuedAnchor>()
            while (cursor.moveToNext()) {
                items.add(cursor.toQueuedAnchor())
            }
            return items
        }
    }

    override fun markAnchored(id: Long, anchorId: String, verifyUrl: String) {
        val values = ContentValues().apply {
            put("status", QueueStatus.ANCHORED.name)
            put("anchor_id", anchorId)
            put("verify_url", verifyUrl)
            put("error", null as String?)
        }
        writableDatabase.update("anchor_queue", values, "id = ?", arrayOf(id.toString()))
    }

    override fun markFailed(id: Long, error: String) {
        val values = ContentValues().apply {
            put("status", QueueStatus.FAILED.name)
            put("error", error)
        }
        writableDatabase.update("anchor_queue", values, "id = ?", arrayOf(id.toString()))
    }

    override fun incrementRetry(id: Long, error: String) {
        writableDatabase.execSQL(
            "UPDATE anchor_queue SET retry_count = retry_count + 1, error = ? WHERE id = ?",
            arrayOf(error, id),
        )
    }

    override fun countPending(): Int = countByStatus(QueueStatus.PENDING)

    override fun countAnchored(): Int = countByStatus(QueueStatus.ANCHORED)

    override fun countFailed(): Int = countByStatus(QueueStatus.FAILED)

    override fun getById(id: Long): QueuedAnchor? {
        val cursor = readableDatabase.query(
            "anchor_queue",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
        )
        cursor.use {
            return if (cursor.moveToFirst()) cursor.toQueuedAnchor() else null
        }
    }

    override fun recordAnchored(
        referenceId: String,
        eventType: String,
        payload: Map<String, Any>,
        anchorId: String,
        verifyUrl: String,
    ): Long {
        val values = ContentValues().apply {
            put("reference_id", referenceId)
            put("event_type", eventType)
            put("payload_json", gson.toJson(payload))
            put("retry_count", 0)
            put("status", QueueStatus.ANCHORED.name)
            put("anchor_id", anchorId)
            put("verify_url", verifyUrl)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert("anchor_queue", null, values)
    }

    override fun recent(limit: Int): List<QueuedAnchor> {
        val cursor = readableDatabase.query(
            "anchor_queue",
            null,
            null,
            null,
            null,
            null,
            "created_at DESC",
            limit.toString(),
        )
        cursor.use {
            val items = mutableListOf<QueuedAnchor>()
            while (cursor.moveToNext()) {
                items.add(cursor.toQueuedAnchor())
            }
            return items
        }
    }

    private fun countByStatus(status: QueueStatus): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM anchor_queue WHERE status = ?",
            arrayOf(status.name),
        )
        cursor.use {
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    @Suppress("DEPRECATION")
    private fun android.database.Cursor.toQueuedAnchor(): QueuedAnchor =
        QueuedAnchor(
            id = getLong(getColumnIndex("id")),
            referenceId = getString(getColumnIndex("reference_id")),
            eventType = getString(getColumnIndex("event_type")),
            payloadJson = getString(getColumnIndex("payload_json")),
            retryCount = getInt(getColumnIndex("retry_count")),
            status = QueueStatus.valueOf(getString(getColumnIndex("status"))),
            anchorId = getString(getColumnIndex("anchor_id")),
            verifyUrl = getString(getColumnIndex("verify_url")),
            error = getString(getColumnIndex("error")),
            createdAt = getLong(getColumnIndex("created_at")),
        )
}
