package com.branchlesspay.auditshield.clover

enum class QueueStatus {
    PENDING,
    ANCHORED,
    FAILED,
}

data class QueuedAnchor(
    val id: Long,
    val referenceId: String,
    val eventType: String,
    val payloadJson: String,
    val retryCount: Int,
    val status: QueueStatus,
    val anchorId: String?,
    val verifyUrl: String?,
    val error: String?,
    val createdAt: Long,
)

interface AnchorQueueRepository {
    fun enqueue(referenceId: String, eventType: String, payload: Map<String, Any>): Long
    fun listPending(maxRetries: Int = AnchorProcessor.MAX_RETRIES): List<QueuedAnchor>
    fun markAnchored(id: Long, anchorId: String, verifyUrl: String)
    fun markFailed(id: Long, error: String)
    fun incrementRetry(id: Long, error: String)
    fun countPending(): Int
    fun countAnchored(): Int
    fun countFailed(): Int
    fun recent(limit: Int = 20): List<QueuedAnchor>
    fun getById(id: Long): QueuedAnchor?
    fun recordAnchored(
        referenceId: String,
        eventType: String,
        payload: Map<String, Any>,
        anchorId: String,
        verifyUrl: String,
    ): Long
}
