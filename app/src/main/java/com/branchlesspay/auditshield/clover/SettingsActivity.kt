package com.branchlesspay.auditshield.clover

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.branchlesspay.auditshield.clover.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.licenseKeyInput.setText(Prefs.getLicenseKey(this).ifEmpty {
            if (BuildConfig.DEFAULT_LICENSE_KEY.isNotBlank()) {
                BuildConfig.DEFAULT_LICENSE_KEY
            } else {
                ""
            }
        })
        binding.apiUrlInput.setText(Prefs.getApiUrl(this))

        binding.appVersionText.text = getString(
            R.string.app_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
        )

        binding.privacyPolicyButton.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url))),
            )
        }

        binding.saveButton.setOnClickListener {
            Prefs.save(
                this,
                binding.licenseKeyInput.text?.toString().orEmpty(),
                binding.apiUrlInput.text?.toString().orEmpty(),
            )
            binding.settingsStatusText.text = getString(R.string.status_idle)
        }

        binding.testConnectionButton.setOnClickListener {
            testConnection()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun testConnection() {
        val licenseKey = binding.licenseKeyInput.text?.toString().orEmpty().trim()
        val apiUrl = binding.apiUrlInput.text?.toString().orEmpty().trim()
        Prefs.save(this, licenseKey, apiUrl)

        if (licenseKey.isEmpty()) {
            binding.settingsStatusText.text = getString(R.string.status_error, "License key required")
            return
        }

        binding.settingsStatusText.text = getString(R.string.status_testing)
        binding.testConnectionButton.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                BpApiClient(licenseKey = licenseKey, apiUrl = apiUrl)
                    .postAnchor(BpAnchorPayload.testTransaction(this@SettingsActivity))
            }

            binding.testConnectionButton.isEnabled = true
            binding.settingsStatusText.text = if (result.ok && result.httpStatus in 200..299) {
                getString(R.string.status_success, result.httpStatus)
            } else {
                getString(R.string.status_error, result.error ?: "HTTP ${result.httpStatus}")
            }
        }
    }
}
