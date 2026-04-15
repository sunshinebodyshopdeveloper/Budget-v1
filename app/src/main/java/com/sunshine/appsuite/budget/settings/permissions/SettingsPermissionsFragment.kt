package com.sunshine.appsuite.budget.settings.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.sunshine.appsuite.budget.databinding.FragmentSettingsPermissionsBinding

class SettingsPermissionsFragment : Fragment() {

    private var _binding: FragmentSettingsPermissionsBinding? = null
    private val binding get() = _binding!!

    private var host: SettingsPermissionsHost? = null

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshPermissionsState()
    }

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshPermissionsState()
    }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshPermissionsState()
    }

    private val requestNotificationsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshPermissionsState()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        host = when {
            parentFragment is SettingsPermissionsHost -> parentFragment as SettingsPermissionsHost
            context is SettingsPermissionsHost -> context
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
        _binding = FragmentSettingsPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPermissionRows()
        setupSupportCards()
        refreshPermissionsState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // -----------------------------
    // Setup UI / listeners
    // -----------------------------

    private fun setupPermissionRows() = with(binding) {
        // Cámara
        rowPermissionCamera.setOnClickListener { handleCameraPermissionClick() }
        switchCamera.setOnClickListener { handleCameraPermissionClick() }

        // Archivos / fotos
        rowPermissionStorage.setOnClickListener { handleStoragePermissionClick() }
        switchStorage.setOnClickListener { handleStoragePermissionClick() }

        // Ubicación
        rowPermissionLocation.setOnClickListener { handleLocationPermissionClick() }
        switchLocation.setOnClickListener { handleLocationPermissionClick() }

        // Notificaciones
        rowPermissionNotifications.setOnClickListener { handleNotificationsPermissionClick() }
        switchNotifications.setOnClickListener { handleNotificationsPermissionClick() }
    }

    private fun setupSupportCards() = with(binding) {
        cardSettingsAdmin.setOnClickListener { host?.onOpenSettingsAdmin() }
        cardSupport.setOnClickListener { host?.onOpenSupport() }
        cardAbout.setOnClickListener { host?.onOpenAbout() }
        cardLegal.setOnClickListener { host?.onOpenLegal() }
    }

    // -----------------------------
    // Estado de switches
    // -----------------------------

    private fun refreshPermissionsState() = with(binding) {
        // Cámara
        val cameraPermission = Manifest.permission.CAMERA
        val cameraGranted = isPermissionGranted(cameraPermission)
        switchCamera.isChecked = cameraGranted

        // Storage / fotos
        val storagePermission = getStoragePermission()
        val storageGranted = isPermissionGranted(storagePermission)
        switchStorage.isChecked = storageGranted

        // Ubicación (usamos FINE_LOCATION como referencia)
        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val locationGranted = isPermissionGranted(locationPermission)
        switchLocation.isChecked = locationGranted

        // Notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationsPermission = Manifest.permission.POST_NOTIFICATIONS
            val notificationsGranted = isPermissionGranted(notificationsPermission)
            switchNotifications.isChecked = notificationsGranted
            switchNotifications.isEnabled = true
            rowPermissionNotifications.isEnabled = true
            rowPermissionNotifications.alpha = 1f
        } else {
            // En versiones anteriores no hay permiso runtime de notificaciones
            switchNotifications.isChecked = true
            switchNotifications.isEnabled = false
            rowPermissionNotifications.isEnabled = false
            rowPermissionNotifications.alpha = 0.5f
        }
    }

    // -----------------------------
    // Handlers de cada permiso
    // -----------------------------

    private fun handleCameraPermissionClick() {
        val permission = Manifest.permission.CAMERA
        if (isPermissionGranted(permission)) {
            openAppSettings()
        } else {
            requestCameraPermission.launch(permission)
        }
    }

    private fun handleStoragePermissionClick() {
        val permission = getStoragePermission()
        if (isPermissionGranted(permission)) {
            openAppSettings()
        } else {
            requestStoragePermission.launch(permission)
        }
    }

    private fun handleLocationPermissionClick() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (isPermissionGranted(permission)) {
            openAppSettings()
        } else {
            requestLocationPermission.launch(permission)
        }
    }

    private fun handleNotificationsPermissionClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (isPermissionGranted(permission)) {
                openAppSettings()
            } else {
                requestNotificationsPermission.launch(permission)
            }
        } else {
            // En versiones antiguas, solo mandamos a ajustes de la app
            openAppSettings()
        }
    }

    // -----------------------------
    // Helpers
    // -----------------------------

    private fun isPermissionGranted(permission: String): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun openAppSettings() {
        val ctx = context ?: return
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", ctx.packageName, null)
        )
        startActivity(intent)
    }

    // -----------------------------
    // Host navigation hooks
    // -----------------------------

    interface SettingsPermissionsHost {
        fun onOpenSettingsAdmin()
        fun onOpenSupport()
        fun onOpenAbout()
        fun onOpenLegal()
    }
}