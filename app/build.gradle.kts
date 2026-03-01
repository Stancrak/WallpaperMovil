plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}


android {
    namespace = "com.stanly.wallpapermovil"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.stanly.wallpapermovil"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Room schema export
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }


    // ── Signing ────────────────────────────────────────────────────────────────
    // Codemagic injects these four environment variables automatically when an
    // android_signing keystore is configured in codemagic.yaml.
    // Locally (without the vars) the release build falls back to the debug key.
    signingConfigs {
        val cmKeystorePath = System.getenv("CM_KEYSTORE_PATH")
        if (cmKeystorePath != null) {
            create("release") {
                storeFile      = file(cmKeystorePath)
                storePassword  = System.getenv("CM_KEYSTORE_PASSWORD") ?: ""
                keyAlias       = System.getenv("CM_KEY_ALIAS") ?: ""
                keyPassword    = System.getenv("CM_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use the release signing config when available; fall back to debug
            signingConfig = if (System.getenv("CM_KEYSTORE_PATH") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose (Material 3)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // AndroidX Media3 – ExoPlayer + GL Effects
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.effect)

    // DataStore Preferences
    implementation(libs.datastore.preferences)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Debug tooling
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

