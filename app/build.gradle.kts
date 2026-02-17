plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bioacoustic.visualizer"
    compileSdk = 35 // Android 16-hoz már a legmagasabb kell

    defaultConfig {
        applicationId = "com.bioacoustic.visualizer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-A16-FIX"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // ANDROID 16 SPECIÁLIS BEÁLLÍTÁS: 16KB-os lapméret támogatása
    packaging {
        jniLibs {
            useLegacyPackaging = true // Segít a betöltésben régebbi és új rendszereken is
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // FILAMENT - A legfrissebb verzió kell az Android 16 kompatibilitáshoz
    implementation("com.google.android.filament:filament-android:1.51.0")
    implementation("com.google.android.filament:gltfio-android:1.51.0")
    implementation("com.google.android.filament:filament-utils-android:1.51.0")

    // MATEK (FFT)
    implementation("com.github.wendykierp:JTransforms:3.1")

    // KORUTINOK
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
