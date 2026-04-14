package com.sunshine.appsuite.budget.update

data class UpdateResponse(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)