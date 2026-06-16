# Optional local AAR override

Clover SDK is pulled from Maven Central by default:

```kotlin
implementation("com.clover.sdk:clover-android-sdk:323")
implementation("com.clover.sdk:clover-android-connector-sdk:323")
```

To pin a specific GitHub release AAR instead:

1. Download from https://github.com/clover/clover-android-sdk/releases
2. Place in this folder
3. Replace Maven lines in `app/build.gradle.kts` with `implementation(files("libs/clover-android-sdk.aar"))`

Payment capture is wired in:

- `CloverPaymentCapture.kt` — broadcast + Payment parcel
- `CloverSdkPaymentCapture.kt` — `PaymentConnector.onSaleResponse()`

See `docs/SDK_SETUP.md`.
