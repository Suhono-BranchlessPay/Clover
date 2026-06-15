package com.branchlesspay.auditshield.clover

import java.util.Locale
import java.util.UUID

data class PaymentEvent(
    val transactionId: String,
    val amountCents: Long,
    val currency: String,
    val paymentMethod: String,
    val timestampIso: String = BpAnchorPayload.isoNow(),
    val merchantId: String = "",
) {
    companion object {
        fun simulated(currency: String = "USD", amountCents: Long = 1500): PaymentEvent {
            val suffix = UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.US)
            return PaymentEvent(
                transactionId = "PAY-$suffix",
                amountCents = amountCents,
                currency = currency,
                paymentMethod = "card",
                merchantId = "sim-merchant",
            )
        }
    }
}

interface PaymentCapture {
    fun start(onPayment: (PaymentEvent) -> Unit)
    fun stop()
    val sourceName: String
}
