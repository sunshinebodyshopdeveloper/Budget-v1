package com.sunshine.appsuite.budget.menu

import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.sunshine.appsuite.budget.R

class MainDrawerController(
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView,
    private val onItemSelected: (Item) -> Unit
) : NavigationView.OnNavigationItemSelectedListener {

    enum class Item {
        HOME,
        SCAN_QR,
        SERVICES_ORDERS,
        ENTER_TOW,
        NEW_INVENTORY,
        APPOINTMENTS,
        ASSIGNMENTS,
        TRACKING,
        PARTS,
        HELP,
        ABOUT,
        SETTINGS,
        LOGOUT
    }

    init {
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val mapped = when (item.itemId) {
            R.id.nav_home           -> Item.HOME
            R.id.nav_scan_qr        -> Item.SCAN_QR
            R.id.nav_orders         -> Item.SERVICES_ORDERS
            R.id.nav_enter_tow      -> Item.ENTER_TOW
            R.id.nav_new_inventory  -> Item.NEW_INVENTORY
            R.id.nav_appointments   -> Item.APPOINTMENTS
            R.id.nav_assignments    -> Item.ASSIGNMENTS
            R.id.nav_tracking       -> Item.TRACKING
            R.id.nav_parts          -> Item.PARTS
            R.id.nav_help           -> Item.HELP
            R.id.nav_about          -> Item.ABOUT
            //R.id.nav_settings       -> Item.SETTINGS
            //R.id.nav_logout         -> Item.LOGOUT
            else -> null
        } ?: return false

        onItemSelected(mapped)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
