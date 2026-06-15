package com.branchlesspay.auditshield.clover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.branchlesspay.auditshield.clover.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val anchorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BpAuditService.BROADCAST_ANCHOR_RESULT) return
            showPaymentResult(intent)
        }
    }

    private val queueReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BpAuditService.BROADCAST_QUEUE_STATS) return
            updateQueueStats(intent.getIntExtra("pending", 0), intent.getIntExtra("anchored", 0))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.deviceInfoText.text = getString(
            R.string.device_info,
            DeviceInfo.model(),
            DeviceInfo.serial(this),
        )

        startAuditService()

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        binding.testAnchorButton.setOnClickListener {
            sendTestAnchor()
        }

        binding.simulatePaymentButton.setOnClickListener {
            if (!Prefs.isConfigured(this)) {
                binding.statusText.text = getString(R.string.status_error, "Configure license key first")
                return@setOnClickListener
            }
            binding.statusText.text = getString(R.string.status_testing)
            startService(
                Intent(this, BpAuditService::class.java).apply {
                    action = BpAuditService.ACTION_SIMULATE_PAYMENT
                },
            )
        }

        binding.flushQueueButton.setOnClickListener {
            startService(
                Intent(this, BpAuditService::class.java).apply {
                    action = BpAuditService.ACTION_FLUSH_QUEUE
                },
            )
            binding.statusText.text = getString(R.string.status_testing)
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(BpAuditService.BROADCAST_ANCHOR_RESULT)
            addAction(BpAuditService.BROADCAST_QUEUE_STATS)
        }
        ContextCompat.registerReceiver(this, anchorReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(this, queueReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        unregisterReceiver(anchorReceiver)
        unregisterReceiver(queueReceiver)
        super.onPause()
    }

    private fun startAuditService() {
        ServiceStarter.startAuditService(this)
    }

    private fun showPaymentResult(intent: Intent) {
        val ok = intent.getBooleanExtra("ok", false)
        val queued = intent.getBooleanExtra("queued_offline", false)
        val verifyUrl = intent.getStringExtra("verify_url")
        val referenceId = intent.getStringExtra("reference_id")
        val error = intent.getStringExtra("error")

        binding.resultText.visibility = View.VISIBLE
        binding.openVerifyButton.visibility = View.GONE
        when {
            ok -> {
                binding.statusText.text = getString(R.string.status_success, 202)
                binding.resultText.text = buildString {
                    append("Payment anchored: ")
                    append(referenceId)
                    append("\n\n")
                    append(getString(R.string.verify_url_label))
                    append(": ")
                    append(verifyUrl ?: "-")
                }
                if (!verifyUrl.isNullOrBlank()) {
                    binding.openVerifyButton.visibility = View.VISIBLE
                    binding.openVerifyButton.setOnClickListener {
                        startActivity(
                            Intent(this@MainActivity, VerifyActivity::class.java).apply {
                                putExtra(VerifyActivity.EXTRA_VERIFY_URL, verifyUrl)
                                putExtra(VerifyActivity.EXTRA_REFERENCE_ID, referenceId)
                                putExtra(VerifyActivity.EXTRA_ANCHOR_ID, intent.getStringExtra("anchor_id"))
                            },
                        )
                    }
                }
            }
            queued -> {
                binding.statusText.text = getString(R.string.status_idle)
                binding.resultText.text = "Payment queued offline: $referenceId\n$error"
            }
            else -> {
                binding.statusText.text = getString(R.string.status_error, error ?: "failed")
                binding.resultText.text = error ?: "Payment anchor failed"
            }
        }
    }

    private fun updateQueueStats(pending: Int, anchored: Int) {
        binding.queueStatsText.text = getString(R.string.queue_stats, pending, anchored)
    }

    private fun sendTestAnchor() {
        if (!Prefs.isConfigured(this)) {
            binding.statusText.text = getString(R.string.status_error, "Configure license key in Settings")
            binding.resultText.visibility = View.VISIBLE
            binding.resultText.text = "Open Settings and save your BP license key first."
            return
        }

        binding.statusText.text = getString(R.string.status_testing)
        binding.resultText.visibility = View.GONE
        binding.testAnchorButton.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val client = BpApiClient(
                    licenseKey = Prefs.getLicenseKey(this@MainActivity),
                    apiUrl = Prefs.getApiUrl(this@MainActivity),
                )
                client.postAnchor(BpAnchorPayload.testTransaction(this@MainActivity))
            }

            binding.testAnchorButton.isEnabled = true
            if (result.ok && result.httpStatus in 200..299) {
                binding.statusText.text = getString(R.string.status_success, result.httpStatus)
                binding.resultText.visibility = View.VISIBLE
                binding.resultText.text = buildString {
                    append(getString(R.string.anchor_success, result.status ?: "queued"))
                    append("\n\nanchor_id: ")
                    append(result.anchorId ?: "-")
                    append("\n\n")
                    append(getString(R.string.verify_url_label))
                    append(": ")
                    append(result.verifyUrl ?: "-")
                }
            } else {
                binding.statusText.text = getString(
                    R.string.status_error,
                    result.error ?: "HTTP ${result.httpStatus}",
                )
                binding.resultText.visibility = View.VISIBLE
                binding.resultText.text = result.rawBody.ifBlank { result.error ?: "Unknown error" }
            }
        }
    }
}
