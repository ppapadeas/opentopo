# OpenTopo

**Open-source Android GNSS survey app with HEPOS/GGRS87 grid transformation for Greece.**

[![CI](https://github.com/ppapadeas/opentopo/actions/workflows/ci.yml/badge.svg)](https://github.com/ppapadeas/opentopo/actions/workflows/ci.yml)
[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)
[![Android API](https://img.shields.io/badge/API-26%2B-green.svg)](https://developer.android.com/about/versions/oreo)

---

## Why OpenTopo?

Greek surveyors and engineers using low-cost RTK GNSS receivers (ArduSimple, u-blox ZED-F9P, Emlid) on Android have no open-source app that correctly transforms WGS84/ETRS89 coordinates to GGRS87/EGSA87 (EPSG:2100). The transformation requires a 7-parameter Helmert datum shift **plus** a 2D correction grid published by Ktimatologio S.A. Without the grid, errors of 0.5-2.0m make results unusable for cadastral and engineering work.

OpenTopo solves this by implementing the full HEPOS transformation model and packaging it in a free, open-source survey data collector.

## Features

- **HEPOS Transformation Engine** - Full 6-step pipeline: geographic-to-Cartesian, 7-parameter Helmert, Cartesian-to-geographic, Transverse Mercator projection, grid interpolation, correction application. Sub-centimetre accuracy validated against 10 reference points across Greece.
- **Bluetooth & USB GNSS** - Connect to any NMEA-compatible receiver via Bluetooth Classic (SPP) or USB-OTG serial. Parses GGA, RMC, GSA, GSV, GST sentences.
- **NTRIP Client** - Built-in NTRIP v1/v2 client with presets for HEPOS, CivilPOS, and Hexagon SmartNet Greece. GGA forwarding for VRS. Auto-reconnect with exponential backoff.
- **Survey Data Collection** - Create projects, record points with configurable epoch averaging (1-60s), quality filters (minimum accuracy, RTK-only mode), auto-incrementing point IDs.
- **Stakeout Navigation** - Navigate to target EGSA87 coordinates with live distance, bearing, and delta E/N display.
- **Map-Centric UI** - Full-screen MapLibre map as the primary interface. Bottom sheet with tool panels for connection, survey, stakeout, and export.
- **Export** - CSV (both CRS), GeoJSON (EPSG:2100), DXF (AutoCAD compatible). Share via Android intent.

## Architecture

```
opentopo/
├── lib-transform/          Pure Kotlin/JVM library (no Android deps)
│   └── org.opentopo.transform
│       ├── Ellipsoid       GRS80 constants, XYZ <-> geographic
│       ├── Helmert         7-parameter similarity transform
│       ├── TransverseMercator  Forward TM projection
│       ├── CorrectionGrid  Grid parser + bilinear interpolation
│       └── HeposTransform  Full pipeline orchestrator
│
└── app/                    Android application
    └── org.opentopo.app
        ├── gnss/           NmeaParser, BluetoothGnssService, UsbGnssService, GnssState
        ├── ntrip/          NtripClient, NtripConfig
        ├── survey/         SurveyManager, Stakeout
        ├── export/         CsvExporter, GeoJsonExporter, DxfExporter
        ├── db/             Room database (Projects, Points)
        └── ui/             Jetpack Compose (MainMapScreen, panels)
```

The transformation engine (`lib-transform`) is a pure Kotlin/JVM library with zero Android dependencies. It can be unit-tested on JVM, reused server-side, or published as a standalone library for Greek coordinate transformations.

## Build

**Prerequisites:** Android Studio (includes JDK) or JDK 17+, Android SDK API 35.

```bash
git clone https://github.com/ppapadeas/opentopo.git
cd opentopo

# Run transformation tests (no device needed)
./gradlew :lib-transform:test

# Run all unit tests
./gradlew :lib-transform:test :app:testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Test Vectors

The transformation engine is validated against 10 points across Greece with sub-centimetre tolerance:

| Location | Latitude | Longitude | Expected E (EGSA87) | Expected N (EGSA87) |
|----------|----------|-----------|---------------------|---------------------|
| Athens | 37.9715 | 23.7267 | 475846.417 | 4202401.145 |
| Thessaloniki | 40.6401 | 22.9444 | 410590.254 | 4499055.448 |
| Kalamata | 37.0388 | 22.1143 | 332145.258 | 4100551.548 |
| Heraklion | 35.3387 | 25.1442 | 603830.795 | 3910917.558 |
| Corfu | 39.6243 | 19.9217 | 149769.169 | 4393724.733 |
| Rhodes | 36.4341 | 28.2176 | 877980.791 | 4040083.307 |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, coding conventions, and PR guidelines.

## License

OpenTopo is licensed under the [GNU Affero General Public License v3.0](LICENSE).

The HEPOS correction grids (`dE_2km_V1-0.grd`, `dN_2km_V1-0.grd`) are published by Ktimatologio S.A. for use by equipment manufacturers and surveyors.

## Privacy

OpenTopo does not collect, transmit, or store any personal data beyond what you explicitly save in survey projects on your device. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md).
