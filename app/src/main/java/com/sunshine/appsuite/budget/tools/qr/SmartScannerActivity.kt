package com.sunshine.appsuite.budget.tools.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.orders.budget.BudgetActivity
import com.sunshine.appsuite.budget.orders.data.OrdersApi
import com.sunshine.appsuite.budget.security.TokenManager
import com.sunshine.appsuite.databinding.ActivityQrScannerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class SmartScannerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QR_VALUE = "extra_qr_value"
        const val OT_PREFIX = "OT-"
        private const val SHEET_TAG = "QrScanResultBottomSheet"
        private const val ANTI_SPAM_MS = 2000L
    }

    private lateinit var binding: ActivityQrScannerBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val uiExecutor by lazy { ContextCompat.getMainExecutor(this) }

    // Motores ML Kit (Play Services)
    private val barcodeScanner = BarcodeScanning.getClient()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var camera: Camera? = null
    private var scanLocked = false
    private var lastHitAt = 0L
    private var fetchJob: Job? = null

    private val ordersApi by lazy {
        ApiClient.createRetrofit(TokenManager(this)).create(OrdersApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener { finish() }
        binding.btnFlash.setOnClickListener { toggleFlash() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = bars.top)
            binding.footerBar.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun checkPermissions() {
        val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else toast(R.string.qr_scanner_toast_camera_permission_required)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }

                provider.unbindAll()
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }.onFailure {
                toast(R.string.qr_scanner_toast_camera_start_failed)
            }
        }, uiExecutor)
    }

    @ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (scanLocked) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        val barcodeTask = barcodeScanner.process(image)
        val textTask = textRecognizer.process(image)

        Tasks.whenAllComplete(barcodeTask, textTask).addOnCompleteListener {
            // ✅ CORRECCIÓN: Validar éxito para evitar CancellationException
            if (!isFinishing && !isDestroyed && !scanLocked) {
                val barcodes = if (barcodeTask.isSuccessful) barcodeTask.result else null
                val visionText = if (textTask.isSuccessful) textTask.result else null

                // 1. Prioridad QR (Orden de Servicio)
                val qrCode = barcodes?.firstOrNull()?.rawValue
                if (!qrCode.isNullOrBlank() && qrCode.startsWith(OT_PREFIX, ignoreCase = true)) {
                    handleDetection(qrCode, isQr = true)
                }
                // 2. Si no hay QR, buscar Placa o NIV en el texto
                else if (visionText != null) {
                    analyzeVehicleText(visionText.text)
                }
            }
            imageProxy.close()
        }
    }

    private fun analyzeVehicleText(rawText: String) {
        // Quitamos espacios y saltos de línea, pero MANTENEMOS los guiones si el OCR los lee
        val text = rawText.uppercase().trim()

        // Regex ajustado: 3 letras, guion, 3 números, guion, 1 alfanumérico
        // Ejemplo: ABC-123-A o ABC-123-4
        val platePattern = Regex("[A-Z]{3}-\\d{3}-[A-Z0-9]")
        val nivPattern = Regex("[A-HJ-NPR-Z0-9]{17}")

        val plate = platePattern.find(text)?.value
        val niv = nivPattern.find(text)?.value

        when {
            plate != null -> handleDetection(plate, isQr = false, typeLabel = "Placa")
            niv != null -> handleDetection(niv, isQr = false, typeLabel = "NIV")
        }
    }

    private fun handleDetection(value: String, isQr: Boolean, typeLabel: String = "") {
        val now = SystemClock.elapsedRealtime()
        if (now - lastHitAt < ANTI_SPAM_MS) return

        lastHitAt = now
        scanLocked = true

        uiExecutor.execute {
            // ✅ Toast solo si es Placa o NIV
            if (!isQr && typeLabel.isNotEmpty()) {
                toast("$typeLabel detectado: ${value.uppercase()}")

                val intent = Intent(this, BudgetActivity::class.java).apply {
                    putExtra("EXTRA_BUDGET_SEARCH_QUERY", value)
                    putExtra("EXTRA_BUDGET_SCAN_TYPE", typeLabel)
                }
                startActivity(intent)

                finish()
            } else {
                showBottomSheet(value, isQr)
                if (isQr) fetchOrderByCode(value)
            }
        }
    }

    private fun showBottomSheet(code: String, isQr: Boolean) {
        QrScanResultBottomSheet.newInstance(code, isQr).apply {
            callback = object : QrScanResultBottomSheet.Callback {
                override fun onDismissed() {
                    scanLocked = false
                    fetchJob?.cancel()
                }
            }
        }.show(supportFragmentManager, SHEET_TAG)
    }

    private fun fetchOrderByCode(code: String) {
        fetchJob?.cancel()
        fetchJob = lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { ordersApi.getOrder(code) }
                val sheet = supportFragmentManager.findFragmentByTag(SHEET_TAG) as? QrScanResultBottomSheet
                sheet?.showOrder(response.data)
            } catch (e: Exception) {
                // Solo mostrar error si realmente esperábamos una OT
                toast(R.string.qr_scanner_invalid_qr_title)
                val sheet = supportFragmentManager.findFragmentByTag(SHEET_TAG) as? QrScanResultBottomSheet
                sheet?.dismiss()
            }
        }
    }

    private fun toggleFlash() {
        camera?.let {
            val isTorchOn = it.cameraInfo.torchState.value == TorchState.ON
            it.cameraControl.enableTorch(!isTorchOn)
            binding.btnFlash.setImageResource(if (isTorchOn) R.drawable.ic_flash_off else R.drawable.ic_flash_on)
        }
    }

    private fun setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    // --- Helpers de Toast ---
    private fun toast(@StringRes resId: Int) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        scanLocked = true
        fetchJob?.cancel()
        cameraExecutor.shutdown()
        runCatching { barcodeScanner.close() }
        runCatching { textRecognizer.close() }
        super.onDestroy()
    }
}