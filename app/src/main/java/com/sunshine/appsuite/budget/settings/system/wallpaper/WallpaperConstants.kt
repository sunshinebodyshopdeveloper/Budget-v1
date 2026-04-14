package com.sunshine.appsuite.budget.settings.system.wallpaper

object WallpaperConstants {
    //private const val BASE_URL = "https://app.sunshineappsuite.com/wallpaper/"
    private const val BASE_URL = "https://pilotosadac.com/appsuite/wallpaper/"
    private const val TOTAL_WALLPAPERS = 10

    val WALLPAPERS: List<String> = (1..TOTAL_WALLPAPERS).map { index ->
        "${BASE_URL}wallpaper_$index.png"
    }
}