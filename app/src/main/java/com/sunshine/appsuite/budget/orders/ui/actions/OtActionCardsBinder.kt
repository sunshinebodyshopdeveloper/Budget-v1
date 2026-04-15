package com.sunshine.appsuite.budget.orders.ui.actions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.card.MaterialCardView
import com.sunshine.appsuite.budget.R

object OtActionCardsBinder {

    data class Handlers(
        val onOrderDetail: () -> Unit,
        val onCallClient: () -> Unit,
        val onWhatsApp: () -> Unit
    )

    /**
     * Placeholder actions (Toasts). Útil mientras conectamos funciones reales.
     */
    fun bindPlaceholders(root: View) {
        val ctx = root.context
        bind(
            root,
            Handlers(
                onOrderDetail = { ctx.toast(R.string.qr_scanner_toast_order_details) },
                onCallClient = { ctx.toast(R.string.qr_scanner_toast_call_client) },
                onWhatsApp = { ctx.toast(R.string.qr_scanner_toast_whatsapp_client) }
            )
        )
    }

    /**
     * Acciones reales (lambdas). Aquí conectas OrderActivity / llamada / WhatsApp.
     */
    fun bind(root: View, handlers: Handlers) {
        root.onCardClick(R.id.orderDetail) { handlers.onOrderDetail() }
        root.onCardClick(R.id.orderTel) { handlers.onCallClient() }
        root.onCardClick(R.id.orderWhatsapp) { handlers.onWhatsApp() }
    }

    /**
     * Uso el teléfono del OrderDto (contactPhone) sin cargar el BottomSheet.
     * Paso getters para que el click siempre agarre el valor actualizado.
     */
    fun bindOrderActions(
        root: View,
        getCode: () -> String,
        getContactPhone: () -> String?,
        onOrderDetail: (() -> Unit)? = null
    ) {
        val ctx = root.context

        root.onCardClick(R.id.orderDetail) {
            (onOrderDetail ?: { ctx.toast(R.string.qr_scanner_toast_order_details) }).invoke()
        }

        root.onCardClick(R.id.orderTel) {
            val telUri = PhoneMx.toDialUri(getContactPhone())
            if (telUri == null) {
                ctx.toast(R.string.qr_scanner_toast_call_client_missing)
                return@onCardClick
            }
            ctx.startActivitySafely(Intent(Intent.ACTION_DIAL, telUri))
        }

        root.onCardClick(R.id.orderWhatsapp) {
            val waNumber = PhoneMx.toWhatsAppNumber(getContactPhone())
            if (waNumber == null) {
                ctx.toast(R.string.qr_scanner_toast_whatsapp_missing)
                return@onCardClick
            }

            val code = getCode().trim()
            val msg = if (code.isBlank()) {
                ctx.getString(R.string.qr_scanner_whatsapp_message_no_code)
            } else {
                ctx.getString(R.string.qr_scanner_whatsapp_message_with_code, code)
            }

            val waUri = Uri.parse("https://wa.me/$waNumber?text=${Uri.encode(msg)}")

            // Intento WhatsApp normal, luego Business, luego fallback sin paquete.
            val opened = ctx.openUrlPreferPackages(
                waUri,
                preferredPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
            )

            if (!opened) {
                ctx.toast(R.string.qr_scanner_toast_whatsapp_not_installed)
            }
        }
    }

    // ---------------------------
    // Helpers
    // ---------------------------

    private inline fun View.onCardClick(id: Int, crossinline action: () -> Unit) {
        findViewById<MaterialCardView>(id)?.setOnClickListener { action() }
    }

    private fun Context.toast(@StringRes resId: Int) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
    }

    /**
     * Abre una URL probando paquetes preferidos (si existen) y luego sin paquete.
     * Esto evita falsos "no instalado" en Android 11+ cuando usas resolveActivity().
     */
    private fun Context.openUrlPreferPackages(
        uri: Uri,
        preferredPackages: List<String>
    ): Boolean {
        // 1) Preferidos
        for (pkg in preferredPackages) {
            val ok = startActivitySafely(
                Intent(Intent.ACTION_VIEW, uri).apply { setPackage(pkg) }
            )
            if (ok) return true
        }

        // 2) Fallback (sin paquete)
        return startActivitySafely(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun Context.startActivitySafely(intent: Intent): Boolean {
        return try {
            // Si el contexto no es Activity, necesitas NEW_TASK sí o sí.
            if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Normalización simple para MX:
     * - ACTION_DIAL: tel:+52XXXXXXXXXX
     * - wa.me: 52XXXXXXXXXX (sin '+', sin el viejo '1')
     */
    private object PhoneMx {

        fun toDialUri(raw: String?): Uri? {
            val mx10 = normalizeMx10(raw) ?: return null
            return Uri.parse("tel:$mx10")
        }

        fun toWhatsAppNumber(raw: String?): String? {
            val mx10 = normalizeMx10(raw) ?: return null
            return "52$mx10"
        }

        private fun normalizeMx10(raw: String?): String? {
            val digits = raw?.trim()?.filter { it.isDigit() }.orEmpty()
            if (digits.isBlank()) return null

            // Quitar prefijos comunes viejos (MX)
            val withoutCarrierPrefix = when {
                digits.startsWith("044") || digits.startsWith("045") -> digits.drop(3)
                digits.startsWith("01") -> digits.drop(2)
                else -> digits
            }

            // Si viene con país incluido, quedarnos con últimos 10
            // Ej: 52 + 10 dígitos = 12
            if (withoutCarrierPrefix.startsWith("52") && withoutCarrierPrefix.length >= 12) {
                return withoutCarrierPrefix.takeLast(10)
            }

            // Si viene con 521 (vieja costumbre), igual últimos 10
            if (withoutCarrierPrefix.startsWith("521") && withoutCarrierPrefix.length >= 13) {
                return withoutCarrierPrefix.takeLast(10)
            }

            // Local 10 dígitos
            if (withoutCarrierPrefix.length == 10) return withoutCarrierPrefix

            // Si viene más largo, últimos 10
            return if (withoutCarrierPrefix.length > 10) withoutCarrierPrefix.takeLast(10) else null
        }
    }
}
