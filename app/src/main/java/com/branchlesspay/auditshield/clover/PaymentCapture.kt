package com.branchlesspay.auditshield.clover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.clover.sdk.v1.Intents

/**
 * Listens for Clover payment broadcasts on Clover hardware (Register sales).
 * Parses [com.clover.sdk.v3.payments.Payment] parcels via [CloverPaymentMapper].
 */
class CloverPaymentCapture(
    private val context: Context,
) : PaymentCapture {
    companion object {
        private const val TAG = "CloverPaymentCapture"
        val CLOVER_ACTIONS = listOf(
            Intents.ACTION_PAYMENT_PROCESSED,
            "com.clover.payment.action.COMPLETE",
            "com.clover.sdk.intent.action.PAYMENT",
            "com.clover.intent.action.ACTION_PAYMENT",
        )
    }

    override val sourceName: String = "clover_broadcast"

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
        Log.i(TAG, "Clover broadcast listener active on ${DeviceInfo.displayModel()}")
    }

    override fun stop() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
        callback = null
    }

    internal fun parsePaymentIntent(intent: Intent): PaymentEvent? =
        CloverPaymentMapper.fromIntent(intent)
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
        val appContext = context.applicationContext
        return if (isCloverDevice(appContext)) {
            CompositePaymentCapture(
                listOf(
                    CloverPaymentCapture(appContext),
                    CloverSdkPaymentCapture(appContext),
                ),
            )
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
