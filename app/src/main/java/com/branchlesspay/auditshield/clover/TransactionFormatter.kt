package com.branchlesspay.auditshield.clover

object TransactionFormatter {
    fun statusLabel(status: QueueStatus): String = when (status) {
        QueueStatus.PENDING -> "Pending"
        QueueStatus.ANCHORED -> "Anchored"
        QueueStatus.FAILED -> "Failed"
    }

    fun formatAmount(payloadJson: String): String {
        return runCatching {
            val amount = Regex("\"amount\"\\s*:\\s*([0-9.]+)")
                .find(payloadJson)?.groupValues?.get(1)?.toDoubleOrNull() ?: return "—"
            val currency = Regex("\"currency\"\\s*:\\s*\"([A-Z]+)\"")
                .find(payloadJson)?.groupValues?.get(1) ?: "USD"
            formatMoney(amount, currency)
        }.getOrDefault("—")
    }

    fun formatMoney(amount: Double, currency: String): String = when (currency.uppercase()) {
        "IDR" -> "Rp ${amount.toLong()}"
        "USD" -> "$${"%.2f".format(amount)}"
        else -> "$currency ${"%.2f".format(amount)}"
    }

    fun formatDate(timestampMs: Long): String {
        val formatter = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.US)
        return formatter.format(java.util.Date(timestampMs))
    }

    fun buildVerifyUrl(anchorId: String?): String? =
        anchorId?.takeIf { it.isNotBlank() }?.let { BpApiClient.VERIFY_BASE + it }
}
