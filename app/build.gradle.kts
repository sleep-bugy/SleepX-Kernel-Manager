plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlinx-serialization")
}

android {
    namespace = "id.xms.xtrakernelmanager"
    compileSdk = 35
    defaultConfig {
        applicationId = "id.xms.xtrakernelmanager"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Versi yang aman untuk Kotlin 1.9.24
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    implementation(libs.androidx.material3.android)
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // LibSu & Coil
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Serialization yang cocok dengan Kotlin 1.9.24
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}