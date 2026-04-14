package com.sunshine.appsuite.budget.tools.appointments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunshine.appsuite.budget.AppSuiteApp
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.tools.appointments.data.network.AppointmentsApi
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.AppointmentDto
import com.sunshine.appsuite.budget.tools.appointments.data.repository.AppointmentsRepository
import com.sunshine.appsuite.budget.tools.appointments.ui.adapter.AppointmentsAdapter
import com.sunshine.appsuite.budget.tools.appointments.ui.create.CreateAppointmentActivity
import com.sunshine.appsuite.budget.tools.appointments.ui.details.AppointmentDetailsBottomSheet
import com.sunshine.appsuite.budget.tools.appointments.ui.mapper.AppointmentMapper
import com.sunshine.appsuite.budget.tools.appointments.ui.model.AppointmentUi
import com.sunshine.appsuite.budget.tools.appointments.util.AppointmentNotificationHelper
import com.sunshine.appsuite.databinding.ActivityAppointmentsBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AppointmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppointmentsBinding

    private val adapter by lazy {
        AppointmentsAdapter { item -> openAppointmentDetails(item) }
    }

    private lateinit var repo: AppointmentsRepository
    private var allAppointments: List<AppointmentUi> = emptyList()

    private val mxZone: ZoneId = ZoneId.of("America/Monterrey")

    private val serverLocalFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    )

    private enum class ListState { LOADING, CONTENT, EMPTY, ERROR }

    // ✅ Abrir CreateAppointmentActivity y refrescar al regresar
    private val createAppointmentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadAppointments()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupToolbar()
        setupRecycler()
        setupSwipeRefresh()
        setupChips()
        setupRepository()
        setupFab()

        binding.tvAppointmentsToday.text = formatTodayLong()
        binding.tvAppointmentsTotal.text = getString(R.string.appointments_total_placeholder, 0)

        // Si confirmas/cambias algo en el BottomSheet, refresca lista
        supportFragmentManager.setFragmentResultListener(
            AppointmentDetailsBottomSheet.Companion.RESULT_KEY_UPDATED,
            this
        ) { _, _ ->
            loadAppointments()
        }

        // Si viene desde una notificación (o deep-link), abre el detalle directo
        handleOpenFromIntent(intent)

        loadAppointments()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenFromIntent(intent)
    }

    private fun setupRepository() {
        val app = application as AppSuiteApp
        val api = app.retrofit.create(AppointmentsApi::class.java)
        repo = AppointmentsRepository(api)
    }

    private fun setupFab() {
        binding.fabCreateAppointment.setOnClickListener {
            val intent = Intent(this, CreateAppointmentActivity::class.java)
            createAppointmentLauncher.launch(intent)
        }
    }

    private fun setupSystemBars() {
        /*val surfaceColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorSurface,
            ContextCompat.getColor(this, R.color.google_white)
        )*/

        val status = ContextCompat.getColor(this, R.color.google_background_settings)
        val nav = ContextCompat.getColor(this, R.color.google_white)

        //window.statusBarColor = surfaceColor
        //window.navigationBarColor = surfaceColor

        window.statusBarColor = status
        window.navigationBarColor = nav

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecycler() {
        binding.recyclerAppointmentsList.layoutManager = LinearLayoutManager(this)
        binding.recyclerAppointmentsList.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshAppointments.setOnRefreshListener { loadAppointments() }
    }

    private fun setupChips() {
        binding.chipAll.isChecked = true
        updateChipLabels()

        binding.cgPurpose.setOnCheckedStateChangeListener { _, _ ->
            applyPurposeFilter(currentFilter())
        }
    }

    private fun currentFilter(): AppointmentType? {
        return when {
            binding.chipPresupuesto.isChecked -> AppointmentType.PRESUPUESTO
            binding.chipIngreso.isChecked -> AppointmentType.INGRESO
            binding.chipEntrega.isChecked -> AppointmentType.ENTREGA
            else -> null
        }
    }

    private fun loadAppointments() {
        setListState(ListState.LOADING)

        lifecycleScope.launch {
            try {
                repo.fetchAppointments()
                    .onSuccess { dtos ->
                        // ✅ SOLO HOY (zona America/Monterrey)
                        val todayDtos = dtos.filter { isDtoForToday(it) }
                        val listToday = todayDtos.map { AppointmentMapper.toUi(it) }
                        onAppointmentsLoaded(listToday)
                    }
                    .onFailure { err ->
                        err.printStackTrace()

                        // Si no hay nada cargado todavía, muestra estado de error
                        if (allAppointments.isEmpty()) {
                            setListState(ListState.ERROR)
                            showToast(getString(R.string.appointments_error_loading))
                        } else {
                            // Si ya había data, no "rompas" la pantalla, solo avisa
                            showToast(getString(R.string.appointments_error_loading))
                            applyPurposeFilter(currentFilter())
                        }
                    }
            } finally {
                binding.swipeRefreshAppointments.isRefreshing = false
            }
        }
    }

    private fun onAppointmentsLoaded(list: List<AppointmentUi>) {
        allAppointments = list

        // Contador base (luego applyPurposeFilter lo ajusta si estás filtrando)
        binding.tvAppointmentsTotal.text =
            getString(R.string.appointments_total_placeholder, allAppointments.size)

        updateChipLabels()
        applyPurposeFilter(currentFilter())
    }

    private fun applyPurposeFilter(filter: AppointmentType?) {
        val filtered = when (filter) {
            null -> allAppointments
            else -> allAppointments.filter { it.type == filter }
        }

        adapter.submitList(filtered)

        // Mantén el contador alineado a lo que estás viendo
        binding.tvAppointmentsTotal.text =
            getString(R.string.appointments_total_placeholder, filtered.size)

        // Estado visual: lista vs vacío
        if (filtered.isEmpty()) {
            binding.tvAppointmentsEmpty.text = getString(R.string.appointments_empty)
            setListState(ListState.EMPTY)
        } else {
            setListState(ListState.CONTENT)
        }
    }

    private fun setListState(state: ListState) {
        when (state) {
            ListState.LOADING -> {
                binding.swipeRefreshAppointments.isRefreshing = true
                // Conserva lo que ya tenías en pantalla, solo oculta el mensaje vacío si estaba
                binding.tvAppointmentsEmpty.isVisible = false
                binding.recyclerAppointmentsList.isVisible = true
            }

            ListState.CONTENT -> {
                binding.tvAppointmentsEmpty.isVisible = false
                binding.recyclerAppointmentsList.isVisible = true
            }

            ListState.EMPTY -> {
                binding.recyclerAppointmentsList.isVisible = false
                binding.tvAppointmentsEmpty.isVisible = true
            }

            ListState.ERROR -> {
                binding.recyclerAppointmentsList.isVisible = false
                binding.tvAppointmentsEmpty.text = getString(R.string.appointments_error_loading)
                binding.tvAppointmentsEmpty.isVisible = true
            }
        }
    }

    private fun updateChipLabels() {
        val total = allAppointments.size
        val p = allAppointments.count { it.type == AppointmentType.PRESUPUESTO }
        val i = allAppointments.count { it.type == AppointmentType.INGRESO }
        val e = allAppointments.count { it.type == AppointmentType.ENTREGA }

        binding.chipAll.text = getString(R.string.appointments_filter_all, total)
        binding.chipPresupuesto.text = getString(R.string.appointments_filter_presupuesto, p)
        binding.chipIngreso.text = getString(R.string.appointments_filter_ingreso, i)
        binding.chipEntrega.text = getString(R.string.appointments_filter_entrega, e)
    }

    private fun openAppointmentDetails(item: AppointmentUi) {
        val statusKey = item.status?.trim()?.lowercase(Locale("es", "MX")).orEmpty()
        if (statusKey.contains("cancel")) {
            Toast.makeText(
                this,
                getString(R.string.appointments_cancelled_notice),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val tag = AppointmentDetailsBottomSheet.Companion.TAG
        val alreadyShown = supportFragmentManager.findFragmentByTag(tag)
        if (alreadyShown != null) return

        AppointmentDetailsBottomSheet.Companion
            .newInstance(item)
            .show(supportFragmentManager, tag)
    }

    // ✅ Lee el extra de la notificación y abre el detalle
    private fun handleOpenFromIntent(intent: Intent?) {
        val id = intent?.getLongExtra(
            AppointmentNotificationHelper.EXTRA_APPOINTMENT_ID,
            -1L
        ) ?: -1L

        if (id <= 0L) return

        // Evita re-abrir si la Activity queda “guardando” el extra
        intent?.removeExtra(AppointmentNotificationHelper.EXTRA_APPOINTMENT_ID)

        openAppointmentDetailsById(id)
    }

    private fun openAppointmentDetailsById(id: Long) {
        lifecycleScope.launch {
            repo.fetchAppointment(id)
                .onSuccess { dto ->
                    val ui = AppointmentMapper.toUi(dto)
                    openAppointmentDetails(ui)
                }
                .onFailure {
                    showToast(getString(R.string.appointments_details_open_error))
                }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun isDtoForToday(dto: AppointmentDto): Boolean {
        val today = LocalDate.now(mxZone)
        val zdt = parseToZonedMx(dto.date) ?: return false
        return zdt.toLocalDate() == today
    }

    private fun parseToZonedMx(raw: String?): ZonedDateTime? {
        if (raw.isNullOrBlank()) return null

        // ISO con zona (ej: 2026-01-09T10:30:00Z)
        runCatching { return ZonedDateTime.parse(raw).withZoneSameInstant(mxZone) }.getOrNull()

        // LocalDateTime sin zona
        val ldt = serverLocalFormatters.firstNotNullOfOrNull { fmt ->
            runCatching { LocalDateTime.parse(raw, fmt) }.getOrNull()
        } ?: return null

        return ldt.atZone(mxZone)
    }

    private fun formatTodayLong(): String {
        val locale = Locale("es", "MX")
        val today = LocalDate.now(mxZone)

        val dayName = today.format(DateTimeFormatter.ofPattern("EEEE", locale))
            .replaceFirstChar { it.titlecase(locale) }

        val dayNum = today.format(DateTimeFormatter.ofPattern("dd", locale))

        val monthName = today.format(DateTimeFormatter.ofPattern("MMMM", locale))
            .replaceFirstChar { it.titlecase(locale) }

        val year = today.year

        return "$dayName, $dayNum de $monthName de $year"
    }
}
