package com.sunshine.appsuite.budget.settings

interface SettingsNavigator {
    fun openSection(section: SettingsActivity.Section, addToBackStack: Boolean = true)
}
