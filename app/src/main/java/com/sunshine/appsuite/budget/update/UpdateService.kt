package com.sunshine.appsuite.budget.update

import retrofit2.http.GET

interface UpdateService {
    @GET("appsuite/update.json") // Ajusta la ruta según tu servidor
    suspend fun getUpdateInfo(): UpdateResponse
}