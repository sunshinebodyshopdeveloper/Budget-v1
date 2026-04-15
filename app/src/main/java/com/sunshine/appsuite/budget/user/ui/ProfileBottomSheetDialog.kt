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
import com.sunshine.appsuite.budget.BudgetApp
import com.sunshine.appsuite.budget.menu.ProfileMenuListener
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
                    com.sunshine.appsuite.budget.R.drawable.bg_sheet_surface
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
                com.sunshine.appsuite.budget.R.color.google_background_surface
            )
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(com.sunshine.appsuite.budget.R.layout.bottom_sheet_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(com.sunshine.appsuite.budget.R.id.btnClose)?.setOnClickListener { dismiss() }

        // Datos de perfil (SSOT)
        val app = requireActivity().application as BudgetApp
        val store = app.userProfileStore

        val tvUserName = view.findViewById<TextView>(com.sunshine.appsuite.budget.R.id.tvUserName)
        val tvUserEmail = view.findViewById<TextView>(com.sunshine.appsuite.budget.R.id.tvUserEmail)
        val ivAvatar = view.findViewById<ImageView>(com.sunshine.appsuite.budget.R.id.ivAvatar)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.profile.collect { profile ->
                    tvUserName.text = profile?.displayName(getString(com.sunshine.appsuite.budget.R.string.profile_user_name_placeholder))
                    tvUserEmail.text = profile?.displayEmail(getString(com.sunshine.appsuite.budget.R.string.profile_user_email_placeholder))

                    // Avatar (si existe URL)
                    AvatarImageLoader.load(
                        scope = viewLifecycleOwner.lifecycleScope,
                        imageView = ivAvatar,
                        url = profile?.avatarUrl,
                        okHttpClient = app.okHttpClient,
                        placeholderRes = com.sunshine.appsuite.budget.R.drawable.ic_avatar
                    )
                }
            }
        }

        store.ensureFresh()


        // Logout
        val btnLogout = view.findViewById<Button>(com.sunshine.appsuite.budget.R.id.btnLogout)
        btnLogout?.setOnClickListener {
            profileMenuListener?.onProfileLogout()
            dismiss()
        }
    }
}