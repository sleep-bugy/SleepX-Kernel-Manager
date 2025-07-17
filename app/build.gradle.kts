plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "id.xms.xtrakernelmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.xms.xtrakernelmanager"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Material 3 expressive + icons extended
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2")

    // Accompanist untuk animasi dan system UI
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    // ViewModel dan LiveData (optional tapi umum)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coil untuk image (kalau mau load logo atau gambar builder nanti)
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Device Info (untuk info hardware/software kernel, build, dll)
    implementation("com.github.iamutkarshtiwari:DeviceInfo:1.0.2")

    // Lottie (opsional untuk easter egg animasi lucu)
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // Coroutines (kalau butuh proses async ringan)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // LibSU (akses root shell, file API, dan utils)
    implementation("com.github.topjohnwu.libsu:core:5.0.4")
    implementation("com.github.topjohnwu.libsu:io:5.0.4")
    implementation("com.github.topjohnwu.libsu:nio:5.0.4")
}