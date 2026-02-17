# Bioacoustic_Visualizer

Bioacoustic_Visualizer/
├── .github/
│   └── workflows/
│       └── android-ci.yml   (A "Tyúk" build scriptje)
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── bioacoustic/
│   │       │           └── visualizer/
│   │       │               ├── core/
│   │       │               │   ├── audio/
│   │       │               │   │   └── AudioAnalyzer.kt
│   │       │               │   ├── render/
│   │       │               │   │   └── FilamentPointCloudRenderer.kt
│   │       │               │   └── stream/
│   │       │               │       └── VisualDataStreamer.kt
│   │       │               └── MainActivity.kt
│   │       ├── res/                  (Itt vannak az erőforrások)
│   │       │   ├── drawable/
│   │       │   │   └── ic_launcher_foreground.xml
│   │       │   ├── layout/           (EZT A MAPPÁT KELL PÓTOLNI!)
│   │       │   │   └── activity_main.xml (ÉS EZT A FÁJLT!)
│   │       │   ├── mipmap-anydpi-v26/
│   │       │   │   └── ic_launcher.xml
│   │       │   └── values/
│   │       │       ├── strings.xml
│   │       │       └── themes.xml
│   │       └── AndroidManifest.xml   (Engedélyek: mikrofon)
│   └── build.gradle.kts          (App szintű beállítások, függőségek)
├── build.gradle.kts              (Projekt szintű beállítások)
├── gradlew
├── settings.gradle.kts
└── README.md                     (Ebbe a fájlba teheted ezt a rajzot!)
