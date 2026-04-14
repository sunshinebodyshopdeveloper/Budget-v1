package com.sunshine.appsuite.budget.onboarding

import android.animation.ArgbEvaluator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sunshine.appsuite.budget.AppSuiteApp
import com.sunshine.appsuite.budget.LoginActivity
import com.sunshine.appsuite.R
import com.sunshine.appsuite.databinding.ActivityOnboardingBinding
import kotlin.math.abs

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val app by lazy { application as AppSuiteApp }

    private lateinit var adapter: OnboardingAdapter

    // Ahora solo 4 slides
    private val layouts = listOf(
        R.layout.item_onboarding_1,
        R.layout.item_onboarding_2,
        R.layout.item_onboarding_3,
        R.layout.item_onboarding_4
    )

    // Colores de fondo por pantalla (1..4)
    private val bgColors by lazy {
        intArrayOf(
            getColorCompat(R.color.onboarding_bg_1),
            getColorCompat(R.color.onboarding_bg_2),
            getColorCompat(R.color.onboarding_bg_3),
            getColorCompat(R.color.onboarding_bg_4)
        )
    }

    private val argbEvaluator = ArgbEvaluator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val firstColor = bgColors[0]
        binding.rootOnboarding.setBackgroundColor(firstColor)
        updateSystemBarsColor(firstColor)

        setupViewPager()
        setupBottomBar()
    }

    private fun setupViewPager() {
        adapter = OnboardingAdapter(layouts)
        binding.viewPagerOnboarding.adapter = adapter

        // Efecto fade
        binding.viewPagerOnboarding.setPageTransformer(FadePageTransformer())

        // Dots
        TabLayoutMediator(binding.tabDots, binding.viewPagerOnboarding) { tab, _ ->
            tab.setCustomView(R.layout.item_onboarding_dot)
        }.attach()

        updateDots(0)
        updateBottomBarForPosition(0)

        binding.viewPagerOnboarding.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                if (position < bgColors.size - 1) {
                    val color = argbEvaluator.evaluate(
                        positionOffset,
                        bgColors[position],
                        bgColors[position + 1]
                    ) as Int
                    binding.rootOnboarding.setBackgroundColor(color)
                    updateSystemBarsColor(color)
                } else {
                    val lastColor = bgColors.last()
                    binding.rootOnboarding.setBackgroundColor(lastColor)
                    updateSystemBarsColor(lastColor)
                }
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
                updateBottomBarForPosition(position)
            }
        })
    }

    private fun setupBottomBar() {
        // SKIP: marca onboarding como completado y va a Login
        binding.tvSkip.setOnClickListener {
            completeOnboardingAndGoToLogin()
        }

        // Flechita siguiente
        binding.btnNext.setOnClickListener {
            val current = binding.viewPagerOnboarding.currentItem
            if (current < layouts.lastIndex) {
                binding.viewPagerOnboarding.currentItem = current + 1
            }
        }

        // Texto FINALIZAR (solo visible en el slide 4)
        binding.tvFinish.setOnClickListener {
            completeOnboardingAndGoToLogin()
        }
    }

    /**
     * Actualiza dots custom.
     */
    private fun updateDots(selectedPosition: Int) {
        val tabLayout: TabLayout = binding.tabDots
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            val dotView = tab?.customView?.findViewById<View>(R.id.dotView)
            val bgRes = if (i == selectedPosition) {
                R.drawable.bg_dot_selected
            } else {
                R.drawable.bg_dot_unselected
            }
            dotView?.setBackgroundResource(bgRes)
        }
    }

    /**
     * En el slide 4:
     *  - Ocultamos SKIP
     *  - Ocultamos flecha
     *  - Mostramos "Finalizar"
     */
    private fun updateBottomBarForPosition(position: Int) {
        val isLast = position == layouts.lastIndex

        binding.tvSkip.visibility = if (isLast) View.GONE else View.VISIBLE
        binding.btnNext.visibility = if (isLast) View.GONE else View.VISIBLE
        binding.tvFinish.visibility = if (isLast) View.VISIBLE else View.GONE
    }

    private fun completeOnboardingAndGoToLogin() {
        setOnboardingCompleted()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setOnboardingCompleted() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("onboarding_completed", true)
            .apply()
    }

    private fun getColorCompat(@ColorRes id: Int): Int =
        ContextCompat.getColor(this, id)

    private class FadePageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            page.translationX = -position * page.width

            when {
                position <= -1f || position >= 1f -> page.alpha = 0f
                position == 0f -> page.alpha = 1f
                else -> page.alpha = 1f - abs(position)
            }
        }
    }

    private fun updateSystemBarsColor(color: Int) {
        window.statusBarColor = color
        window.navigationBarColor = color

        val controller = WindowCompat.getInsetsController(window, binding.rootOnboarding)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
}
