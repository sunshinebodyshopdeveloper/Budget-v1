package com.sunshine.appsuite.budget.orders.budget

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.ActivityBudgetBinding

/**
 * Actividad para la gestión de presupuestos.
 * Optimizada para tablets siguiendo los lineamientos de Material 3 Expressive.
 */
class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding

    // TODO: Implementar el Adapter cuando se definan los DTOs de presupuesto
    // private val budgetAdapter by lazy { BudgetAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de ViewBinding (Habilitado en build.gradle.kts)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupToolbar()
        setupRecyclerView()
        setupListeners()

        // Ejemplo de carga inicial de datos
        loadBudgetData()
    }

    /**
     * Configuración estética de barras de sistema para Material 3.
     */
    private fun setupEdgeToEdge() {
        enableEdgeToEdge()

        // Ajuste de paddings para no quedar detrás de las barras de navegación/estado
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Estilo de barras de sistema (M3)
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            // El título ya está definido en el XML, pero se puede cambiar dinámicamente
            title = getString(R.string.budget_title)
        }
    }

    private fun setupRecyclerView() {
        binding.rvBudgetItems.apply {
            layoutManager = LinearLayoutManager(this@BudgetActivity)
            // adapter = budgetAdapter
            setHasFixedSize(true)
            // Deshabilitar scroll interno para que NestedScrollView maneje el scroll suave de la Card
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.btnBookNow.setOnClickListener {
            handleApproval()
        }
    }

    private fun loadBudgetData() {
        // Simulación de carga: Aquí conectarías con tu ViewModel/Repository
        binding.tvSubtotal.text = "$1,344.00"
        binding.tvTotalAmount.text = "$1,320.00"
    }

    private fun handleApproval() {
        // Feedback al usuario (puedes usar un MaterialAlertDialog para tablets)
        Toast.makeText(this, "Procesando aprobación...", Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}