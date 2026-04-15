package com.sunshine.appsuite.budget.user.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import com.sunshine.appsuite.budget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Loader súper simple (sin Coil/Glide) para mostrar avatar desde URL.
 * - Cache en memoria para no estar bajando lo mismo
 * - Evita race conditions usando un tag por ImageView
 */
object AvatarImageLoader {

    private val cache = object : LruCache<String, Bitmap>(maxCacheKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun load(
        scope: CoroutineScope,
        imageView: ImageView,
        url: String?,
        okHttpClient: OkHttpClient,
        placeholderRes: Int = R.drawable.ic_avatar
    ) {
        val normalized = url?.trim().orEmpty()

        if (normalized.isBlank()) {
            imageView.setImageResource(placeholderRes)
            imageView.setTag(R.id.tag_avatar_url, null)
            return
        }

        imageView.setTag(R.id.tag_avatar_url, normalized)

        cache.get(normalized)?.let { cached ->
            imageView.setImageBitmap(cached)
            return
        }

        imageView.setImageResource(placeholderRes)

        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                downloadBitmap(normalized, okHttpClient)
            } ?: return@launch

            cache.put(normalized, bitmap)

            val stillWantsThisUrl = imageView.getTag(R.id.tag_avatar_url) == normalized
            if (stillWantsThisUrl) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun downloadBitmap(url: String, okHttpClient: OkHttpClient): Bitmap? {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    private fun maxCacheKb(): Int {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        // 1/8 de la memoria máxima suele ser suficiente para thumbs
        return maxMemoryKb / 8
    }
}
