package com.sunshine.appsuite.budget.settings.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sunshine.appsuite.budget.BudgetApp
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.settings.SettingsNavigator
import com.sunshine.appsuite.budget.user.ui.AvatarImageLoader
import com.sunshine.appsuite.budget.databinding.FragmentSettingsAccountBinding
import kotlinx.coroutines.launch

class SettingsAccountFragment : Fragment() {

    private var _binding: FragmentSettingsAccountBinding? = null
    private val binding get() = _binding!!

    private var navigator: SettingsNavigator? = null
    private var host: SettingsAccountHost? = null



    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as? SettingsNavigator

        host = when {
            parentFragment is SettingsAccountHost -> parentFragment as SettingsAccountHost
            context is SettingsAccountHost -> context
            else -> null
        }
    }

    override fun onDetach() {
        super.onDetach()
        navigator = null
        host = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as BudgetApp
        val store = app.userProfileStore

        setupClickListeners()

        // UI reactiva: se actualiza sola cuando cambie el perfil
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.profile.collect { profile ->
                    binding.tvUserName.text = profile?.displayName(getString(R.string.profile_user_name_placeholder))
                    binding.tvUserEmail.text = profile?.displayEmail(getString(R.string.profile_user_email_placeholder))

                    AvatarImageLoader.load(
                        scope = viewLifecycleOwner.lifecycleScope,
                        imageView = binding.ivAvatar,
                        url = profile?.avatarUrl,
                        okHttpClient = app.okHttpClient,
                        placeholderRes = R.drawable.ic_avatar
                    )
                }
            }
        }

        store.ensureFresh()
    }

    private fun setupClickListeners() = with(binding) {

        rowProfilePhoto.setOnClickListener {
            startActivity(Intent(requireContext(), ProfilePhotoActivity::class.java))
        }

        rowProfileName.setOnClickListener {
            EditProfileBottomSheet
                .newInstance(EditProfileBottomSheet.FieldType.NAME)
                .show(parentFragmentManager, "edit_name")
        }

        rowProfileEmail.setOnClickListener {
            EditProfileBottomSheet
                .newInstance(EditProfileBottomSheet.FieldType.EMAIL)
                .show(parentFragmentManager, "edit_email")
        }

        rowProfilePhone.setOnClickListener {
            EditProfileBottomSheet
                .newInstance(EditProfileBottomSheet.FieldType.PHONE)
                .show(parentFragmentManager, "edit_phone")
        }

        rowAccountStatus.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Tu cuenta está verificada por AppSuite.",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardSupport.setOnClickListener { host?.onOpenSupport() }
        cardAbout.setOnClickListener { host?.onOpenAbout() }
        cardLegal.setOnClickListener { host?.onOpenLegal() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface SettingsAccountHost {
        fun onOpenSupport()
        fun onOpenAbout()
        fun onOpenLegal()
    }
}
