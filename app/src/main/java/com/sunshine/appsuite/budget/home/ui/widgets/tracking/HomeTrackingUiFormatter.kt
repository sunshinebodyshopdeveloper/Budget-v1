package com.sunshine.appsuite.budget.home.ui.widgets.tracking

object HomeTrackingUiFormatter {

    fun formatCount(value: Int?): String {
        return (value ?: 0).toString()
    }

    fun placeholder(): String = "—"
}
