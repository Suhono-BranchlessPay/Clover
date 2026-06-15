package com.branchlesspay.auditshield.clover

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class AnchorProcessResult(
    val ok: Boolean,
    val queuedOffline: Boolean,
    val anchorId: String?,
    val verifyUrl: String?,
    val error: String?,
    val referenceId: String,
)

data class FlushResult(
    val processed: Int,
    val anchored: Int,
    val failed: Int,
)

class AnchorProcessor(
    private val queue: AnchorQueueRepository,
    private val apiClientFactory: () -> BpApiClient,
    private val isOnline: () -> Boolean,
) {
    companion object {
        const val MAX_RETRIES = 3
        private val gson = Gson()
        private val mapType = object : TypeToken<Map<String, Any>>() {}.type
    }

    fun processPayment(
        event: PaymentEvent,
        deviceModel: String,
        deviceSn: String,
    ): AnchorProcessResult {
        val payload = BpAnchorPayload.buildPayment(event, deviceModel, deviceSn)
        val referenceId = payload["reference_id"] as String

        if (!isOnline()) {
            queue.enqueue(referenceId, payload["event_type"] as String, payload)
            return AnchorProcessResult(
                ok = false,
                queuedOffline = true,
                anchorId = null,
                verifyUrl = null,
                error = "offline — queued for sync",
                referenceId = referenceId,
            )
        }

        val result = postPayload(payload, referenceId)
        if (result.ok && result.anchorId != null) {
            queue.recordAnchored(
                referenceId = referenceId,
                eventType = payload["event_type"] as String,
                payload = payload,
                anchorId = result.anchorId,
                verifyUrl = result.verifyUrl ?: TransactionFormatter.buildVerifyUrl(result.anchorId) ?: "",
            )
            return result
        }
        if (result.ok) return result

        queue.enqueue(referenceId, payload["event_type"] as String, payload)
        return result.copy(error = result.error ?: "queued after failure")
    }

    fun flushQueue(): FlushResult {
        if (!isOnline()) {
            return FlushResult(processed = 0, anchored = 0, failed = 0)
        }

        var processed = 0
        var anchored = 0
        var failed = 0

        for (item in queue.listPending(MAX_RETRIES)) {
            processed++
            val payload = gson.fromJson<Map<String, Any>>(item.payloadJson, mapType)
            val result = apiClientFactory().postAnchor(payload)
            if (result.ok && result.anchorId != null) {
                queue.markAnchored(item.id, result.anchorId, result.verifyUrl ?: "")
                anchored++
            } else if (item.retryCount + 1 >= MAX_RETRIES) {
                queue.markFailed(item.id, result.error ?: "max retries")
                failed++
            } else {
                queue.incrementRetry(item.id, result.error ?: "HTTP ${result.httpStatus}")
            }
        }
        return FlushResult(processed = processed, anchored = anchored, failed = failed)
    }

    private fun postPayload(payload: Map<String, Any>, referenceId: String): AnchorProcessResult {
        val result = apiClientFactory().postAnchor(payload)
        return AnchorProcessResult(
            ok = result.ok,
            queuedOffline = false,
            anchorId = result.anchorId,
            verifyUrl = result.verifyUrl,
            error = result.error,
            referenceId = referenceId,
        )
    }
}
