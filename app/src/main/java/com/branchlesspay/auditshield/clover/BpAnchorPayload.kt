package com.branchlesspay.auditshield.clover

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object DeviceInfo {
    fun model(): String = Build.MODEL ?: "unknown"

    fun serial(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                Build.SERIAL
            }
        } catch (_: SecurityException) {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown"
        }
    }
}

object BpAnchorPayload {
    fun testTransaction(context: Context): Map<String, Any> =
        buildTestTransaction(DeviceInfo.model(), DeviceInfo.serial(context))

    fun buildTestTransaction(deviceModel: String, deviceSn: String): Map<String, Any> {
        val suffix = UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.US)
        return mapOf(
            "event_type" to "clover_transaction",
            "reference_id" to "TEST-$suffix",
            "amount" to 15.0,
            "currency" to "USD",
            "timestamp" to isoNow(),
            "vendor" to "clover",
            "merchant_id" to deviceSn,
            "metadata" to baseMetadata(deviceModel, deviceSn),
        )
    }

    fun buildPayment(
        event: PaymentEvent,
        deviceModel: String,
        deviceSn: String,
    ): Map<String, Any> {
        val merchantId = event.merchantId.ifBlank { deviceSn }
        val metadata = baseMetadata(deviceModel, deviceSn).toMutableMap()
        metadata["payment_method"] = event.paymentMethod
        metadata["merchant_id"] = merchantId
        metadata["amount_cents"] = event.amountCents

        return mapOf(
            "event_type" to "clover_payment",
            "reference_id" to event.transactionId,
            "amount" to event.amountCents / 100.0,
            "currency" to event.currency,
            "timestamp" to event.timestampIso,
            "vendor" to "clover",
            "merchant_id" to merchantId,
            "metadata" to metadata,
        )
    }

    private fun baseMetadata(deviceModel: String, deviceSn: String): Map<String, Any> =
        mapOf(
            "erp" to "clover",
            "erp_system" to "Clover POS",
            "device_model" to deviceModel,
            "device_sn" to deviceSn,
        )

    fun isoNow(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }
}
