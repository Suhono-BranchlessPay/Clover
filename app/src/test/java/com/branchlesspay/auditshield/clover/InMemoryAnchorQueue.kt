package com.branchlesspay.auditshield.clover

import com.google.gson.Gson

/** In-memory queue for JVM unit tests. */
class InMemoryAnchorQueue : AnchorQueueRepository {
    private val gson = Gson()
    private val items = mutableListOf<QueuedAnchor>()
    private var nextId = 1L

    override fun enqueue(referenceId: String, eventType: String, payload: Map<String, Any>): Long {
        val item = QueuedAnchor(
            id = nextId++,
            referenceId = referenceId,
            eventType = eventType,
            payloadJson = gson.toJson(payload),
            retryCount = 0,
            status = QueueStatus.PENDING,
            anchorId = null,
            verifyUrl = null,
            error = null,
            createdAt = System.currentTimeMillis(),
        )
        items.add(item)
        return item.id
    }

    override fun listPending(maxRetries: Int): List<QueuedAnchor> =
        items.filter { it.status == QueueStatus.PENDING && it.retryCount < maxRetries }

    override fun markAnchored(id: Long, anchorId: String, verifyUrl: String) {
        replace(id) { it.copy(status = QueueStatus.ANCHORED, anchorId = anchorId, verifyUrl = verifyUrl) }
    }

    override fun markFailed(id: Long, error: String) {
        replace(id) { it.copy(status = QueueStatus.FAILED, error = error) }
    }

    override fun incrementRetry(id: Long, error: String) {
        replace(id) { it.copy(retryCount = it.retryCount + 1, error = error) }
    }

    override fun countPending(): Int = items.count { it.status == QueueStatus.PENDING }

    override fun countAnchored(): Int = items.count { it.status == QueueStatus.ANCHORED }

    override fun countFailed(): Int = items.count { it.status == QueueStatus.FAILED }

    override fun getById(id: Long): QueuedAnchor? = items.firstOrNull { it.id == id }

    override fun recordAnchored(
        referenceId: String,
        eventType: String,
        payload: Map<String, Any>,
        anchorId: String,
        verifyUrl: String,
    ): Long {
        val item = QueuedAnchor(
            id = nextId++,
            referenceId = referenceId,
            eventType = eventType,
            payloadJson = gson.toJson(payload),
            retryCount = 0,
            status = QueueStatus.ANCHORED,
            anchorId = anchorId,
            verifyUrl = verifyUrl,
            error = null,
            createdAt = System.currentTimeMillis(),
        )
        items.add(item)
        return item.id
    }

    override fun recent(limit: Int): List<QueuedAnchor> =
        items.sortedByDescending { it.createdAt }.take(limit)

    private fun replace(id: Long, transform: (QueuedAnchor) -> QueuedAnchor) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) items[index] = transform(items[index])
    }
}
