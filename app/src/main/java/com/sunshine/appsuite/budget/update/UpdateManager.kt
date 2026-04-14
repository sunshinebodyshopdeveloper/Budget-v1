package com.sunshine.appsuite.budget.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

object UpdateManager {

    private var downloadId: Long = -1

    /**
     * Inicia la descarga del APK e intenta instalarlo automáticamente al terminar.
     */
    fun downloadAndInstall(context: Context, url: String) {
        // Usamos siempre el applicationContext para evitar fugas de memoria y asegurar persistencia
        val appContext = context.applicationContext
        val fileName = "appsuite_update.apk"

        // Ruta: /storage/emulated/0/Android/data/com.sunshine.appsuite/files/Download/appsuite_update.apk
        val file = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        // Limpieza previa si ya existe un archivo de una descarga fallida anterior
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Actualización de AppSuite")
            .setDescription("Descargando la nueva versión...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        // El Receptor que espera el aviso del Sistema Operativo
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == id) {
                    // Feedback visual: Si ves este Toast, el Receiver funcionó
                    Toast.makeText(ctx, "Descarga completada. Iniciando instalador...", Toast.LENGTH_SHORT).show()

                    installApk(ctx, file)

                    try {
                        ctx.unregisterReceiver(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // REGISTRO DEL RECEIVER
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // USAR RECEIVER_EXPORTED: Crucial para que el proceso del sistema (DownloadManager)
            // pueda comunicarse con nuestra App.
            appContext.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(onComplete, filter)
        }
    }

    private fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "Error: El archivo APK no se encontró.", Toast.LENGTH_LONG).show()
            return
        }

        // 1. VERIFICAR PERMISO DE INSTALACIÓN (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                // Si no tiene permiso, lo mandamos a la pantalla de Ajustes específica de nuestra App
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                Toast.makeText(context, "Por favor, concede permiso para instalar la actualización.", Toast.LENGTH_LONG).show()
                return
            }
        }

        // 2. CREAR EL URI SEGURO MEDIANTE FILEPROVIDER
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        // 3. LANZAR EL INTENT DE INSTALACIÓN
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Necesario al lanzar desde un Context no-Activity
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Permiso temporal de lectura para el instalador
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al abrir el instalador: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://pilotosadac.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(UpdateService::class.java)

    suspend fun checkNewVersion(currentCode: Long): UpdateResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = service.getUpdateInfo()
                if (response.versionCode > currentCode) response else null
            } catch (e: Exception) {
                null
            }
        }
    }
}