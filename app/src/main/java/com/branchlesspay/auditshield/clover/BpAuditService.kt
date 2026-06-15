package com.branchlesspay.auditshield.clover

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * M2 — background payment capture, offline SQLite queue, BP anchor sync.
 */
class BpAuditService : Service() {
    companion object {
        private const val TAG = "BpAuditService"
        const val ACTION_START = "com.branchlesspay.auditshield.clover.START"
        const val ACTION_SIMULATE_PAYMENT = "com.branchlesspay.auditshield.clover.SIMULATE_PAYMENT"
        const val ACTION_FLUSH_QUEUE = "com.branchlesspay.auditshield.clover.FLUSH_QUEUE"
        const val BROADCAST_ANCHOR_RESULT = "com.branchlesspay.auditshield.clover.ANCHOR_RESULT"
        const val BROADCAST_QUEUE_STATS = "com.branchlesspay.auditshield.clover.QUEUE_STATS"

        private const val CHANNEL_ID = "bp_audit_service"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var queue: SqliteAnchorQueue
    private lateinit var processor: AnchorProcessor
    private var paymentCapture: PaymentCapture? = null
    private var debugCapture: DebugPaymentCapture? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        queue = SqliteAnchorQueue(this)
        processor = AnchorProcessor(
            queue = queue,
            apiClientFactory = {
                BpApiClient(
                    licenseKey = Prefs.getLicenseKey(this),
                    apiUrl = Prefs.getApiUrl(this),
                )
            },
            isOnline = { NetworkMonitor.isOnline(this) },
        )
        startPaymentListener()
        broadcastQueueStats()
        Log.i(TAG, "M2 service started capture=${paymentCapture?.sourceName}")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SIMULATE_PAYMENT -> {
                debugCapture?.simulatePayment()
                    ?: Log.w(TAG, "Simulate payment only available in debug capture mode")
            }
            ACTION_FLUSH_QUEUE -> backgroundWork { flushQueueInternal() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        paymentCapture?.stop()
        super.onDestroy()
    }

    private fun startPaymentListener() {
        debugCapture = DebugPaymentCapture()
        debugCapture?.start { event -> backgroundWork { handlePayment(event) } }

        val primary = PaymentCaptureFactory.createPrimary(this)
        if (primary !is DebugPaymentCapture) {
            primary.start { event -> backgroundWork { handlePayment(event) } }
            paymentCapture = primary
        } else {
            paymentCapture = debugCapture
        }
    }

    private fun handlePayment(event: PaymentEvent) {
        if (!Prefs.isConfigured(this)) {
            Log.w(TAG, "Payment ignored — BP license key not configured")
            return
        }
        val result = processor.processPayment(
            event = event,
            deviceModel = DeviceInfo.model(),
            deviceSn = DeviceInfo.serial(this),
        )
        if (!result.ok && !result.queuedOffline) {
            processor.flushQueue()
        } else if (result.queuedOffline) {
            Log.i(TAG, "Payment queued offline ref=${result.referenceId}")
        } else {
            Log.i(TAG, "Payment anchored ref=${result.referenceId} url=${result.verifyUrl}")
        }
        sendAnchorResultBroadcast(result)
        broadcastQueueStats()
    }

    private fun flushQueueInternal() {
        val flush = processor.flushQueue()
        Log.i(
            TAG,
            "Queue flush processed=${flush.processed} anchored=${flush.anchored} failed=${flush.failed}",
        )
        broadcastQueueStats()
    }

    private fun backgroundWork(block: () -> Unit) {
        Thread(block).start()
    }

    private fun sendAnchorResultBroadcast(result: AnchorProcessResult) {
        val intent = Intent(BROADCAST_ANCHOR_RESULT).apply {
            setPackage(packageName)
            putExtra("ok", result.ok)
            putExtra("reference_id", result.referenceId)
            putExtra("anchor_id", result.anchorId)
            putExtra("verify_url", result.verifyUrl)
            putExtra("error", result.error)
            putExtra("queued_offline", result.queuedOffline)
        }
        sendBroadcast(intent)
    }

    private fun broadcastQueueStats() {
        val intent = Intent(BROADCAST_QUEUE_STATS).apply {
            setPackage(packageName)
            putExtra("pending", queue.countPending())
            putExtra("anchored", queue.countAnchored())
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BP Audit Shield",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Anchors Clover POS payments to BranchlessPay"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(launchIntent)
            .setOngoing(true)
            .build()
    }
}
