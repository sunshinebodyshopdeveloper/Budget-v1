package com.sunshine.appsuite.budget.settings.system.notifications.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.settings.system.notifications.data.NotificationSettingsRepository
import com.sunshine.appsuite.budget.settings.system.notifications.model.NotificationTopic
import com.sunshine.appsuite.budget.databinding.BottomSheetNotificationControlsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationControlsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNotificationControlsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: NotificationSettingsRepository
    private lateinit var adapter: NotificationTogglesAdapter

    private var isBindingUi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = NotificationSettingsRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNotificationControlsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = NotificationTogglesAdapter { topic, enabled ->
            lifecycleScope.launch { repo.setEnabled(topic, enabled) }
        }

        binding.rvToggles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvToggles.adapter = adapter

        binding.btnClose.setOnClickListener { dismiss() }

        binding.switchAll.setOnCheckedChangeListener { _, checked ->
            if (isBindingUi) return@setOnCheckedChangeListener
            lifecycleScope.launch { repo.setAll(checked) }
        }

        // Observar estados y pintar UI
        viewLifecycleOwner.lifecycleScope.launch {
            repo.settingsFlow.collectLatest { states ->
                isBindingUi = true

                val allOn = states.values.all { it }
                val allOff = states.values.all { !it }

                // Master switch solo “true” si TODO está ON.
                binding.switchAll.isChecked = allOn

                binding.txtAllState.isVisible = true
                binding.txtAllState.text = when {
                    allOn -> "Todas activadas"
                    allOff -> "Todas desactivadas"
                    else -> "Personalizado"
                }

                val items = buildUiItems(states)
                adapter.submit(items, bindingMode = true)

                isBindingUi = false
            }
        }
    }

    private fun buildUiItems(states: Map<NotificationTopic, Boolean>): List<NotificationToggleUi> {
        val r = requireContext().resources
        fun title(resId: Int) = r.getString(resId)

        // Orden tal cual tu lista
        return listOf(
            NotificationToggleUi(NotificationTopic.TOWING_NEW_INTAKE, title(R.string.notif_towing_new_intake), states[NotificationTopic.TOWING_NEW_INTAKE] == true),
            NotificationToggleUi(NotificationTopic.OT_NEW_ORDER, title(R.string.notif_ot_new_order), states[NotificationTopic.OT_NEW_ORDER] == true),
            NotificationToggleUi(NotificationTopic.ASSIGNMENT_UNIT, title(R.string.notif_assignment_unit), states[NotificationTopic.ASSIGNMENT_UNIT] == true),
            NotificationToggleUi(NotificationTopic.TRACKING_LOCATION_CHANGE, title(R.string.notif_tracking_location_change), states[NotificationTopic.TRACKING_LOCATION_CHANGE] == true),
            NotificationToggleUi(NotificationTopic.APPOINTMENT_NEW, title(R.string.notif_appointment_new), states[NotificationTopic.APPOINTMENT_NEW] == true),
            NotificationToggleUi(NotificationTopic.UNIT_STATUS, title(R.string.notif_unit_status), states[NotificationTopic.UNIT_STATUS] == true),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = NotificationControlsBottomSheet()
    }
}
