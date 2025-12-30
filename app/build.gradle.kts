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
    compileSdk {
        version = release(36)
    }

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
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
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
    //
    implementation("com.github.arpitkatiyar1999:Country-Picker:3.0.1")
    // Kotlin extensions for Android framework - provides .dp, .sp units and Kotlin-friendly APIs
    implementation("androidx.core:core-ktx:1.12.0")

    // Lifecycle management - handles onCreate, onDestroy and provides lifecycleScope for coroutines
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Enables Jetpack Compose in Activities - allows setContent { } to display Compose UI
    implementation("androidx.activity:activity-compose:1.8.2")

    // Version management for Compose libraries - ensures all Compose dependencies are compatible
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))

    // Core Compose UI - provides basic composables like Box, Column, Row, Text, Image
    implementation("androidx.compose.ui:ui")

    // Graphics support - provides Color, Brush, Shape for custom drawing and styling
    implementation("androidx.compose.ui:ui-graphics")

    // Preview support - enables @Preview annotation to see UI in Android Studio without running app
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material Design 3 components - provides Button, Card, TextField, Chip and modern Material theming
    implementation("androidx.compose.material3:material3")

    // Extended Material icons - provides Icons.Default.Search, LocationOn, FavoriteBorder, etc.
    implementation("androidx.compose.material:material-icons-extended")

    // Image loading from URLs - loads and caches images from internet efficiently with rememberAsyncImagePainter()
    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("com.google.maps.android:maps-compose:4.3.3") // Maps for Compose
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Google Maps SDK
    implementation("com.google.android.gms:play-services-location:21.1.0") // Location services
    implementation("com.google.accompanist:accompanist-permissions:0.33.2-alpha")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("io.coil-kt:coil-compose:2.4.0")
//    // for PhoneAuth
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-auth")

    implementation("com.github.ZEGOCLOUD:zego_uikit_prebuilt_call_android:+")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    implementation("com.cloudinary:cloudinary-android:2.1.0")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
}