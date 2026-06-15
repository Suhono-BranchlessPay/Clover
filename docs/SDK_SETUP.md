# Clover Android SDK setup

## Prerequisites

- Clover developer account: https://www.clover.com/developers
- App ID + API token (from Bos via WhatsApp)
- Clover device or sandbox

## Add SDK AAR

1. Download from https://github.com/clover/clover-android-sdk
2. Place AAR in `app/libs/clover-android-sdk.aar`
3. Add to `app/build.gradle.kts`:

```kotlin
implementation(files("libs/clover-android-sdk.aar"))
```

## Wire payment capture

In `CloverPaymentCapture.kt`, connect `CloverConnector`:

```kotlin
// Pseudocode — see Clover SDK docs
connector.addListener(object : IPaymentConnectorListener {
    override fun onSaleResponse(response: SaleResponse) {
        val payment = response.payment ?: return
        val event = PaymentEvent(
            transactionId = payment.id,
            amountCents = payment.amount,
            currency = payment.currency ?: "USD",
            paymentMethod = payment.cardTransaction?.cardType ?: "card",
            merchantId = merchantId,
        )
        callback?.invoke(event)
    }
})
```

## Webhook vs on-device

| Path | Event type | Amount source |
|------|------------|---------------|
| BP webhook + M1 worker | `clover_payment_created` | Clover REST API |
| M2 Android APK | `clover_payment` | Device sale response |

Both use cents internally on Clover; BP anchor `amount` is dollars for USD.
