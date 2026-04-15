package com.sunshine.appsuite.budget.settings.system

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sunshine.appsuite.budget.BuildConfig
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.home.data.HomeSectionRepository
import com.sunshine.appsuite.budget.settings.SettingsHomeFragment
import com.sunshine.appsuite.budget.settings.appearance.ThemePreferences
import com.sunshine.appsuite.budget.settings.system.notifications.data.NotificationSettingsRepository
import com.sunshine.appsuite.budget.settings.system.notifications.ui.NotificationControlsBottomSheet
import com.sunshine.appsuite.budget.settings.system.verifications.SystemCheckAction
import com.sunshine.appsuite.budget.settings.system.verifications.SystemCheckId
import com.sunshine.appsuite.budget.settings.system.verifications.SystemCheckState
import com.sunshine.appsuite.budget.settings.system.verifications.SystemVerificationAdapter
import com.sunshine.appsuite.budget.settings.system.verifications.SystemVerificationRunner
import com.sunshine.appsuite.budget.settings.system.wallpaper.ui.WallpaperActivity
import com.sunshine.appsuite.budget.databinding.FragmentSettingsSystemBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SettingsSystemFragment : Fragment() {

    private var _binding: FragmentSettingsSystemBinding? = null
    private val binding get() = _binding!!

    // ✅ Reutilizamos el mismo host que usa SettingsHomeFragment
    private var host: SettingsHomeFragment.SettingsHomeHost? = null

    // Animaciones
    private var skeletonAnimator: ObjectAnimator? = null
    private var ringAnimator: ValueAnimator? = null

    // System verifications (modular)
    private var checksJob: Job? = null
    private var rerunChecksOnResume: Boolean = false
    private var verifAdapter: SystemVerificationAdapter? = null
    private var verifRunner: SystemVerificationRunner? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        host = when {
            parentFragment is SettingsHomeFragment.SettingsHomeHost ->
                parentFragment as SettingsHomeFragment.SettingsHomeHost
            context is SettingsHomeFragment.SettingsHomeHost ->
                context
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
        _binding = FragmentSettingsSystemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Primero: UI “cargando”
        showTilesSkeleton(true)
        startHeroPreloadAnimation()

        // Luego: lógica normal
        syncThemeValue()
        syncSwitches()
        setupClicks()

        // ✅ Checks (se muestran con loader y luego resultado)
        setupSystemVerifications()
        runSystemVerifications() // auto-run al entrar
    }

    override fun onResume() {
        super.onResume()
        if (rerunChecksOnResume) {
            rerunChecksOnResume = false
            runSystemVerifications()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ringAnimator?.cancel()
        skeletonAnimator?.cancel()
        checksJob?.cancel()
        verifAdapter = null
        verifRunner = null
        _binding = null
    }

    private fun setupClicks() = with(binding) {

        // Tema
        cardTheme.setOnClickListener { showThemePicker() }

        // Restablecer orden del Home
        cardHomeOrder.setOnClickListener { resetHomeOrder() }

        // Wallpaper
        cardWallpaper.setOnClickListener {
            val intent = Intent(requireContext(), WallpaperActivity::class.java)
            startActivity(intent)
        }

        // Notificaciones
        cardNotifications.setOnClickListener {
            NotificationControlsBottomSheet.Companion
                .newInstance()
                .show(childFragmentManager, "NotificationControlsBottomSheet")
        }

        // ✅ MISMO estilo que SettingsHomeFragment: delegar al host
        cardSupport.setOnClickListener { host?.onOpenSupport() }
        cardAbout.setOnClickListener { host?.onOpenAbout() }
        cardLegal.setOnClickListener { host?.onOpenLegal() }
    }

    private fun syncSwitches() = with(binding) {
        val ctx = requireContext()

        switchShortcuts.isEnabled = true
        switchShortcuts.isChecked = SystemPreferences.isShortcutsEnabled(ctx)
    }

    private fun syncThemeValue() {
        val ctx = requireContext()

        val label = when (ThemePreferences.getNightMode(ctx)) {
            AppCompatDelegate.MODE_NIGHT_YES -> "Oscuro"
            AppCompatDelegate.MODE_NIGHT_NO -> "Claro"
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_UNSPECIFIED -> getString(R.string.settings_system_theme_value_system)
            else -> getString(R.string.settings_system_theme_value_system)
        }

        binding.tvThemeValue.text = label
    }

    private fun showThemePicker() {
        val ctx = requireContext()

        val items = arrayOf("Automático", "Claro", "Oscuro")
        val currentIndex = when (ThemePreferences.getNightMode(ctx)) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_UNSPECIFIED -> 0
            else -> 0
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(R.string.settings_system_theme_title))
            .setSingleChoiceItems(items, currentIndex) { dialog, which ->
                val mode = when (which) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                ThemePreferences.setNightMode(ctx, mode)
                syncThemeValue()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun resetHomeOrder() {
        val ctx = requireContext()
        HomeSectionRepository(ctx).resetToDefault()
        Toast.makeText(ctx, "Orden del Inicio restablecido", Toast.LENGTH_SHORT).show()
    }

    /**
     * ✅ Intro hero:
     * - Ring 0 → 100 (simula “precarga”)
     * - Title: “Cargando…”
     * - Al final: aparece el check + texto original
     * - Mientras: skeleton en tiles
     */
    private fun startHeroPreloadAnimation() = with(binding) {

        // Estado inicial
        imgSystemCheck.visibility = View.INVISIBLE
        imgSystemCheck.alpha = 0f
        imgSystemCheck.scaleX = 0.85f
        imgSystemCheck.scaleY = 0.85f

        tvSystemStatusTitle.text = "Cargando…"
        tvSystemStatusSubtitle.text = "Preparando controles…"

        // Ring desde 0 a 100
        progressSystemRing.setProgressCompat(0, false)

        ringAnimator?.cancel()
        ringAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 1900L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { anim ->
                val v = anim.animatedValue as Int
                //progressSystemRing.setProgressCompat(v, false)
                binding.progressSystemRing.setProgressCompat(v, true)

                // mini-etapa de texto, solo para que se sienta vivo
                if (v >= 60) tvSystemStatusSubtitle.text = "Casi listo…"
            }
            doOnEnd {
                // Bounce sutil del ring
                progressSystemRing.animate()
                    .scaleX(1.02f).scaleY(1.02f)
                    .setDuration(120)
                    .withEndAction {
                        progressSystemRing.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(140)
                            .start()
                    }
                    .start()

                // Check aparece
                imgSystemCheck.visibility = View.VISIBLE
                imgSystemCheck.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                // Texto final (los strings originales)
                tvSystemStatusTitle.text = getString(R.string.settings_system_status_title)
                tvSystemStatusSubtitle.text = getString(R.string.settings_system_status_subtitle)

                // Quita skeleton y deja el contenido real
                showTilesSkeleton(false)
            }
            start()
        }
    }

    private fun ValueAnimator.doOnEnd(block: () -> Unit) {
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) = Unit
            override fun onAnimationEnd(animation: Animator) = block()
            override fun onAnimationCancel(animation: Animator) = Unit
            override fun onAnimationRepeat(animation: Animator) = Unit
        })
    }

    private fun showTilesSkeleton(show: Boolean) = with(binding) {
        if (show) {
            tilesContent.visibility = View.INVISIBLE
            tilesSkeleton.visibility = View.VISIBLE

            skeletonAnimator?.cancel()
            skeletonAnimator = ObjectAnimator.ofFloat(tilesSkeleton, View.ALPHA, 0.55f, 1f).apply {
                duration = 650L
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = FastOutSlowInInterpolator()
                start()
            }
        } else {
            skeletonAnimator?.cancel()
            tilesSkeleton.visibility = View.GONE
            tilesContent.visibility = View.VISIBLE
        }
    }

    // ==========================================================
    //   System verifications (modular)
    // ==========================================================

    private fun setupSystemVerifications() = with(binding) {
        val ctx = requireContext().applicationContext

        val adapter = SystemVerificationAdapter { _, action ->
            when (action) {
                SystemCheckAction.EnableAutoTime -> openAutoTimeSettings()
                else -> Unit
            }
        }

        rvSystemVerifications.layoutManager = LinearLayoutManager(requireContext())
        rvSystemVerifications.adapter = adapter

        val runner = SystemVerificationRunner(
            context = ctx,
            notificationRepo = NotificationSettingsRepository(ctx),
            baseUrl = BuildConfig.API_BASE_URL,
            // Cuando tengas WorkManager sync real, pon aquí tu TAG:
            syncWorkTag = null,
            serverPath = "api/test"
        )

        adapter.submitList(runner.initialItems())

        btnRunSystemVerifications.setOnClickListener { runSystemVerifications() }

        verifAdapter = adapter
        verifRunner = runner
    }

    private fun runSystemVerifications() {
        val adapter = verifAdapter ?: return
        val runner = verifRunner ?: return

        checksJob?.cancel()
        checksJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.btnRunSystemVerifications.isEnabled = false

            try {
                runner.runAll { upd ->
                    adapter.update(upd.id) { item ->

                        val isSecurityRow = upd.id == SystemCheckId.APP_SECURITY
                        val needsFix = isSecurityRow && upd.state != SystemCheckState.OK

                        val subtitle = if (needsFix) {
                            "Activa Huella/PIN en Ajustes → Seguridad."
                        } else {
                            upd.subtitle
                        }

                        item.copy(
                            state = upd.state,
                            subtitle = subtitle,
                            meta = upd.meta,
                            // si es Seguridad y falla: sin botón, solo mensaje
                            action = if (needsFix) null else upd.action,
                            actionLabel = if (needsFix) null else upd.actionLabel
                        )
                    }
                }
            } finally {
                binding.btnRunSystemVerifications.isEnabled = true
            }
        }
    }

    private fun openAutoTimeSettings() {
        rerunChecksOnResume = true
        runCatching {
            startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
        }.onFailure {
            Toast.makeText(requireContext(), "No pude abrir ajustes de hora.", Toast.LENGTH_SHORT).show()
        }
    }
}
