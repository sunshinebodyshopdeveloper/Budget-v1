package com.sunshine.appsuite.budget.orders

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.orders.data.OrderDto
import com.sunshine.appsuite.budget.orders.data.OrdersApi
import com.sunshine.appsuite.budget.orders.repository.OrdersRepository
import com.sunshine.appsuite.budget.orders.timeline.OtTimelineAdapter
import com.sunshine.appsuite.budget.orders.timeline.OtTimelineMapper
import com.sunshine.appsuite.budget.orders.timeline.OtTimelineRailDecoration
import com.sunshine.appsuite.budget.security.TokenManager
import com.sunshine.appsuite.databinding.ActivityServiceOrderDetailBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ServiceOrderDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_CODE = "extra_order_code"
        fun start(context: Context, code: String) {
            val intent = Intent(context, ServiceOrderDetailActivity::class.java).apply {
                putExtra(EXTRA_ORDER_CODE, code)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityServiceOrderDetailBinding
    private val repository by lazy {
        OrdersRepository(ApiClient.createRetrofit(TokenManager(this)).create(OrdersApi::class.java))
    }

    private var fetchJob: Job? = null
    private lateinit var timelineAdapter: OtTimelineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupTimeline()
        setupTabNavigation()

        val code = intent.getStringExtra(EXTRA_ORDER_CODE).orEmpty()
        if (code.isNotBlank()) {
            fetchOrder(code)
        }

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun setupTimeline() {
        timelineAdapter = OtTimelineAdapter(this)
        binding.rvOtTimeline.apply {
            layoutManager = LinearLayoutManager(this@ServiceOrderDetailActivity)
            adapter = timelineAdapter
            addItemDecoration(
                OtTimelineRailDecoration(
                context = this@ServiceOrderDetailActivity,
                getActiveIndex = { timelineAdapter.activeIndex }
            ))
            isNestedScrollingEnabled = false
        }
    }

    private fun setupTabNavigation() {
        // Mapeo de botones de tarjetas a sus contenedores correspondientes
        val tabs = mapOf(
            binding.orderDetail to binding.containerOrderDetail,
            binding.orderClient to binding.containerOrderClient,
            binding.orderInsurance to binding.containerOrderInsurance,
            binding.orderVehicle to binding.containerOrderVehicle
        )

        tabs.forEach { (tabView, container) ->
            tabView.setOnClickListener {
                // Ocultar todos los contenedores y resetear bordes
                tabs.values.forEach { it.isVisible = false }
                tabs.keys.forEach { it.strokeWidth = 0 }

                // Mostrar el seleccionado y marcar borde
                container.isVisible = true
                tabView.strokeWidth = dpToPx(2)
            }
        }
    }

    private fun fetchOrder(code: String) {
        binding.tvQrRawValue.text = code
        fetchJob?.cancel()
        fetchJob = lifecycleScope.launch {
            runCatching { repository.fetch(code) }
                .onSuccess { renderOrder(it) }
                .onFailure { Toast.makeText(this@ServiceOrderDetailActivity, "Error al cargar", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun renderOrder(order: OrderDto) {
        // 1. Cabecera e Imagen
        binding.tvQrRawValue.text = order.code
        binding.tvQrNameClient.text = order.clientName ?: order.client?.name ?: "Sin nombre"

        val v = order.vehicle
        binding.tvVehicleHeader.text = "${v?.carBrand ?: ""} ${v?.carName ?: ""} | ${v?.plates ?: ""}".trim()

        if (!order.coverPhotoUrl.isNullOrBlank()) {
            Picasso.get()
                .load(order.coverPhotoUrl) // Ya no es order.coverPhoto?.url
                .fit()
                .centerCrop()
                .placeholder(com.sunshine.appsuite.R.drawable.scrim_gradient_expressive)
                .error(com.sunshine.appsuite.R.color.google_background_settings)
                .into(binding.ivServiceOrderCover, object : Callback {
                    override fun onSuccess() {
                        // Imagen cargada
                    }
                    override fun onError(e: Exception?) {
                        Log.e("PICASSO_ERROR", "Error cargando imagen: ${e?.message}")
                    }
                })
        }

        // 2. Timeline
        timelineAdapter.submitList(OtTimelineMapper.build(order))

        // 3. Llenar Listados Dinámicos
        fillOrderDetails(order)
        fillClientDetails(order)
        fillInsuranceDetails(order)
        fillVehicleDetails(order)
    }

    private fun fillOrderDetails(order: OrderDto) {
        with(binding.containerOrderDetail) {
            // Borramos solo los datos previos, manteniendo el título y el Card del Timeline
            // En tu XML: Título (0), Subtítulo Timeline (1), Card Timeline (2), Space (3)
            if (childCount > 4) removeViews(4, childCount - 4)

            addDetailRow(this, "Estado", order.status?.uppercase())
            addDetailRow(this, "Admisión", order.dateOfAdmission)
            addDetailRow(this, "Grúa", if (order.receivedByTow == true) "Sí" else "No")
            if (order.receivedByTow == true) {
                addDetailRow(this, "Cía. Grúa", order.towCompany)
                addDetailRow(this, "Operador", order.towDriverName)
            }
        }
    }

    private fun fillClientDetails(order: OrderDto) {
        with(binding.containerOrderClient) {
            if (childCount > 1) removeViews(1, childCount - 1)
            val c = order.client
            addDetailRow(this, "Nombre", c?.name ?: order.clientName)
            addDetailRow(this, "Teléfono", c?.phone)
            addDetailRow(this, "Email", c?.email)
            addDetailRow(this, "Contacto", order.contactName)
            addDetailRow(this, "Tel. Contacto", order.contactPhone)
        }
    }

    private fun fillInsuranceDetails(order: OrderDto) {
        val ins = order.insuranceClaim
        with(binding.containerOrderInsurance) {
            if (childCount > 1) removeViews(1, childCount - 1)
            ins?.let {
                addDetailRow(this, "Póliza", it.policyNumber)
                addDetailRow(this, "Siniestro", it.sinisterNumber)
                addDetailRow(this, "Ajustador", it.adjusterName)
                addDetailRow(this, "Deducible", it.deductible)
            } ?: addDetailRow(this, "Seguro", "No aplica / Particular")
        }
    }

    private fun fillVehicleDetails(order: OrderDto) {
        val v = order.vehicle
        with(binding.containerOrderVehicle) {
            // Mantenemos Título (0) y tvVehicleMeta (1)
            if (childCount > 2) removeViews(2, childCount - 2)
            v?.let {
                binding.tvVehicleMeta.text = "${it.type ?: ""} • ${it.color ?: ""}"
                addDetailRow(this, "Marca", it.carBrand)
                addDetailRow(this, "Modelo", it.carName)
                addDetailRow(this, "Año", it.carYear?.toString())
                addDetailRow(this, "VIN", it.serialNumber)
                addDetailRow(this, "Versión", it.version?.name)
            }
        }
    }

    private fun addDetailRow(container: LinearLayout, label: String, value: String?) {
        val textView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dpToPx(8), 0, dpToPx(8)) }

            val span = SpannableStringBuilder("$label: ${value ?: "—"}")
            span.setSpan(StyleSpan(Typeface.BOLD), 0, label.length + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            text = span
            textSize = 15f
            setTextColor(MaterialColors.getColor(this, R.attr.colorOnSurface))
        }
        container.addView(textView)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}