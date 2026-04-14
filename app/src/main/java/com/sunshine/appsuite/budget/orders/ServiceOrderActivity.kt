package com.sunshine.appsuite.budget.orders

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.sunshine.appsuite.R
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.orders.data.ServiceOrderListItemDto
import com.sunshine.appsuite.budget.orders.data.ServiceOrdersApi
import com.sunshine.appsuite.budget.orders.repository.ServiceOrderRepository
import com.sunshine.appsuite.budget.security.TokenManager
import com.sunshine.appsuite.budget.tools.qr.QrScannerActivity
import com.sunshine.appsuite.databinding.ActivityServiceOrderBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.trim

class ServiceOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceOrderBinding

    // Lazy initialization de dependencias
    private val tokenManager by lazy { TokenManager(applicationContext) }
    private val serviceOrdersApi by lazy { ApiClient.createRetrofit(tokenManager).create(
        ServiceOrdersApi::class.java) }
    private val repository by lazy { ServiceOrderRepository(serviceOrdersApi) }

    private val adapter by lazy {
        ServiceOrderAdapter { item ->
            item.code?.trim()?.takeIf { it.isNotBlank() }?.let { code ->
                ServiceOrderDetailActivity.start(this, code = code)
            } ?: toast(getString(R.string.service_orders_toast_generic_error))
        }
    }

    private val footerAdapter by lazy {
        ServiceOrdersFooterAdapter(onRetry = { loadMore() })
    }

    private val layoutManager by lazy { LinearLayoutManager(this) }
    private var fetchJob: Job? = null

    // Configuración de paginación mejorada
    private val pageSize = 15 // Aumentado para mejor UX en pantallas grandes
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false

    // Cooldown de mensajes
    private var lastNoMoreToastAt = 0L
    private var lastProblemToastAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupToolbar()
        setupRecycler()
        setupSwipeRefresh()

        // Carga inicial
        refresh(showSpinner = true)
    }

    private fun setupSystemBars() {
        val status = ContextCompat.getColor(this, R.color.google_background_settings)
        val nav = ContextCompat.getColor(this, R.color.google_white)
        window.statusBarColor = status
        window.navigationBarColor = nav

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecycler() = with(binding.rvServiceOrders) {
        // Usamos nuestra propiedad local 'layoutManager' que ya es de tipo LinearLayoutManager
        layoutManager = this@ServiceOrderActivity.layoutManager
        adapter = ConcatAdapter(this@ServiceOrderActivity.adapter, footerAdapter)
        setHasFixedSize(true)

        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // dy > 0 significa que el scroll va hacia abajo
                if (dy <= 0 || isLoading || isLastPage) return

                // Accedemos a nuestra propiedad local directamente para evitar la nulabilidad
                val totalItems = this@ServiceOrderActivity.layoutManager.itemCount
                val lastVisible = this@ServiceOrderActivity.layoutManager.findLastVisibleItemPosition()

                val threshold = 5
                if (lastVisible >= (totalItems - threshold) && !footerAdapter.isErrorVisible()) {
                    loadMore()
                }
            }
        })
    }

    private fun setupSwipeRefresh() = with(binding.swipeRefreshServiceOrders) {
        val colorSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        setProgressBackgroundColorSchemeColor(colorSurface)
        setOnRefreshListener { refresh(showSpinner = true) }
    }

    private fun refresh(showSpinner: Boolean) {
        if (isLoading) return

        fetchPage(
            targetPage = 1,
            showSpinner = showSpinner,
            isLoadMore = false,
            onSuccess = { newItems, reachedEnd ->
                currentPage = 1
                isLastPage = reachedEnd
                adapter.submit(newItems)
                binding.rvServiceOrders.scrollToPosition(0)
            },
            onFailure = {
                toastProblemCooldown()
            }
        )
    }

    private fun loadMore() {
        if (isLoading || isLastPage) return

        fetchPage(
            targetPage = currentPage + 1,
            showSpinner = false,
            isLoadMore = true,
            onSuccess = { moreItems, reachedEnd ->
                if (moreItems.isNotEmpty()) {
                    // Filtrado de duplicados por CÓDIGO (más seguro que ID en sistemas distribuidos)
                    val currentCodes = adapter.getItems().mapNotNull { it.code }.toSet()
                    val filtered = moreItems.filter { it.code != null && !currentCodes.contains(it.code) }

                    if (filtered.isNotEmpty()) {
                        adapter.append(filtered)
                        currentPage += 1
                    }
                }
                isLastPage = reachedEnd
                if (reachedEnd) toastNoMoreCooldown()
            },
            onFailure = {
                toastProblemCooldown()
            }
        )
    }

    private fun fetchPage(
        targetPage: Int,
        showSpinner: Boolean,
        isLoadMore: Boolean,
        onSuccess: (items: List<ServiceOrderListItemDto>, reachedEnd: Boolean) -> Unit,
        onFailure: () -> Unit
    ) {
        fetchJob?.cancel()
        isLoading = true

        if (isLoadMore) footerAdapter.showLoading() else footerAdapter.hide()

        if (showSpinner) {
            binding.swipeRefreshServiceOrders.isRefreshing = true
        }

        fetchJob = lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    repository.fetchPage(page = targetPage, perPage = pageSize)
                }
            }

            result.onSuccess { items ->
                footerAdapter.hide()
                // Si recibimos menos de lo pedido, llegamos al final
                val reachedEnd = items.isEmpty() || items.size < pageSize
                onSuccess(items, reachedEnd)
            }.onFailure {
                if (isLoadMore) footerAdapter.showError() else footerAdapter.hide()
                onFailure()
            }

            isLoading = false
            binding.swipeRefreshServiceOrders.isRefreshing = false
        }
    }

    // --- Menú ---

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_service_orders, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_scan_qr -> {
                startActivity(Intent(this, QrScannerActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // --- Helpers ---

    private fun toastNoMoreCooldown() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNoMoreToastAt < 2000L) return
        lastNoMoreToastAt = now
        toast(getString(R.string.service_orders_toast_no_more))
    }

    private fun toastProblemCooldown() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastProblemToastAt < 2000L) return
        lastProblemToastAt = now
        toast(getString(R.string.service_orders_toast_generic_error))
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        fetchJob?.cancel() // Evitar fugas si la actividad se destruye durante una carga
        super.onDestroy()
    }
}