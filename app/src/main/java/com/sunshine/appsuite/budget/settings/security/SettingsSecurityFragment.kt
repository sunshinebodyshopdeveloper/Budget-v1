package com.sunshine.appsuite.budget.settings.security

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.FragmentSettingsSecurityBinding

class SettingsSecurityFragment : Fragment() {

    private var _binding: FragmentSettingsSecurityBinding? = null
    private val binding get() = _binding!!

    private var host: SettingsSecurityHost? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        host = when {
            parentFragment is SettingsSecurityHost -> parentFragment as SettingsSecurityHost
            context is SettingsSecurityHost -> context
            else -> null
        }
    }

    override fun onDetach() {
        super.onDetach()
        host = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDeviceInfo()
        setupClicks()
        updateSecurityStatusChips()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --------------------
    // Device info
    // --------------------
    private fun loadDeviceInfo() {
        val ctx = context ?: return
        val info = DeviceInfoHelper.getDeviceInfo(ctx)

        binding.tvDeviceName.text = info.deviceInfo
        binding.tvDeviceVersion.text = info.so
        binding.tvDeviceBattery.text = info.battery
        binding.tvDeviceConnection.text = info.lastConnection
    }

    // --------------------
    // Clicks
    // --------------------
    private fun setupClicks() = with(binding) {

        rowCodeAccess.setOnClickListener {
            val ctx = context ?: return@setOnClickListener

            val pinEnabled = SecurityPreferences.isPinEnabled(ctx)
            val hasPin = !SecurityPreferences.getPin(ctx).isNullOrEmpty()

            if (hasPin && pinEnabled) {
                showPinDisableBottomSheet()
            } else {
                showPinBottomSheet()
            }
        }

        rowFingerPrint.setOnClickListener {
            showBiometricBottomSheet()
        }

        // ✅ SOLO UNA RUTA: delegar al host (SettingsActivity)
        cardSupport.setOnClickListener { host?.onOpenSupport() }
        cardAbout.setOnClickListener { host?.onOpenAbout() }
        cardLegal.setOnClickListener { host?.onOpenLegal() }

        rowAccountStatus.setOnClickListener { host?.onRunSecurityCheck() }
    }

    private fun updateSecurityStatusChips() {
        val ctx = context ?: return

        val pinEnabled = SecurityPreferences.isPinEnabled(ctx)
        val biometricEnabled = SecurityPreferences.isBiometricEnabled(ctx)

        val activeColor = ContextCompat.getColor(ctx, R.color.apps_chip_installed_bg)
        val inactiveColor = ContextCompat.getColor(ctx, R.color.apps_chip_not_installed_bg)

        with(binding) {
            chipPinStatus.text = if (pinEnabled) "Activo" else "Inactivo"
            chipPinStatus.chipBackgroundColor = ColorStateList.valueOf(
                if (pinEnabled) activeColor else inactiveColor
            )

            chipBiometricStatus.text = if (biometricEnabled) "Activo" else "Inactivo"
            chipBiometricStatus.chipBackgroundColor = ColorStateList.valueOf(
                if (biometricEnabled) activeColor else inactiveColor
            )
        }
    }

    // --------------------
    // PIN bottom sheet
    // --------------------
    private fun showPinBottomSheet() {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_security_pin, null)

        val etPin = view.findViewById<TextInputEditText>(R.id.etPin)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvPinSubtitle)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelPin)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSavePin)

        val existingPin = SecurityPreferences.getPin(ctx)
        if (existingPin.isNullOrEmpty()) {
            tvSubtitle.text = "Crea un PIN de 4 dígitos para entrar más rápido a AppSuite."
        } else {
            tvSubtitle.text = "Actualiza tu código PIN de 4 dígitos."
            etPin.setText(existingPin)
            etPin.setSelection(existingPin.length)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val pin = etPin.text?.toString()?.trim().orEmpty()

            if (pin.length != 4 || pin.any { !it.isDigit() }) {
                etPin.error = "Ingresa un PIN de 4 dígitos."
            } else {
                SecurityPreferences.savePin(ctx, pin)
                SecurityPreferences.setPinEnabled(ctx, true)
                SecurityPreferences.setBiometricEnabled(ctx, false)

                Toast.makeText(ctx, "PIN guardado correctamente.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                updateSecurityStatusChips()
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    // --------------------
    // Desactivar PIN bottom sheet
    // --------------------
    private fun showPinDisableBottomSheet() {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_security_pin_disable, null)

        val tvStatus = view.findViewById<TextView>(R.id.tvPinStatus)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelPin)
        val btnDisable = view.findViewById<MaterialButton>(R.id.btnDisablePin)

        val hasPin = !SecurityPreferences.getPin(ctx).isNullOrEmpty()
        val pinEnabled = SecurityPreferences.isPinEnabled(ctx)

        tvStatus.text = when {
            !hasPin -> "No tienes un PIN configurado actualmente."
            !pinEnabled -> "Tienes un PIN guardado, pero está desactivado."
            else -> "Tu PIN de 4 dígitos está activo para acceder a AppSuite."
        }

        btnDisable.isEnabled = hasPin

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnDisable.setOnClickListener {
            SecurityPreferences.setPinEnabled(ctx, false)
            SecurityPreferences.savePin(ctx, "")

            Toast.makeText(ctx, "PIN desactivado correctamente.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            updateSecurityStatusChips()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    // --------------------
    // Huella bottom sheet
    // --------------------
    private fun showBiometricBottomSheet() {
        val ctx = context ?: return

        val biometricManager = BiometricManager.from(ctx)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK

        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                ctx,
                "Tu dispositivo no soporta huella o no tiene huella registrada.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val dialog = BottomSheetDialog(ctx)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_security_biometric, null)

        val tvStatus = view.findViewById<TextView>(R.id.tvBiometricStatus)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelBiometric)
        val btnEnable = view.findViewById<MaterialButton>(R.id.btnEnableBiometric)

        val isEnabled = SecurityPreferences.isBiometricEnabled(ctx)

        tvStatus.text = if (isEnabled) {
            "La huella está actualmente activada para esta cuenta."
        } else {
            "La huella aún no está activada para esta cuenta."
        }

        btnEnable.text = if (isEnabled) "Desactivar huella" else "Activar huella"

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnEnable.setOnClickListener {
            if (isEnabled) {
                SecurityPreferences.setBiometricEnabled(ctx, false)
                Toast.makeText(ctx, "Huella desactivada.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                updateSecurityStatusChips()
            } else {
                dialog.dismiss()
                startBiometricPrompt()
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun startBiometricPrompt() {
        val ctx = context ?: return

        val executor = ContextCompat.getMainExecutor(ctx)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    SecurityPreferences.setBiometricEnabled(ctx, true)

                    Toast.makeText(
                        ctx,
                        "Huella activada correctamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    updateSecurityStatusChips()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(ctx, errString, Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        ctx,
                        "No se reconoce la huella, intenta de nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear con huella")
            .setSubtitle("Usa tu huella para acceder de forma segura a AppSuite.")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    interface SettingsSecurityHost {
        fun onOpenSupport()
        fun onOpenAbout()
        fun onOpenLegal()
        fun onRunSecurityCheck()
    }
}
