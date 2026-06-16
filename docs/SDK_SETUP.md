# Clover Android SDK setup

## Prerequisites

- Clover developer account: https://www.clover.com/developers
- App ID (`CLOVER_APP_ID`) + BP license key
- Clover device: **A920 Pro**, **Flex**, or **Mini** (or sandbox devkit)

## Gradle dependencies (Maven Central)

Already in `app/build.gradle.kts`:

```kotlin
implementation("com.clover.sdk:clover-android-sdk:323")
implementation("com.clover.sdk:clover-android-connector-sdk:323")
```

> Maven Central latest is **323** (May 2025). Pin both artifacts to the same version.

## Clover App ID

Set in `local.properties`:

```properties
clover.app.id=YOUR_CLOVER_APP_ID
```

Synced from `Downloads\Clover\.env.txt` via:

```powershell
python scripts/sync_env_from_downloads.py
```

Used as `remoteApplicationId` for `PaymentConnector`.

## Payment capture (wired)

| Source | Class | Trigger |
|--------|-------|---------|
| Register sale broadcast | `CloverPaymentCapture` | `Intents.ACTION_PAYMENT_PROCESSED` |
| SDK Payment parcel | `CloverPaymentMapper.fromIntent()` | `Intents.EXTRA_PAYMENT` |
| Connector callback | `CloverSdkPaymentCapture` | `IPaymentConnectorListener.onSaleResponse()` |
| Emulator | `DebugPaymentCapture` | Simulate Payment button |

`CompositePaymentCapture` deduplicates by payment ID.

## Webhook vs on-device

| Path | Event type | Amount source |
|------|------------|---------------|
| BP webhook + M1 worker | `clover_payment_created` | Clover REST API |
| M2 Android APK | `clover_payment` | Device sale response |

Both use cents internally on Clover; BP anchor `amount` is dollars for USD.

## Permissions

`AndroidManifest.xml` includes `GET_ACCOUNTS` for `CloverAccount.getAccount()`.
