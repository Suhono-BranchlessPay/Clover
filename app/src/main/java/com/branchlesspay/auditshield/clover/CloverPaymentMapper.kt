package com.branchlesspay.auditshield.clover

import android.content.Intent
import android.os.Build
import com.clover.sdk.v1.Intents
import com.clover.sdk.v3.payments.Payment
import com.clover.sdk.v3.payments.CardTransaction
import com.clover.sdk.v3.remotepay.SaleResponse

/**
 * Maps Clover SDK [Payment] / [SaleResponse] objects to [PaymentEvent].
 */
object CloverPaymentMapper {
    fun fromPayment(payment: Payment, merchantId: String = ""): PaymentEvent? {
        val paymentId = payment.id?.trim().orEmpty()
        if (paymentId.isEmpty()) return null
        val amountCents = payment.amount ?: return null
        val method = paymentMethod(payment.cardTransaction)
        return PaymentEvent(
            transactionId = paymentId,
            amountCents = amountCents,
            currency = "USD",
            paymentMethod = method,
            merchantId = merchantId,
        )
    }

    fun fromSaleResponse(response: SaleResponse, merchantId: String = ""): PaymentEvent? {
        if (response.success != true) return null
        val payment = response.payment ?: return null
        return fromPayment(payment, merchantId)
    }

    fun fromIntent(intent: Intent): PaymentEvent? {
        val payment = readPaymentExtra(intent)
        if (payment != null) {
            return fromPayment(payment)
        }

        val paymentId = firstNonBlank(
            intent.getStringExtra(Intents.EXTRA_CLOVER_PAYMENT_ID),
            intent.getStringExtra(Intents.EXTRA_PAYMENT_ID),
            intent.getStringExtra("paymentId"),
            intent.getStringExtra("payment_id"),
        )
        if (paymentId != null) {
            val extras = mutableMapOf<String, String?>()
            intent.extras?.keySet()?.forEach { key ->
                extras[key] = intent.extras?.get(key)?.toString()
            }
            extras["paymentId"] = paymentId
            CloverPaymentParser.parseExtras(extras)?.let { return it }
        }

        val extras = mutableMapOf<String, String?>()
        intent.extras?.keySet()?.forEach { key ->
            extras[key] = intent.extras?.get(key)?.toString()
        }
        return CloverPaymentParser.parseExtras(extras)
    }

    private fun readPaymentExtra(intent: Intent): Payment? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intents.EXTRA_PAYMENT, Payment::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intents.EXTRA_PAYMENT) as? Payment
        }
    }

    private fun paymentMethod(cardTransaction: CardTransaction?): String {
        val cardType = cardTransaction?.cardType?.name?.trim().orEmpty()
        if (cardType.isNotEmpty()) return cardType
        val tender = cardTransaction?.entryType?.name?.trim().orEmpty()
        return tender.ifBlank { "card" }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
