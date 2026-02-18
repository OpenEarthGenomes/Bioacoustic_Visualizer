plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bioacoustic.visualizer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bioacoustic.visualizer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // Frissítve a stabil 1.53.0-ra a Kotlin 2.0 miatt
    implementation("com.google.android.filament:filament-android:1.53.0")
    implementation("com.google.android.filament:gltfio-android:1.53.0")
    implementation("com.google.android.filament:filament-utils-android:1.53.0")
    
    implementation("com.github.wendykierp:JTransforms:3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
