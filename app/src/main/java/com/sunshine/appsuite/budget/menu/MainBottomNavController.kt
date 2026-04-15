package com.sunshine.appsuite.budget.menu

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.card.MaterialCardView
import com.sunshine.appsuite.budget.R

class MainBottomNavController(
    rootView: View,
    private val onTabSelected: (Tab) -> Unit
) {

    enum class Tab { HOME, QR, ASSISTANT }

    private val context = rootView.context

    // Cards
    private val btnHome: MaterialCardView = rootView.findViewById(R.id.bottom_nav_home)
    private val btnQr: MaterialCardView = rootView.findViewById(R.id.bottom_nav_qr)
    private val btnAssistant: MaterialCardView = rootView.findViewById(R.id.bottom_nav_assistant)

    // Labels (con drawableTopCompat)
    private val textHome: TextView = rootView.findViewById(R.id.bottom_nav_label_home)
    private val textQr: TextView = rootView.findViewById(R.id.bottom_nav_label_qr)
    private val textAssistant: TextView = rootView.findViewById(R.id.bottom_nav_label_assistant)

    init {
        btnHome.setOnClickListener {
            setHomeSelected()
            onTabSelected(Tab.HOME)
        }

        btnQr.setOnClickListener {
            setQrSelected()
            onTabSelected(Tab.QR)
        }

        btnAssistant.setOnClickListener {
            setAssistantSelected()
            onTabSelected(Tab.ASSISTANT)
        }
    }

    private fun resetMenuStyle() {
        val defaultBg = ContextCompat.getColor(context, R.color.menu_chip_bg_unselected)
        val defaultText = ContextCompat.getColor(context, R.color.menu_option_unselected)
        val defaultIcon = ContextCompat.getColor(context, R.color.menu_option_unselected)

        listOf(btnHome, btnQr, btnAssistant).forEach { it.setCardBackgroundColor(defaultBg) }
        listOf(textHome, textQr, textAssistant).forEach { applyLabelColors(it, defaultText, defaultIcon) }
    }

    private fun highlight(card: MaterialCardView, label: TextView) {
        val selectedBg = ContextCompat.getColor(context, R.color.menu_chip_bg_selected)
        val selectedText = ContextCompat.getColor(context, R.color.menu_option_selected)
        val selectedIcon = ContextCompat.getColor(context, R.color.menu_option_selected)

        card.setCardBackgroundColor(selectedBg)
        applyLabelColors(label, selectedText, selectedIcon)
    }

    private fun applyLabelColors(label: TextView, textColor: Int, iconTint: Int) {
        label.setTextColor(textColor)
        TextViewCompat.setCompoundDrawableTintList(label, ColorStateList.valueOf(iconTint))
    }

    fun setHomeSelected() {
        resetMenuStyle()
        highlight(btnHome, textHome)
    }

    fun setQrSelected() {
        resetMenuStyle()
        highlight(btnQr, textQr)
    }

    fun setAssistantSelected() {
        resetMenuStyle()
        highlight(btnAssistant, textAssistant)
    }
}
