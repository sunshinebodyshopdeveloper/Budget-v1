package com.sunshine.appsuite.budget.home.data

import android.content.Context
import com.sunshine.appsuite.budget.home.model.HomeSection

class HomeSectionRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSections(): MutableList<HomeSection> {
        val defaultTypes = listOf(
            HomeSection.Type.FUNCTIONS,
            HomeSection.Type.QUICK_ACTIONS,
            HomeSection.Type.APPOINTMENTS,
            HomeSection.Type.TRACKING
        )

        val savedOrderIds = prefs.getString(KEY_ORDER, null)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        // Sin orden guardado -> default
        if (savedOrderIds.isEmpty()) {
            return defaultTypes
                .map { type -> HomeSection(id = type.id, type = type) }
                .toMutableList()
        }

        // Orden guardado -> map a tipos válidos
        val savedTypes = savedOrderIds.mapNotNull { HomeSection.Type.fromId(it) }

        // Si agregas nuevas secciones después, se agregan al final
        val missingTypes = defaultTypes.filter { defaultType ->
            savedTypes.none { it.id == defaultType.id }
        }

        val finalTypes = savedTypes + missingTypes

        return finalTypes
            .map { type -> HomeSection(id = type.id, type = type) }
            .toMutableList()
    }

    fun saveOrder(sections: List<HomeSection>) {
        val value = sections.joinToString(",") { it.type.id }
        prefs.edit()
            .putString(KEY_ORDER, value)
            .putBoolean(KEY_DIRTY, false) // ya quedó actualizado en Home
            .apply()
    }

    /**
     * Por si desde Settings u otro lado quieres forzar refresh del Home.
     */
    fun markDirty() {
        prefs.edit().putBoolean(KEY_DIRTY, true).apply()
    }

    /**
     * Se consume una sola vez.
     */
    fun consumeDirty(): Boolean {
        val dirty = prefs.getBoolean(KEY_DIRTY, false)
        if (dirty) prefs.edit().putBoolean(KEY_DIRTY, false).apply()
        return dirty
    }

    /**
     * Reset del orden + marca dirty para que Home recargue al volver.
     */
    fun resetToDefault() {
        prefs.edit()
            .remove(KEY_ORDER)
            .putBoolean(KEY_DIRTY, true)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "home_sections_prefs"
        private const val KEY_ORDER = "home_sections_order"
        private const val KEY_DIRTY = "home_sections_dirty"
    }
}
