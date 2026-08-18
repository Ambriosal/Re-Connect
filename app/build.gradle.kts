plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.reconnect"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.reconnect"
        minSdk = 26
        targetSdk = 35
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
    val room_version = "2.6.1"

    // ── Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ── Jetpack Compose (BOM manages all ui/material3 versions automatically)
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling") // ← preview renderer, debug only

    // ── ViewModel inside Composables  (was MISSING — caused viewModel() errors)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // ── Navigation (updated: 2.7.7 → 2.8.9)
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // ── Room
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // ── DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── WorkManager
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // ── Permissions (updated: 0.34.0 → 0.36.0 to match Compose BOM)
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // ── Image loading (contact photos, content:// URIs)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:$room_version")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}