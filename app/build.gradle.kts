plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlinx-serialization")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "id.xms.xtrakernelmanager"
    compileSdk = 35
    defaultConfig {
        applicationId = "id.xms.xtrakernelmanager"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.1.-Release"
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
    kotlin {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.work:work-runtime-ktx:2.10.3")

    implementation(platform("androidx.compose:compose-bom:2025.07.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Versi yang aman untuk Kotlin 1.9.24
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")

    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57")
    implementation(libs.androidx.material3.android)
    kapt("com.google.dagger:hilt-compiler:2.57")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    testImplementation("com.google.dagger:hilt-android-testing:2.57")
    kaptTest("com.google.dagger:hilt-compiler:2.57")


    // LibSu & Coil
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Serialization yang cocok dengan Kotlin 1.9.24
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}