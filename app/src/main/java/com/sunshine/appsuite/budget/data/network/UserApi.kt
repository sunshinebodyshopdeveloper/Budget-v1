package com.sunshine.appsuite.budget.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface UserApi {

    @GET("api/v1/user")
    suspend fun getUser(): UserResponse

    /**
     * Actualiza datos del perfil (texto) con JSON
     * POST api/v1/user/profile
     */
    @POST("api/v1/user/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): ResponseBody

    /**
     * Sube avatar (y opcionalmente otros campos) con MULTIPART
     * POST api/v1/user/profile
     *
     * ⚠️ El nombre del file part lo ponemos como "avatar".
     * Si tu backend espera "photo" o "file", cámbialo en el Repository.
     */
    @Multipart
    @POST("api/v1/user/profile")
    suspend fun updateProfileMultipart(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part avatar: MultipartBody.Part?
    ): ResponseBody
}

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    val username: String? = null,
    val phone: String? = null,
    // Si tu backend acepta avatar como URL (string), puedes usar esto:
    val avatar: String? = null
)

// DTO que mapea 1 a 1 el JSON de /api/v1/user
data class UserResponse(
    val id: Int? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("email_verified_at")
    val emailVerifiedAt: String? = null,

    @SerializedName("two_factor_secret")
    val twoFactorSecret: String? = null,

    @SerializedName("two_factor_recovery_codes")
    val twoFactorRecoveryCodes: String? = null,

    @SerializedName("two_factor_confirmed_at")
    val twoFactorConfirmedAt: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("media")
    val media: List<MediaItem>? = null
)

// Estructura básica para "media" (aunque hoy venga [])
data class MediaItem(
    val id: Int? = null,

    @SerializedName("file_name")
    val fileName: String? = null,

    @SerializedName("original_url")
    val originalUrl: String? = null,

    @SerializedName("preview_url")
    val previewUrl: String? = null
)
