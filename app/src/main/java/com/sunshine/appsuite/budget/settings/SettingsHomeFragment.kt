package com.sunshine.appsuite.budget.settings

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.Fragment
import com.sunshine.appsuite.budget.AppSuiteApp
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.user.ui.ProfileBottomSheetDialog
import com.sunshine.appsuite.databinding.FragmentSettingsHomeBinding
import com.sunshine.appsuite.budget.menu.ProfileMenuListener
import com.sunshine.appsuite.budget.settings.apps.AppsActivity
import com.sunshine.appsuite.budget.settings.account.PasswordActivity
import com.sunshine.appsuite.budget.settings.account.ProfilePhotoActivity
import com.sunshine.appsuite.budget.user.ui.AvatarImageLoader
import kotlinx.coroutines.launch
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Path
import android.os.Build
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.min

class SettingsHomeFragment : Fragment() {

    private var _binding: FragmentSettingsHomeBinding? = null
    private val binding get() = _binding!!
    private var shapeRotation = 0f

    private var navigator: SettingsNavigator? = null
    private var host: SettingsHomeHost? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as? SettingsNavigator

        host = when {
            parentFragment is SettingsHomeHost -> parentFragment as SettingsHomeHost
            context is SettingsHomeHost -> context
            else -> null
        }
    }

    override fun onDetach() {
        super.onDetach()
        navigator = null
        host = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivAvatar.post {
            setupCookieShape(binding.ivAvatar)
        }

        val app = requireActivity().application as AppSuiteApp
        val store = app.userProfileStore

        // UI reactiva: se actualiza sola cuando cambie el perfil
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.profile.collect { profile ->
                    binding.tvUserName.text = profile?.displayName(getString(R.string.profile_user_name_placeholder))
                    binding.tvUserEmail.text = profile?.displayEmail(getString(R.string.profile_user_email_placeholder))

                    AvatarImageLoader.load(
                        scope = viewLifecycleOwner.lifecycleScope,
                        imageView = binding.ivAvatar,
                        url = profile?.avatarUrl,
                        okHttpClient = app.okHttpClient,
                        placeholderRes = R.drawable.ic_avatar
                    )
                }
            }
        }

        // Dispara refresh si hace falta (TTL interno)
        store.ensureFresh()

        // Navegación por tarjetas
        binding.cardAccount.setOnClickListener {
            navigator?.openSection(SettingsActivity.Section.ACCOUNT)
        }

        //Foto de perfil
        binding.ivAvatar.setOnClickListener {
            startActivity(Intent(requireContext(), ProfilePhotoActivity::class.java))
        }

        // BottomSheet de perfil
        binding.cardAccountOptions.setOnClickListener {
            val activity = requireActivity()
            val sheet = ProfileBottomSheetDialog()

            if (activity is ProfileMenuListener) {
                sheet.setProfileMenuListener(activity)
            }

            sheet.show(parentFragmentManager, "ProfileBottomSheet")
        }

        binding.cardSecurity.setOnClickListener {
            navigator?.openSection(SettingsActivity.Section.SECURITY)
        }

        binding.cardPassword.setOnClickListener {
            startActivity(Intent(requireContext(), PasswordActivity::class.java))
        }

        binding.cardPermissions.setOnClickListener {
            navigator?.openSection(SettingsActivity.Section.PERMISSIONS)
        }

        binding.cardApps.setOnClickListener {
            startActivity(Intent(requireContext(), AppsActivity::class.java))
        }

        binding.cardSettings.setOnClickListener {
            navigator?.openSection(SettingsActivity.Section.SYSTEM)
        }

        // ✅ Delegado al host (SettingsActivity) para mantener consistencia
        binding.cardSupport.setOnClickListener { host?.onOpenSupport() }
        binding.cardAbout.setOnClickListener { host?.onOpenAbout() }
        binding.cardLegal.setOnClickListener { host?.onOpenLegal() }
        binding.rowPrivacy.setOnClickListener { host?.onOpenLegal() }
        binding.rowAbout.setOnClickListener { host?.onOpenAbout() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface SettingsHomeHost {
        fun onOpenSupport()
        fun onOpenSystem()
        fun onOpenAbout()
        fun onOpenLegal()
    }

    private fun setupCookieShape(view: View) {
        // 1. Definir el OutlineProvider con rotación
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val width = view.width
                val height = view.height
                if (width <= 0 || height <= 0) return

                val size = min(width, height).toFloat()
                val cookie = RoundedPolygon.star(
                    numVerticesPerRadius = 12,        // Cambiado de numVertices a numVerticesPerRadius
                    innerRadius = 0.88f,              // Este DEBE ser un Float
                    rounding = CornerRounding(size * 0.18f) // Este DEBE ser el objeto CornerRounding
                )

                val path = Path()
                cookie.toPath(path)

                val matrix = Matrix()
                val scale = size / 2f
                matrix.postScale(scale, scale)

                // APLICAR ROTACIÓN: Giramos el Path sobre su centro (0,0 antes del translate)
                matrix.postRotate(shapeRotation)

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

        // 2. Crear el Animador (Sentido horario lento)
        val animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 30000 // 10 segundos por vuelta completa (ajusta para más/menos velocidad)
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                shapeRotation = animation.animatedValue as Float
                view.invalidateOutline() // Notifica al sistema que el recorte cambió
            }
        }
        animator.start()
    }
}
