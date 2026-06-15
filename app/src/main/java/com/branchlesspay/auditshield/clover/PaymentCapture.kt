package com.branchlesspay.auditshield.clover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Listens for Clover payment broadcasts on Clover hardware.
 * Wire [CloverConnector.onSaleResponse] when clover-android-sdk AAR is added (see app/libs/README.md).
 */
class CloverPaymentCapture(
    private val context: Context,
) : PaymentCapture {
    companion object {
        private const val TAG = "CloverPaymentCapture"
        val CLOVER_ACTIONS = listOf(
            "com.clover.payment.action.COMPLETE",
            "com.clover.sdk.intent.action.PAYMENT",
            "com.clover.intent.action.ACTION_PAYMENT",
        )
    }

    override val sourceName: String = "clover_sdk"

    private var callback: ((PaymentEvent) -> Unit)? = null
    private var receiver: BroadcastReceiver? = null

    override fun start(onPayment: (PaymentEvent) -> Unit) {
        stop()
        callback = onPayment
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                parsePaymentIntent(intent)?.let { event ->
                    Log.i(TAG, "Payment captured tx=${event.transactionId} cents=${event.amountCents}")
                    callback?.invoke(event)
                }
            }
        }
        val filter = IntentFilter()
        CLOVER_ACTIONS.forEach { filter.addAction(it) }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        Log.i(TAG, "Clover payment listener active on ${Build.MANUFACTURER} ${Build.MODEL}")
    }

    override fun stop() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
        callback = null
    }

    internal fun parsePaymentIntent(intent: Intent): PaymentEvent? {
        val extras = mutableMapOf<String, String?>()
        intent.extras?.keySet()?.forEach { key ->
            extras[key] = intent.extras?.get(key)?.toString()
        }
        listOf(
            "paymentId", "payment_id", "id", "transactionId", "transId",
            "orderId", "order_id", "amount", "currency", "merchantId", "merchant_id",
            "tenderType", "payType", "paymentMethod", "cardType",
        ).forEach { key ->
            extras[key] = intent.getStringExtra(key) ?: extras[key]
        }
        return CloverPaymentParser.parseExtras(extras)
    }
}

object PaymentCaptureFactory {
    fun isCloverDevice(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        if (manufacturer.contains("clover", ignoreCase = true)) return true
        if (model.contains("clover", ignoreCase = true)) return true
        return runCatching {
            context.packageManager.getPackageInfo("com.clover.engine", 0)
            true
        }.getOrDefault(false)
    }

    fun createPrimary(context: Context): PaymentCapture {
        return if (isCloverDevice(context.applicationContext)) {
            CloverPaymentCapture(context.applicationContext)
        } else {
            DebugPaymentCapture()
        }
    }
}

/**
 * Emulator / dev devices — payments triggered via service simulate action.
 */
class DebugPaymentCapture : PaymentCapture {
    override val sourceName: String = "debug_simulator"

    private var callback: ((PaymentEvent) -> Unit)? = null

    override fun start(onPayment: (PaymentEvent) -> Unit) {
        callback = onPayment
    }

    override fun stop() {
        callback = null
    }

    fun simulatePayment(event: PaymentEvent = PaymentEvent.simulated()) {
        callback?.invoke(event)
    }
}
