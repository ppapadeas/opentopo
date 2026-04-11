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
│  │ Export       │ │ GnssState     │ │                     │  │
│  │ Transform    │ │               │ │                     │  │
│  │ Settings     │ │               │ │                     │  │
│  │ Skyplot      │ │               │ │                     │  │
│  │ theme/       │ │               │ │                     │  │
│  └──────────────┘ └───────────────┘ └─────────────────────┘  │
│  ┌──────────────┐ ┌───────────────┐ ┌─────────────────────┐  │
│  │ survey/      │ │ export/       │ │ db/                 │  │
│  │ Manager      │ │ CsvExporter   │ │ Room (SQLite)       │  │
│  │ Stakeout     │ │ CsvImporter   │ │ Projects, Points    │  │
│  │              │ │ GeoJSON       │ │                     │  │
│  │              │ │ DXF           │ │                     │  │
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
                          Room Database ──► Export (CSV/GeoJSON/DXF)

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
- material3 1.5.0-alpha17 providing `ContainedLoadingIndicator`, `FloatingActionButtonMenu`, `ToggleFloatingActionButton`
- `graphics-shapes` for `MaterialShapes` / `RoundedPolygon` clip paths

### Layout
- **Full-screen MapLibre map** -- always visible, shows live position (fix-quality colored dot) and recorded survey points (colored by fix quality with labels)
- **Map layers** -- Street Map (vathra.xyz Protomaps), Ktimatologio orthophoto WMS, contour lines; switched via layer toggle
- **Fix status pill** (top) -- persistent display of fix type, satellite count, accuracy
- **FAB menu** (bottom-right) -- quick point recording with haptic/audio feedback
- **Bottom sheet** -- `BottomSheetScaffold` with 6 scrollable tabs:

| Tab | Panel | Purpose |
|-----|-------|---------|
| GNSS | `ConnectionPanel` | BT/USB/Internal GPS connection, satellite skyplot |
| Survey | `SurveyPanel` | Project management, point list, edit/delete, photos |
| Stake | `StakeoutPanel` | Target navigation with compass, distance, deltas |
| Export | `ExportPanel` | CSV/GeoJSON/DXF export, CSV import, share |
| Transform | `TransformPanel` | Coordinate converter (WGS84 to EGSA87), pipeline inspector |
| Config | `SettingsPanel` | Averaging, accuracy, baud rate, GGA interval, format |

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
