package com.sunshine.appsuite.budget.settings.account

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sunshine.appsuite.budget.AppSuiteApp
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.ActivityProfilePhotoBinding
import com.sunshine.appsuite.budget.user.ui.AvatarImageLoader
import kotlinx.coroutines.launch
import java.io.File
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Path
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath // Esta es la extensión correcta para Shapes
import kotlin.math.min

class ProfilePhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePhotoBinding

    private val app by lazy { application as AppSuiteApp }
    private val store by lazy { app.userProfileStore }

    private var selectedImageUri: Uri? = null
    private var tempCameraUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                binding.ivAvatar.setImageURI(uri)
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && tempCameraUri != null) {
                selectedImageUri = tempCameraUri
                binding.ivAvatar.setImageURI(tempCameraUri)
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(this, "No se otorgó permiso de cámara.", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivAvatar.post {
            setupCookieShape(binding.ivAvatar)
        }

        configureSystemBars()
        setupToolbar()
        bindInitialState()
        setupClickListeners()
    }

    private fun configureSystemBars() {
        val bgColor = ContextCompat.getColor(this, R.color.google_background_settings)
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor

        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    private fun setupToolbar() = with(binding.toolbar) {
        title = getString(R.string.account_profile_title_1)
        setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupCookieShape(view: View) {
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val width = view.width
                val height = view.height
                if (width <= 0 || height <= 0) return

                val size = min(width, height).toFloat()

                // FIRMA CORRECTA: star(numVertices, innerRadius, rounding)
                // 1. numVertices (Int): 12 para una forma más fluida
                // 2. innerRadius (Float): 0.88f (entre 0 y 1). Controla la "muesca"
                // 3. rounding (CornerRounding): El objeto que suaviza las puntas
                val cookie = RoundedPolygon.star(
                    numVerticesPerRadius = 12,        // Cambiado de numVertices a numVerticesPerRadius
                    innerRadius = 0.88f,              // Este DEBE ser un Float
                    rounding = CornerRounding(size * 0.18f) // Este DEBE ser el objeto CornerRounding
                )

                val path = Path()
                cookie.toPath(path)

                // Centrado y escalado: La forma nativa es 2x2, la llevamos al tamaño de la vista
                val matrix = Matrix()
                val scale = size / 2f
                matrix.postScale(scale, scale)
                matrix.postTranslate(width / 2f, height / 2f)
                path.transform(matrix)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    outline.setPath(path)
                } else {
                    @Suppress("DEPRECATION")
                    outline.setConvexPath(path)
                }
            }
        }
        view.clipToOutline = true
    }

    private fun bindInitialState() {
        // Avatar desde Store (API/cache). Si el usuario selecciona una imagen local, respetamos el preview.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.profile.collect { profile ->
                    if (selectedImageUri == null) {
                        AvatarImageLoader.load(
                            scope = lifecycleScope,
                            imageView = binding.ivAvatar,
                            url = profile?.avatarUrl,
                            okHttpClient = app.okHttpClient,
                            placeholderRes = R.drawable.ic_avatar
                        )
                    }
                }
            }
        }

        store.ensureFresh()
    }

    private fun setLoading(loading: Boolean) = with(binding) {
        btnChangePhoto.isEnabled = !loading
        btnRemovePhoto.isEnabled = !loading
        ivAvatar.isEnabled = !loading
        btnChangePhoto.text = if (loading) "Subiendo..." else getString(R.string.account_profile_btn_save_photo)
    }

    private fun setupClickListeners() = with(binding) {

        btnEditProfilePhoto.setOnClickListener { showImageSourceChooser() }

        btnChangePhoto.setOnClickListener {
            val uri = selectedImageUri
            if (uri == null) {
                Toast.makeText(
                    this@ProfilePhotoActivity,
                    "Primero selecciona una foto tocando la imagen.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            setLoading(true)

            lifecycleScope.launch {
                runCatching { store.updateAvatar(uri) }
                    .onSuccess {
                        Toast.makeText(this@ProfilePhotoActivity, "Avatar actualizado ✅", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .onFailure { e ->
                        Toast.makeText(
                            this@ProfilePhotoActivity,
                            "No se pudo subir: ${e.message ?: "error"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                setLoading(false)
            }
        }

        // Si en el backend no existe endpoint para borrar, aquí solo podemos informar.
        btnRemovePhoto.setOnClickListener {
            Toast.makeText(
                this@ProfilePhotoActivity,
                "Para quitar avatar en serio, necesitamos endpoint en backend (ej. DELETE /api/v1/user/avatar).",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showImageSourceChooser() {
        val options = arrayOf("Elegir de la galería", "Tomar foto")

        AlertDialog.Builder(this)
            .setTitle("Seleccionar foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
            .show()
    }

    private fun openCamera() {
        val imageFile = File.createTempFile("avatar_", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )

        tempCameraUri = uri
        takePictureLauncher.launch(uri)
    }
}
