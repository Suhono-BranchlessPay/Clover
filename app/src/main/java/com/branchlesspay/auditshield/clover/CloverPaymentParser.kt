package com.branchlesspay.auditshield.clover

object CloverPaymentParser {
    fun parseExtras(extras: Map<String, String?>): PaymentEvent? {
        val transactionId = firstNonBlank(
            extras["paymentId"],
            extras["payment_id"],
            extras["id"],
            extras["transactionId"],
            extras["transId"],
            extras["orderId"],
            extras["order_id"],
        ) ?: return null

        val amountRaw = firstNonBlank(
            extras["amount"],
            extras["payAmount"],
            extras["totalAmount"],
            extras["amount_cents"],
        )
        val amountCents = parseAmountCents(amountRaw) ?: return null
        val currency = firstNonBlank(extras["currency"], extras["currencyCode"]) ?: "USD"
        val method = firstNonBlank(
            extras["tenderType"],
            extras["payType"],
            extras["paymentMethod"],
            extras["cardType"],
        ) ?: "card"
        val merchantId = firstNonBlank(extras["merchantId"], extras["merchant_id"]) ?: ""

        return PaymentEvent(
            transactionId = transactionId,
            amountCents = amountCents,
            currency = currency.uppercase(),
            paymentMethod = method,
            merchantId = merchantId,
        )
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    /**
     * Clover REST/SDK amounts are integer cents. Decimal strings are treated as dollars.
     */
    private fun parseAmountCents(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace(",", "").trim()
        if (cleaned.contains('.')) {
            return cleaned.toDoubleOrNull()?.let { (it * 100).toLong() }
        }
        return cleaned.toLongOrNull()
    }
}
