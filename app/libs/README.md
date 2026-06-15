# Clover Android SDK (optional — real device)

For direct payment capture on Clover hardware:

1. Clone or download AAR from https://github.com/clover/clover-android-sdk
2. Copy `clover-android-sdk/clover-android-sdk.aar` → `app/libs/clover-android-sdk.aar`
3. In `app/build.gradle.kts`:

```kotlin
implementation(files("libs/clover-android-sdk.aar"))
```

4. Wire `CloverConnector.onSaleResponse()` in `CloverPaymentCapture.kt` (see `docs/SDK_SETUP.md`)

Until AAR is added, payment capture uses Clover broadcast intents on real devices and **Simulate Payment** on emulator / dev builds.

Webhook enrichment (M1) uses Clover REST API separately on the BP server.
