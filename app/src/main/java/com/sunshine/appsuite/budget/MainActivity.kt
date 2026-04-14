package com.sunshine.appsuite.budget

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.sunshine.appsuite.budget.assistant.AssistantActivity
import com.sunshine.appsuite.databinding.ActivityMainBinding
import com.sunshine.appsuite.budget.home.ui.HomeFragment
import com.sunshine.appsuite.budget.legal.AboutActivity
import com.sunshine.appsuite.budget.legal.LegalActivity
import com.sunshine.appsuite.budget.menu.MainBottomNavController
import com.sunshine.appsuite.budget.menu.MainDrawerController
import com.sunshine.appsuite.budget.menu.ProfileMenuListener
import com.sunshine.appsuite.budget.onboarding.OnboardingActivity
import com.sunshine.appsuite.budget.tools.qr.SmartScannerActivity
import com.sunshine.appsuite.budget.orders.ServiceOrderActivity
import com.sunshine.appsuite.budget.settings.SettingsActivity
import com.sunshine.appsuite.budget.settings.security.AppLockActivity
import com.sunshine.appsuite.budget.settings.security.SecurityAdvisoryNotifier
import com.sunshine.appsuite.budget.settings.security.SecurityPreferences
import com.sunshine.appsuite.budget.system.network.NetworkUtils
import com.sunshine.appsuite.budget.system.network.NoInternetActivity
import com.sunshine.appsuite.budget.tools.appointments.AppointmentsActivity
import com.sunshine.appsuite.budget.user.ui.ProfileBottomSheetDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.update.UpdateActivity
import com.sunshine.appsuite.budget.update.UpdateManager
import com.sunshine.appsuite.budget.update.UpdateResponse

class MainActivity : AppCompatActivity(), ProfileMenuListener {

    private var backToExit = false
    private var backToExitJob: Job? = null
    private var isMainUiShown = false

    // Guardamos colores originales de barras
    private var defaultStatusBarColor: Int = 0
    private var defaultNavBarColor: Int = 0

    // Binding para la UI REAL (home con drawer + bottom nav)
    private lateinit var binding: ActivityMainBinding
    private val app by lazy { application as AppSuiteApp }

    // Bottom nav controller
    private lateinit var bottomNavController: MainBottomNavController

    // Drawer controller
    private lateinit var drawerController: MainDrawerController

    // Botones superiores (menú hamburguesa + perfil)
    private lateinit var btnMenuCard: MaterialCardView
    private lateinit var btnProfileCard: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash del sistema (Android 12+ / compat)
        val splash = installSplashScreen()

        // Mantener splash solo mientras resolvemos el arranque
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)

        // Si no hay internet: nos vamos y ya (sin montar UI)
        if (!NetworkUtils.isOnline(this)) {
            keepSplash = false
            startActivity(Intent(this, NoInternetActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
            return
        }

        // Arranque sin UI custom: resolvemos lógica y luego mostramos UI real
        lifecycleScope.launch {
            // Onboarding
            if (!isOnboardingCompleted()) {
                keepSplash = false
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
                return@launch
            }

            // Token
            if (!app.tokenManager.hasToken()) {
                keepSplash = false
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // App lock (huella / PIN)
            if (shouldShowAppLock()) {
                keepSplash = false
                startActivity(Intent(this@MainActivity, AppLockActivity::class.java))
                finish()
                return@launch
            }

            // Ya puede ver el home → montamos la UI real
            showMainUi(savedInstanceState)
            keepSplash = false
        }

        setupDoubleBackToExit()
    }

    override fun onResume() {
        super.onResume()
        if (isMainUiShown) {
            SecurityAdvisoryNotifier.maybeShow(this)
        }
    }

    // ------------------------
    //   UI REAL (home / tabs)
    // ------------------------

    private fun showMainUi(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupDrawer()
        setupDrawerBottomActions()
        grabTopButtons()
        setupTopButtons()
        setupBottomNav()
        setupDrawerHeaderUi()
        createNotificationChannel()
        checkForUpdatesSilently()

        if (savedInstanceState == null) {
            bottomNavController.setHomeSelected()
            replaceFragment(HomeFragment())
        }

        isMainUiShown = true
        SecurityAdvisoryNotifier.maybeShow(this)
    }

    private fun setupSystemBars() {
        val status = ContextCompat.getColor(this, R.color.google_white)
        val nav = ContextCompat.getColor(this, R.color.google_white)

        window.statusBarColor = status
        window.navigationBarColor = nav

        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }

    /**
     * Pinta system bars + define si los íconos van claros u oscuros,
     * basándose en el color real (día/noche/dynamic color se adaptan solos).
     */
    private fun applySystemBars(statusColor: Int, navColor: Int) {
        window.statusBarColor = statusColor
        window.navigationBarColor = navColor

        val controller = WindowCompat.getInsetsController(window, binding.drawerLayout)
        controller.isAppearanceLightStatusBars = ColorUtils.calculateLuminance(statusColor) > 0.5
        controller.isAppearanceLightNavigationBars = ColorUtils.calculateLuminance(navColor) > 0.5
    }

    private fun setupDrawer() {
        drawerController = MainDrawerController(
            drawerLayout = binding.drawerLayout,
            navigationView = binding.navigationView
        ) { item -> onDrawerItemSelected(item) }

        val drawerScrimColor = Color.parseColor("#55000000") // ~33% negro
        binding.drawerLayout.setScrimColor(drawerScrimColor)

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val t = slideOffset.coerceIn(0f, 1f)

                val statusBlended = ColorUtils.blendARGB(
                    defaultStatusBarColor,
                    drawerScrimColor,
                    t
                )

                val navBlended = ColorUtils.blendARGB(
                    defaultNavBarColor,
                    drawerScrimColor,
                    t
                )

                applySystemBars(statusBlended, navBlended)
            }

            override fun onDrawerOpened(drawerView: View) {
                applySystemBars(drawerScrimColor, drawerScrimColor)
            }

            override fun onDrawerClosed(drawerView: View) {
                applySystemBars(defaultStatusBarColor, defaultNavBarColor)
            }
        })
    }

    private fun onDrawerItemSelected(item: MainDrawerController.Item) {
        when (item) {
            MainDrawerController.Item.HOME -> {
                bottomNavController.setHomeSelected()
                replaceFragment(HomeFragment())
            }

            MainDrawerController.Item.SCAN_QR -> {
                bottomNavController.setQrSelected()
                openScanner()
            }

            MainDrawerController.Item.SERVICES_ORDERS -> {
                startActivity(Intent(this, ServiceOrderActivity::class.java))
            }

            MainDrawerController.Item.APPOINTMENTS -> {
                startActivity(Intent(this, AppointmentsActivity::class.java))
            }

            MainDrawerController.Item.ABOUT -> {
                startActivity(Intent(this, AboutActivity::class.java))
            }

            MainDrawerController.Item.HELP -> {
                SettingsActivity.Companion.start(this, SettingsActivity.Section.SUPPORT)
            }

            MainDrawerController.Item.SETTINGS -> {
                SettingsActivity.Companion.start(this, SettingsActivity.Section.HOME)
            }

            MainDrawerController.Item.LOGOUT -> {
                performLogout()
            }

            // TODOs
            MainDrawerController.Item.ENTER_TOW -> toastTodo("Ingreso grúa")
            MainDrawerController.Item.NEW_INVENTORY -> toastTodo("Nuevo inventario")
            MainDrawerController.Item.ASSIGNMENTS -> toastTodo("Asignaciones")
            MainDrawerController.Item.TRACKING -> toastTodo("Rastreo")
            MainDrawerController.Item.PARTS -> toastTodo("Refacciones")
        }
    }

    private fun toastTodo(label: String) {
        Toast.makeText(this, "$label (TODO)", Toast.LENGTH_SHORT).show()
    }

    private fun setupDrawerBottomActions() {
        val btnSettings = binding.drawerLayout.findViewById<View>(R.id.btn_settings)
        val btnLogout = binding.drawerLayout.findViewById<View>(R.id.btn_logout)

        btnSettings?.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            SettingsActivity.Companion.start(this@MainActivity, SettingsActivity.Section.HOME)
        }

        btnLogout?.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            performLogout()
        }
    }

    private fun grabTopButtons() {
        btnMenuCard = findViewById(R.id.btnMenu)
        btnProfileCard = findViewById(R.id.btnProfile)
    }

    private fun setupTopButtons() {
        btnMenuCard.setOnClickListener {
            binding.drawerLayout.open()
        }

        btnProfileCard.setOnClickListener {
            val sheet = ProfileBottomSheetDialog()
            sheet.setProfileMenuListener(this)
            sheet.show(supportFragmentManager, "ProfileBottomSheet")
        }
    }

    private fun setupBottomNav() {
        val bottomNavRoot = findViewById<View>(R.id.bottom_nav_main)
        bottomNavController = MainBottomNavController(bottomNavRoot) { tab ->
            when (tab) {
                MainBottomNavController.Tab.HOME -> replaceFragment(HomeFragment())
                MainBottomNavController.Tab.QR -> openScanner()
                MainBottomNavController.Tab.ASSISTANT -> {
                    appsLauncher.launch(Intent(this, AssistantActivity::class.java))
                }
            }
        }
    }

    private fun setupDrawerHeaderUi() {
        val header = binding.navigationView.getHeaderView(0)
        // (Por ahora el header no tiene campos de perfil; el perfil vive en UserProfileStore.)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun openScanner() {
        if (isMainUiShown && binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        //qrScannerLauncher.launch(Intent(this, QrScannerActivity::class.java))
        qrScannerLauncher.launch(Intent(this, SmartScannerActivity::class.java))
    }

    // ------------------------
    //   ONBOARDING FLAG
    // ------------------------

    private fun isOnboardingCompleted(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("onboarding_completed", false)
    }

    // ------------------------
    //   APP LOCK FLAG
    // ------------------------

    private fun shouldShowAppLock(): Boolean {
        val skipLock = intent?.getBooleanExtra(
            AppLockActivity.Companion.EXTRA_SKIP_APP_LOCK,
            false
        ) ?: false

        if (skipLock) return false

        val biometricEnabled = SecurityPreferences.isBiometricEnabled(this)
        val pinEnabled = SecurityPreferences.isPinEnabled(this)

        return biometricEnabled || pinEnabled
    }

    // ------------------------
    //   LOGOUT COMPARTIDO
    // ------------------------

    private fun performLogout() {
        val app = application as AppSuiteApp
        app.tokenManager.clearToken()
        app.userProfileStore.clear()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupDoubleBackToExit() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                    return
                }

                if (backToExit) {
                    finishAffinity()
                    return
                }

                backToExit = true
                Toast.makeText(this@MainActivity, getString(R.string.back_exit_confirm), Toast.LENGTH_SHORT).show()

                backToExitJob?.cancel()
                backToExitJob = lifecycleScope.launch {
                    delay(1800)
                    backToExit = false
                }
            }
        })
    }

    // ------------------------
    //   ProfileMenuListener
    // ------------------------

    override fun onProfileLogout() = performLogout()

    override fun onProfileOpenAbout() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    override fun onProfileOpenLegal() {
        startActivity(Intent(this, LegalActivity::class.java))
    }

    private val appsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            bottomNavController.setHomeSelected()
            replaceFragment(HomeFragment())
        }

    private val qrScannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val qrValue = result.data
                    //?.getStringExtra(QrScannerActivity.EXTRA_QR_VALUE)
                    ?.getStringExtra(SmartScannerActivity.Companion.EXTRA_QR_VALUE)
                    .orEmpty()
                    .trim()

                if (qrValue.isNotBlank()) {
                    onQrScanned(qrValue)
                } else {
                    Toast.makeText(this, "QR vacío 🤨", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (isMainUiShown) {
                    bottomNavController.setHomeSelected()
                    replaceFragment(HomeFragment())
                }
            }
        }

    private fun onQrScanned(value: String) {
        Toast.makeText(this, "QR: $value", Toast.LENGTH_SHORT).show()
    }

    // ------------------------
    //   UpdateManager
    // ------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Actualizaciones de Sistema"
            val descriptionText = "Notificaciones sobre nuevas versiones de AppSuite"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("updates_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkForUpdatesSilently() {
        val currentCode = getAppVersionCode()
        lifecycleScope.launch {
            val newVersion = UpdateManager.checkNewVersion(currentCode)
            if (newVersion != null) {
                // CAMBIO: Ahora llamamos a la notificación en lugar del diálogo
                showUpdateNotification(newVersion)
            }
        }
    }

    private fun showUpdateNotification(update: UpdateResponse) {
        // Intent para que al tocar la notificación abra la UpdateActivity
        val intent = Intent(this, UpdateActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Construcción de la notificación
        val builder = NotificationCompat.Builder(this, "updates_channel")
            .setSmallIcon(R.drawable.ic_update) // <--- ASEGÚRATE DE TENER ESTE ICONO
            .setContentTitle("Nueva versión disponible")
            .setContentText("AppSuite ${update.versionName} está lista para descargar.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Se elimina al hacer clic
            .setColor(ContextCompat.getColor(this, R.color.md_theme_primary))

        // Lanzar la notificación
        try {
            with(NotificationManagerCompat.from(this)) {
                // Usamos un ID fijo (101) para que no se dupliquen si hay varios avisos
                notify(101, builder.build())
            }
        } catch (e: SecurityException) {
            // Manejo para Android 13+ si el usuario denegó permisos de notificación
            e.printStackTrace()
        }
    }

    // Tu función existente para obtener el código de versión
    private fun getAppVersionCode(): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) { 0L }
    }
}
