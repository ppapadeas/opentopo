# Architecture

## Module Overview

```
┌──────────────────────────────────────────────────────────────────┐
│  app (Android)                                                    │
│  ┌──────────────────┐ ┌──────────────┐ ┌──────────────────────┐  │
│  │ ui/              │ │ gnss/        │ │ ntrip/               │  │
│  │ MainMap          │ │ BT Service   │ │ NtripClient (TCP)    │  │
│  │ Connection       │ │ USB Service  │ │ NtripConfig          │  │
│  │ Survey / Trig    │ │ Internal GPS │ │ NtripProfile (entity)│  │
│  │ Stakeout HUD     │ │ NmeaParser   │ │ NtripProfileRepo     │  │
│  │ NtripProfiles    │ │ GnssState    │ │ NtripConnectionState │  │
│  │ NtripProfileEdit │ │              │ │ NtripBadgePalette    │  │
│  │ Skyplot          │ │              │ │                      │  │
│  │ components/      │ │              │ │                      │  │
│  │ theme/           │ │              │ │                      │  │
│  └──────────────────┘ └──────────────┘ └──────────────────────┘  │
│  ┌──────────────┐ ┌───────────────┐ ┌─────────────────────────┐  │
│  │ survey/      │ │ export/       │ │ db/                     │  │
│  │ Manager      │ │ CsvExporter   │ │ Room SQLite v8          │  │
│  │ Stakeout     │ │ CsvImporter   │ │ projects, points,       │  │
│  │ Lines/Polys  │ │ GeoJSON, DXF  │ │ trig_points_cache,      │  │
│  │ TrigPointSvc │ │ Shapefile     │ │ ntrip_profile           │  │
│  └──────────────┘ └───────────────┘ └─────────────────────────┘  │
│  ┌──────────────┐                                                 │
│  │ prefs/       │                                                 │
│  │ UserPrefs    │                                                 │
│  │ (DataStore)  │                                                 │
│  └──────────────┘                                                 │
│                        │                                          │
│                        ▼                                          │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │ lib-transform (Pure Kotlin/JVM)                            │   │
│  │ Coordinates → Ellipsoid → Helmert → TM → Grid →           │   │
│  │ HeposTransform (+ geoid undulation)                        │   │
│  └───────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
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

NtripProfileRepository (Room-backed, Flow API)
    │ observes activeProfile
    ├─► NtripClient.connect(profile.toConfig())   (auto on activate)
    │
    │ derives NtripConnectionState
    │ (Empty/Disconnected/Connecting/Live/Stale/Error)
    └─► UI — NtripActiveProfileRow on Connect,
             NtripProfilesScreen hero card,
             NtripProfileSwitchSheet rows

UserPreferences (DataStore)
    │ settings flows
    ▼
    SurveyManager (averaging, accuracy, RTK filter)
    NtripClient (GGA interval)
    ConnectionPanel (baud rate)
    MainMapScreen (coordinate format, geoid source)
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
- **Map layers** -- Street Map (vathra.xyz Protomaps via Cloudflare Worker + R2 PMTiles), Ktimatologio orthophoto WMS, contour lines; switched via layer toggle. Style is derived from `@protomaps/basemaps` LIGHT with the `water_label_ocean` / `water_label_lakes` layers removed — their nested `is-supported-script` expressions break MapLibre Android 11.8.4 rendering
- **Initial camera** -- last known GPS location at zoom 15 (via `LocationManager.getLastKnownLocation`), falling back to Greece overview (38.5, 23.8, zoom 7) when no location is available
- **Fix status pill** (top) -- persistent display of fix type, satellite count, accuracy
- **FAB menu** (bottom-right) -- quick point recording with haptic/audio feedback
- **Bottom sheet** -- `BottomSheetScaffold` with a custom inline `ShortNavigationBar` rendered *inside* the peek content (4 tabs, pill-shaped active indicator). The peek always shows the project header + `CoordinateBlock` + `SplitButton` + tabs; tapping a tab expands the sheet to the matching panel:

| Tab | Panel | Purpose |
|-----|-------|---------|
| GNSS | `ConnectionPanel` | BT/USB/Internal transport picker, receiver hero card, constellations, centered skyplot, NTRIP active-profile row |
| Survey | `SurveyPanel` | Point/Line/Polygon mode, fix pill, CoordinateCard, epoch averaging gate, record row |
| Stake | `StakeoutPanel` / `StakeoutImmersiveOverlay` | Target navigation; immersive HUD when activated |
| More | `ToolsPanel` / `ExportPanel` / `TransformPanel` / `SettingsPanel` | Settings, tools, transform inspector, export/import |

The hamburger on the map opens an overflow menu routing to `SheetMode.CONNECTION / TRIG / TOOLS / EXPORT` plus the per-layer toggles (`Layer: Ortho`, `Layer: Contours`, `Show Trig Points (GYS)`).

### Full-screen overlays
Rendered at the root of `MainMapScreen`, above the bottom sheet, with `WindowInsets.systemBars` safe zones applied:
- **`StakeoutImmersiveOverlay`** — `#06332A` HUD, 240 dp dashed-mint compass, 54 sp distance, 3 delta cards
- **`NtripProfileSwitchSheet`** — `ModalBottomSheet` for one-tap activate
- **`NtripProfilesScreen`** — full-screen manager (hero card + saved list + dashed New tile)
- **`NtripProfileEditScreen`** — form with inline sourcetable scan

### NTRIP profile flow
```
User taps NtripActiveProfileRow on Connect
       │
       ▼
ntripSwitchSheetOpen = true ──► NtripProfileSwitchSheet renders
       │                             │
       │                             ├─ tap non-active row ──► repo.setActive(id)
       │                             │                             │
       │                             │                             └─► client reconnects
       │                             │
       │                             └─ tap "Manage profiles…" ──► ntripProfilesOpen = true
       │                                                                │
       ▼                                                                ▼
   dismiss                                                 NtripProfilesScreen renders
                                                                        │
                                               tap edit pencil ─────────┤
                                               tap + (new) ─────────────┤
                                                                        ▼
                                                           NtripProfileEditScreen renders
                                                                        │
                                                  onSave ──► repo.upsert(profile)
                                                  onCancel ──► overlay closes
```

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

8. **DataStore for preferences.** `UserPreferences` wraps Jetpack DataStore to persist connection settings (saved servers, baud rate), recording parameters (averaging time, accuracy thresholds), and display options (coordinate format, geoid source). Settings are exposed as `StateFlow` for reactive UI updates.

9. **NTRIP profiles in Room.** Whereas a single NTRIP config fit in DataStore, a fleet of saved profiles fits better in Room — indexed queries for "active row", atomic swap via a `@Transaction` DAO method (`clearActive` then `markActive`), and strong typing via `NtripProfile` / `RtcmVersion`. `NtripProfileRepository` owns the Flow-based API, auto-connects the transport whenever the active-profile id changes, and derives `NtripConnectionState` from the raw `NtripClient.state` (age-of-correction threshold: 10 s Live→Stale). First-run migration ingests the legacy single-config DataStore entry as the active row, then seeds HEPOS/CivilPOS/SmartNet templates.

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
