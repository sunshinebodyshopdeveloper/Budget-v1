package com.sunshine.appsuite.budget.user

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sunshine.appsuite.budget.data.network.UpdateProfileRequest
import com.sunshine.appsuite.budget.data.network.UserApi
import com.sunshine.appsuite.budget.security.TokenManager
import com.sunshine.appsuite.budget.user.data.UserProfileCache
import com.sunshine.appsuite.budget.user.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Single Source of Truth del perfil.
 *
 * - UI observa [profile] y se actualiza en caliente (sin reiniciar app)
 * - La API sigue siendo la verdad; aquí solo cacheamos y emitimos
 */
class UserProfileStore(
    appContext: Context,
    private val tokenManager: TokenManager,
    private val userApi: UserApi
) {

    sealed interface LoadState {
        data object Idle : LoadState
        data object Loading : LoadState
        data object Ready : LoadState
        data class Error(val message: String?) : LoadState
    }

    private val context = appContext.applicationContext
    private val cache = UserProfileCache(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()

    private val _profile = MutableStateFlow<UserProfile?>(cache.load())
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _state = MutableStateFlow<LoadState>(LoadState.Idle)
    val state: StateFlow<LoadState> = _state.asStateFlow()

    private var lastRefreshAtMs: Long = 0L
    private val ttlMs: Long = 30_000L

    /**
     * Dispara refresh en background si hace falta.
     * No bloquea; ideal para onCreate/onResume.
     */
    fun ensureFresh(force: Boolean = false) {
        if (!tokenManager.hasToken()) {
            clear()
            return
        }
        scope.launch {
            runCatching { refresh(force = force) }
        }
    }

    /**
     * Refresca desde API (GET /user/profile o /user según tu backend).
     * Devuelve el perfil nuevo; si falla, lanza excepción.
     */
    suspend fun refresh(force: Boolean = false): UserProfile = mutex.withLock {
        if (!tokenManager.hasToken()) {
            clear()
            throw IllegalStateException("No hay sesión activa")
        }

        val now = System.currentTimeMillis()
        val current = _profile.value

        if (!force && current != null && (now - lastRefreshAtMs) < ttlMs) {
            return current
        }

        _state.value = LoadState.Loading

        return try {
            val user = withContext(Dispatchers.IO) { userApi.getUser() }
            val mapped = user.toUserProfile()

            cache.save(mapped)
            _profile.value = mapped
            lastRefreshAtMs = now
            _state.value = LoadState.Ready

            mapped
        } catch (e: Exception) {
            _state.value = LoadState.Error(e.message)
            throw e
        }
    }

    /**
     * Actualiza datos de perfil (name/email/phone/username) y luego refresca
     * para mantener la verdad sincronizada con la API.
     */
    suspend fun updateProfile(
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        username: String? = null
    ): UserProfile = mutex.withLock {

        if (!tokenManager.hasToken()) {
            clear()
            throw IllegalStateException("No hay sesión activa")
        }

        _state.value = LoadState.Loading

        return try {
            withContext(Dispatchers.IO) {
                userApi.updateProfile(
                    UpdateProfileRequest(
                        name = name,
                        email = email,
                        phone = phone,
                        username = username
                    )
                )
            }

            // Fuente de verdad = API: pedimos el perfil ya actualizado
            val user = withContext(Dispatchers.IO) { userApi.getUser() }
            val mapped = user.toUserProfile()

            cache.save(mapped)
            _profile.value = mapped
            lastRefreshAtMs = System.currentTimeMillis()
            _state.value = LoadState.Ready

            mapped
        } catch (e: Exception) {
            _state.value = LoadState.Error(e.message)
            throw e
        }
    }

    /**
     * Actualiza el avatar vía multipart.
     *
     * OJO: tu UserApi.updateProfileMultipart recibe:
     *  - fields: Map<String, RequestBody>
     *  - avatar: MultipartBody.Part?
     *
     * Así que NO usamos named args (Java) y NO mandamos strings vacíos (riesgo de borrar datos).
     */
    suspend fun updateAvatar(avatarUri: Uri): UserProfile = mutex.withLock {
        if (!tokenManager.hasToken()) {
            clear()
            throw IllegalStateException("No hay sesión activa")
        }

        _state.value = LoadState.Loading

        val temp = withContext(Dispatchers.IO) { createCompressedJpeg(avatarUri) }

        return try {
            val avatarPart = MultipartBody.Part.createFormData(
                "avatar",
                temp.name,
                temp.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )

            // Para no “borrar” campos, mandamos SOLO lo que queremos preservar.
            // Si tu backend no requiere esto, igual es seguro mandarlo.
            val partText = "text/plain".toMediaTypeOrNull()
            val current = _profile.value

            val fields = linkedMapOf<String, RequestBody>()
            current?.name?.takeIf { it.isNotBlank() }?.let { fields["name"] = it.toRequestBody(partText) }
            current?.email?.takeIf { it.isNotBlank() }?.let { fields["email"] = it.toRequestBody(partText) }
            current?.phone?.takeIf { it.isNotBlank() }?.let { fields["phone"] = it.toRequestBody(partText) }
            current?.username?.takeIf { it.isNotBlank() }?.let { fields["username"] = it.toRequestBody(partText) }

            withContext(Dispatchers.IO) {
                // Firma REAL: updateProfileMultipart(fields, avatar)
                userApi.updateProfileMultipart(fields, avatarPart)
            }

            // Refrescamos desde API para obtener el avatar URL final
            val user = withContext(Dispatchers.IO) { userApi.getUser() }
            val mapped = user.toUserProfile()

            cache.save(mapped)
            _profile.value = mapped
            lastRefreshAtMs = System.currentTimeMillis()
            _state.value = LoadState.Ready

            mapped
        } catch (e: Exception) {
            _state.value = LoadState.Error(e.message)
            throw e
        } finally {
            runCatching { temp.delete() }
        }
    }

    fun clear() {
        cache.clear()
        _profile.value = null
        lastRefreshAtMs = 0L
        _state.value = LoadState.Idle
    }

    // ------------------------
    // Avatar compression helper
    // ------------------------

    private fun createCompressedJpeg(uri: Uri): File {
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        // Reescalamos para evitar archivos gigantes (avatar no necesita 4K)
        val maxSide = 1024
        val w = bitmap.width
        val h = bitmap.height
        val scale = min(
            maxSide.toFloat() / w.toFloat(),
            maxSide.toFloat() / h.toFloat()
        ).coerceAtMost(1f)

        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
        } else bitmap

        val outFile = File.createTempFile("avatar_upload_", ".jpg", context.cacheDir)
        FileOutputStream(outFile).use { fos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, fos)
        }
        return outFile
    }
}
