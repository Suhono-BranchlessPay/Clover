package com.branchlesspay.auditshield.clover

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.branchlesspay.auditshield.clover.databinding.ItemTransactionBinding

class TransactionAdapter(
    private val onItemClick: (QueuedAnchor) -> Unit,
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private val items = mutableListOf<QueuedAnchor>()

    fun submitList(data: List<QueuedAnchor>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemTransactionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: QueuedAnchor) {
            binding.referenceText.text = item.referenceId
            binding.amountText.text = TransactionFormatter.formatAmount(item.payloadJson)
            binding.dateText.text = TransactionFormatter.formatDate(item.createdAt)

            val label = TransactionFormatter.statusLabel(item.status)
            binding.statusBadge.text = label
            applyBadgeColors(item.status)

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun applyBadgeColors(status: QueueStatus) {
            val context = binding.root.context
            val (textColor, bgColor) = when (status) {
                QueueStatus.PENDING -> R.color.status_pending to R.color.status_pending_bg
                QueueStatus.ANCHORED -> R.color.status_anchored to R.color.status_anchored_bg
                QueueStatus.FAILED -> R.color.status_failed to R.color.status_failed_bg
            }
            binding.statusBadge.setTextColor(ContextCompat.getColor(context, textColor))
            val background = GradientDrawable().apply {
                cornerRadius = 999f
                setColor(ContextCompat.getColor(context, bgColor))
            }
            binding.statusBadge.background = background
        }
    }
}
