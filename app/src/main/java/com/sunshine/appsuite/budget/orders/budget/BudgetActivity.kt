package com.sunshine.appsuite.budget.orders.budget

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import com.sunshine.appsuite.budget.R
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.databinding.ActivityBudgetBinding
import com.sunshine.appsuite.budget.orders.data.InsuranceClaimDto
import com.sunshine.appsuite.budget.orders.data.OrderDto
import com.sunshine.appsuite.budget.orders.data.OrderResponse
import com.sunshine.appsuite.budget.orders.data.OrdersApi
import com.sunshine.appsuite.budget.orders.data.VehicleDto
import com.sunshine.appsuite.budget.security.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sunshine.appsuite.budget.user.data.UserProfileCache
import com.sunshine.appsuite.budget.user.ui.AvatarImageLoader
import com.sunshine.appsuite.budget.user.ui.ProfileBottomSheetDialog
import okhttp3.OkHttpClient

class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding

    private val ordersApi by lazy {
        ApiClient.createRetrofit(TokenManager(this)).create(OrdersApi::class.java)
    }

    private val userCache by lazy { UserProfileCache(this) }
    private val okHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        loadUserProfile()
        setupTopBar()

        val orderQuery = intent.getStringExtra("EXTRA_BUDGET_SEARCH_QUERY")
        if (!orderQuery.isNullOrEmpty()) {
            fetchOrderDetails(orderQuery)
        } else {
            showToast("No se recibió información para buscar")
            finishWithDelay()
        }
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val themeBgColor = ContextCompat.getColor(this, R.color.md_theme_background)

        window.statusBarColor = themeBgColor
        window.navigationBarColor = themeBgColor

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.updatePadding(
//                left = systemBars.left,
//                top = systemBars.top,
//                right = systemBars.right,
//                bottom = systemBars.bottom
//            )
//            insets
//        }
    }

    private fun loadUserProfile() {
        val profile = userCache.load()

        AvatarImageLoader.load(
            scope = lifecycleScope,
            imageView = binding.ivUserProfile,
            url = profile?.avatarUrl,
            okHttpClient = okHttpClient,
            placeholderRes = R.drawable.ic_profile
        )
    }

    private fun setupTopBar() {
        setSupportActionBar(binding.topAppBar)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ivUserProfile.setOnClickListener {
            val dialog = ProfileBottomSheetDialog()
            dialog.show(supportFragmentManager, "ProfileDialog")
        }
    }

    private fun fetchOrderDetails(query: String) {
        lifecycleScope.launch {
            try {
                val orderData: OrderResponse? = withContext(Dispatchers.IO) {
                    if (query.startsWith("OT-", ignoreCase = true)) {
                        ordersApi.getOrder(query)
                    } else {
                        val searchResponse = ordersApi.searchOrders(query)
                        val results = searchResponse.toList()
                        if (results.isNotEmpty()) {
                            val foundCode = results.first().code ?: ""
                            ordersApi.getOrder(foundCode)
                        } else null
                    }
                }

                if (orderData != null) {
                    bindOrderData(orderData.data, orderData.insuranceClaim)
                } else {
                    showToast("No se encontró ninguna orden con: $query")
                    finishWithDelay()
                }

            } catch (e: Exception) {
                showToast("Error al cargar datos: ${e.message}")
                finishWithDelay()
            }
        }
    }

    private fun bindOrderData(order: OrderDto, insurance: InsuranceClaimDto?) {
        val vehicle = order.vehicle
        val imageUrl = order.coverPhotoUrl

        if (vehicle != null) {
            val brand = vehicle.carBrand ?: ""
            val model = vehicle.carName ?: ""
            val year = vehicle.carYear?.toString() ?: ""
            val plates = vehicle.plates?.uppercase() ?: "S/P"

            val mainInfo = "$brand $model $year".trim()
            val fullTitle = "$mainInfo — ($plates)"

            val spannableTitle = android.text.SpannableStringBuilder(fullTitle)
            val startPlates = fullTitle.indexOf("($plates)")
            if (startPlates != -1) {
                spannableTitle.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    startPlates,
                    fullTitle.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            binding.tvToolbarTitle.text = spannableTitle
        } else {
            binding.tvToolbarTitle.text = "Vehículo no identificado"
        }

        binding.tvToolbarSubtitle.text = order.code ?: "Sin código"

        Picasso.get()
            .load(imageUrl?.takeIf { it.isNotBlank() }) // Si la URL está vacía, Picasso usará el placeholder
            .placeholder(R.drawable.ic_cover)           // Imagen mientras carga
            .error(R.drawable.ic_cover)                 // Imagen si la URL es nula o hay error
            .fit()                                      // Ajusta la imagen al tamaño del ImageView
            .centerCrop()                               // Mantiene la proporción cortando los bordes
            .into(binding.ivVehicleCover)
    }

    private fun finishWithDelay() {
        lifecycleScope.launch {
            delay(2500)
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}