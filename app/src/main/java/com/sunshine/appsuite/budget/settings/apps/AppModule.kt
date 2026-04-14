package com.sunshine.appsuite.budget.settings.apps

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class AppModule(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val packageName: String?,
    val installUrl: String?,
    var isInstalled: Boolean = false,
    var isExpanded: Boolean = false,
    var sizeText: String?
)