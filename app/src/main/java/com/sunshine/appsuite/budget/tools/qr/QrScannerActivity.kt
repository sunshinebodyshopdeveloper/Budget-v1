package com.sunshine.appsuite.budget.tools.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.orders.data.OrdersApi
import com.sunshine.appsuite.budget.security.TokenManager
import com.sunshine.appsuite.budget.settings.SettingsActivity
import com.sunshine.appsuite.budget.databinding.ActivityQrScannerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class QrScannerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QR_VALUE = "extra_qr_value"
        const val EXTRA_GO_HOME = "extra_go_home"

        private const val ANTI_DOUBLE_READ_MS = 900L
        private const val INVALID_TOAST_COOLDOWN_MS = 2500L

        private const val OT_PREFIX = "OT-"
        private const val SHEET_TAG = "QrScanResultBottomSheet"
    }

    private val tokenManager by lazy { TokenManager(this) }
    private val retrofit by lazy { ApiClient.createRetrofit(tokenManager) }

    private val ordersApi by lazy { retrofit.create(OrdersApi::class.java) }

    private lateinit var binding: ActivityQrScannerBinding

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    // 👇 Importante: NO se llame mainExecutor para no chocar con Activity.getMainExecutor()
    private val uiExecutor by lazy { ContextCompat.getMainExecutor(this) }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null

    private var fetchJob: Job? = null

    private var torchOn = false
    private var hasFlashUnit = false

    private var scanLocked = false
    private var lastHitAt = 0L

    // Anti-spam toast para inválidos
    private var lastInvalidAt = 0L
    private var lastInvalidValue: String? = null

    private var permissionDialogShown = false

    // Lifecycle flags
    private var shouldGoHomeOnReturn = false
    private var navigatingToPermissions = false
    private var requestingCameraPermission = false

    private val scanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        requestingCameraPermission = false
        if (granted) startCamera()
        else {
            toast(R.string.qr_scanner_toast_camera_permission_required)
            closeAndGoHome()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        applyInsets()

        binding.btnClose.setOnClickListener { closeAndGoHome() }
        binding.btnFlash.setOnClickListener { toggleFlash() }
        binding.footerContent.setOnClickListener { showGoogleTechDialog() }
        binding.btnInfo.setOnClickListener { showGoogleTechDialog() }

        updateFlashIcon()
        applyFlashAvailabilityUi(false)

        ensureCameraPermissionAndStart()
    }

    override fun onStart() {
        super.onStart()

        // Background real -> al volver, Home
        if (shouldGoHomeOnReturn) {
            shouldGoHomeOnReturn = false
            closeAndGoHome()
            return
        }

        // Volviendo desde Ajustes/permisos
        if (navigatingToPermissions) {
            navigatingToPermissions = false
            if (hasCameraPermission()) {
                if (cameraProvider == null) startCamera()
            } else {
                toast(R.string.qr_scanner_toast_camera_permission_required)
                closeAndGoHome()
            }
            return
        }

        // Fallback: si hay permiso y no hay cámara inicializada, arranca
        if (hasCameraPermission() && cameraProvider == null && !requestingCameraPermission) {
            startCamera()
        }
    }

    override fun onStop() {
        super.onStop()

        fetchJob?.cancel()

        // Torch off al salir (privacidad/batería)
        stopTorchSafely()

        // Config changes / permisos / solicitud -> no cuentan como background real
        if (isChangingConfigurations) return
        if (navigatingToPermissions || requestingCameraPermission) return

        // Background real
        shouldGoHomeOnReturn = true
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = bars.top)
            binding.footerBar.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun showGoogleTechDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.qr_scanner_google_tech_title))
            .setMessage(getString(R.string.qr_scanner_google_tech_message))
            .setPositiveButton(getString(R.string.qr_scanner_understood), null)
            .show()
    }

    private fun ensureCameraPermissionAndStart() {
        when {
            hasCameraPermission() -> startCamera()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ->
                showCameraPermissionDialogOnce()

            else -> {
                requestingCameraPermission = true
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showCameraPermissionDialogOnce() {
        if (permissionDialogShown) return
        permissionDialogShown = true

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.qr_scanner_camera_permission_title)
            .setMessage(R.string.qr_scanner_camera_permission_message)
            .setPositiveButton(R.string.qr_scanner_camera_permission_go) { _, _ ->
                navigatingToPermissions = true
                SettingsActivity.Companion.start(this, SettingsActivity.Section.PERMISSIONS)
            }
            .setNegativeButton(R.string.qr_scanner_camera_permission_close) { _, _ ->
                closeAndGoHome()
            }
            .show()
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().apply {
                        targetRotation = binding.previewView.display?.rotation ?: 0
                    }

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        // ✅ IMPORTANTÍSIMO: siempre cerrar imageProxy en early return
                        if (scanLocked) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        scanner.process(image)
                            .addOnSuccessListener(uiExecutor) { barcodes ->
                                handleScanSuccess(barcodes)
                            }
                            .addOnFailureListener(uiExecutor) {
                                // Si falla un frame, no pasa nada
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } catch (_: Exception) {
                        imageProxy.close()
                    }
                }

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )

                hasFlashUnit = camera?.cameraInfo?.hasFlashUnit() == true
                applyFlashAvailabilityUi(hasFlashUnit)

                if (!hasFlashUnit) {
                    torchOn = false
                    updateFlashIcon()
                }
            }.onFailure {
                toast(R.string.qr_scanner_toast_camera_start_failed)
                closeAndGoHome()
            }
        }, uiExecutor) // ✅ aquí también va uiExecutor
    }

    private fun handleScanSuccess(barcodes: List<Barcode>) {
        if (scanLocked) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastHitAt < ANTI_DOUBLE_READ_MS) return

        val raw = barcodes
            .firstOrNull { !it.rawValue.isNullOrBlank() }
            ?.rawValue
            .orEmpty()

        val normalized = raw
            .replace("\n", "")
            .replace("\r", "")
            .trim()

        if (normalized.isBlank()) return

        val isOt = normalized.startsWith(OT_PREFIX, ignoreCase = true) &&
                normalized.length > OT_PREFIX.length

        lastHitAt = now

        // NO OT => toast con cooldown para no spamear
        if (!isOt) {
            val same = normalized.equals(lastInvalidValue, ignoreCase = true)
            val within = (now - lastInvalidAt) < INVALID_TOAST_COOLDOWN_MS
            if (!same || !within) {
                toast(R.string.qr_scanner_invalid_qr_title)
                lastInvalidValue = normalized
                lastInvalidAt = now
            }
            return
        }

        // OT => abrir sheet y fetch
        scanLocked = true
        showScanBottomSheet(normalized)
        fetchOrderByCode(normalized)
    }

    private fun showScanBottomSheet(code: String) {
        val fm = supportFragmentManager

        val existing = fm.findFragmentByTag(SHEET_TAG) as? QrScanResultBottomSheet
        if (existing != null) {
            existing.showLoading(code)
            return
        }

        QrScanResultBottomSheet.Companion.newInstance(code, true).apply {
            callback = object : QrScanResultBottomSheet.Callback {
                override fun onDismissed() {
                    fetchJob?.cancel()
                    lastHitAt = SystemClock.elapsedRealtime()
                    binding.root.postDelayed({ scanLocked = false }, 250L)
                }
            }
        }.show(fm, SHEET_TAG)
    }

    private fun fetchOrderByCode(code: String) {
        fetchJob?.cancel()
        fetchJob = lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ordersApi.getOrder(code)
                }
                val sheet = supportFragmentManager.findFragmentByTag(SHEET_TAG) as? QrScanResultBottomSheet
                sheet?.showOrder(response.data)
            } catch (_: Exception) {
                toast(R.string.qr_scanner_invalid_qr_title)
                val sheet = supportFragmentManager.findFragmentByTag(SHEET_TAG) as? QrScanResultBottomSheet
                sheet?.dismiss()
            }
        }
    }

    private fun closeAndGoHome() {
        setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_GO_HOME, true))
        finish()
    }

    private fun applyFlashAvailabilityUi(hasFlash: Boolean) {
        binding.btnFlash.isEnabled = hasFlash
        binding.btnFlash.alpha = if (hasFlash) 1f else 0.35f
    }

    private fun updateFlashIcon() {
        binding.btnFlash.setImageResource(
            if (torchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        )
    }

    private fun toggleFlash() {
        val cam = camera ?: run {
            toast(R.string.qr_scanner_toast_flash_not_ready)
            return
        }

        if (!hasFlashUnit || cam.cameraInfo.hasFlashUnit() != true) {
            toast(R.string.qr_scanner_toast_no_flash_unit)
            return
        }

        torchOn = !torchOn
        cam.cameraControl.enableTorch(torchOn)
        updateFlashIcon()

        toast(if (torchOn) R.string.qr_scanner_toast_flash_on else R.string.qr_scanner_toast_flash_off)
    }

    private fun stopTorchSafely() {
        runCatching { camera?.cameraControl?.enableTorch(false) }
        if (torchOn) {
            torchOn = false
            updateFlashIcon()
        }
    }

    // Si algún flujo TUYO todavía usa este result, aquí sigue listo.
    private fun returnResult(value: String) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_QR_VALUE, value))
        finish()
    }

    private fun toast(@StringRes resId: Int) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopTorchSafely()

        runCatching { fetchJob?.cancel() }
        runCatching { cameraProvider?.unbindAll() }
        runCatching { scanner.close() }

        cameraExecutor.shutdown()
    }
}
