plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // Ez a namespace kapcsolja össze a kódot a Gradle-lel
    namespace = "com.bioacoustic.visualizer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bioacoustic.visualizer"
        // Android 8.0-tól felfelé mindenhol futni fog
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-BioAcoustic"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // EZ A RÉSZ JAVÍTJA A "databinding/viewbinding" HIBÁKAT
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    // A Filament 3D motor fájljait nem szabad tömöríteni
    androidResources {
        noCompress += listOf("filamat", "glb", "gltf")
    }
}

dependencies {
    // Alapvető Android könyvtárak
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // 3D MEGJELENÍTÉS (Filament 1.45.0)
    implementation("com.google.android.filament:filament-android:1.45.0")
    implementation("com.google.android.filament:filament-utils-android:1.45.0")
    
    // HANGFELDOLGOZÁS (FFT és Coroutines)
    implementation("com.github.wendykierp:JTransforms:3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Teszteléshez
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
