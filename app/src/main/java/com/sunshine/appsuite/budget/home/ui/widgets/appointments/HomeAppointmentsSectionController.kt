package com.sunshine.appsuite.budget.home.ui.widgets.appointments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.tools.appointments.AppointmentType
import com.sunshine.appsuite.budget.tools.appointments.AppointmentsActivity
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.AppointmentDto
import com.sunshine.appsuite.budget.tools.appointments.data.repository.AppointmentsRepository
import com.sunshine.appsuite.budget.tools.appointments.util.AppointmentNotificationHelper
import com.sunshine.appsuite.databinding.ItemHomeAppointmentRowBinding
import com.sunshine.appsuite.databinding.ItemHomeAppointmentsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeAppointmentsSectionController(
    private val repo: AppointmentsRepository,
    private val onError: (String) -> Unit
) {

    private var binding: ItemHomeAppointmentsBinding? = null
    private var adapter: HomeAppointmentsMiniAdapter? = null

    private val mxZone: ZoneId = ZoneId.of("America/Monterrey")

    // Datetime local (sin zona) + variantes comunes
    private val localDateTimeFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm"),
        DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
    )

    private val dateOnlyFormatters = listOf(
        DateTimeFormatter.ofPattern("dd-MM-uuuu"),
        DateTimeFormatter.ofPattern("dd/MM/uuuu"),
        DateTimeFormatter.ofPattern("uuuu-MM-dd")
    )

    private val timeOnlyFormatters = listOf(
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("H:mm"),
        DateTimeFormatter.ofPattern("HH:mm:ss")
    )

    private var loadedOnce = false
    private var isLoading = false

    /** Todas las citas de HOY (Monterrey), ordenadas por hora. */
    private var items: List<HomeAppointmentItem> = emptyList()

    fun bind(itemView: View) {
        val b = ItemHomeAppointmentsBinding.bind(itemView)
        binding = b

        applyCountTypography(b)

        // Card click => abre el módulo (lista)
        b.caradAppointments.setOnClickListener { openAppointmentsList(it.context) }

        // RV
        val rv = b.rvHomeAppointments
        if (adapter == null) {
            adapter = HomeAppointmentsMiniAdapter { clicked ->
                openAppointmentDetailOrList(rv.context, clicked.id)
            }
        }

        rv.layoutManager = LinearLayoutManager(itemView.context)
        rv.adapter = adapter
        rv.itemAnimator = null

        render()
    }

    private fun formatCount(total: Int): String {
        // 04, 12, etc.
        return String.format(Locale.US, "%02d", total.coerceAtLeast(0))
    }

    private fun applyCountTypography(b: ItemHomeAppointmentsBinding) {
        b.tvHomeAppointmentsCount.textScaleX = 0.78f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b.tvHomeAppointmentsCount.setFontVariationSettings("'wdth' 42, 'wght' 240")
        }
    }

    fun unbind() {
        binding?.rvHomeAppointments?.adapter = null
        binding = null
    }

    fun ensureLoaded(scope: CoroutineScope) {
        if (loadedOnce || isLoading) return
        refresh(scope)
    }

    fun refresh(scope: CoroutineScope) {
        if (isLoading) return

        // evita “Sin citas” mientras carga (flicker)
        val hadItems = items.isNotEmpty()
        isLoading = true
        if (!hadItems) items = emptyList()
        render()

        scope.launch {
            repo.fetchAppointments()
                .onSuccess { dtos ->
                    val now = ZonedDateTime.now(mxZone)
                    val today = now.toLocalDate()

                    val mapped = dtos.mapNotNull { it.toHomeItemOrNull() }

                    // SOLO citas de HOY
                    items = mapped
                        .filter { it.dateTime.withZoneSameInstant(mxZone).toLocalDate() == today }
                        .sortedBy { it.dateTime.toInstant() }

                    loadedOnce = true
                    isLoading = false
                    render()
                }
                .onFailure { err ->
                    err.printStackTrace()
                    onError("No pude cargar las citas del Home.")
                    items = emptyList()
                    loadedOnce = false
                    isLoading = false
                    render()
                }
        }
    }

    private fun render() {
        val b = binding ?: return

        val loadingOverlay = isLoading && items.isEmpty()
        b.layoutHomeAppointmentsSkeleton.isVisible = loadingOverlay
        b.layoutHomeAppointmentsContent.isVisible = !loadingOverlay

        // Left (fecha + total)
        val now = ZonedDateTime.now(mxZone)
        b.tvHomeAppointmentsToday.text = HomeAppointmentsUiFormatter.formatWidgetDate(now)
        b.tvHomeAppointmentsCount.text = formatCount(items.size)

        // Right (listado)
        b.tvHomeAppointmentsEmpty.isVisible = !isLoading && items.isEmpty()

        val listToShow = pickWidgetItems(items, now)
        adapter?.submitList(listToShow)
    }

    /**
     * Mostramos máx 3 en el Home:
     * - Primero las próximas (>= ahora)
     * - Si ya pasaron todas, las últimas 3
     */
    private fun pickWidgetItems(
        all: List<HomeAppointmentItem>,
        now: ZonedDateTime
    ): List<HomeAppointmentItem> {
        if (all.isEmpty()) return emptyList()

        val nowMx = now.withZoneSameInstant(mxZone)
        val upcoming = all.filter { it.dateTime.withZoneSameInstant(mxZone) >= nowMx }

        return when {
            upcoming.isNotEmpty() -> upcoming.take(3)
            all.size <= 3 -> all
            else -> all.takeLast(3)
        }
    }

    private fun openAppointmentsList(ctx: Context) {
        val intent = Intent(ctx, AppointmentsActivity::class.java)
        if (ctx !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    private fun openAppointmentDetailOrList(ctx: Context, appointmentId: Long) {
        val intent = Intent(ctx, AppointmentsActivity::class.java)

        // Si tenemos id válido, AppointmentsActivity abre el BottomSheetDetails.
        if (appointmentId > 0L) {
            intent.putExtra(AppointmentNotificationHelper.EXTRA_APPOINTMENT_ID, appointmentId)
        }

        if (ctx !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    // =============================
    // Mapping + parsing
    // =============================

    private fun AppointmentDto.toHomeItemOrNull(): HomeAppointmentItem? {
        val zdt = parseAppointmentZdt(this) ?: return null

        val idVal = resolveIdOrZero(this)
        val name = resolveClientName()
        val type = resolveType()
        val status = this.status ?: getStringProp("status")

        val vehicleSummary = buildVehicleSummary(
            brand = this.brand ?: getStringProp("brand"),
            model = this.model ?: getStringProp("model"),
            year = this.year ?: getAnyProp("year"),
            color = this.color ?: getStringProp("color"),
            plate = this.plate ?: getStringProp("plate")
        )

        return HomeAppointmentItem(
            id = idVal,
            dateTime = zdt,
            type = type,
            customerName = name.ifBlank { "Sin cliente" },
            status = status,
            vehicleSummary = vehicleSummary
        )
    }

    private fun AppointmentDto.resolveType(): AppointmentType {
        val raw = (this.type ?: getStringProp("type"))?.trim().orEmpty()
        val key = normalizeKey(raw)

        return when {
            key.contains("presup") -> AppointmentType.PRESUPUESTO
            key.contains("ingres") -> AppointmentType.INGRESO
            key.contains("entreg") -> AppointmentType.ENTREGA
            else -> AppointmentType.UNKNOWN
        }
    }

    private fun normalizeKey(text: String): String {
        return Normalizer.normalize(text.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    private fun resolveIdOrZero(dto: AppointmentDto): Long {
        val v = dto.getLongProp("id", "appointmentId", "appointment_id", "appointmentID")
        return v ?: (dto.id ?: 0L)
    }

    private fun parseAppointmentZdt(dto: AppointmentDto): ZonedDateTime? {
        // 1) Si existe un datetime completo real (no "date" del día)
        val dateTimeRaw = dto.getStringProp(
            "dateTime", "datetime", "date_time",
            "scheduledAt", "scheduled_at",
            "startAt", "start_at", "start",
            "appointmentDateTime", "appointment_date_time"
        )

        // 2) Date/Time separados (tu API: date ISO con Z, time HH:mm:ss)
        val datePart = dto.date ?: dto.getStringProp(
            "date", "appointmentDate", "appointment_date",
            "scheduledDate", "scheduled_date", "day"
        )
        val timePart = dto.time ?: dto.getStringProp(
            "time", "scheduledTime", "scheduled_time",
            "startTime", "start_time", "hour"
        )

        // Si dateTimeRaw es solo hora, lo tratamos como timePart
        val dtLooksTimeOnly = dateTimeRaw?.matches(Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$")) == true
        val effectiveDateTimeRaw = if (dtLooksTimeOnly) null else dateTimeRaw
        val effectiveTimePart = if (dtLooksTimeOnly) dateTimeRaw else timePart

        fun parseDatePartToLocalDate(raw: String): LocalDate? {
            val s = raw.trim()

            // ISO con hora/offset/Z -> parsea y toma solo la fecha en zona Monterrey
            if (s.contains("T")) {
                runCatching {
                    return OffsetDateTime.parse(s).atZoneSameInstant(mxZone).toLocalDate()
                }.getOrNull()

                runCatching {
                    return ZonedDateTime.parse(s).withZoneSameInstant(mxZone).toLocalDate()
                }.getOrNull()

                val ldt = localDateTimeFormatters.firstNotNullOfOrNull { fmt ->
                    runCatching { LocalDateTime.parse(s, fmt) }.getOrNull()
                }
                if (ldt != null) return ldt.toLocalDate()
            }

            // Fecha-only
            return dateOnlyFormatters.firstNotNullOfOrNull { fmt ->
                runCatching { LocalDate.parse(s, fmt) }.getOrNull()
            }
        }

        // A) Si tengo date+time: construyo la fecha/hora final
        val ld = datePart?.let { parseDatePartToLocalDate(it) }
        val lt = effectiveTimePart?.let { raw ->
            timeOnlyFormatters.firstNotNullOfOrNull { fmt ->
                runCatching { LocalTime.parse(raw.trim(), fmt) }.getOrNull()
            }
        }

        if (ld != null) return ZonedDateTime.of(ld, lt ?: LocalTime.MIDNIGHT, mxZone)

        // B) Si no hay datePart usable, intento parsear un datetime completo
        if (!effectiveDateTimeRaw.isNullOrBlank()) {
            runCatching {
                return ZonedDateTime.parse(effectiveDateTimeRaw).withZoneSameInstant(mxZone)
            }.getOrNull()

            val ldt = localDateTimeFormatters.firstNotNullOfOrNull { fmt ->
                runCatching { LocalDateTime.parse(effectiveDateTimeRaw, fmt) }.getOrNull()
            }
            if (ldt != null) return ldt.atZone(mxZone)
        }

        // C) Último salvavidas: si solo hay hora, asumimos HOY
        if (lt != null) {
            val today = LocalDate.now(mxZone)
            return ZonedDateTime.of(today, lt, mxZone)
        }

        return null
    }

    private fun AppointmentDto.resolveClientName(): String {
        (this.clientName ?: getStringProp(
            "client_name",
            "clientName",
            "customer_name",
            "customerName",
            "displayClientName",
            "display_client_name",
            "name"
        ))?.let { return it }

        this.appointable?.name?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        return "Sin cliente"
    }

    private fun buildVehicleSummary(
        brand: String?,
        model: String?,
        year: Any?,
        color: String?,
        plate: String?
    ): String? {
        val brandClean = brand?.trim().takeIf { !it.isNullOrBlank() }
        val modelClean = model?.trim().takeIf { !it.isNullOrBlank() }
        val plateClean = plate?.trim().takeIf { !it.isNullOrBlank() }

        if (brandClean == null || modelClean == null) return null

        val main = "$brandClean $modelClean".trim()
        return if (plateClean != null) "$main ( $plateClean )" else main
    }

    private fun AppointmentDto.getStringProp(vararg names: String): String? {
        for (name in names) {
            val v = runCatching { getAnyProp(name) }.getOrNull()
            val s = when (v) {
                is String -> v
                null -> null
                else -> v.toString()
            }?.trim()?.takeIf { it.isNotBlank() }

            if (s != null) return s
        }
        return null
    }

    private fun AppointmentDto.getLongProp(vararg names: String): Long? {
        for (name in names) {
            val v = runCatching { getAnyProp(name) }.getOrNull() ?: continue
            val l = when (v) {
                is Number -> v.toLong()
                is String -> v.trim().toLongOrNull()
                else -> v.toString().trim().toLongOrNull()
            }
            if (l != null) return l
        }
        return null
    }

    private fun AppointmentDto.getAnyProp(vararg names: String): Any? {
        for (name in names) {
            val value = runCatching {
                val f = this.javaClass.getDeclaredField(name)
                f.isAccessible = true
                f.get(this)
            }.getOrNull()

            if (value != null) return value

            val getterValue = runCatching {
                val getter = "get" + name.replaceFirstChar { it.uppercaseChar() }
                val m = this.javaClass.methods.firstOrNull {
                    it.name == getter && it.parameterTypes.isEmpty()
                }
                m?.invoke(this)
            }.getOrNull()

            if (getterValue != null) return getterValue
        }
        return null
    }

    // =============================
    // Adapter (Home mini list)
    // =============================

    private class HomeAppointmentsMiniAdapter(
        private val onClick: (HomeAppointmentItem) -> Unit
    ) : ListAdapter<HomeAppointmentItem, HomeAppointmentsMiniAdapter.VH>(Diff) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemHomeAppointmentRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return VH(binding, onClick)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(getItem(position))
        }

        class VH(
            private val binding: ItemHomeAppointmentRowBinding,
            private val onClick: (HomeAppointmentItem) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: HomeAppointmentItem) = with(binding) {
                val ctx = itemView.context

                val name = item.customerName.trim()
                    .ifBlank { ctx.getString(R.string.appointments_placeholder_value) }

                val typeLabel = HomeAppointmentsUiFormatter.formatTypeShort(item.type)
                tvHomeAppointmentTitle.text = "$typeLabel | $name"

                // Hora (Monterrey)
                tvHomeAppointmentTime.text = HomeAppointmentsUiFormatter.formatWidgetTime(
                    item.dateTime.withZoneSameInstant(ZoneId.of("America/Monterrey"))
                )

                // Color (cancelado => rojo/gray; si no, por tipo)
                val statusKey = normalizeKey(item.status.orEmpty())
                val canceled = statusKey.contains("cancel")

                val bgColorRes = if (canceled) {
                    R.color.appointments_cancel
                } else {
                    item.type.colorRes
                }

                cardHomeAppointmentItem.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, bgColorRes)
                )

                cardHomeAppointmentItem.setOnClickListener { onClick(item) }
            }

            private fun normalizeKey(text: String): String {
                return Normalizer.normalize(text.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            }
        }

        private object Diff : DiffUtil.ItemCallback<HomeAppointmentItem>() {
            override fun areItemsTheSame(
                oldItem: HomeAppointmentItem,
                newItem: HomeAppointmentItem
            ): Boolean = oldItem.id == newItem.id && oldItem.dateTime == newItem.dateTime

            override fun areContentsTheSame(
                oldItem: HomeAppointmentItem,
                newItem: HomeAppointmentItem
            ): Boolean = oldItem == newItem
        }
    }
}