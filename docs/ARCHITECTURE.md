# Architecture

## Module Overview

```
┌─────────────────────────────────────────────────────┐
│  app (Android)                                       │
│  ┌───────────┐ ┌──────────┐ ┌────────────────────┐  │
│  │ ui/       │ │ gnss/    │ │ ntrip/             │  │
│  │ MainMap   │ │ BT Svc   │ │ NtripClient        │  │
│  │ Panels    │ │ USB Svc  │ │ NtripConfig        │  │
│  │ Theme     │ │ NMEA     │ │                    │  │
│  │           │ │ State    │ │                    │  │
│  └───────────┘ └──────────┘ └────────────────────┘  │
│  ┌───────────┐ ┌──────────┐ ┌────────────────────┐  │
│  │ survey/   │ │ export/  │ │ db/                │  │
│  │ Manager   │ │ CSV      │ │ Room (SQLite)      │  │
│  │ Stakeout  │ │ GeoJSON  │ │ Projects, Points   │  │
│  │           │ │ DXF      │ │                    │  │
│  └───────────┘ └──────────┘ └────────────────────┘  │
│                      │                               │
│                      ▼                               │
│  ┌──────────────────────────────────────────────┐   │
│  │ lib-transform (Pure Kotlin/JVM)               │   │
│  │ Ellipsoid → Helmert → TM → Grid → Pipeline   │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

## Data Flow

```
GNSS Receiver (Bluetooth/USB)
    │ NMEA sentences
    ▼
NmeaParser ──► GnssState (StateFlows)
                   │            │
                   ▼            ▼
              UI updates    SurveyManager
                               │ records point
                               ▼
                          HeposTransform ──► EGSA87 (E, N)
                               │
                               ▼
                          Room Database ──► Export (CSV/GeoJSON/DXF)

NTRIP Caster
    │ RTCM3 corrections
    ▼
NtripClient ──► BluetoothGnssService.write()
            ──► UsbGnssService.write()
```

## UI Architecture

The app uses a map-centric design with a single `MainMapScreen` composable:

- **Full-screen MapLibre map** — always visible, shows live position and recorded points
- **Status chips** (top-left) — GNSS fix type, satellite count, NTRIP status
- **FAB** (bottom-right) — quick point recording
- **Bottom sheet** — draggable panel containing tool buttons:
  - **Connection** — Bluetooth/USB GNSS + NTRIP config
  - **Survey** — project management, point recording
  - **Stakeout** — target navigation
  - **Export** — CSV, GeoJSON, DXF with share intent

## Key Design Decisions

1. **Transformation library is Android-independent.** `lib-transform` has zero Android dependencies, making it testable on JVM and reusable in server/desktop applications.

2. **InputStream-based grid loading.** Since `lib-transform` can't access Android `AssetManager`, the app opens assets and passes `InputStream` to the library. This maintains the clean module boundary.

3. **Dual transport for RTCM.** NTRIP corrections are written to both Bluetooth and USB services. Whichever is connected accepts the data; the disconnected one silently ignores writes.

4. **Coordinates stored as WGS84.** Points in the database store WGS84 lat/lon as the canonical representation. EGSA87 E/N is computed at recording time and stored alongside for convenience, but could be recomputed.

5. **Epoch averaging.** Points are recorded by averaging N consecutive 1Hz GNSS positions, with configurable quality filters (minimum accuracy, RTK-only mode).

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.0.20 |
| UI | Jetpack Compose + Material3 | BOM 2024.12 |
| Map | MapLibre GL Native | 11.8.4 |
| Database | Room (SQLite) | 2.6.1 |
| USB Serial | usb-serial-for-android | 3.8.1 |
| Async | Kotlin Coroutines | 1.8.1 |
| Build | Gradle (Kotlin DSL) | 8.9 |
| Android | minSdk 26, targetSdk 35 | AGP 8.7.3 |
