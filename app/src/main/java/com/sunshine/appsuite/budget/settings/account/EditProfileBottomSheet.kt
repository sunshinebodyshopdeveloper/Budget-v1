package com.sunshine.appsuite.budget.settings.account

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sunshine.appsuite.budget.AppSuiteApp
import com.sunshine.appsuite.budget.user.model.UserProfile
import com.sunshine.appsuite.databinding.BottomSheetEditProfileBinding
import kotlinx.coroutines.launch

class EditProfileBottomSheet : BottomSheetDialogFragment() {

    enum class FieldType { NAME, EMAIL, PHONE, USERNAME }

    companion object {
        private const val ARG_FIELD_TYPE = "arg_field_type"
        const val REQUEST_KEY_PROFILE_UPDATED = "request_profile_updated"

        fun newInstance(fieldType: FieldType): EditProfileBottomSheet {
            return EditProfileBottomSheet().apply {
                arguments = bundleOf(ARG_FIELD_TYPE to fieldType.name)
            }
        }
    }

    private var _binding: BottomSheetEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var fieldType: FieldType
    private var isBusy = false

    private val store by lazy {
        (requireActivity().application as AppSuiteApp).userProfileStore
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val typeName = requireArguments().getString(ARG_FIELD_TYPE)
        fieldType = try {
            FieldType.valueOf(typeName ?: FieldType.NAME.name)
        } catch (_: Exception) {
            FieldType.NAME
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()

        // Prefill desde Store (cache/flow). Si aún no existe, refrescamos una vez.
        val cached = store.profile.value
        if (cached != null) {
            setupUi(cached)
        } else {
            loadFromApiOnce()
        }
    }

    private fun setupListeners() = with(binding) {
        btnCancel.setOnClickListener { dismiss() }
        btnSave.setOnClickListener { saveValue() }
        etValue.doAfterTextChanged { tilValue.error = null }
    }

    private fun setLoading(loading: Boolean) = with(binding) {
        isBusy = loading
        btnSave.isEnabled = !loading
        btnCancel.isEnabled = !loading
        etValue.isEnabled = !loading
        btnSave.text = if (loading) "Guardando..." else "Guardar"
    }

    private fun loadFromApiOnce() {
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { store.refresh(force = true) }
                .onSuccess { profile ->
                    setupUi(profile)
                }
                .onFailure { e ->
                    Toast.makeText(
                        requireContext(),
                        "No se pudo cargar tu perfil: ${e.message ?: "error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }

            setLoading(false)
        }
    }

    private fun setupUi(profile: UserProfile) = with(binding) {
        when (fieldType) {
            FieldType.NAME -> {
                tvTitle.text = "Editar nombre"
                tvSubtitle.text = "Actualiza el nombre que se muestra en tu perfil."
                tilValue.hint = "Nombre"
                etValue.setText(profile.name.orEmpty())
                etValue.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }

            FieldType.EMAIL -> {
                tvTitle.text = "Editar correo"
                tvSubtitle.text = "Cambia el correo asociado a tu cuenta."
                tilValue.hint = "Correo"
                etValue.setText(profile.email.orEmpty())
                etValue.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }

            FieldType.PHONE -> {
                tvTitle.text = "Editar teléfono"
                tvSubtitle.text = "Agrega o modifica tu número de contacto."
                tilValue.hint = "Teléfono"
                etValue.setText(profile.phone.orEmpty())
                etValue.inputType = InputType.TYPE_CLASS_PHONE
            }

            FieldType.USERNAME -> {
                tvTitle.text = "Editar usuario"
                tvSubtitle.text = "Actualiza tu nombre de usuario."
                tilValue.hint = "Usuario"
                etValue.setText(profile.username.orEmpty())
                etValue.inputType = InputType.TYPE_CLASS_TEXT
            }
        }
        tilValue.error = null
    }

    private fun saveValue() {
        if (isBusy) return

        val value = binding.etValue.text?.toString()?.trim().orEmpty()
        if (value.isEmpty()) {
            binding.tilValue.error = "Este campo no puede estar vacío."
            return
        }

        if (fieldType == FieldType.EMAIL && !Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            binding.tilValue.error = "Ese correo no se ve válido."
            return
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                when (fieldType) {
                    FieldType.NAME -> store.updateProfile(name = value)
                    FieldType.EMAIL -> store.updateProfile(email = value)
                    FieldType.PHONE -> store.updateProfile(phone = value)
                    FieldType.USERNAME -> store.updateProfile(username = value)
                }
            }.onSuccess {
                // Compat con flujos viejos (aunque ya no es necesario)
                parentFragmentManager.setFragmentResult(REQUEST_KEY_PROFILE_UPDATED, bundleOf())
                dismiss()
            }.onFailure { e ->
                Toast.makeText(
                    requireContext(),
                    "No se pudo actualizar: ${e.message ?: "error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            setLoading(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
