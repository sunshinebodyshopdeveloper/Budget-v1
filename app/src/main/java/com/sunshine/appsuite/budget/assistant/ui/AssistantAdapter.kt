package com.sunshine.appsuite.budget.assistant.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.CornerFamily
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.ItemChatMessageBinding

class AssistantAdapter : RecyclerView.Adapter<AssistantAdapter.ChatViewHolder>() {

    private val messages: MutableList<ChatMessage> = mutableListOf()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateMessageAt(position: Int, newText: String) {
        if (position in messages.indices) {
            messages[position] = messages[position].copy(text = newText)
            notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("mensaje", text))
        Toast.makeText(context, "Copiado ✅", Toast.LENGTH_SHORT).show()
    }

    private fun attachCopyHandlers(targetView: View, rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) {
            targetView.setOnClickListener(null)
            targetView.setOnLongClickListener(null)
            targetView.isClickable = false
            targetView.isLongClickable = false
            return
        }

        targetView.isClickable = true
        targetView.isLongClickable = true

        // Tap para copiar
        targetView.setOnClickListener {
            copyToClipboard(targetView.context, text)
        }

        // Long press por si lo quieres más intencional
        targetView.setOnLongClickListener {
            copyToClipboard(targetView.context, text)
            true
        }
    }

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val context = binding.root.context

            binding.tvMessage.text = message.text

            // El click va sobre la burbuja (cardMessage). Si prefieres solo en el texto,
            // cambia a: attachCopyHandlers(binding.tvMessage, message.text)
            attachCopyHandlers(binding.cardMessage, message.text)

            val constraintSet = ConstraintSet().apply { clone(binding.root) }

            if (message.isUser) {
                applyUserStyle(context, constraintSet)
            } else {
                applyAssistantStyle(context, constraintSet)
            }

            constraintSet.applyTo(binding.root)
        }

        private fun applyUserStyle(context: Context, constraintSet: ConstraintSet) {
            // Alineación a la derecha
            constraintSet.connect(R.id.cardMessage, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(R.id.cardMessage, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.setHorizontalBias(R.id.cardMessage, 1.0f)

            binding.cardMessage.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.menu_chip_bg_selected)
            )
            binding.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.google_black))

            // setAllCorners() primero y luego el override, si no se pisa
            binding.cardMessage.shapeAppearanceModel = binding.cardMessage.shapeAppearanceModel.toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, 20f)
                .setBottomRightCorner(CornerFamily.ROUNDED, 4f)
                .build()
        }

        private fun applyAssistantStyle(context: Context, constraintSet: ConstraintSet) {
            // Alineación a la izquierda
            constraintSet.connect(R.id.cardMessage, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(R.id.cardMessage, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.setHorizontalBias(R.id.cardMessage, 0.0f)

            binding.cardMessage.setCardBackgroundColor(ContextCompat.getColor(context, R.color.google_white))
            binding.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.google_black))

            // setAllCorners() primero y luego el override, si no se pisa
            binding.cardMessage.shapeAppearanceModel = binding.cardMessage.shapeAppearanceModel.toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, 20f)
                .setBottomLeftCorner(CornerFamily.ROUNDED, 4f)
                .build()
        }
    }
}
