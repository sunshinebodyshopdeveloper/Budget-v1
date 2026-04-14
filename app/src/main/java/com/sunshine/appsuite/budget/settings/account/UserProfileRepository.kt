package com.sunshine.appsuite.budget.settings.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.sunshine.appsuite.budget.data.network.ApiClient
import com.sunshine.appsuite.budget.data.network.UpdateProfileRequest
import com.sunshine.appsuite.budget.data.network.UserApi
import com.sunshine.appsuite.budget.data.network.UserResponse
import com.sunshine.appsuite.budget.security.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class UserProfileRepository(context: Context) {

    private val appContext = context.applicationContext

    private val tokenManager = TokenManager(appContext)
    private val retrofit = ApiClient.createRetrofit(tokenManager)
    private val api = retrofit.create(UserApi::class.java)

    suspend fun getUser(): UserResponse = withContext(Dispatchers.IO) {
        api.getUser()
    }

    /**
     * Actualiza datos del perfil (texto).
     * Después siempre hacemos GET /user para traer la verdad del backend.
     */
    suspend fun updateProfile(
        name: String? = null,
        email: String? = null,
        username: String? = null,
        phone: String? = null
    ): UserResponse = withContext(Dispatchers.IO) {
        api.updateProfile(
            UpdateProfileRequest(
                name = name,
                email = email,
                username = username,
                phone = phone
            )
        )
        api.getUser()
    }

    /**
     * Sube avatar como multipart (cuando backend lo soporte).
     * Después hacemos GET /user para confirmar lo que quedó.
     */
    suspend fun updateAvatar(avatarUri: Uri): UserResponse = withContext(Dispatchers.IO) {
        val jpegFile = createCompressedJpeg(avatarUri, maxDim = 1280, quality = 85)

        try {
            val avatarPart = MultipartBody.Part.createFormData(
                "avatar", // si backend usa "photo" o "file", cámbialo aquí
                jpegFile.name,
                jpegFile.asRequestBody("image/jpeg".toMediaType())
            )

            val fields = emptyMap<String, RequestBody>()
            api.updateProfileMultipart(fields, avatarPart)

            api.getUser()
        } finally {
            runCatching { jpegFile.delete() }
        }
    }

    private fun createCompressedJpeg(uri: Uri, maxDim: Int, quality: Int): File {
        val resolver = appContext.contentResolver
        val source = ImageDecoder.createSource(resolver, uri)

        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val biggest = max(w, h)

            if (biggest > maxDim) {
                val scale = maxDim.toFloat() / biggest.toFloat()
                decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
            }
            decoder.isMutableRequired = false
        }

        val outFile = File.createTempFile("avatar_upload_", ".jpg", appContext.cacheDir)
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
        }
        return outFile
    }
}
