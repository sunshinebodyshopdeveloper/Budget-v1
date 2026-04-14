package com.sunshine.appsuite.budget.tools.appointments.ui.create

import android.content.res.Configuration
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.JsonParser
import com.sunshine.appsuite.budget.AppSuiteApp
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.ActivityCreateAppointmentBinding
import com.sunshine.appsuite.budget.tools.appointments.AppointmentType
import com.sunshine.appsuite.budget.tools.appointments.data.network.AppointmentsApi
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.AppointmentDto
import com.sunshine.appsuite.budget.tools.appointments.util.AppointmentDateTimeHelper
import com.sunshine.appsuite.budget.tools.appointments.util.WeekdayValidator
import com.sunshine.appsuite.budget.tools.appointments.util.AppointmentNotificationHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import android.content.Intent
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.gson.JsonElement
import com.sunshine.appsuite.budget.tools.appointments.ui.model.ClientUi
import java.time.Instant
import java.time.LocalDateTime

class CreateAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAppointmentBinding
    private lateinit var api: AppointmentsApi

    // valores canónicos
    private var selectedDate: LocalDate? = null
    private var selectedStartTime: LocalTime? = null
    private var selectedType: AppointmentType? = null


    // Catálogo clientes: en UI mostramos name; en API guardamos id
    private val clients = mutableListOf<ClientUi>()
    private lateinit var clientAdapter: ArrayAdapter<ClientUi>
    private var selectedClientId: Long? = null

    // Slots base: 08:00–17:00 (horas cerradas)
    private val baseTimeSlots: List<LocalTime> = AppointmentDateTimeHelper.buildTimeSlots()

    private val mxZone: ZoneId = ZoneId.of("America/Monterrey")

    // ✅ Formato requerido por backend para crear cita
    // (En Android/Java lo correcto es usar yyyy/uuuu; "YYYY" es week-year y puede fallar en fin de anio)
    private val apiDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu")
    private val apiTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = (application as AppSuiteApp).retrofit.create(AppointmentsApi::class.java)

        setupSystemBars()
        setupToolbar()

        setupDatePickerField()
        setDefaultTodayDate()

        setupStartTimeDropdown()
        setDefaultEmptyTime()

        setupTypeDropdown()

        setupClientDropdown()
        loadClients()

        setupStatusDefault()
        setupSaveButton()
        setupFormWatchers()

        updateSaveButtonState()
    }

    private fun setupSystemBars() {
        val surfaceColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorSurface,
            ContextCompat.getColor(this, R.color.google_white)
        )

        window.statusBarColor = surfaceColor
        window.navigationBarColor = surfaceColor

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
            title = getString(R.string.appointments_create_title)
        }
        binding.toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // -----------------------------
    // STATUS (read-only)
    // -----------------------------
    private fun setupStatusDefault() {
        binding.etStatus.setText(getString(R.string.appointments_status_pending))
    }

    // -----------------------------
    // FECHA (MaterialDatePicker)
    // -----------------------------
    private fun setupDatePickerField() {
        AppointmentDateTimeHelper.makeAsPickerField(binding.etDate)
        binding.etDate.setOnClickListener { openDatePicker() }
        binding.tilDate.setEndIconOnClickListener { openDatePicker() }
    }

    private fun openDatePicker() {
        val selectionDate = selectedDate ?: LocalDate.now(mxZone)
        val selectionMillis = AppointmentDateTimeHelper.localDateToUtcMillis(selectionDate)

        val constraints = buildBusinessDayConstraints()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.appointments_create_date_open_picker))
            .setCalendarConstraints(constraints)
            .setSelection(selectionMillis)
            .setTheme(R.style.ThemeOverlay_AppSuite_DatePicker) // ✅ google_blue selected day
            .build()

        picker.addOnPositiveButtonClickListener { utcMillis ->
            val picked = AppointmentDateTimeHelper.millisToLocalDate(utcMillis)
            val businessPicked = AppointmentDateTimeHelper.nextBusinessDay(picked)

            selectedDate = businessPicked
            binding.etDate.setText(AppointmentDateTimeHelper.formatDateUi(businessPicked))

            // reset hora + recargar slots filtrados
            selectedStartTime = null
            setDefaultEmptyTime()
            updateTimeSlotsForSelectedDate()

            updateSaveButtonState()
        }

        picker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun buildBusinessDayConstraints(): CalendarConstraints {
        val today = LocalDate.now(mxZone)
        val todayMillis = AppointmentDateTimeHelper.localDateToUtcMillis(today)

        val validators = listOf(
            DateValidatorPointForward.from(todayMillis), // no fechas pasadas
            WeekdayValidator()                           // no sáb/dom
        )

        return CalendarConstraints.Builder()
            .setValidator(CompositeDateValidator.allOf(validators))
            .build()
    }

    private fun setDefaultTodayDate() {
        val todayLocal = LocalDate.now(mxZone)
        val businessDay = AppointmentDateTimeHelper.nextBusinessDay(todayLocal)

        selectedDate = businessDay
        binding.etDate.setText(AppointmentDateTimeHelper.formatDateUi(businessDay))

        selectedStartTime = null
    }

    // -----------------------------
    // HORA (dropdown)
    // -----------------------------
    private fun setupStartTimeDropdown() {
        AppointmentDateTimeHelper.makeAsPickerField(binding.actvStartTime)

        // Carga slots según la fecha actual (y filtra horas pasadas si es hoy)
        updateTimeSlotsForSelectedDate()

        binding.actvStartTime.setOnClickListener {
            val slots = AppointmentDateTimeHelper.lastSlotsShown
            if (slots.isEmpty()) {
                toast("No hay horarios disponibles")
                return@setOnClickListener
            }
            binding.actvStartTime.showDropDown()
        }
        binding.tilStartTime.setEndIconOnClickListener {
            val slots = AppointmentDateTimeHelper.lastSlotsShown
            if (slots.isEmpty()) {
                toast("No hay horarios disponibles")
                return@setEndIconOnClickListener
            }
            binding.actvStartTime.showDropDown()
        }

        binding.actvStartTime.setOnItemClickListener { _, _, position, _ ->
            val currentSlots = AppointmentDateTimeHelper.lastSlotsShown
            selectedStartTime = currentSlots.getOrNull(position)
            updateSaveButtonState()
        }
    }

    private fun setDefaultEmptyTime() {
        // ✅ visualmente "--:--" pero selectedStartTime sigue null
        binding.actvStartTime.setText(getString(R.string.appointments_time_placeholder), false)
    }

    private fun updateTimeSlots(slots: List<LocalTime>) {
        AppointmentDateTimeHelper.lastSlotsShown = slots
        val labels = slots.map { AppointmentDateTimeHelper.formatTimeUi12h(it) } // "2:00 p. m. hrs"
        binding.actvStartTime.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        )
    }

    // -----------------------------
    // TIPO (dropdown)
    // -----------------------------
    private fun setupTypeDropdown() {
        AppointmentDateTimeHelper.makeAsPickerField(binding.actvPurpose)

        val types = listOf(
            AppointmentType.PRESUPUESTO,
            AppointmentType.INGRESO,
            AppointmentType.ENTREGA
        )
        val labels = types.map { it.displayName }

        binding.actvPurpose.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        )

        binding.actvPurpose.setOnClickListener { binding.actvPurpose.showDropDown() }
        binding.tilPurpose.setEndIconOnClickListener { binding.actvPurpose.showDropDown() }

        binding.actvPurpose.setOnItemClickListener { _, _, position, _ ->
            selectedType = types.getOrNull(position)
            updateSaveButtonState()

            // A futuro: aquí filtras horarios por (fecha + tipo)
        }
    }


    private fun setupClientDropdown() {
        clientAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, clients)
        binding.etClientType.setAdapter(clientAdapter)
        binding.etClientType.setOnClickListener { binding.etClientType.showDropDown() }
        binding.etClientType.setOnItemClickListener { parent, _, position, _ ->
            val picked = parent.getItemAtPosition(position) as ClientUi
            selectedClientId = picked.id
        }

        clearClientPlaceholderIfNeeded()
    }

    private fun clearClientPlaceholderIfNeeded() {
        val placeholder = runCatching { getString(R.string.appointments_placeholder_value) }.getOrNull()
        val current = binding.etClientType.text?.toString().orEmpty()
        if (!placeholder.isNullOrBlank() && current == placeholder) {
            binding.etClientType.setText("", false)
        }
    }

    private fun loadClients() {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.getClients().data }
            }.onSuccess { list ->
                clients.clear()
                clients.addAll(list.map { ClientUi(id = it.id, name = it.name, type = it.type) })

                clientAdapter.clear()
                clientAdapter.addAll(clients)
                clientAdapter.notifyDataSetChanged()

                applyDefaultClientIfNeeded()
            }.onFailure { err ->
                // No bloqueamos el flujo si falla; simplemente no habrá dropdown.
                toast("No se pudo cargar el catálogo de clientes")
            }
        }
    }

    private fun applyDefaultClientIfNeeded() {
        // Para creación: si no hay selección, usamos "Cliente Mostrador" si existe
        if (selectedClientId == null) {
            val def = clients.firstOrNull { it.name.equals("Cliente Mostrador", ignoreCase = true) }
            if (def != null) {
                selectedClientId = def.id
                binding.etClientType.setText(def.name, false)
            }
            return
        }

        // Si ya hay selección (por ejemplo si reabrimos), resolvemos id -> name
        val match = clients.firstOrNull { it.id == selectedClientId }
        if (match != null) {
            binding.etClientType.setText(match.name, false)
        }
    }

    // -----------------------------
    // BOTÓN GUARDAR + VALIDACIONES
    // -----------------------------
    private fun setupSaveButton() {
        binding.btnSaveAppointment.isEnabled = false

        binding.btnSaveAppointment.setOnClickListener {
            lifecycleScope.launch { saveAppointment() }
        }
    }

    private fun setupFormWatchers() {
        binding.etName.addTextChangedListener { updateSaveButtonState() }
        binding.etTel.addTextChangedListener { updateSaveButtonState() }
        binding.etEmail.addTextChangedListener { updateSaveButtonState() }
        binding.etComment.addTextChangedListener { updateSaveButtonState() }
        // fecha/hora/tipo ya llaman updateSaveButtonState() al seleccionar
    }

    private fun updateSaveButtonState() {
        val nameOk = binding.etName.text?.toString()?.trim().orEmpty().isNotEmpty()
        val phoneOk = binding.etTel.text?.toString()?.trim().orEmpty().isNotEmpty()
        // email/comment pueden ser opcionales

        val dateOk = selectedDate != null
        val timeOk = selectedStartTime != null
        val typeOk = selectedType != null
        binding.btnSaveAppointment.isEnabled =
            nameOk && phoneOk && dateOk && timeOk && typeOk
    }

    private suspend fun saveAppointment() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val phoneRaw = binding.etTel.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val comment = binding.etComment.text?.toString()?.trim().orEmpty()

        // Vehiculo (opcionales)
        val brand = binding.etVehicleBrand.text?.toString()?.trim().orEmpty()
        val model = binding.etVehicleModel.text?.toString()?.trim().orEmpty()
        val color = binding.etVehicleColor.text?.toString()?.trim().orEmpty()
        val yearRaw = binding.etVehicleYear.text?.toString()?.trim().orEmpty()
        val plate = binding.etVehiclePlate.text?.toString()?.trim().orEmpty()

        val date = selectedDate
        val time = selectedStartTime
        val allowed = AppointmentDateTimeHelper.lastSlotsShown
        if (time != null && time !in allowed) {
            toast("Selecciona un horario válido")
            return
        }
        val type = selectedType

        if (name.isBlank() || phoneRaw.isBlank() || date == null || time == null || type == null) {
            toast("Te falta completar campos.")
            return
        }

        val phoneDigits = phoneRaw.filter { it.isDigit() }
        if (phoneDigits.length != 10) {
            toast("Teléfono inválido. Usa 10 dígitos (ej. 8123456789).")
            return
        }

        if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Correo inválido. Revisa el formato.")
            return
        }

        val year: Int? = when {
            yearRaw.isBlank() -> null
            yearRaw.length == 4 && yearRaw.all { it.isDigit() } -> yearRaw.toInt()
            else -> {
                toast("Año inválido. Usa 4 dígitos (ej. 2018).")
                return
            }
        }

        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            toast("No se pueden agendar citas en sábado o domingo.")
            return
        }

        val min = LocalTime.of(8, 0)
        val max = LocalTime.of(17, 0)
        if (time.isBefore(min) || time.isAfter(max) || time.minute != 0) {
            toast("El horario es de 8:00 am a 5:00 pm (horas cerradas).")
            return
        }

        setSavingUi(true)

        try {
            val existing = api.getAppointments().data
            val isDuplicate = hasDuplicate(existing, type, date, time)

            if (isDuplicate) {
                val uiDate = AppointmentDateTimeHelper.formatDateUi(date)
                val uiTime = AppointmentDateTimeHelper.formatTimeUi12h(time)
                toast("Ese horario ya está ocupado para ${type.displayName}: $uiDate · $uiTime")
                return
            }

            // ✅ Backend requiere dd-MM-YYYY (en realidad: dd-MM-yyyy/uuuu)
            val apiDate = date.format(apiDateFmt)
            val apiTime = time.format(apiTimeFmt) // HH:mm


            val body = linkedMapOf<String, Any>(
                "type" to type.displayName,
                "date" to apiDate,
                "time" to apiTime,
                "client_name" to name,
                "phone" to phoneDigits
            )

            // Guardamos el ID del cliente (en UI mostramos el name)
            selectedClientId?.let { body["client_type"] = it }

            if (comment.isNotBlank()) body["comment"] = comment
            if (email.isNotBlank()) body["email"] = email

            if (brand.isNotBlank()) body["brand"] = brand
            if (model.isNotBlank()) body["model"] = model
            if (color.isNotBlank()) body["color"] = color
            if (year != null) body["year"] = year
            if (plate.isNotBlank()) body["plate"] = plate

            val resp = api.createAppointment(body)

            if (resp.isSuccessful) {
                val rawCreated = runCatching { resp.body()?.string() }.getOrNull()
                var createdId: Long? = extractCreatedAppointmentId(rawCreated)

                if (createdId == null || createdId <= 0L) {
                    createdId = runCatching {
                        val after = api.getAppointments().data
                        findCreatedAppointmentId(
                            before = existing,
                            after = after,
                            type = type,
                            date = date,
                            time = time,
                            phoneDigits = phoneDigits
                        )
                    }.getOrNull()
                }

                toast("Cita creada ✅")

                val uiDate = AppointmentDateTimeHelper.formatDateUi(date)
                val uiTime = AppointmentDateTimeHelper.formatTimeUi12h(time)
                val notifText = "${type.displayName} · $uiDate · $uiTime"

                AppointmentNotificationHelper.showAppointmentCreated(
                    context = this@CreateAppointmentActivity,
                    appointmentId = createdId,
                    contentText = notifText
                )

                setResult(RESULT_OK)
                openAppointmentsActivity(createdId, apiDate)
                finish()
            } else {
                val msg = extractApiError(resp.errorBody()?.string())
                    ?: "No se pudo crear la cita. (${resp.code()})"
                toast(msg)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            toast("No se pudo crear la cita. Revisa tu internet o token.")
        } finally {
            setSavingUi(false)
            updateSaveButtonState()
        }
    }

    private fun openAppointmentsActivity(createdId: Long?, dateApi: String) {
        val intent = Intent().apply {
            // TODO: A futuro: abrir otra Activity con todos los campos para editar (fecha, hora, etc.)
            setClassName(this@CreateAppointmentActivity, APPOINTMENTS_ACTIVITY_CLASS)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_HIGHLIGHT_APPOINTMENT_ID, createdId ?: -1L)
            putExtra(EXTRA_FOCUS_DATE_API, dateApi)
        }

        runCatching { startActivity(intent) }
            .onFailure { /* si no existe la Activity, al menos no crashea */ }

        finish()
    }

    private companion object {
        // Si tu Activity tiene otro paquete/nombre, solo cambia esta constante.
        const val APPOINTMENTS_ACTIVITY_CLASS = "com.sunshine.appsuite.tools.appointments.ui.AppointmentsActivity"

        // Extras opcionales (por si quieres hacer scroll/resaltar la cita creada en el listado)
        const val EXTRA_HIGHLIGHT_APPOINTMENT_ID = "extra_highlight_appointment_id"
        const val EXTRA_FOCUS_DATE_API = "extra_focus_date_api"
    }


    private fun hasDuplicate(
        existing: List<AppointmentDto>,
        selectedType: AppointmentType,
        selectedDate: LocalDate,
        selectedTime: LocalTime
    ): Boolean {
        return existing.any { dto ->
            val t = AppointmentType.Companion.fromApi(dto.type)
            if (t != selectedType) return@any false

            val d = parseServerToLocalDate(dto.date) ?: return@any false
            val tm = parseServerToLocalTime(dto.time) ?: return@any false

            d == selectedDate && tm.hour == selectedTime.hour && tm.minute == selectedTime.minute
        }
    }

    private fun parseServerToLocalDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null

        return runCatching {
            Instant.parse(raw).atZone(mxZone).toLocalDate()
        }.getOrNull()
    }

    private fun parseServerToLocalTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null

        // "17:00:00" o "17:00"
        return runCatching {
            LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm:ss"))
        }.getOrNull()?.withSecond(0)?.withNano(0)
            ?: runCatching {
                LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm"))
            }.getOrNull()?.withSecond(0)?.withNano(0)
    }

    private fun parseServerZoned(raw: String?): ZonedDateTime? {
        if (raw.isNullOrBlank()) return null

        runCatching { return ZonedDateTime.parse(raw) }.getOrNull()

        val fmts = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
        ).map { DateTimeFormatter.ofPattern(it) }

        val ldt = fmts.firstNotNullOfOrNull { fmt ->
            runCatching { LocalDateTime.parse(raw, fmt) }.getOrNull()
        } ?: return null

        return ldt.atZone(ZoneId.systemDefault())
    }

    private fun extractApiError(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        return runCatching {
            val json = JsonParser.parseString(raw).asJsonObject

            json.get("message")?.asString?.takeIf { it.isNotBlank() }
                ?: json.getAsJsonObject("errors")
                    ?.entrySet()
                    ?.firstOrNull()
                    ?.value
                    ?.asJsonArray
                    ?.firstOrNull()
                    ?.asString
        }.getOrNull()
    }

    private fun extractCreatedAppointmentId(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null

        return runCatching {
            val root = JsonParser.parseString(raw)
            if (!root.isJsonObject) return@runCatching null
            val obj = root.asJsonObject

            fun asLongSafe(e: JsonElement?): Long? {
                if (e == null || e.isJsonNull) return null
                if (e.isJsonPrimitive) return runCatching { e.asLong }.getOrNull()
                return null
            }

            val dataObj = obj.get("data")
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject

            asLongSafe(dataObj?.get("id"))
                ?: asLongSafe(obj.get("id"))
                ?: asLongSafe(obj.get("appointment_id"))
        }.getOrNull()
    }

    private fun findCreatedAppointmentId(
        before: List<AppointmentDto>,
        after: List<AppointmentDto>,
        type: AppointmentType,
        date: LocalDate,
        time: LocalTime,
        phoneDigits: String
    ): Long? {
        val beforeIds = before.mapNotNull { it.id }.toSet()
        val newOnes = after.filter { it.id != null && it.id !in beforeIds }

        val target = time.withSecond(0).withNano(0)

        fun matchesLoose(dto: AppointmentDto): Boolean {
            if (AppointmentType.Companion.fromApi(dto.type) != type) return false
            val zdt = parseServerZoned(dto.date) ?: return false
            val local = zdt.withZoneSameInstant(mxZone)
            val dtoTime = local.toLocalTime().withSecond(0).withNano(0)
            return local.toLocalDate() == date &&
                    dtoTime.hour == target.hour && dtoTime.minute == target.minute
        }

        fun matchesStrict(dto: AppointmentDto): Boolean {
            if (!matchesLoose(dto)) return false
            val dtoPhone = dtoPhoneDigits(dto)
            return dtoPhone.isNotBlank() && dtoPhone == phoneDigits
        }

        return newOnes.firstOrNull { matchesStrict(it) }?.id
            ?: after.firstOrNull { matchesStrict(it) }?.id
            ?: newOnes.firstOrNull { matchesLoose(it) }?.id
            ?: after.firstOrNull { matchesLoose(it) }?.id
            ?: newOnes.maxByOrNull { it.id ?: 0L }?.id
    }

    private fun dtoPhoneDigits(dto: AppointmentDto): String {
        val p = dto.appointable?.phone
        val raw = when {
            p == null || p.isJsonNull -> ""
            p.isJsonPrimitive -> runCatching { p.asString }.getOrNull() ?: p.toString()
            else -> p.toString()
        }
        return raw.filter { it.isDigit() }
    }

    private fun setSavingUi(saving: Boolean) {
        binding.btnSaveAppointment.isEnabled = !saving
        binding.btnSaveAppointment.text =
            if (saving) "Guardando..." else getString(R.string.appointments_create_save)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val focused = currentFocus
            if (focused is EditText) {
                val outRect = Rect()
                focused.getGlobalVisibleRect(outRect)

                // Si el tap fue fuera del EditText -> cerrar teclado
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    focused.clearFocus()
                    hideKeyboard(focused)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun updateTimeSlotsForSelectedDate() {
        val date = selectedDate ?: return
        val slots = buildAllowedHourlySlots(date)

        AppointmentDateTimeHelper.lastSlotsShown = slots

        val labels = slots.map { it.format(apiTimeFmt) } // "HH:mm"
        binding.actvStartTime.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        )

        val hasSlots = slots.isNotEmpty()
        binding.tilStartTime.isEnabled = hasSlots
        binding.actvStartTime.isEnabled = hasSlots

        if (!hasSlots) {
            selectedStartTime = null
            setDefaultEmptyTime()
            toast("Ya no hay horarios disponibles para hoy")
        } else {
            // Si la hora seleccionada ya no existe (cambió fecha), la limpiamos
            if (selectedStartTime != null && selectedStartTime !in slots) {
                selectedStartTime = null
                setDefaultEmptyTime()
            }
        }
    }

    private fun buildAllowedHourlySlots(date: LocalDate): List<LocalTime> {
        val start = LocalTime.of(8, 0)
        val end = LocalTime.of(17, 0)

        var first = start

        // Si es hoy: aplica lead + redondea a la siguiente hora cerrada
        val today = LocalDate.now(mxZone)
        if (date == today) {
            val now = ZonedDateTime.now(mxZone).toLocalTime()
            val minAllowed = roundUpToNextHour(now.plusMinutes(15))
            if (minAllowed.isAfter(first)) first = minAllowed
        }

        if (first.isAfter(end)) return emptyList()

        val slots = mutableListOf<LocalTime>()
        var t = first.withMinute(0).withSecond(0).withNano(0)
        while (!t.isAfter(end)) {
            slots.add(t)
            t = t.plusHours(1)
        }
        return slots
    }

    private fun roundUpToNextHour(time: LocalTime): LocalTime {
        val base = time.withMinute(0).withSecond(0).withNano(0)
        return if (time == base) base else base.plusHours(1)
    }
}
