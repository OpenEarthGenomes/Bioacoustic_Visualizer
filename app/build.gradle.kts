plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bioacoustic.visualizer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bioacoustic.visualizer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
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

    // Filament - A verzió 1.51.0-ra javítva a build hiba miatt
    implementation("com.google.android.filament:filament-android:1.51.0")
    implementation("com.google.android.filament:gltfio-android:1.51.0")
    implementation("com.google.android.filament:filament-utils-android:1.51.0")

    // FFT matek
    implementation("com.github.wendykierp:JTransforms:3.1")

    // Korutinok
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
