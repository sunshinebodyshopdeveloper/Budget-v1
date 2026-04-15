package com.sunshine.appsuite.budget.home.ui.widgets.tracking

import android.view.View
import com.sunshine.appsuite.budget.databinding.ItemHomeTrackingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeTrackingSectionController(
    private val repo: OrdersStatsRepository,
    private val onError: (String) -> Unit = {},
    private val onTileClick: (HomeTrackingTile) -> Unit = {}
) {
    private var binding: ItemHomeTrackingBinding? = null
    private var loadedOnce: Boolean = false

    fun bind(itemView: View) {
        binding = ItemHomeTrackingBinding.bind(itemView)
        setupClicks()
        renderLoadingPlaceholder()
    }

    fun unbind() {
        binding = null
    }

    fun ensureLoaded(scope: CoroutineScope) {
        if (loadedOnce) return
        refresh(scope)
    }

    fun refresh(scope: CoroutineScope) {
        val b = binding ?: return
        renderLoadingPlaceholder()

        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { repo.fetch() }
            }

            result.onSuccess { stats ->
                loadedOnce = true
                renderStats(stats)
            }.onFailure { e ->
                onError(e.message ?: "No se pudieron cargar los stats de tracking")
                renderErrorPlaceholder()
            }
        }
    }

    private fun setupClicks() {
        val b = binding ?: return
        b.cardTrackingTotal.setOnClickListener { onTileClick(HomeTrackingTile.TOTAL) }
        b.cardTrackingOnFloor.setOnClickListener { onTileClick(HomeTrackingTile.ON_FLOOR) }
        b.cardTrackingInTransit.setOnClickListener { onTileClick(HomeTrackingTile.IN_TRANSIT) }
        b.cardTrackingPendingEntry.setOnClickListener { onTileClick(HomeTrackingTile.PENDING_ENTRY) }
    }

    private fun renderLoadingPlaceholder() {
        val b = binding ?: return
        setCountsAlpha(0.65f)
        b.tvCountTotal.text = "—"
        b.tvCountOnFloor.text = "—"
        b.tvCountInTransit.text = "—"
        b.tvCountPendingEntry.text = "—"
    }

    private fun renderErrorPlaceholder() {
        val b = binding ?: return
        setCountsAlpha(0.8f)
        b.tvCountTotal.text = "—"
        b.tvCountOnFloor.text = "—"
        b.tvCountInTransit.text = "—"
        b.tvCountPendingEntry.text = "—"
    }

    private fun renderStats(stats: OrdersStatsResponse) {
        val b = binding ?: return
        setCountsAlpha(1f)

        b.tvCountTotal.text = HomeTrackingUiFormatter.formatCount(stats.total)
        b.tvCountOnFloor.text = HomeTrackingUiFormatter.formatCount(stats.onFloor)
        b.tvCountInTransit.text = HomeTrackingUiFormatter.formatCount(stats.inTransit)
        b.tvCountPendingEntry.text = HomeTrackingUiFormatter.formatCount(stats.pendingEntry)
    }

    private fun setCountsAlpha(alpha: Float) {
        val b = binding ?: return
        b.tvCountTotal.alpha = alpha
        b.tvCountOnFloor.alpha = alpha
        b.tvCountInTransit.alpha = alpha
        b.tvCountPendingEntry.alpha = alpha
    }
}

enum class HomeTrackingTile {
    TOTAL,
    ON_FLOOR,
    IN_TRANSIT,
    PENDING_ENTRY
}
