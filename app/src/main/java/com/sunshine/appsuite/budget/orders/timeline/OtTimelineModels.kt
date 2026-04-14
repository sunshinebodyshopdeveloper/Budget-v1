package com.sunshine.appsuite.budget.orders.timeline

import androidx.annotation.StringRes

enum class OtTimelineState { DONE, CURRENT, TODO }

data class OtTimelineStepUi(
    @StringRes val titleRes: Int,
    val dateText: String?,
    @StringRes val captionRes: Int? = null,
    val state: OtTimelineState
)
