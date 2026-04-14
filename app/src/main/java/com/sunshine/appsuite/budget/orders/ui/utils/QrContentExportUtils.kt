package com.sunshine.appsuite.budget.orders.ui.utils

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.iterator

object QrContentExportUtils {

    private const val CACHE_FOLDER = "qr_exports"

    fun captureViewBitmap(
        view: View,
        backgroundColor: Int = Color.WHITE,
        excludeIds: IntArray? = null,
        hideMode: Int = View.GONE
    ): Bitmap? {
        val excludedViews = ArrayList<View>()
        excludeIds?.forEach { id ->
            view.findViewById<View>(id)?.let { excludedViews.add(it) }
        }

        val originalVisibility = HashMap<View, Int>(excludedViews.size)
        for (v in excludedViews) originalVisibility[v] = v.visibility

        // Oculta temporalmente
        for (v in excludedViews) v.visibility = hideMode

        return try {
            val w = view.width.takeIf { it > 0 } ?: view.measuredWidth
            val h = view.height.takeIf { it > 0 } ?: view.measuredHeight
            if (w <= 0 || h <= 0) return null

            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                val canvas = Canvas(bmp)
                canvas.drawColor(backgroundColor)
                view.draw(canvas)
            }
        } catch (_: Exception) {
            null
        } finally {
            // Restaura visibilidad original sí o sí
            for ((v, vis) in originalVisibility) v.visibility = vis
        }
    }

    fun saveBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        fileNamePrefix: String = "ot_preview"
    ): Uri? {
        return runCatching {
            val dir = File(context.cacheDir, CACHE_FOLDER).apply { mkdirs() }
            val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.png")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrNull()
    }

    fun shareImage(
        context: Context,
        imageUri: Uri,
        chooserTitle: CharSequence,
        text: String? = null
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            if (!text.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, text)

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("ot_preview", imageUri)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun shareView(
        context: Context,
        view: View,
        chooserTitle: CharSequence,
        text: String? = null,
        backgroundColor: Int = Color.WHITE,
        fileNamePrefix: String = "ot_preview",
        excludeIds: IntArray? = null,
        hideMode: Int = View.GONE
    ): Boolean {
        val bitmap = captureViewBitmap(
            view = view,
            backgroundColor = backgroundColor,
            excludeIds = excludeIds,
            hideMode = hideMode
        ) ?: return false

        val uri = saveBitmapToCache(context, bitmap, fileNamePrefix) ?: return false
        shareImage(context, uri, chooserTitle, text)
        return true
    }

    fun printBitmap(context: Context, bitmap: Bitmap, jobName: String) {
        val helper = PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }
        helper.printBitmap(jobName, bitmap)
    }

    fun printView(
        context: Context,
        view: View,
        jobName: String,
        backgroundColor: Int = Color.WHITE,
        excludeIds: IntArray? = null,
        hideMode: Int = View.GONE
    ): Boolean {
        val bitmap = captureViewBitmap(
            view = view,
            backgroundColor = backgroundColor,
            excludeIds = excludeIds,
            hideMode = hideMode
        ) ?: return false

        printBitmap(context, bitmap, jobName)
        return true
    }
}
