package com.sunshine.appsuite.budget.user.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sunshine.appsuite.budget.AppSuiteApp
import com.sunshine.appsuite.budget.NotificationsActivity
import com.sunshine.appsuite.budget.menu.ProfileMenuListener
import com.sunshine.appsuite.budget.settings.SettingsActivity
import com.sunshine.appsuite.budget.settings.appearance.ThemePreferences
import com.sunshine.appsuite.budget.settings.apps.AppsActivity
import com.sunshine.appsuite.budget.update.UpdateActivity
import kotlinx.coroutines.launch

class ProfileBottomSheetDialog : BottomSheetDialogFragment() {

    private var profileMenuListener: ProfileMenuListener? = null

    fun setProfileMenuListener(listener: ProfileMenuListener) {
        profileMenuListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)

        dialog.setOnShowListener { dlg ->
            val bottomSheetDialog = dlg as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet)

            bottomSheet?.let { sheet ->
                sheet.background = ContextCompat.getDrawable(
                    requireContext(),
                    com.sunshine.appsuite.R.drawable.bg_sheet_surface
                )

                val behavior = BottomSheetBehavior.from(sheet)

                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                behavior.skipCollapsed = false
                behavior.isHideable = true

                val displayHeight = resources.displayMetrics.heightPixels
                behavior.peekHeight = (displayHeight * 0.55f).toInt()

                behavior.addBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) { }
                })
            }
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            statusBarColor = ContextCompat.getColor(
                context,
                com.sunshine.appsuite.R.color.google_background_surface
            )
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(com.sunshine.appsuite.R.layout.bottom_sheet_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(com.sunshine.appsuite.R.id.btnClose)?.setOnClickListener { dismiss() }

        // Datos de perfil (SSOT)
        val app = requireActivity().application as AppSuiteApp
        val store = app.userProfileStore

        val tvUserName = view.findViewById<TextView>(com.sunshine.appsuite.R.id.tvUserName)
        val tvUserEmail = view.findViewById<TextView>(com.sunshine.appsuite.R.id.tvUserEmail)
        val ivAvatar = view.findViewById<ImageView>(com.sunshine.appsuite.R.id.ivAvatar)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.profile.collect { profile ->
                    tvUserName.text = profile?.displayName(getString(com.sunshine.appsuite.R.string.profile_user_name_placeholder))
                    tvUserEmail.text = profile?.displayEmail(getString(com.sunshine.appsuite.R.string.profile_user_email_placeholder))

                    // Avatar (si existe URL)
                    AvatarImageLoader.load(
                        scope = viewLifecycleOwner.lifecycleScope,
                        imageView = ivAvatar,
                        url = profile?.avatarUrl,
                        okHttpClient = app.okHttpClient,
                        placeholderRes = com.sunshine.appsuite.R.drawable.ic_avatar
                    )
                }
            }
        }

        store.ensureFresh()

        // 🌙 Row: modo claro/oscuro (texto + ícono)
        val rowNight = view.findViewById<LinearLayout>(com.sunshine.appsuite.R.id.rowNight)
        val themeIcon = view.findViewById<ImageView>(com.sunshine.appsuite.R.id.themeStyleIcon)
        val themeLabel = view.findViewById<TextView>(com.sunshine.appsuite.R.id.themeStyleLabel)

        fun renderThemeRow() {
            val isDark = ThemePreferences.isDarkActive(requireContext())
            if (isDark) {
                themeIcon?.setImageResource(com.sunshine.appsuite.R.drawable.ic_light_mode)
                themeLabel?.setText(com.sunshine.appsuite.R.string.profile_theme_light)
            } else {
                themeIcon?.setImageResource(com.sunshine.appsuite.R.drawable.ic_dark_mode)
                themeLabel?.setText(com.sunshine.appsuite.R.string.profile_theme_dark)
            }
        }

        renderThemeRow()

        rowNight?.setOnClickListener {
            ThemePreferences.toggle(requireContext())
            dismiss() // al cambiar theme, se recrea la activity; mejor cerrar el sheet
        }

        // Logout
        val btnLogout = view.findViewById<Button>(com.sunshine.appsuite.R.id.btnLogout)
        btnLogout?.setOnClickListener {
            profileMenuListener?.onProfileLogout()
            dismiss()
        }

        val cardSettingsAccount = view.findViewById<CardView>(com.sunshine.appsuite.R.id.cardSettingsAccount)
        cardSettingsAccount.setOnClickListener {
            SettingsActivity.Companion.start(requireContext(), SettingsActivity.Section.ACCOUNT)
            dismiss()
        }

        view.findViewById<LinearLayout>(com.sunshine.appsuite.R.id.rowNotifications)?.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
            dismiss()
        }

        view.findViewById<LinearLayout>(com.sunshine.appsuite.R.id.rowApps)?.setOnClickListener {
            startActivity(Intent(requireContext(), AppsActivity::class.java))
            dismiss()
        }

        view.findViewById<LinearLayout>(com.sunshine.appsuite.R.id.rowSecurity)?.setOnClickListener {
            SettingsActivity.Companion.start(requireContext(), SettingsActivity.Section.SECURITY)
            dismiss()
        }

        view.findViewById<LinearLayout>(com.sunshine.appsuite.R.id.rowUpdates)?.setOnClickListener {
            startActivity(Intent(requireContext(), UpdateActivity::class.java))
            dismiss()
        }

        view.findViewById<LinearLayout>(com.sunshine.appsuite.R.id.rowSettings)?.setOnClickListener {
            SettingsActivity.Companion.start(requireContext(), SettingsActivity.Section.HOME)
            dismiss()
        }

        view.findViewById<LinearLayout>(com.sunshine.appsuite.R.id.rowHelp)?.setOnClickListener {
            SettingsActivity.Companion.start(requireContext(), SettingsActivity.Section.SUPPORT)
            dismiss()
        }

        view.findViewById<LinearLayout>(com.sunshine.appsuite.R.id.rowAbout)?.setOnClickListener {
            profileMenuListener?.onProfileOpenAbout()
            dismiss()
        }

        view.findViewById<LinearLayout>(com.sunshine.appsuite.R.id.rowPrivacy)?.setOnClickListener {
            profileMenuListener?.onProfileOpenLegal()
            dismiss()
        }
    }
}