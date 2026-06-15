package com.branchlesspay.auditshield.clover

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.branchlesspay.auditshield.clover.databinding.ActivityTransactionHistoryBinding

class TransactionHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var queue: SqliteAnchorQueue
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.transaction_history)

        queue = SqliteAnchorQueue(this)
        adapter = TransactionAdapter { item -> openItem(item) }
        binding.historyRecycler.layoutManager = LinearLayoutManager(this)
        binding.historyRecycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadHistory() {
        val items = queue.recent(limit = 100)
        adapter.submitList(items)
        binding.historyEmptyText.visibility =
            if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun openItem(item: QueuedAnchor) {
        val verifyUrl = item.verifyUrl ?: TransactionFormatter.buildVerifyUrl(item.anchorId)
        if (item.status == QueueStatus.ANCHORED && !verifyUrl.isNullOrBlank()) {
            startActivity(
                Intent(this, VerifyActivity::class.java).apply {
                    putExtra(VerifyActivity.EXTRA_VERIFY_URL, verifyUrl)
                    putExtra(VerifyActivity.EXTRA_REFERENCE_ID, item.referenceId)
                    putExtra(VerifyActivity.EXTRA_ANCHOR_ID, item.anchorId)
                },
            )
            return
        }
        Toast.makeText(this, getString(R.string.history_pending_hint), Toast.LENGTH_SHORT).show()
    }
}
