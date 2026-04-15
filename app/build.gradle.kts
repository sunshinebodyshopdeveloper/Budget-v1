plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.android.secrets)
}

android {
    namespace = "com.sunshine.appsuite.budget"

    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.sunshine.appsuite.budget"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // URL de API para ambiente de pruebas / desarrollo
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://api.devtesthub.online/\""
            )
        }
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // URL de API para producción (por ahora la misma)
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://api.devtesthub.online/\""
            )
        }
    }

    // Vamos a usar ViewBinding en TODA la app
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Core AndroidX / UI base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.graphics.shapes)

    // SplashScreen
    implementation(libs.androidx.core.splashscreen)

    // SlidingPaneLayout
    implementation(libs.androidx.slidingpanelayout)

    // UI extra: lists, pagination type onboarding
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)

    // Lifecycle / ViewModel
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Coroutines (calls async, Retrofit, etc.)
    implementation(libs.kotlinx.coroutines.android)

    // Networking (API Sunshine)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp.logging.interceptor)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Location (Google Play Services Location)
    implementation(libs.play.services.location)

    // Security Crypto (EncryptedSharedPreferences + Keystore)
    implementation(libs.androidx.security.crypto)

    // Biometric
    implementation(libs.androidx.biometric)

    // QR Scanner (CameraX + ML Kit)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.play.services.mlkit.barcode.scanning)
    implementation(libs.play.services.mlkit.text.recognition)

    // Fragment KTX
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)

    // PintHelper
    implementation(libs.androidx.print)

    // Gemini
    implementation(libs.generativeai)

    //Wallpaper
    implementation(libs.coil)
    implementation(libs.androidx.ui)

    //Glide
    implementation(libs.picasso)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.gms.play.services.mlkit.text.recognition)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
