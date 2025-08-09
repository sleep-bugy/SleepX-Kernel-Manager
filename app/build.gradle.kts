

import org.apache.http.client.methods.HttpPost
import org.apache.http.client.config.RequestConfig
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.util.Date
import kotlin.text.substringAfterLast


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
        versionName = "1.2.-Release"
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
        lint.disable.add("NullSafeMutableLiveData")


    }
    kotlin {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    configurations.all {
        resolutionStrategy {
            force("com.google.guava:guava:32.1.3-jre")
        }
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
    implementation(libs.androidx.compilercommon)
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

    implementation("com.google.guava:guava:32.1.3-jre") {
        exclude(mapOf("group" to "com.google.guava", "module" to "listenablefuture"))
    }
}


tasks.register("sendTelegramMessage", SendTelegramMessageTask::class) {
    group = "custom"
    description = "Sends a build status message to Telegram."
    // Properties will be set via convention mapping or direct assignment
}

abstract class SendTelegramMessageTask : DefaultTask() {

    @get:Input
    abstract val telegramBotToken: Property<String>

    @get:Input
    abstract val telegramChatId: Property<String>

    @get:Input
    abstract val appVersionName: Property<String>

    @get:Input
    abstract val appPackageName: Property<String>

    @get:Input
    abstract val appProjectName: Property<String>


    init {
        // Initialize from project properties
        telegramBotToken.convention(project.findProperty("telegramBotToken")?.toString() ?: "")
        telegramChatId.convention(project.findProperty("telegramChatId")?.toString() ?: "")
        // These will be configured by the invoking task or a general build listener
        appVersionName.convention("")
        appPackageName.convention("")
        appProjectName.convention(project.name) // Default to current project name
    }

    @TaskAction
    fun sendMessage() {
        if (telegramBotToken.get().isEmpty() || telegramChatId.get().isEmpty()) {
            logger.warn("Telegram Bot Token or Chat ID not found in gradle.properties. Skipping message.")
            return
        }

        val buildStatus = if (project.gradle.taskGraph.allTasks.any { it.state.failure != null }) {
            "FAILED"
        } else {
            "SUCCESS"
        }

        val currentAppVersion = appVersionName.getOrElse(project.android.defaultConfig.versionName ?: "N/A")
        val currentAppPackage = appPackageName.getOrElse(project.android.defaultConfig.applicationId ?: "N/A")
        val currentProjectName = appProjectName.get()

        val javaVersion = JavaVersion.current().toString()
        val gradleVersion = project.gradle.gradleVersion
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val processor = try {
            val process = ProcessBuilder("cat", "/proc/cpuinfo")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.lines().find { it.startsWith("model name") }?.substringAfter(":")?.trim() ?: osArch
        } catch (e: Exception) {
            logger.warn("Could not read /proc/cpuinfo: ${e.message}")
            osArch // Fallback
        }

        // Function to escape MarkdownV2 characters
        fun escapeMarkdownV2(text: String): String {
            val escapeChars = charArrayOf('_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!')
            var result = text
            for (ch in escapeChars) {
                result = result.replace(ch.toString(), "\\" + ch)
            }
            return result
        }

        var message = "🚀 *Build Status: ${escapeMarkdownV2(project.name)} \\- $buildStatus* 🚀\n\n" +
                "📦 *App:* ${escapeMarkdownV2(currentProjectName)}\n" +
                "🏷️ *Version:* ${escapeMarkdownV2(currentAppVersion)}\n" +
                "🆔 *Package:* ${escapeMarkdownV2(currentAppPackage)}\n" +
                "📅 *Time:* ${escapeMarkdownV2(Date().toString())}\n\n" +
                "🔧 *Build Environment:*\n" +
                "  \\- Java: ${escapeMarkdownV2(javaVersion)}\n" +
                "  \\- Gradle: ${escapeMarkdownV2(gradleVersion)}\n" +
                "  \\- OS: ${escapeMarkdownV2("$osName ($osArch)")}\n" +
                "  \\- Processor: ${escapeMarkdownV2(processor)}"

        if (buildStatus == "FAILED") {
            val failedTasks = project.gradle.taskGraph.allTasks.filter { it.state.failure != null }
            if (failedTasks.isNotEmpty()) {
                val errorDetails = failedTasks.joinToString(separator = "\n") { task ->
                    val taskName = escapeMarkdownV2(task.path)
                    // Ambil baris pertama dari pesan error, atau pesan umum jika tidak ada
                    val errorMessage = escapeMarkdownV2(
                        task.state.failure?.message?.lines()?.firstOrNull()?.trim() ?: "No specific error message"
                    )
                    "  \\- Task `$taskName` failed: $errorMessage"
                }
                message += "\n\n⚠️ *Error Details:*\n$errorDetails"
            } else {
                message += "\n\n⚠️ *Build failed. Check build logs for details.*"
            }
        }

        // Tidak perlu memanggil escapeMarkdownV2 pada keseluruhan message lagi
        // karena kita sudah meng-escape setiap bagian secara individual.
        // val escapedMessage = escapeMarkdownV2(message) // HAPUS ATAU KOMENTARI BARIS INI

        val url = "https://api.telegram.org/bot${telegramBotToken.get()}/sendMessage"
        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(15 * 1000)
            .setSocketTimeout(30 * 1000)
            .build()

        HttpClients.custom().setDefaultRequestConfig(requestConfig).build().use { httpClient ->
            val post = HttpPost(url)
            val jsonPayload = """
            {
                "chat_id": "${telegramChatId.get().replace("\"", "\\\"")}",
                "text": "${message.replace("\"", "\\\"")}", // Gunakan message langsung
                "parse_mode": "MarkdownV2"
            }
            """.trimIndent()
            post.entity = StringEntity(jsonPayload, "UTF-8")
            post.setHeader("Content-Type", "application/json")

            try {
                logger.info("Sending build status message to Telegram...")
                logger.info("Payload: $jsonPayload") // Tambahkan log untuk payload
                val response = httpClient.execute(post)
                val responseBody = EntityUtils.toString(response.entity, "UTF-8")
                if (response.statusLine.statusCode in 200..299) {
                    logger.lifecycle("Successfully sent message to Telegram. Response: $responseBody")
                } else {
                    logger.error("Failed to send message to Telegram. Status: ${response.statusLine}, Body: $responseBody. Payload: $jsonPayload")
                }
                EntityUtils.consumeQuietly(response.entity)
            } catch (e: Exception) {
                logger.error("Failed to send message to Telegram: ${e.message}", e)
            }
        }
    }
}

tasks.register("uploadDebugApkToTelegram", UploadApkToTelegramTask::class) {
    group = "custom"
    description = "Builds and uploads the debug APK to Telegram."
    // This task will depend on assembleDebug implicitly if not specified,
    // but explicit is better for clarity if you intend to run it standalone.
    dependsOn("assembleRelease") // If you want to run this task and ensure debug APK is built.
    // If it's only run *after* assembleDebug, this isn't strictly needed here.
    apkFile.set(project.layout.projectDirectory.file("release/app-release.apk"))
    // Alternative for finding the first APK if the name is dynamic:
    // apkFile.set(project.layout.buildDirectory.dir("outputs/apk/debug").map { dir ->
    //     dir.asFile.listFiles { _, name -> name.endsWith(".apk") }?.firstOrNull()
    // })
}

abstract class UploadApkToTelegramTask : DefaultTask() {

    @get:Input
    abstract val telegramBotToken: Property<String>

    @get:Input
    abstract val telegramChatId: Property<String>

    @get:InputFile
    abstract val apkFile: RegularFileProperty // Use RegularFileProperty for task inputs

    @get:Input
    abstract val appVersionName: Property<String>

    @get:Input
    abstract val appName: Property<String> // You might want to make this configurable

    init {
        telegramBotToken.convention(project.findProperty("telegramBotToken")?.toString() ?: "")
        telegramChatId.convention(project.findProperty("telegramChatId")?.toString() ?: "")
        appVersionName.convention(project.android.defaultConfig.versionName ?: "N/A")
        appName.convention(project.name) // Default to project name, or set a fixed string
    }

    @TaskAction
    fun uploadApk() {
        if (telegramBotToken.get().isEmpty() || telegramChatId.get().isEmpty()) {
            logger.warn("Telegram Bot Token or Chat ID not found. Skipping APK upload.")
            return
        }

        val currentApkFile = apkFile.get().asFile
        if (!currentApkFile.exists()) {
            logger.error("Release APK not found at ${currentApkFile.absolutePath}. Ensure assembleRelease has run.")
            return
        }

        val fileSizeMb = currentApkFile.length() / (1024.0 * 1024.0)
        logger.lifecycle("Attempting to upload APK: ${currentApkFile.name} (Size: ${"%.2f".format(fileSizeMb)} MB)")

        // Telegram Bot API limit can be up to 2GB for bots, but typically 50MB for general use without special setup.
        if (fileSizeMb > 109) { // Check slightly less than 110MB to be safe
            logger.error("APK size (${"%.2f".format(fileSizeMb)} MB) exceeds Telegram's typical 50MB limit. Skipping upload.")
            // Optionally send a message indicating the failure to upload due to size
            // tasks.named("sendTelegramMessage", SendTelegramMessageTask::class) {
            //     // Configure message about upload failure
            // }.get().sendMessage() // This is a bit complex; better to have a dedicated message for this
            return
        }


        val caption = "📦 New Release build: ${appName.get()} v${appVersionName.get()}\n" +
                "Build time: ${Date()}\n" +
                "File: ${currentApkFile.name} (${"%.2f".format(fileSizeMb)} MB)"

        val url = "https://api.telegram.org/bot${telegramBotToken.get()}/sendDocument"
        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(30 * 1000) // 30 seconds connection timeout
            .setSocketTimeout(5 * 60 * 1000)  // 5 minutes socket timeout (for larger uploads)
            .build()

        HttpClients.custom().setDefaultRequestConfig(requestConfig).build().use { httpClient ->
            val post = HttpPost(url)
            val entityBuilder = MultipartEntityBuilder.create()
            entityBuilder.addTextBody("chat_id", telegramChatId.get())
            entityBuilder.addTextBody("caption", caption, org.apache.http.entity.ContentType.TEXT_PLAIN.withCharset("UTF-8"))
            entityBuilder.addPart("document", FileBody(currentApkFile))
            post.entity = entityBuilder.build()

            try {
                logger.info("Uploading APK to Telegram...")
                val response = httpClient.execute(post)
                val responseBody = EntityUtils.toString(response.entity, "UTF-8")
                if (response.statusLine.statusCode in 200..299) {
                    logger.lifecycle("Successfully uploaded APK to Telegram. Response: $responseBody")
                } else if (response.statusLine.statusCode == 400 && responseBody.contains("can't parse entities")) {
                    logger.error("Failed to upload APK to Telegram due to Markdown parsing error. Status: ${response.statusLine}, Body: $responseBody. Caption was: $caption")
                } else {
                    logger.error("Failed to upload APK to Telegram. Status: ${response.statusLine}, Body: $responseBody")
                }
                EntityUtils.consumeQuietly(response.entity)
            } catch (e: Exception) {
                logger.error("Failed to upload APK to Telegram: ${e.message}", e)
                // e.printStackTrace() // logger.error with exception will print stack trace with --stacktrace
            }
        }
    }
}

// --- Hook tasks into the build lifecycle ---

// Configure the sendTelegramMessage task
project.afterEvaluate { // Ensure android extension is available
    tasks.named("sendTelegramMessage", SendTelegramMessageTask::class) {
        appVersionName.set(project.provider { android.defaultConfig.versionName ?: "N/A" })
        appPackageName.set(project.provider { android.defaultConfig.applicationId ?: "N/A" })
        appProjectName.set(project.provider {
            android.namespace?.substringAfterLast('.') ?: project.name
        })
    }
}

// Configure the uploadDebugApkToTelegram task
tasks.named("uploadDebugApkToTelegram", UploadApkToTelegramTask::class) {
    // Configuration...
}

// DEFER HOOKING INTO PLUGIN TASKS:
project.afterEvaluate {
    tasks.named("assembleRelease") {
        finalizedBy("sendTelegramMessage")
    } // Add this closing brace

    // Run uploadDebugApkToTelegram after assembleDebug
    tasks.named("assembleRelease") {
        finalizedBy("uploadDebugApkToTelegram")
    }
}