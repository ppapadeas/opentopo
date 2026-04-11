# HEPOS Transformation Model — Technical Specification

## Project Overview

OpenTopo is an open-source Android GNSS survey app that transforms WGS84/ETRS89 coordinates to GGRS87/EGSA87 (the Greek national coordinate system) using the official HEPOS transformation model. It connects to any NMEA-compatible GNSS receiver (primarily ArduSimple RTK) via Bluetooth or USB-OTG.

**License:** AGPL-3.0
**Package:** org.opentopo.app
**Min SDK:** API 26 (Android 8.0)
**Target SDK:** API 35

## Critical Context: Why This App Exists

No open-source Android app correctly transforms WGS84 → GGRS87 for surveying. The transformation requires a **7-parameter Helmert shift PLUS a 2D correction grid** published by Ktimatologio S.A. (Greek Cadastre). Without the grid, errors of 0.5–2m exist. Commercial controllers (Leica, Trimble) include this natively. Low-cost RTK users (ArduSimple + SW Maps) currently have no solution.

---

## THE HEPOS TRANSFORMATION MODEL (Authoritative)

Source: `reference/HEPOS_coord_transf_model_summary_081107_gr.pdf` (official Ktimatologio document, October 2008).

### Forward Transform: HTRS07 → EGSA87

#### Step 1: 7-Parameter Helmert (Cartesian)

Convert HTRS07 (X,Y,Z) to approximate EGSA87 (X',Y',Z'):

```
X'     X       tx     δs    εz   -εy    X
Y'  =  Y   +   ty  + -εz    δs    εx  * Y
Z'     Z       tz     εy   -εx    δs    Z
```

**Official parameters (THESE ARE NOT EPSG PARAMS — DO NOT SUBSTITUTE):**

| Parameter | Value |
|-----------|-------|
| tx | 203.437 m |
| ty | -73.461 m |
| tz | -243.594 m |
| εx | -0.170 arcsec |
| εy | -0.060 arcsec |
| εz | -0.151 arcsec |
| δs | -0.294 ppm |

**WARNING:** These Helmert parameters use a specific sign convention (equation 1 in the PDF). The rotations and scale are applied as a matrix multiplication with the source coordinates, NOT as EPSG's Position Vector or Coordinate Frame convention. Implement exactly as shown. The sign convention matches what PROJ calls "Position Vector" (EPSG:9606) but confirm by checking the numerical example in the PDF.

Note: To convert to towgs84-style parameters (for the INVERSE direction, GGRS87→WGS84), you would negate and approximate: towgs84 ≈ (-203.437, 73.461, 243.594, 0.170, 0.060, 0.151, 0.294). But DO NOT use towgs84 — implement the forward Helmert directly.

#### Step 2: Cartesian → Geographic

Convert (X',Y',Z') on GRS80 ellipsoid to (φ',λ',h') using iterative method.

GRS80: a = 6378137.0 m, 1/f = 298.257222101

#### Step 3: Geographic → TM87 Projection

Project (φ',λ') to (E',N') using Transverse Mercator:

| Parameter | TM87 Value |
|-----------|------------|
| Central meridian λ₀ | 24° |
| Scale factor k₀ | 0.9996 |
| Latitude of origin φ₀ | 0° |
| False Easting | 500,000 m |
| False Northing | 0 m |
| Ellipsoid | GRS80 |

#### Step 4: Compute Grid Lookup Coordinates (TM07)

**Separately** project the **original HTRS07** geographic coordinates (φ,λ) to TM07:

| Parameter | TM07 Value |
|-----------|------------|
| Central meridian λ₀ | 24° |
| Scale factor k₀ | 0.9996 |
| Latitude of origin φ₀ | 0° |
| False Easting | 500,000 m |
| **False Northing** | **-2,000,000 m** |
| Ellipsoid | GRS80 |

**CRITICAL:** TM07 ≠ TM87. They share all parameters EXCEPT False Northing. TM07_N = TM87_N - 2,000,000. The grid is indexed by TM07 coordinates, NOT by EGSA87 coordinates.

To get the HTRS07 geographic coordinates for this step: convert the original (X,Y,Z)_HTRS07 to (φ,λ,h) on GRS80 — this is the INPUT, before the Helmert.

#### Step 5: Bilinear Grid Interpolation

Using TM07 (E,N) from Step 4, look up corrections in the grid files:

**Grid file structure** (both dE and dN):
```
Line 0: 408          ← number of rows (Northing direction, S→N)  
Line 1: 422          ← number of columns (Easting direction, W→E)
Line 2: 2000.00      ← cell size in metres
Line 3: 1845619.000  ← TM07 Northing of SW corner node
Line 4: 41600.000    ← TM07 Easting of SW corner node
Line 5+: data values in cm, row-major, starting from SW corner
         each row = 422 values (W→E), rows progress S→N
```

**Grid bounds in TM07:**
- Easting: 41,600 to 883,600 m
- Northing: 1,845,619 to 2,659,619 m

**Index computation:**
```
col = (TM07_E - 41600) / 2000      ← fractional column (Easting)
row = (TM07_N - 1845619) / 2000    ← fractional row (Northing)
```

**Bilinear interpolation** from the 4 surrounding nodes:
```
c0 = floor(col), r0 = floor(row)
dc = col - c0, dr = row - r0
val = grid[r0][c0]*(1-dc)*(1-dr) + grid[r0][c0+1]*dc*(1-dr)
    + grid[r0+1][c0]*(1-dc)*dr   + grid[r0+1][c0+1]*dc*dr
```

Result: dE (cm) and dN (cm).

#### Step 6: Apply Corrections

```
E_final = E' + dE/100    (E' from Step 3, dE in cm → m)
N_final = N' + dN/100
```

(E_final, N_final) are the EGSA87/GGRS87 coordinates in EPSG:2100.

### Kastellorizo Exception

Kastellorizo uses different TM07 parameters: λ₀=30°, k₀=1.0, and separate grid files. Not needed for MVP but must be handled eventually.

---

## Grid Files

Located in `assets/`:
- `dE_2km_V1-0.grd` — Easting corrections (cm)
- `dN_2km_V1-0.grd` — Northing corrections (cm)

Source: https://cdn.ktimatologio.hast.gr/HEPOS_transformations_200219_484787931f.zip

These should be bundled as Android assets. Total size ~2.1 MB uncompressed. Consider compressing and decompressing on first run.

**Licensing note:** These grids are published by Ktimatologio for free use by equipment manufacturers and surveyors. No explicit license is stated. The PDF says manufacturers "can incorporate the transformation model into systems sold in the Greek market."

---

## KNOWN VALIDATION ISSUE

Our Python reference implementation gives ~0.74m residual at a known trig point near Kalamata. Possible causes:
1. The trig point coordinates loaded into the stakeout may have been from a different source (colleague's system, not official HEPOS)
2. Need to validate against the Windows HEPOS_TT tool (included in `reference/` zip) by running the exact same lat/lon through both
3. The model accuracy in rural Peloponnese may genuinely be lower than the ~2-3cm average

**First development task:** Run the HEPOS_TT Windows tool (via Wine if needed) with test coordinates and compare output with our implementation to determine if the code is correct.

---

## PROJECT STRUCTURE

```
opentopo/
├── docs/HEPOS_SPECIFICATION.md         ← THIS FILE
├── app/                               ← Android application
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   ├── dE_2km_V1-0.grd
│       │   │   └── dN_2km_V1-0.grd
│       │   ├── java/org/opentopo/app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ui/                ← Jetpack Compose screens
│       │   │   │   ├── MapScreen.kt
│       │   │   │   ├── SkyplotScreen.kt
│       │   │   │   ├── SurveyScreen.kt
│       │   │   │   ├── StakeoutScreen.kt
│       │   │   │   ├── SettingsScreen.kt
│       │   │   │   └── NtripScreen.kt
│       │   │   ├── gnss/             ← Hardware connection
│       │   │   │   ├── BluetoothGnssService.kt
│       │   │   │   ├── UsbGnssService.kt
│       │   │   │   ├── NmeaParser.kt
│       │   │   │   └── GnssState.kt
│       │   │   ├── ntrip/            ← NTRIP client
│       │   │   │   ├── NtripClient.kt
│       │   │   │   └── NtripConfig.kt
│       │   │   ├── survey/           ← Data collection
│       │   │   │   ├── SurveyManager.kt
│       │   │   │   └── Stakeout.kt
│       │   │   ├── export/           ← File export
│       │   │   │   ├── CsvExporter.kt
│       │   │   │   ├── GeoJsonExporter.kt
│       │   │   │   └── DxfExporter.kt
│       │   │   └── db/               ← Room database
│       │   │       ├── AppDatabase.kt
│       │   │       ├── ProjectDao.kt
│       │   │       ├── PointDao.kt
│       │   │       └── Entities.kt
│       │   └── res/
│       └── test/                      ← Unit tests (run on JVM)
├── lib-transform/                     ← Pure Kotlin, no Android deps
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/org/opentopo/transform/
│       │   ├── Ellipsoid.kt           ← GRS80 constants, XYZ↔geographic
│       │   ├── Helmert.kt             ← 7-parameter similarity transform
│       │   ├── TransverseMercator.kt  ← Forward/inverse TM projection
│       │   ├── CorrectionGrid.kt      ← Grid file parser + bilinear interp
│       │   ├── HeposTransform.kt      ← Full pipeline orchestrator
│       │   └── Coordinates.kt         ← Data classes (WGS84, GGRS87, etc.)
│       └── test/kotlin/org/opentopo/transform/
│           ├── EllipsoidTest.kt
│           ├── HelmertTest.kt
│           ├── TransverseMercatorTest.kt
│           ├── CorrectionGridTest.kt
│           └── HeposTransformTest.kt  ← Integration tests vs HEPOS_TT output
├── reference/                          ← Not shipped, dev reference only
│   ├── HEPOS_coord_transf_model_summary_081107_gr.pdf
│   ├── hepos_transform_reference.py   ← Python reference implementation
│   └── test_vectors.csv               ← HEPOS_TT validated test points
├── build.gradle.kts                   ← Root build file
├── settings.gradle.kts
├── gradle.properties
├── LICENSE                            ← AGPL-3.0
└── README.md
```

---

## DEVELOPMENT ORDER (Milestones)

### M0: Project Scaffold (do this first)

1. Create Android project in Android Studio: Empty Compose Activity, package `org.opentopo.app`, min SDK 26, Kotlin DSL
2. Create `lib-transform` as a pure Kotlin/JVM module (File → New → New Module → Java/Kotlin Library)
3. Add dependencies (see below)
4. Verify: `./gradlew :lib-transform:test` passes, `./gradlew assembleDebug` produces APK
5. Initialize git, push to GitHub

### M1: Transformation Engine (lib-transform)

This is the core value. Build and test WITHOUT any Android code.

**Implementation order within lib-transform:**

1. `Ellipsoid.kt` — GRS80 constants, XYZ↔geographic conversion (test against known values)
2. `TransverseMercator.kt` — Forward & inverse TM projection (test: PROJ output for known lat/lon)
3. `Helmert.kt` — 7-param similarity transform using HEPOS equation (test: PDF numerical example)
4. `CorrectionGrid.kt` — Parse .grd files, bilinear interpolation (test: known grid node values)
5. `HeposTransform.kt` — Full pipeline (test: compare with HEPOS_TT Windows tool output)

**Test vectors:** Generate by running 20+ points through the HEPOS_TT Windows tool. Include points across Greece: Athens, Thessaloniki, Kalamata, Crete, Corfu, Rhodes, islands. Store in `reference/test_vectors.csv` with columns: `lat,lon,h,expected_E,expected_N`.

### M2: GNSS Connection

1. `NmeaParser.kt` — Parse GGA, RMC, GSA, GSV, GST sentences. Pure Kotlin, no Android deps. Test with captured NMEA strings.
2. `BluetoothGnssService.kt` — Android Bluetooth Classic SPP connection. Uses `BluetoothSocket` with `InputStream` feeding into `NmeaParser`.
3. `GnssState.kt` — StateFlow holding current position, accuracy, fix type, satellite info.
4. Basic UI: show lat/lon, accuracy, fix type, satellite count on screen.

### M3: NTRIP Client

1. `NtripClient.kt` — HTTP-based NTRIP v1/v2 client. Sends GGA, receives RTCM3. Forwards RTCM bytes to the GNSS receiver via Bluetooth write.
2. `NtripConfig.kt` — Caster URL, port, mountpoint, credentials. Preset for HEPOS.
3. UI: NTRIP connection status, data rate, age of correction.

### M4: Survey Core

1. Room database: Projects, Points, Layers tables
2. Record point: store both WGS84 and GGRS87 coords, accuracy, fix, timestamp
3. MapLibre map display with recorded points
4. Stakeout: target point, live ΔE/ΔN/distance/bearing

### M5: Export

1. CSV with both coordinate systems
2. GeoJSON (EPSG:2100)
3. Share via Android intent

### M6: Field Validation & Release

1. Visit 5+ trig points, compare with known EGSA87 coordinates
2. Bug fixes
3. GitHub Release with signed APK
4. F-Droid metadata submission

---

## KEY DEPENDENCIES

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // for Room
}

android {
    namespace = "org.opentopo.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.opentopo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":lib-transform"))
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    
    // MapLibre
    implementation("org.maplibre.gl:android-sdk:11.8.4")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // USB Serial
    implementation("com.github.mik3y:usb-serial-for-android:3.8.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
```

```kotlin
// lib-transform/build.gradle.kts
plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
```

---

## ANDROID PERMISSIONS NEEDED

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-feature android:name="android.hardware.bluetooth" android:required="false" />
<uses-feature android:name="android.hardware.usb.host" android:required="false" />
```

---

## CODING CONVENTIONS

- Pure Kotlin throughout (no Java)
- Coroutines for async (no RxJava)
- StateFlow for reactive state
- Compose for all UI (no XML layouts)
- No proprietary dependencies
- All coordinates internally stored as WGS84 lat/lon/h; GGRS87 computed on demand
- Double precision for all coordinate math
- Grid files loaded lazily on first transform call, then cached in memory

---

## REFERENCE PYTHON IMPLEMENTATION

See `reference/hepos_transform_reference.py` for a complete working Python implementation of the transformation pipeline. Use this to generate test vectors and validate the Kotlin implementation.
