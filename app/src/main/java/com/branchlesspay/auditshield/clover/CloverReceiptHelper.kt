package com.branchlesspay.auditshield.clover

import android.content.Context
import android.util.Log

/**
 * Clover receipt / customer-facing print via Clover SDK (OrderConnector / ReceiptRegistrationConnector).
 * Until SDK is wired, callers should fall back to share intent or on-screen QR.
 */
object CloverReceiptHelper {
    private const val TAG = "CloverReceiptHelper"

    fun printVerifyQr(
        context: Context,
        @Suppress("UNUSED_PARAMETER")
        verifyUrl: String,
        onResult: (ok: Boolean, message: String) -> Unit,
    ) {
        Log.i(TAG, "Receipt print not wired — add clover-android-sdk (see app/libs/README.md)")
        onResult(
            false,
            context.getString(R.string.receipt_unavailable),
        )
    }
}
