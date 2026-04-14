package com.sunshine.appsuite.budget.tools.appointments.ui.edit

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.sunshine.appsuite.budget.AppSuiteApp
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.tools.appointments.data.network.AppointmentsApi
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.AppointmentDto
import com.sunshine.appsuite.budget.tools.appointments.data.repository.AppointmentsRepository
import com.sunshine.appsuite.budget.tools.appointments.ui.model.ClientUi
import com.sunshine.appsuite.budget.tools.appointments.util.WeekdayValidator
import com.sunshine.appsuite.databinding.ActivityEditAppointmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class EditAppointmentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"
        private const val TAG = "EditAppointment"
    }

    private lateinit var binding: ActivityEditAppointmentBinding
    private lateinit var api: AppointmentsApi
    private lateinit var repo: AppointmentsRepository

    private val zoneMx: ZoneId = ZoneId.of("America/Monterrey")

    private var appointmentId: Long = -1L

    // valores del server (se conservan en el PUT)
    private var currentType: String? = null
    private var currentStatus: String? = null

    private var selectedDate: LocalDate? = null
    private var selectedStartTime: LocalTime? = null

    // Hora (dropdown)
    private val timeSlots = mutableListOf<LocalTime>()
    private lateinit var timeAdapter: ArrayAdapter<String>

    // Clientes (dropdown): mostrar name, guardar id
    private val clients = mutableListOf<ClientUi>()       // lista real con ids
    private lateinit var clientAdapter: ArrayAdapter<String> // adapter de nombres (solo UI)
    private var selectedClientId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        api = (application as AppSuiteApp).retrofit.create(AppointmentsApi::class.java)
        repo = AppointmentsRepository(api)

        appointmentId = intent.getLongExtra(EXTRA_APPOINTMENT_ID, -1L)
        if (appointmentId <= 0L) appointmentId = intent.getLongExtra("appointment_id", -1L)

        if (appointmentId <= 0L) {
            toast("No se encontró el ID de la cita")
            finish()
            return
        }

        setupSystemBars()
        setupToolbar()
        setupInputs()
        setupActions()

        // Importante: el catálogo puede llegar después que la cita (o viceversa)
        loadClients()
        loadAppointment()
    }

    private fun setupSystemBars() {
        val surfaceColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorSurface,
            ContextCompat.getColor(this, R.color.google_white)
        )

        window.statusBarColor = surfaceColor
        window.navigationBarColor = surfaceColor

        val isDarkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
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
            title = getString(R.string.appointments_edit_title)
        }
        binding.toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun setupInputs() {
        // Fecha: solo picker
        binding.etDate.isFocusable = false
        binding.etDate.isClickable = true

        // Tel: 10 dígitos
        binding.etTel.filters = arrayOf(InputFilter.LengthFilter(10))
        binding.etTel.inputType = InputType.TYPE_CLASS_PHONE

        // Hora (dropdown)
        timeAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.actvStartTime.setAdapter(timeAdapter)
        binding.actvStartTime.setOnItemClickListener { _, _, position, _ ->
            selectedStartTime = timeSlots.getOrNull(position)
        }
        binding.actvStartTime.setOnClickListener { binding.actvStartTime.showDropDown() }

        // Cliente (dropdown): adapter SOLO de nombres
        clientAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.etClientType.setAdapter(clientAdapter)
        binding.etClientType.setOnClickListener { binding.etClientType.showDropDown() }

        // Evita texto libre (tu backend valida ID real)
        binding.etClientType.keyListener = null

        binding.etClientType.setOnItemClickListener { _, _, position, _ ->
            val picked = clients.getOrNull(position)
            selectedClientId = picked?.id
            renderClientField()
        }

        // Estado inicial (antes de cargar nada)
        renderClientField()
    }

    private fun setupActions() {
        binding.etDate.setOnClickListener { openDatePicker() }
        binding.tilDate.setEndIconOnClickListener { openDatePicker() }

        binding.btnSaveAppointment.setOnClickListener { onSave() }
    }

    private fun loadClients() {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repo.fetchClients().getOrThrow() }
            }.onSuccess { list ->
                clients.clear()
                clients.addAll(list.map { ClientUi(id = it.id, name = it.name, type = it.type) })

                clientAdapter.clear()
                clientAdapter.addAll(clients.map { it.name })
                clientAdapter.notifyDataSetChanged()

                Log.d(TAG, "clients loaded: ${clients.size}")

                // Si ya teníamos ID de la cita, ahora sí lo resolvemos a NAME
                renderClientField()
            }.onFailure { err ->
                Log.e(TAG, "getClients failed", err)
                binding.tilClientType.helperText = "No se pudo cargar la lista de clientes"
                // OJO: no pintamos ID. Si no hay catálogo, el usuario tendrá que seleccionar después.
            }
        }
    }

    private fun loadAppointment() {
        setLoading(true)

        lifecycleScope.launch {
            repo.fetchAppointment(appointmentId)
                .onSuccess { dto -> bindFromDto(dto) }
                .onFailure { err ->
                    Log.e(TAG, "fetchAppointment failed id=$appointmentId", err)
                    toast(getString(R.string.appointments_error_loading))
                }

            setLoading(false)
        }
    }

    private fun bindFromDto(dto: AppointmentDto) {
        currentType = dto.type
        currentStatus = dto.status

        // ===== Cliente =====
        binding.etName.setText(dto.displayClientName.orEmpty())
        binding.etTel.setText(dto.displayPhone.orEmpty().replace("\\D".toRegex(), ""))
        binding.etEmail.setText(dto.displayEmail.orEmpty())

        // ===== Cliente (catálogo) =====
        // Backend manda ID (ej. "1"). Nosotros lo guardamos en memoria y mostramos el NAME.
        selectedClientId = dto.clientType?.trim()?.toLongOrNull()
        renderClientField()

        // ===== Comentarios =====
        binding.etComment.setText(dto.comment.orEmpty())

        // ===== Vehículo =====
        binding.etVehicleBrand.setText(dto.brand.orEmpty())
        binding.etVehicleModel.setText(dto.model.orEmpty())
        binding.etVehicleYear.setText(dto.yearText.orEmpty())
        binding.etVehicleColor.setText(dto.color.orEmpty())
        binding.etVehiclePlate.setText(dto.plate.orEmpty())

        // ===== Fecha =====
        selectedDate = parseServerToLocalDate(dto.date) ?: LocalDate.now(zoneMx)
        val d = selectedDate!!
        binding.etDate.setText(formatDateUi(d))
        rebuildTimeSlots(d)

        // ===== Hora =====
        val rawTime = dto.time?.trim()
        selectedStartTime = parseServerToLocalTime(rawTime)

        // Visual: exacto como lo manda el server (aunque sea "09:00:00")
        binding.actvStartTime.setText(rawTime.orEmpty(), false)
    }

    /**
     * Render del campo client:
     * - Nunca mostramos el ID.
     * - Si no hay catálogo aún: mostramos helper "Cargando..."
     * - Si hay catálogo: mostramos el name correspondiente.
     */
    private fun renderClientField() {
        val id = selectedClientId

        if (id == null) {
            binding.tilClientType.helperText = null
            binding.etClientType.setText("", false)
            return
        }

        if (clients.isEmpty()) {
            // catálogo aún no llega
            binding.tilClientType.helperText = "Cargando clientes..."
            binding.etClientType.setText("", false)
            return
        }

        val match = clients.firstOrNull { it.id == id }
        if (match != null) {
            binding.tilClientType.helperText = null
            binding.etClientType.setText(match.name, false)
        } else {
            // No existe ese id en catálogo
            binding.tilClientType.helperText = "Cliente no encontrado para ID: $id"
            binding.etClientType.setText("", false)
        }
    }

    // -----------------------------
    // DatePicker: NO pasado + NO sábados + NO domingos
    // -----------------------------
    private fun openDatePicker() {
        val todayLocal = LocalDate.now(zoneMx)

        // DatePicker valida en UTC; evitamos desfase con inicio del día en UTC
        val todayUtcMillis = todayLocal.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val initialDate = selectedDate ?: todayLocal
        val initialUtcMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val validators = listOf(
            DateValidatorPointForward.from(todayUtcMillis), // ✅ no pasado
            WeekdayValidator()                              // ✅ no sáb/dom
        )

        val constraints = CalendarConstraints.Builder()
            .setStart(todayUtcMillis)
            .setValidator(CompositeDateValidator.allOf(validators))
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.appointments_create_date_open_picker))
            .setSelection(initialUtcMillis)
            .setCalendarConstraints(constraints)
            .setTheme(R.style.ThemeOverlay_AppSuite_DatePicker)
            .build()

        picker.addOnPositiveButtonClickListener { utcMillis ->
            val newDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
            selectedDate = newDate
            binding.etDate.setText(formatDateUi(newDate))
            rebuildTimeSlots(newDate)
        }

        picker.show(supportFragmentManager, "edit_date_picker")
    }

    private fun rebuildTimeSlots(date: LocalDate) {
        val (slots, labels) = buildTimeSlotsFor(date)

        timeSlots.clear()
        timeSlots.addAll(slots)

        timeAdapter.clear()
        timeAdapter.addAll(labels)
        timeAdapter.notifyDataSetChanged()

        if (timeSlots.isEmpty()) {
            selectedStartTime = null
            binding.actvStartTime.setText("", false)
            toast("No hay horarios disponibles para ese día")
            return
        }

        if (selectedStartTime != null && selectedStartTime !in timeSlots) {
            selectedStartTime = null
            binding.actvStartTime.setText("", false)
            toast("La hora anterior ya no es válida, elige otra")
        }
    }

    private fun onSave() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val phoneDigits = binding.etTel.text?.toString()?.replace("\\D".toRegex(), "")?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()

        val date = selectedDate
        val time = selectedStartTime

        if (name.isBlank()) {
            toast("Falta el nombre")
            return
        }
        if (phoneDigits.length != 10) {
            toast(getString(R.string.appointments_create_phone_helper))
            return
        }
        if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Correo inválido")
            return
        }
        if (date == null) {
            toast("Falta la fecha")
            return
        }

        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            toast("No se permiten citas en sábado ni domingo")
            return
        }

        if (time == null) {
            toast("Falta la hora")
            return
        }

        val clientId = selectedClientId
        if (clientId == null) {
            toast("Selecciona un cliente")
            return
        }
        if (clients.isNotEmpty() && clients.none { it.id == clientId }) {
            toast("El cliente seleccionado no es válido")
            return
        }

        val apiDate = formatDateApi(date)
        val apiTime = formatTimeApi(time)

        val type = currentType?.trim().orEmpty()
        val status = currentStatus?.trim().orEmpty()

        if (type.isBlank()) {
            toast("Falta el tipo (type)")
            return
        }
        if (status.isBlank()) {
            toast("Falta el estatus (status)")
            return
        }

        val comment = binding.etComment.text?.toString()?.trim().orEmpty()

        val brand = binding.etVehicleBrand.text?.toString()?.trim().orEmpty()
        val model = binding.etVehicleModel.text?.toString()?.trim().orEmpty()
        val color = binding.etVehicleColor.text?.toString()?.trim().orEmpty()
        val plate = binding.etVehiclePlate.text?.toString()?.trim().orEmpty()

        val yearStr = binding.etVehicleYear.text?.toString()?.trim().orEmpty()
        val yearInt = yearStr.toIntOrNull()

        val body = linkedMapOf<String, Any?>(
            "type" to type,
            "status" to status,
            "date" to apiDate,
            "time" to apiTime,
            "comment" to comment.ifBlank { null },
            "client_name" to name,
            "phone" to phoneDigits,
            "email" to email.ifBlank { null },
            "brand" to brand.ifBlank { null },
            "model" to model.ifBlank { null },
            "color" to color.ifBlank { null },
            "year" to (yearInt ?: yearStr.ifBlank { null }),
            "plate" to plate.ifBlank { null },
            "client_type" to clientId
        )

        setSaving(true)

        lifecycleScope.launch {
            val updateResult = withContext(Dispatchers.IO) {
                runCatching {
                    val resp = api.updateAppointment(appointmentId, body)
                    val err = if (!resp.isSuccessful)
                        runCatching { resp.errorBody()?.string() }.getOrNull()
                    else null
                    Triple(resp.isSuccessful, resp.code(), err)
                }.getOrElse { e ->
                    Triple(false, null, e.message ?: "exception")
                }
            }

            setSaving(false)

            val ok = updateResult.first
            val code = updateResult.second
            val errBody = updateResult.third

            if (ok) {
                toast("Cita actualizada ✅")
                setResult(RESULT_OK, Intent().putExtra(EXTRA_APPOINTMENT_ID, appointmentId))
                finish()
            } else {
                val msg = buildString {
                    append("No se pudo actualizar la cita")
                    if (code != null) append(" (HTTP $code)")
                    if (!errBody.isNullOrBlank()) append("\n$errBody")
                }
                Log.e(TAG, "update failed id=$appointmentId code=$code err=$errBody")
                toast(msg)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSaveAppointment.isEnabled = !loading
        binding.btnSaveAppointment.text =
            if (loading) getString(R.string.home_loading) else getString(R.string.appointments_edit_save)
    }

    private fun setSaving(saving: Boolean) {
        binding.btnSaveAppointment.isEnabled = !saving
        binding.btnSaveAppointment.text =
            if (saving) getString(R.string.home_loading) else getString(R.string.appointments_edit_save)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun parseServerToLocalDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null

        if (raw.contains('T')) {
            return runCatching { Instant.parse(raw).atZone(zoneMx).toLocalDate() }.getOrElse {
                runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(zoneMx).toLocalDate() }.getOrNull()
            }
        }

        return runCatching { LocalDate.parse(raw, DateTimeFormatter.ofPattern("dd-MM-uuuu")) }.getOrNull()
            ?: runCatching { LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }

    private fun parseServerToLocalTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null

        runCatching { return LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm:ss")) }
        return runCatching { LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
    }

    private fun formatDateUi(date: LocalDate): String {
        val locale = Locale("es", "MX")
        val fmt = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' uuuu", locale)
        return fmt.format(date)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    private fun formatDateApi(date: LocalDate): String {
        return DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.US).format(date)
    }

    private fun formatTimeApi(time: LocalTime): String {
        return DateTimeFormatter.ofPattern("HH:mm", Locale.US)
            .format(time.withSecond(0).withNano(0))
    }

    private fun formatTimeUi(time: LocalTime): String {
        return DateTimeFormatter.ofPattern("HH:mm", Locale.US).format(time)
    }

    private fun buildTimeSlotsFor(date: LocalDate): Pair<List<LocalTime>, List<String>> {
        val dow = date.dayOfWeek

        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return emptyList<LocalTime>() to emptyList()
        }

        val start = LocalTime.of(8, 0)
        val end = LocalTime.of(17, 0)

        val now = Instant.now().atZone(zoneMx)
        val today = now.toLocalDate()

        val minTimeToday = if (date == today) {
            roundUpToNextHour(now.toLocalTime().plusMinutes(15))
        } else null

        val slots = mutableListOf<LocalTime>()
        var t = start
        while (!t.isAfter(end)) {
            if (minTimeToday == null || !t.isBefore(minTimeToday)) {
                slots.add(t)
            }
            t = t.plusHours(1)
        }

        val labels = slots.map { formatTimeUi(it) }
        return slots to labels
    }

    private fun roundUpToNextHour(time: LocalTime): LocalTime {
        val base = time.withMinute(0).withSecond(0).withNano(0)
        return if (time == base) base else base.plusHours(1)
    }
}
