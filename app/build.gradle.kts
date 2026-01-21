import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.gharbato"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gharbato"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties().apply {
            val files = listOf(
                rootProject.file("local.properties"),
                project.file("local.properties"),
            )
            files.filter { it.exists() }.forEach { file ->
                file.inputStream().use { load(it) }
            }
        }

        val zegoAppId = (
                project.findProperty("ZEGO_APP_ID")
                    ?: localProps.getProperty("ZEGO_APP_ID")
                    ?: System.getenv("ZEGO_APP_ID")
                    ?: System.getProperty("ZEGO_APP_ID")
                    ?: "554967872"
                ).toString().trim()

        val zegoAppSign = (
                project.findProperty("ZEGO_APP_SIGN")
                    ?: localProps.getProperty("ZEGO_APP_SIGN")
                    ?: System.getenv("ZEGO_APP_SIGN")
                    ?: System.getProperty("ZEGO_APP_SIGN")
                    ?: "d244d75c0f12cb4eb2c41d74adb071467ba16e82eb9ef2625e06453bd4347873"
                ).toString().trim()

        val zegoAppSignEscaped = zegoAppSign
            .replace("\\\\", "\\\\\\\\")
            .replace("\"", "\\\\\"")

        buildConfigField("long", "ZEGO_APP_ID", zegoAppId)
        buildConfigField("String", "ZEGO_APP_SIGN", "\"${zegoAppSignEscaped}\"")

        val geminiApiKey = (
                project.findProperty("GEMINI_API_KEY")
                    ?: localProps.getProperty("GEMINI_API_KEY")
                    ?: System.getenv("GEMINI_API_KEY")
                    ?: System.getProperty("GEMINI_API_KEY")
                    ?: ""
                ).toString().trim()

        val geminiApiKeyEscaped = geminiApiKey
            .replace("\\\\", "\\\\\\\\")
            .replace("\"", "\\\\\"")

        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKeyEscaped}\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4")

    // ========== FIREBASE BOM - Manages all Firebase versions ==========
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    // ========== ADD THIS LINE - Firebase Cloud Messaging ==========
    implementation("com.google.firebase:firebase-messaging-ktx")
    // ========== END OF FCM ADDITION ==========

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation("androidx.browser:browser:1.9.0")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.compose.runtime.livedata)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("com.github.arpitkatiyar1999:Country-Picker:3.0.1")

    // Kotlin extensions for Android framework
    implementation("androidx.core:core-ktx:1.12.0")

    // Lifecycle management
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Jetpack Compose in Activities
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))

    // Core Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material Design 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.accompanist:accompanist-permissions:0.33.2-alpha")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Firebase (already managed by BOM above)
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-auth")

    // Zego Video Call
    implementation("com.github.ZEGOCLOUD:zego_uikit_prebuilt_call_android:+")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:2.1.0")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
}