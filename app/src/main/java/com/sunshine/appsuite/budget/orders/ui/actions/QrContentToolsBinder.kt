package com.sunshine.appsuite.budget.orders.ui.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.orders.ui.utils.QrContentExportUtils

object QrContentToolsBinder {

    // IDs que NO deben aparecer en la imagen final (share/print)
    private val EXPORT_EXCLUDE_IDS = intArrayOf(
        R.id.contentActions,
        R.id.btnClose,
        R.id.contentTools
    )

    data class Handlers(
        val onCopy: () -> Unit,
        val onShare: () -> Unit,
        val onPrint: () -> Unit,
        val onClose: () -> Unit
    )

    fun bind(root: View, handlers: Handlers) {
        root.onClick(R.id.btnCopy) { handlers.onCopy() }
        root.onClick(R.id.btnShare) { handlers.onShare() }
        root.onClick(R.id.btnPrint) { handlers.onPrint() }
        root.onClick(R.id.btnClose) { handlers.onClose() }
    }

    fun bindDefaults(
        root: View,
        getCode: () -> String,
        onClose: () -> Unit,
        // view a exportar (tu contentGroup)
        getContentView: (() -> View?)? = null,
        onShare: (() -> Unit)? = null,
        onPrint: (() -> Unit)? = null,
        clipboardLabel: String = "OT"
    ) {
        val ctx = root.context

        bind(
            root = root,
            handlers = Handlers(

                onCopy = copy@{
                    val code = getCode().trim()
                    if (code.isBlank()) {
                        ctx.toast(R.string.qr_scanner_invalid_qr_title)
                        return@copy
                    }
                    copyToClipboard(ctx, clipboardLabel, code)
                    ctx.toast(R.string.qr_scanner_ot_toast_copied)
                },

                onShare = share@{
                    // Si el dev pasa una lambda custom, respétala.
                    if (onShare != null) {
                        onShare.invoke()
                        return@share
                    }

                    val content = getContentView?.invoke()
                    if (content == null) {
                        ctx.toast(R.string.qr_scanner_contentTools_share)
                        return@share
                    }

                    // Post para asegurar que el view ya está dibujado.
                    root.post {
                        val ok = QrContentExportUtils.shareView(
                            context = ctx,
                            view = content,
                            chooserTitle = ctx.getString(R.string.qr_scanner_contentTools_share),
                            text = getCode().trim(),
                            fileNamePrefix = "ot_preview",
                            excludeIds = EXPORT_EXCLUDE_IDS,
                            hideMode = View.GONE
                        )

                        if (!ok) ctx.toast(R.string.qr_scanner_contentTools_share)
                    }
                },

                onPrint = print@{
                    if (onPrint != null) {
                        onPrint.invoke()
                        return@print
                    }

                    val content = getContentView?.invoke()
                    if (content == null) {
                        ctx.toast(R.string.qr_scanner_contentTools_print)
                        return@print
                    }

                    root.post {
                        val jobName = getCode().trim().ifBlank { "OT" }
                        val ok = QrContentExportUtils.printView(
                            context = ctx,
                            view = content,
                            jobName = jobName,
                            excludeIds = EXPORT_EXCLUDE_IDS,
                            hideMode = View.GONE
                        )

                        if (!ok) ctx.toast(R.string.qr_scanner_contentTools_print)
                    }
                },

                onClose = onClose
            )
        )
    }

    private fun copyToClipboard(ctx: Context, label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private inline fun View.onClick(id: Int, crossinline action: () -> Unit) {
        findViewById<View>(id)?.setOnClickListener { action() }
    }

    private fun Context.toast(@StringRes resId: Int) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
    }
}
