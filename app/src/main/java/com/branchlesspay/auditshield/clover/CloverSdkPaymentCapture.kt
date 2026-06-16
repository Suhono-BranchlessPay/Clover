package com.branchlesspay.auditshield.clover

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.clover.connector.sdk.v3.PaymentConnector
import com.clover.sdk.util.CloverAccount
import com.clover.sdk.v3.connector.IPaymentConnectorListener
import com.clover.sdk.v3.remotepay.AuthResponse
import com.clover.sdk.v3.remotepay.CapturePreAuthResponse
import com.clover.sdk.v3.remotepay.CloseoutResponse
import com.clover.sdk.v3.remotepay.ConfirmPaymentRequest
import com.clover.sdk.v3.remotepay.ManualRefundResponse
import com.clover.sdk.v3.remotepay.PreAuthResponse
import com.clover.sdk.v3.remotepay.ReadCardDataResponse
import com.clover.sdk.v3.remotepay.RefundPaymentResponse
import com.clover.sdk.v3.remotepay.RetrievePaymentResponse
import com.clover.sdk.v3.remotepay.RetrievePendingPaymentsResponse
import com.clover.sdk.v3.remotepay.SaleResponse
import com.clover.sdk.v3.remotepay.TipAdded
import com.clover.sdk.v3.remotepay.TipAdjustAuthResponse
import com.clover.sdk.v3.remotepay.VaultCardResponse
import com.clover.sdk.v3.remotepay.VerifySignatureRequest
import com.clover.sdk.v3.remotepay.VoidPaymentRefundResponse
import com.clover.sdk.v3.remotepay.VoidPaymentResponse

/**
 * Passive listener via [PaymentConnector] — fires [IPaymentConnectorListener.onSaleResponse]
 * for connector-initiated sales on Clover hardware (A920 Pro, Flex, Mini).
 */
class CloverSdkPaymentCapture(
    private val context: Context,
) : PaymentCapture {
    companion object {
        private const val TAG = "CloverSdkCapture"
    }

    override val sourceName: String = "clover_connector"

    private var callback: ((PaymentEvent) -> Unit)? = null
    private var paymentConnector: PaymentConnector? = null

    override fun start(onPayment: (PaymentEvent) -> Unit) {
        stop()
        callback = onPayment

        val appId = BuildConfig.CLOVER_APP_ID.trim()
        if (appId.isEmpty()) {
            Log.w(TAG, "CLOVER_APP_ID not configured — connector listener skipped")
            return
        }

        val account: Account? = CloverAccount.getAccount(context)
        if (account == null) {
            Log.w(TAG, "No Clover account on device — connector listener skipped")
            return
        }

        val listener = object : IPaymentConnectorListener {
            override fun onSaleResponse(response: SaleResponse) {
                val event = CloverPaymentMapper.fromSaleResponse(response) ?: return
                Log.i(
                    TAG,
                    "onSaleResponse tx=${event.transactionId} cents=${event.amountCents}",
                )
                callback?.invoke(event)
            }

            override fun onConfirmPaymentRequest(request: ConfirmPaymentRequest) {
                paymentConnector?.acceptPayment(request.payment)
            }

            override fun onVerifySignatureRequest(request: VerifySignatureRequest) {
                paymentConnector?.acceptSignature(request)
            }

            override fun onDeviceConnected() {
                Log.i(TAG, "Clover PaymentConnector connected")
            }

            override fun onDeviceDisconnected() {
                Log.w(TAG, "Clover PaymentConnector disconnected")
            }

            override fun onPreAuthResponse(response: PreAuthResponse) = Unit
            override fun onAuthResponse(response: AuthResponse) = Unit
            override fun onTipAdjustAuthResponse(response: TipAdjustAuthResponse) = Unit
            override fun onCapturePreAuthResponse(response: CapturePreAuthResponse) = Unit
            override fun onManualRefundResponse(response: ManualRefundResponse) = Unit
            override fun onRefundPaymentResponse(response: RefundPaymentResponse) = Unit
            override fun onVoidPaymentResponse(response: VoidPaymentResponse) = Unit
            override fun onVoidPaymentRefundResponse(response: VoidPaymentRefundResponse) = Unit
            override fun onRetrievePaymentResponse(response: RetrievePaymentResponse) = Unit
            override fun onRetrievePendingPaymentsResponse(
                retrievePendingPaymentResponse: RetrievePendingPaymentsResponse,
            ) = Unit
            override fun onReadCardDataResponse(response: ReadCardDataResponse) = Unit
            override fun onVaultCardResponse(response: VaultCardResponse) = Unit
            override fun onCloseoutResponse(response: CloseoutResponse) = Unit
            override fun onTipAdded(tipAdded: TipAdded) = Unit
        }

        paymentConnector = PaymentConnector(context, account, listener, appId).also {
            it.initializeConnection()
            Log.i(TAG, "PaymentConnector initialized appId=$appId")
        }
    }

    override fun stop() {
        paymentConnector?.dispose()
        paymentConnector = null
        callback = null
    }
}
