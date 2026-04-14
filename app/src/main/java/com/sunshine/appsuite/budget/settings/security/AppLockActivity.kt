package com.sunshine.appsuite.budget.settings.security

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.sunshine.appsuite.budget.MainActivity
import com.sunshine.appsuite.R

class AppLockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SKIP_APP_LOCK = "skip_app_lock"
    }

    private enum class UnlockMode {
        NONE,
        BIOMETRIC,
        PIN
    }

    // --- PIN state ---
    private val pinBuffer = StringBuilder()
    private lateinit var pinDots: List<View>
    private var tvPinError: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val biometricEnabled = SecurityPreferences.isBiometricEnabled(this)
        val pinEnabled = SecurityPreferences.isPinEnabled(this)

        val mode = when {
            biometricEnabled -> UnlockMode.BIOMETRIC
            pinEnabled -> UnlockMode.PIN
            else -> UnlockMode.NONE
        }

        if (mode == UnlockMode.NONE) {
            // Nada activo: no tiene sentido estar aquí
            navigateToMain()
            return
        }

        when (mode) {
            UnlockMode.BIOMETRIC -> {
                // Pantalla dedicada para biometría
                setContentView(R.layout.layout_screen_biometric)

                // Botón para reintentar el prompt biométrico
                findViewById<View?>(R.id.btnBiometricRetry)?.setOnClickListener {
                    startBiometricFlow()
                }

                // Lanzamos el prompt al entrar
                startBiometricFlow()
            }
            UnlockMode.PIN -> {
                // Pantalla dedicada para PIN
                setContentView(R.layout.layout_screen_pin)
                setupPinUi()
            }
            else -> Unit
        }
    }

    // ==========================================================
    //   BIOMÉTRICO
    // ==========================================================

    private fun startBiometricFlow() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@AppLockActivity, errString, Toast.LENGTH_SHORT).show()
                    // El botón de retry queda disponible para volver a intentar
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@AppLockActivity,
                        "No se reconoció la huella, intenta de nuevo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear AppSuite")
            .setSubtitle("Usa tu huella para entrar a la app.")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // ==========================================================
    //   PIN
    // ==========================================================

    private fun setupPinUi() {
        tvPinError = findViewById(R.id.tvPinError)
        pinDots = listOf(
            findViewById(R.id.pinDot1),
            findViewById(R.id.pinDot2),
            findViewById(R.id.pinDot3),
            findViewById(R.id.pinDot4)
        )

        // Mapeo de teclas 0–9
        val keyMap: Map<Int, String> = mapOf(
            R.id.btnKey0 to "0",
            R.id.btnKey1 to "1",
            R.id.btnKey2 to "2",
            R.id.btnKey3 to "3",
            R.id.btnKey4 to "4",
            R.id.btnKey5 to "5",
            R.id.btnKey6 to "6",
            R.id.btnKey7 to "7",
            R.id.btnKey8 to "8",
            R.id.btnKey9 to "9",
        )

        keyMap.forEach { (viewId, digit) ->
            findViewById<View>(viewId).setOnClickListener {
                onDigitPressed(digit)
            }
        }

        // Borrar último dígito
        findViewById<View>(R.id.btnKeyClear).setOnClickListener {
            onClearPressed()
        }

        // Botón OK (por si el usuario prefiere confirmar)
        findViewById<View>(R.id.btnKeyOk).setOnClickListener {
            onOkPressed()
        }

        updatePinDots()
    }

    private fun onDigitPressed(digit: String) {
        if (pinBuffer.length >= 4) return

        tvPinError?.visibility = View.GONE
        pinBuffer.append(digit)
        updatePinDots()

        if (pinBuffer.length == 4) {
            // Auto-validación al meter 4 dígitos
            validatePin()
        }
    }

    private fun onClearPressed() {
        if (pinBuffer.isNotEmpty()) {
            pinBuffer.deleteCharAt(pinBuffer.length - 1)
            updatePinDots()
            tvPinError?.visibility = View.GONE
        }
    }

    private fun onOkPressed() {
        if (pinBuffer.length < 4) {
            tvPinError?.apply {
                text = getString(R.string.pin_error)
                visibility = View.VISIBLE
            }
            return
        }
        validatePin()
    }

    private fun updatePinDots() {
        val filled = pinBuffer.length.coerceAtMost(pinDots.size)

        pinDots.forEachIndexed { index, view ->
            val resId = if (index < filled) {
                // Asegúrate de tener este drawable creado
                R.drawable.bg_pin_dot_filled
            } else {
                R.drawable.bg_pin_dot_empty
            }
            view.setBackgroundResource(resId)
        }
    }

    private fun validatePin() {
        val storedPin = loadStoredPin()

        if (!storedPin.isNullOrEmpty() && storedPin == pinBuffer.toString()) {
            // PIN correcto
            navigateToMain()
        } else {
            // Incorrecto → limpiar buffer, resetear dots y mostrar error
            pinBuffer.clear()
            updatePinDots()
            tvPinError?.apply {
                text = getString(R.string.pin_error)
                visibility = View.VISIBLE
            }
        }
    }

    /**
     * Leemos directamente las mismas prefs que usa SecurityPreferences.
     * (PREFS_NAME = "security_prefs", KEY_PIN = "security_pin")
     */
    private fun loadStoredPin(): String? {
        val prefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
        return prefs.getString("security_pin", null)
    }

    // ==========================================================
    //   Navegación / back
    // ==========================================================

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_SKIP_APP_LOCK, true)
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Si se salen desde el lock, se cierra la app
        finish()
    }
}
