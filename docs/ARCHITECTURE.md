# Architecture

## Module Overview

```
┌──────────────────────────────────────────────────────────────┐
│  app (Android)                                                │
│  ┌──────────────┐ ┌───────────────┐ ┌─────────────────────┐  │
│  │ ui/          │ │ gnss/         │ │ ntrip/              │  │
│  │ MainMap      │ │ BT Service    │ │ NtripClient (TCP)   │  │
│  │ Connection   │ │ USB Service   │ │ NtripConfig         │  │
│  │ Survey       │ │ Internal GPS  │ │                     │  │
│  │ Stakeout     │ │ NmeaParser    │ │                     │  │
│  │ StakeoutImm. │ │ GnssState     │ │                     │  │
│  │ ToolsPanel   │ │               │ │                     │  │
│  │ Skyplot      │ │               │ │                     │  │
│  │ components/  │ │               │ │                     │  │
│  │ theme/       │ │               │ │                     │  │
│  └──────────────┘ └───────────────┘ └─────────────────────┘  │
│  ┌──────────────┐ ┌───────────────┐ ┌─────────────────────┐  │
│  │ survey/      │ │ export/       │ │ db/                 │  │
│  │ Manager      │ │ CsvExporter   │ │ Room (SQLite v5)    │  │
│  │ Stakeout     │ │ CsvImporter   │ │ Projects, Points    │  │
│  │ Lines/Polys  │ │ GeoJSON, DXF  │ │ Lines, Polygons     │  │
│  │              │ │ Shapefile     │ │                     │  │
│  │              │ │               │ │                     │  │
│  └──────────────┘ └───────────────┘ └─────────────────────┘  │
│  ┌──────────────┐                                             │
│  │ prefs/       │                                             │
│  │ UserPrefs    │                                             │
│  │ (DataStore)  │                                             │
│  └──────────────┘                                             │
│                        │                                      │
│                        ▼                                      │
│  ┌───────────────────────────────────────────────────────┐   │
│  │ lib-transform (Pure Kotlin/JVM)                        │   │
│  │ Coordinates → Ellipsoid → Helmert → TM → Grid →       │   │
│  │ HeposTransform                                         │   │
│  └───────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

## Data Flow

```
GNSS Receiver (Bluetooth / USB / Internal GPS)
    │ NMEA sentences
    ▼
NmeaParser ──► GnssState (StateFlows)
                   │            │            │
                   ▼            ▼            ▼
              UI updates   SurveyManager   Skyplot
                               │ records point
                               ▼
                          HeposTransform ──► EGSA87 (E, N)
                               │
                               ▼
                          Room Database ──► Export (CSV/GeoJSON/DXF/SHP)

GnssState ──► Synthetic GGA ──► NtripClient (GGA forwarding for VRS)

NTRIP Caster
    │ RTCM3 corrections (raw TCP socket)
    ▼
NtripClient ──► BluetoothGnssService.write()
            ──► UsbGnssService.write()
    (whichever transport is connected accepts the data)

UserPreferences (DataStore)
    │ settings flows
    ▼
    SurveyManager (averaging, accuracy, RTK filter)
    NtripClient (GGA interval, auto-reconnect)
    ConnectionPanel (baud rate, saved servers)
    MainMapScreen (coordinate format)
```

## UI Architecture

The app uses Material 3 Expressive with a map-centric single-screen design.

### Theme
- `MaterialExpressiveTheme` with `MotionScheme.expressive()` for animated transitions
- material3 1.5.0-alpha17 providing `ContainedLoadingIndicator`, `FloatingActionButtonMenu`, `ToggleFloatingActionButton`, `ShortNavigationBar`, `ButtonGroup`, `ToggleButton`
- `graphics-shapes` for `MaterialShapes` / `RoundedPolygon` clip paths
- Emphasized typography variants (all 15 `*Emphasized` text styles for active/selected states)
- Shape system: 4/8/12/16/28dp (M3E spec-aligned)
- Full surface container hierarchy in both light and dark modes
- AMOLED dark mode option (pure black surfaces for field battery conservation)

### Layout
- **Full-screen MapLibre map** -- always visible, shows live position (fix-quality colored dot) and recorded survey points (colored by fix quality with labels)
- **Map layers** -- Street Map (vathra.xyz Protomaps), Ktimatologio orthophoto WMS, contour lines; switched via layer toggle
- **Fix status pill** (top) -- persistent display of fix type, satellite count, accuracy
- **FAB menu** (bottom-right) -- quick point recording with haptic/audio feedback
- **Bottom sheet** -- `BottomSheetScaffold` with M3E `ShortNavigationBar` (4 tabs, pill-shaped active indicator):

| Tab | Panel | Purpose |
|-----|-------|---------|
| GNSS | `ConnectionPanel` | BT/USB/Internal GPS connection, satellite skyplot |
| Survey | `SurveyPanel` | Project management, point list, edit/delete, photos |
| Stake | `StakeoutPanel` | Target navigation with compass, distance, deltas; immersive full-screen overlay |
| Export | `ExportPanel` | CSV/GeoJSON/DXF/Shapefile export, CSV import, share |
| Transform | `TransformPanel` | Coordinate converter (WGS84 to EGSA87), pipeline inspector |
| Config | `SettingsPanel` | Averaging, accuracy, baud rate, GGA interval, format |

### Stakeout Immersive Overlay
`StakeoutImmersiveOverlay` provides a full-screen dark HUD for heads-up stakeout navigation. Uses `inverseSurface` background, `displayLarge` distance text, a 240 dp compass, delta E/N readout, and a fix status pill. Entered via "Full Screen" button from the StakeoutPanel.

### Animation System
- **Pulse ring** on the record FAB: expanding/fading ring (`Animatable` alpha + scale) during epoch averaging
- **MotionScheme tokens** for panel transitions (`MaterialTheme.motionScheme.defaultEffectsSpec()`)
- **ButtonGroup shape morphing** — connected buttons morph on press via M3E `ToggleButtonShapes`
- Fix status pill pulsing dot animation

### Satellite Skyplot
`Skyplot.kt` renders a polar chart of tracked satellites with constellation-based colors (GPS, GLONASS, Galileo, BeiDou), elevation rings, and azimuth grid. Drawn on a Compose `Canvas`.

## Key Design Decisions

1. **Transformation library is Android-independent.** `lib-transform` has zero Android dependencies, making it testable on JVM and reusable in server/desktop applications.

2. **InputStream-based grid loading.** Since `lib-transform` cannot access Android `AssetManager`, the app opens assets and passes `InputStream` to the library. This maintains the clean module boundary.

3. **Triple transport for GNSS.** Three independent services (Bluetooth, USB, Internal GPS) all feed the same `NmeaParser` and produce a unified `GnssState`. Only one can be active at a time.

4. **Raw TCP for NTRIP.** The NTRIP client uses raw TCP sockets instead of `HttpURLConnection` to handle NTRIP v1 `ICY 200 OK` responses that standard HTTP clients reject. Synthetic GGA sentences are generated from `GnssState` and forwarded on a configurable interval for VRS support.

5. **Dual transport for RTCM.** NTRIP corrections are written to both Bluetooth and USB services. Whichever is connected accepts the data; the disconnected one silently ignores writes.

6. **Coordinates stored as WGS84.** Points in the database store WGS84 lat/lon as the canonical representation. EGSA87 E/N is computed at recording time and stored alongside for convenience, but could be recomputed.

7. **Epoch averaging.** Points are recorded by averaging N consecutive 1 Hz GNSS positions, with configurable quality filters (minimum accuracy, RTK-only mode).

8. **DataStore for preferences.** `UserPreferences` wraps Jetpack DataStore to persist connection settings (saved servers, baud rate), recording parameters (averaging time, accuracy thresholds), and display options (coordinate format). Settings are exposed as `StateFlow` for reactive UI updates.

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.3.0 |
| UI | Jetpack Compose + Material 3 Expressive | BOM 2026.03, M3 1.5.0-alpha17 |
| Map | MapLibre GL Native | 11.8.4 |
| Database | Room (SQLite) | 2.8.4 |
| USB Serial | usb-serial-for-android | 3.10.0 |
| Async | Kotlin Coroutines | 1.8.1 |
| Preferences | Jetpack DataStore | 1.1.1 |
| Annotation Processing | KSP | 2.3.6 |
| Build | Gradle (Kotlin DSL) | 9.4.1 |
| Android | compileSdk 36, targetSdk 35, minSdk 26 | AGP 9.1.0 |
| CI/CD | GitHub Actions | -- |

## CI/CD Pipeline

```
Push to main / PR                     Push v* tag
    │                                      │
    ▼                                      ▼
  ci.yml                              release.yml
    │                                      │
    ├─ lib-transform:test                  ├─ lib-transform:test
    ├─ app:testDebugUnitTest               ├─ app:testDebugUnitTest
    ├─ lintDebug                           ├─ Decode keystore (from secret)
    ├─ assembleDebug                       ├─ assembleRelease (signed APK)
    └─ Upload artifacts                    ├─ bundleRelease (signed AAB)
                                           ├─ Upload AAB → Google Play
                                           └─ Create GitHub Release (APK + AAB)
```

Signing credentials and Google Play service account key are stored as GitHub Secrets. The release workflow decodes the keystore from base64 and writes `local.properties` at build time.
