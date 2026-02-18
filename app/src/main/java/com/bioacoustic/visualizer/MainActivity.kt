package com.bioacoustic.visualizer

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ez csak egy üres képernyőt mutat, hogy lássuk, elindul-e
        setContentView(R.layout.activity_main)
        
        // Csak engedélyt kérünk, semmi mást
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
    }
}

