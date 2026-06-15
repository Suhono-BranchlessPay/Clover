package com.branchlesspay.auditshield.clover

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.branchlesspay.auditshield.clover.databinding.ActivityVerifyBinding

class VerifyActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_VERIFY_URL = "verify_url"
        const val EXTRA_REFERENCE_ID = "reference_id"
        const val EXTRA_ANCHOR_ID = "anchor_id"
    }

    private lateinit var binding: ActivityVerifyBinding
    private var verifyUrl: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.verify_title)

        verifyUrl = intent.getStringExtra(EXTRA_VERIFY_URL).orEmpty()
        val referenceId = intent.getStringExtra(EXTRA_REFERENCE_ID).orEmpty()
        binding.verifyReferenceText.text = referenceId

        binding.verifyWebView.settings.javaScriptEnabled = true
        binding.verifyWebView.settings.domStorageEnabled = true
        binding.verifyWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.verifyProgress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.verifyProgress.visibility = View.GONE
                binding.verifiedBadge.visibility = View.VISIBLE
            }
        }
        binding.verifyWebView.webChromeClient = WebChromeClient()

        if (verifyUrl.isNotBlank()) {
            binding.verifyWebView.loadUrl(verifyUrl)
        }

        binding.shareVerifyButton.setOnClickListener { shareVerifyUrl() }
        binding.printQrButton.setOnClickListener { printVerifyQr() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun shareVerifyUrl() {
        if (verifyUrl.isBlank()) return
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "BranchlessPay Verify")
                putExtra(Intent.EXTRA_TEXT, verifyUrl)
            },
        )
    }

    private fun printVerifyQr() {
        if (verifyUrl.isBlank()) return
        CloverReceiptHelper.printVerifyQr(this, verifyUrl) { ok, message ->
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (ok) getString(R.string.printer_sent) else message,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
}
