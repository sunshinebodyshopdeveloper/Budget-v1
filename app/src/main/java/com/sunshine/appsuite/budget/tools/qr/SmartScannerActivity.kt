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
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.databinding.ActivitySmartScannerBinding
import com.sunshine.appsuite.budget.orders.budget.BudgetActivity
import java.util.concurrent.Executors

/**
 * Scanner inteligente que detecta QR (OT-), Placas y NIV.
 * Redirige directamente a BudgetActivity sin usar BottomSheets.
 */
class SmartScannerActivity : AppCompatActivity() {

    companion object {
        const val OT_PREFIX = "OT-"
        private const val ANTI_SPAM_MS = 1500L
    }

    private lateinit var binding: ActivitySmartScannerBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val uiExecutor by lazy { ContextCompat.getMainExecutor(this) }

    // Motores ML Kit [cite: 1]
    private val barcodeScanner = BarcodeScanning.getClient()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var camera: Camera? = null
    private var scanLocked = false
    private var lastHitAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmartScannerBinding.inflate(layoutInflater)
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

        // Procesamiento paralelo de Barcode y Texto [cite: 1]
        val barcodeTask = barcodeScanner.process(image)
        val textTask = textRecognizer.process(image)

        Tasks.whenAllComplete(barcodeTask, textTask).addOnCompleteListener {
            if (!isFinishing && !isDestroyed && !scanLocked) {
                val barcodes = if (barcodeTask.isSuccessful) barcodeTask.result else null
                val visionText = if (textTask.isSuccessful) textTask.result else null

                // 1. Prioridad QR (Orden de Servicio)
                val qrCode = barcodes?.firstOrNull()?.rawValue
                if (!qrCode.isNullOrBlank() && qrCode.startsWith(OT_PREFIX, ignoreCase = true)) {
                    navigateToBudget(qrCode.uppercase().trim())
                }
                // 2. Si no hay QR, buscar Placa o NIV
                else if (visionText != null) {
                    analyzeVehicleText(visionText.text)
                }
            }
            imageProxy.close()
        }
    }

    private fun analyzeVehicleText(rawText: String) {
        val text = rawText.uppercase().trim()

        // Regex para Placas (Ej: ABC-123-A) y NIV (17 caracteres alfanuméricos) [cite: 1]
        val platePattern = Regex("[A-Z]{3}-\\d{3}-[A-Z0-9]")
        val nivPattern = Regex("[A-HJ-NPR-Z0-9]{17}")

        val plate = platePattern.find(text)?.value
        val niv = nivPattern.find(text)?.value

        when {
            plate != null -> navigateToBudget(plate)
            niv != null -> navigateToBudget(niv)
        }
    }

    /**
     * Navega directamente a la pantalla de presupuesto.
     * Ya no se llama al QrScanResultBottomSheet.
     */
    private fun navigateToBudget(query: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastHitAt < ANTI_SPAM_MS) return

        lastHitAt = now
        scanLocked = true

        uiExecutor.execute {
            val intent = Intent(this, BudgetActivity::class.java).apply {
                putExtra("EXTRA_BUDGET_SEARCH_QUERY", query)
                // Limpiar el stack para que al dar atrás no regrese al scanner si no es necesario
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish() // Cerramos el scanner para liberar cámara
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

    private fun toast(@StringRes resId: Int) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        scanLocked = true
        cameraExecutor.shutdown()
        runCatching { barcodeScanner.close() }
        runCatching { textRecognizer.close() }
        super.onDestroy()
    }
}