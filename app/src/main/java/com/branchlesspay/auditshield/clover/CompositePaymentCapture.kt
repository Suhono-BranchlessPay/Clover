package com.branchlesspay.auditshield.clover

import java.util.concurrent.ConcurrentHashMap

/**
 * Runs multiple capture sources (broadcast + connector) and deduplicates by transaction ID.
 */
class CompositePaymentCapture(
    private val delegates: List<PaymentCapture>,
) : PaymentCapture {
    override val sourceName: String =
        delegates.joinToString("+") { it.sourceName }

    private val seenIds = ConcurrentHashMap.newKeySet<String>()
    private var callback: ((PaymentEvent) -> Unit)? = null

    override fun start(onPayment: (PaymentEvent) -> Unit) {
        stop()
        callback = onPayment
        val wrapped: (PaymentEvent) -> Unit = { event ->
            if (seenIds.add(event.transactionId)) {
                callback?.invoke(event)
            }
        }
        delegates.forEach { it.start(wrapped) }
    }

    override fun stop() {
        delegates.forEach { it.stop() }
        seenIds.clear()
        callback = null
    }
}
