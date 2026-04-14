package com.sunshine.appsuite.budget.settings.security

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build

data class DeviceInfoModel(
    val device: String,
    val so: String,
    val deviceInfo: String,
    val battery: String,
    val lastConnection: String
)

object DeviceInfoHelper {

    fun getDeviceInfo(context: Context): DeviceInfoModel {
        val model = Build.MODEL ?: "Android"
        val brand = Build.BRAND ?: ""
        val version = Build.VERSION.RELEASE ?: "?"

        val deviceInfo = listOf(brand, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val deviceOS = "Android $version"

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val batteryLabel = "$batteryLevel%"

        val connectionLabel = getConnectionLabel(context)

        return DeviceInfoModel(
            device = model,
            so = deviceOS,
            deviceInfo = deviceInfo,
            battery = batteryLabel,
            lastConnection = connectionLabel
        )
    }

    private fun getConnectionLabel(context: Context): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(network)
            val type = when {
                caps == null -> null
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Red móvil"
                else -> "Desconocido"
            }
            if (type != null) {
                "Tipo de conexión: $type"
            } else {
                "Tipo de conexión: Sin conexión"
            }
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                @Suppress("DEPRECATION")
                val type = when (activeNetworkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
                    ConnectivityManager.TYPE_MOBILE -> "Red móvil"
                    else -> "Desconocido"
                }
                "Tipo de conexión: $type"
            } else {
                "Tipo de conexión: Sin conexión"
            }
        }
    }
}
