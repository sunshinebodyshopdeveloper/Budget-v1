package com.sunshine.appsuite.budget.home.model

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.sunshine.appsuite.budget.R

data class HomeSection(
    val id: String,
    val type: Type
) {
    enum class Type(
        val id: String,
        @StringRes val titleRes: Int,
        @LayoutRes val layoutRes: Int
    ) {
        FUNCTIONS(
            id = "functions",
            titleRes = R.string.home_section_functions_subtitle,
            layoutRes = R.layout.item_home_functions
        );

        companion object {
            fun fromId(id: String): Type? = entries.firstOrNull { it.id == id }
        }
    }
}
