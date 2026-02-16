# Filament megőrzése (Native kód miatt)
-keep class com.google.android.filament.** { *; }
-keep class com.google.android.filament.android.** { *; }

# JTransforms megőrzése (FFT számításokhoz)
-keep class org.jtransforms.** { *; }

# Kotlin Coroutines megőrzése
-keep class kotlinx.coroutines.** { *; }

