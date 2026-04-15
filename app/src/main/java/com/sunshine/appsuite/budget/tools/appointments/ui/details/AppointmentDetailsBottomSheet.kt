package com.sunshine.appsuite.budget.tools.appointments.ui.details

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sunshine.appsuite.budget.BudgetApp
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.tools.appointments.data.network.AppointmentsApi
import com.sunshine.appsuite.budget.tools.appointments.data.remote.model.AppointmentDto
import com.sunshine.appsuite.budget.tools.appointments.data.repository.AppointmentsRepository
import com.sunshine.appsuite.budget.tools.appointments.ui.edit.EditAppointmentActivity
import com.sunshine.appsuite.budget.tools.appointments.ui.mapper.AppointmentMapper
import com.sunshine.appsuite.budget.tools.appointments.ui.model.AppointmentUi
import com.sunshine.appsuite.budget.databinding.BottomSheetAppointmentDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

class AppointmentDetailsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAppointmentDetailsBinding? = null
    private val binding get() = _binding!!
    private var currentPhone: String? = null
    private var currentEmail: String? = null
    private var currentId: Long = 0L
    private var currentName: String? = null
    private var currentType: String? = null
    private var currentDateLabel: String? = null
    private var currentTimeLabel: String? = null
    private var currentClientId: Long? = null
    private var currentClientName: String? = null
    private var clientNameById: Map<Long, String> = emptyMap()
    private var isLoadingClients: Boolean = false
    private enum class StatusState { PENDING, CONFIRMED, CANCELLED, OTHER }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAppointmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        setupFullHeightBehavior()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val a = readArgs()
        currentId = a.id
        render(a)

        binding.cardTel.setOnClickListener { openDialer() }
        binding.cardEmail.setOnClickListener { openEmail() }
        binding.cardWts.setOnClickListener { openWhatsApp() }

        binding.btnStatusAppointment.setOnClickListener { confirmThroughApi() }
        binding.btnCancelAppointment.setOnClickListener { showCancelConfirmDialog() }

        if (a.id > 0L) refreshFromApi(a.id)

        binding.editAppointment.setOnClickListener { openEditAppointment() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFullHeightBehavior() {
        val bottomSheet =
            dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return

        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        BottomSheetBehavior.from(bottomSheet).apply {
            isFitToContents = true
            expandedOffset = 0
            skipCollapsed = false

            val screenH = resources.displayMetrics.heightPixels
            peekHeight = (screenH * 0.60f).roundToInt()
        }
    }

    private fun buildRepo(): AppointmentsRepository? {
        val app = requireActivity().application as? BudgetApp ?: return null
        val api = app.retrofit.create(AppointmentsApi::class.java)
        return AppointmentsRepository(api)
    }

    private fun refreshFromApi(id: Long) {
        val repo = buildRepo() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            repo.fetchAppointment(id)
                .onSuccess { dto ->
                    renderDto(dto)
                    applyClientTypeFromDto(dto, repo)
                }
                .onFailure { err ->
                    Log.w(TAG, "refreshFromApi failed id=$id: ${err.message}")
                }
        }
    }

    private fun renderDto(dto: AppointmentDto) {
        val ui = runCatching { AppointmentMapper.toUi(dto) }.getOrNull()

        render(
            Args(
                id = ui?.id ?: (dto.id ?: 0L),
                type = ui?.type?.displayName ?: dto.type,
                dateLabel = ui?.dateLabel ?: dto.date,
                timeLabel = ui?.timeLabel ?: dto.time,
                customerName = ui?.customerName ?: dto.displayClientName,
                customerPhone = ui?.customerPhone ?: dto.displayPhone,
                customerEmail = ui?.customerEmail ?: dto.displayEmail,
                statusLabel = ui?.status ?: dto.status,
                comment = ui?.comment ?: dto.comment,
                vehicleBrand = dto.brand,
                vehicleModel = dto.model,
                vehicleColor = dto.color,
                vehicleYear = dto.yearText,
                vehiclePlate = dto.plate
            )
        )
    }

    private suspend fun applyClientTypeFromDto(dto: AppointmentDto, repo: AppointmentsRepository) {
        val placeholder = runCatching { getString(R.string.appointments_placeholder_value) }.getOrNull() ?: "-"
        val raw = dto.clientType?.trim().orEmpty()

        // Si el backend ya manda nombre (no numérico), lo pintamos directo.
        val id = raw.toLongOrNull()
        if (raw.isBlank()) {
            currentClientId = null
            currentClientName = null
            isLoadingClients = false
            renderClientChip(placeholder)
            return
        }

        if (id == null) {
            currentClientId = null
            currentClientName = raw
            isLoadingClients = false
            renderClientChip(placeholder)
            return
        }

        currentClientId = id

        // Si ya tenemos el catálogo cacheado en el BottomSheet, resolvemos sin red.
        clientNameById[id]?.let { name ->
            currentClientName = name
            isLoadingClients = false
            renderClientChip(placeholder)
            return
        }

        // No mostramos el ID. Mostramos estado de carga mientras resolvemos.
        isLoadingClients = true
        renderClientChip(placeholder)

        val map = runCatching {
            withContext(Dispatchers.IO) {
                // fetchClients(): Result<List<ClientDto>>
                repo.fetchClients().getOrThrow()
            }.associate { it.id to it.name }
        }.getOrElse { err ->
            Log.w(TAG, "fetchClients failed: ${err.message}")
            emptyMap()
        }

        clientNameById = map
        currentClientName = map[id]
        isLoadingClients = false
        renderClientChip(placeholder)
    }

    private fun renderClientChip(placeholder: String) {
        val label = when {
            !currentClientName.isNullOrBlank() -> currentClientName!!.trim()
            isLoadingClients && currentClientId != null -> "Cargando cliente…"
            currentClientId != null -> "Cliente desconocido"
            else -> placeholder
        }

        binding.chipClientType.text = label
    }

    private fun render(a: Args) = with(binding) {
        val placeholder = getString(R.string.appointments_placeholder_value)

        renderClientChip(placeholder)

        tvAppointmentType.text = a.type.orPlaceholder(placeholder)
        tvFactDateValue.text = a.dateLabel.orPlaceholder(placeholder)
        tvFactTimeValue.text = a.timeLabel.orPlaceholder(placeholder)
        tvFactHostValue.text = a.customerName.orPlaceholder(placeholder)
        tvFactStatusValue.text = a.statusLabel.orPlaceholder(placeholder)
        tvVehicleBrandValue.text = a.vehicleBrand.orPlaceholder(placeholder)
        tvVehicleModelValue.text = a.vehicleModel.orPlaceholder(placeholder)
        tvVehicleColorValue.text = a.vehicleColor.orPlaceholder(placeholder)
        tvVehicleYearValue.text = a.vehicleYear.orPlaceholder(placeholder)
        tvVehiclePlatesValue.text = a.vehiclePlate.orPlaceholder(placeholder)

        val commentText = a.comment?.trim()?.takeIf { it.isNotBlank() }
            ?: getString(R.string.appointments_comment_empty)
        tvAppointmentComment.text = commentText

        currentPhone = a.customerPhone?.trim()?.takeIf { it.isNotBlank() }
        currentEmail = a.customerEmail?.trim()?.takeIf { it.isNotBlank() }
        currentName = a.customerName?.trim()?.takeIf { it.isNotBlank() }
        currentType = a.type?.trim()?.takeIf { it.isNotBlank() }
        currentDateLabel = a.dateLabel?.trim()?.takeIf { it.isNotBlank() }
        currentTimeLabel = a.timeLabel?.trim()?.takeIf { it.isNotBlank() }

        tvTel.text = currentPhone.orPlaceholder(placeholder)
        tvEmail.text = currentEmail.orPlaceholder(placeholder)
        tvWts.text = currentPhone.orPlaceholder(placeholder)

        val state = resolveStatusState(a.statusLabel)

        // ✅ Confirmar SOLO si está PENDING (Sin confirmar)
        val showConfirm = (state == StatusState.PENDING) && (a.id > 0L)
        btnStatusAppointment.isVisible = showConfirm
        btnStatusAppointment.isEnabled = showConfirm
        if (showConfirm) btnStatusAppointment.text = getString(R.string.appointments_details_confirm_button)

        // ✅ Cancelar SOLO si está CONFIRMED (Confirmada)
        val showCancel = (state == StatusState.CONFIRMED) && (a.id > 0L)
        btnCancelAppointment.isVisible = showCancel
        btnCancelAppointment.isEnabled = showCancel
        if (showCancel) btnCancelAppointment.text = getString(R.string.appointments_cancel_appointment)
    }

    private fun confirmThroughApi(): Unit = with(binding) {
        val id = currentId
        if (id <= 0L) {
            Toast.makeText(requireContext(), "ID inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val repo = buildRepo() ?: return

        btnStatusAppointment.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    repo.confirmAppointment(id).getOrThrow()
                }
            }.isSuccess

            btnStatusAppointment.isEnabled = true

            if (ok) {
                Toast.makeText(requireContext(), "Cita confirmada ✅", Toast.LENGTH_SHORT).show()
                refreshFromApi(id)
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY_UPDATED,
                    bundleOf(RESULT_APPOINTMENT_ID to id)
                )
            } else {
                Toast.makeText(requireContext(), "No se pudo confirmar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCancelConfirmDialog() {
        val id = currentId
        if (id <= 0L) {
            Toast.makeText(requireContext(), "ID inválido", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.appointments_cancel_appointment))
            .setMessage(getString(R.string.appointments_cancel_confirm_message))
            .setPositiveButton(getString(R.string.appointments_cancel_confirm_yes)) { _, _ ->
                cancelThroughApi(id)
            }
            .show()
    }

    private fun cancelThroughApi(id: Long) {
        val repo = buildRepo() ?: return

        binding.btnCancelAppointment.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    repo.cancelAppointment(id).getOrThrow()
                }
            }.isSuccess

            binding.btnCancelAppointment.isEnabled = true

            if (ok) {
                Toast.makeText(requireContext(), "Cita cancelada ✅", Toast.LENGTH_SHORT).show()
                refreshFromApi(id)
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY_UPDATED,
                    bundleOf(RESULT_APPOINTMENT_ID to id)
                )
            } else {
                Toast.makeText(requireContext(), "No se pudo cancelar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openDialer() {
        val phone = currentPhone?.trim().orEmpty()
        if (phone.isBlank()) return
        runCatching {
            startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phone") })
        }
    }

    private fun openEmail() {
        val email = currentEmail?.trim().orEmpty()
        if (email.isBlank()) return
        runCatching {
            startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:$email") })
        }
    }

    private fun openWhatsApp() {
        val phone = currentPhone?.trim().orEmpty()
        if (phone.isBlank()) return

        val normalized = phone.replace("\\D".toRegex(), "")
        val msg = buildWhatsappMessage()
        val url = "https://wa.me/52$normalized?text=${Uri.encode(msg)}"

        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(requireContext(), "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildWhatsappMessage(): String {
        val name = currentName.orEmpty()
        val type = currentType.orEmpty()
        val date = currentDateLabel.orEmpty()
        val time = currentTimeLabel.orEmpty()

        val safeName = name.ifBlank { "Cliente" }
        val safeType = type.ifBlank { "cita" }

        val msg = "Hola $safeName, te contacto por tu $safeType programada para $date a las $time."
        return normalizeWhatsappText(msg)
    }

    private fun normalizeWhatsappText(text: String): String {
        val n = Normalizer.normalize(text, Normalizer.Form.NFD)
        return n.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    private fun resolveStatusState(status: String?): StatusState {
        val s = status?.trim()?.lowercase(Locale.US).orEmpty()
        return when {
            s.contains("pending") || s.contains("sin confirmar") -> StatusState.PENDING
            s.contains("confirmed") || s.contains("confirmada") -> StatusState.CONFIRMED
            s.contains("cancel") || s.contains("cancelada") -> StatusState.CANCELLED
            else -> StatusState.OTHER
        }
    }

    private fun String?.orPlaceholder(placeholder: String): String {
        val v = this?.trim().orEmpty()
        return if (v.isBlank()) placeholder else v
    }

    private fun readArgs(): Args {
        val b = arguments
        return Args(
            id = b?.getLong(KEY_ID) ?: 0L,
            type = b?.getString(KEY_TYPE),
            dateLabel = b?.getString(KEY_DATE),
            timeLabel = b?.getString(KEY_TIME),
            customerName = b?.getString(KEY_NAME),
            customerPhone = b?.getString(KEY_PHONE),
            customerEmail = b?.getString(KEY_EMAIL),
            statusLabel = b?.getString(KEY_STATUS),
            comment = b?.getString(KEY_COMMENT),
            vehicleBrand = b?.getString(KEY_VEHICLE_BRAND),
            vehicleModel = b?.getString(KEY_VEHICLE_MODEL),
            vehicleColor = b?.getString(KEY_VEHICLE_COLOR),
            vehicleYear = b?.getString(KEY_VEHICLE_YEAR),
            vehiclePlate = b?.getString(KEY_VEHICLE_PLATE)
        )
    }

    data class Args(
        val id: Long,
        val type: String?,
        val dateLabel: String?,
        val timeLabel: String?,
        val customerName: String?,
        val customerPhone: String?,
        val customerEmail: String?,
        val statusLabel: String?,
        val comment: String?,
        val vehicleBrand: String?,
        val vehicleModel: String?,
        val vehicleColor: String?,
        val vehicleYear: String?,
        val vehiclePlate: String?
    )

    companion object {
        const val TAG = "AppointmentDetailsBottomSheet"

        const val RESULT_KEY_UPDATED = "appointment_updated"
        const val RESULT_APPOINTMENT_ID = "appointment_id"

        private const val KEY_ID = "id"
        private const val KEY_TYPE = "type"
        private const val KEY_DATE = "date"
        private const val KEY_TIME = "time"
        private const val KEY_NAME = "name"
        private const val KEY_PHONE = "phone"
        private const val KEY_EMAIL = "email"
        private const val KEY_STATUS = "status"
        private const val KEY_COMMENT = "comment"

        private const val KEY_VEHICLE_BRAND = "vehicle_brand"
        private const val KEY_VEHICLE_MODEL = "vehicle_model"
        private const val KEY_VEHICLE_COLOR = "vehicle_color"
        private const val KEY_VEHICLE_YEAR = "vehicle_year"
        private const val KEY_VEHICLE_PLATE = "vehicle_plate"

        fun newInstance(appointment: AppointmentUi): AppointmentDetailsBottomSheet {
            return AppointmentDetailsBottomSheet().apply {
                arguments = bundleOf(
                    KEY_ID to appointment.id,
                    KEY_TYPE to appointment.type.displayName,
                    KEY_DATE to appointment.dateLabel,
                    KEY_TIME to appointment.timeLabel,
                    KEY_NAME to appointment.customerName,
                    KEY_PHONE to appointment.customerPhone,
                    KEY_EMAIL to appointment.customerEmail,
                    KEY_STATUS to appointment.status,
                    KEY_COMMENT to appointment.comment
                )
            }
        }
    }

    private val editLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                refreshFromApi(currentId)
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY_UPDATED,
                    bundleOf(RESULT_APPOINTMENT_ID to currentId)
                )
            }
        }

    private fun openEditAppointment() {
        val id = currentId
        if (id <= 0L) {
            Toast.makeText(requireContext(), "ID inválido: $id", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(requireContext(), EditAppointmentActivity::class.java).apply {
            putExtra(EditAppointmentActivity.Companion.EXTRA_APPOINTMENT_ID, id)
        }

        editLauncher.launch(intent)
    }
}
