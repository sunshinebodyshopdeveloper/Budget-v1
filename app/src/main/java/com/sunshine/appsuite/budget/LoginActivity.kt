package com.sunshine.appsuite.budget

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.data.network.LoginRequest
import com.sunshine.appsuite.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app: AppSuiteApp get() = application as AppSuiteApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (username.isEmpty()) {
                Toast.makeText(this, "Ingresa el usuario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Ingresa la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doLogin(username, password)
        }
    }

    private fun doLogin(username: String, password: String) {
        lifecycleScope.launch {
            setLoading(true)

            try {
                val deviceName = "Android-" + (Build.MODEL ?: "Device")

                val request = LoginRequest(
                    username = username,
                    password = password,
                    device_name = deviceName
                )

                // /api/token devuelve un String con el token
                val token = withContext(Dispatchers.IO) {
                    app.authApi.login(request)
                }.trim()

                if (token.isNotEmpty()) {
                    // 1) Guardar token
                    app.tokenManager.saveToken(token)

                    // 2) Calentar perfil en background (SSOT). No bloquea el login.
                    app.userProfileStore.ensureFresh(force = true)

                    // 3) Navegar a Main
                    Toast.makeText(
                        this@LoginActivity,
                        "Inicio de sesión exitoso",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error al recibir el token.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Usuario o contraseña incorrectos.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error ${e.code()} al iniciar sesión.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@LoginActivity,
                    "Error al iniciar sesión. Verifica tus datos o tu conexión.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }


    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etUsername.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun setupSystemBars() {
        val color = ContextCompat.getColor(this, R.color.onboarding_bg_1)
        window.statusBarColor = color
        window.navigationBarColor = color

        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    /**
     * Ocultar teclado cuando el usuario toca fuera de los EditText.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let { view ->
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)

                // Si el toque fue fuera del view con foco (etUsername / etPassword)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    view.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
