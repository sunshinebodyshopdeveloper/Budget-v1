package com.sunshine.appsuite.budget.system.network

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.sunshine.appsuite.budget.MainActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.sunshine.appsuite.databinding.ActivityNoInternetBinding

class NoInternetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoInternetBinding
    private lateinit var cm: ConnectivityManager

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread { goToApp() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cm = getSystemService(ConnectivityManager::class.java)

        binding.btnRetry.setOnClickListener {
            if (NetworkUtils.isOnline(this)) goToApp()
        }

        binding.btnWifi.setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        setupSystemBars()
    }

    private fun setupSystemBars() {
        val status = ContextCompat.getColor(this, com.sunshine.appsuite.R.color.google_background_settings)
        val nav = ContextCompat.getColor(this, com.sunshine.appsuite.R.color.google_background_settings)

        window.statusBarColor = status
        window.navigationBarColor = nav

        val controller = WindowCompat.getInsetsController(window, binding.root)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }

    override fun onStart() {
        super.onStart()
        cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    override fun onStop() {
        super.onStop()
        cm.unregisterNetworkCallback(callback)
    }

    private fun goToApp() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}
