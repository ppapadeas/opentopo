# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Internal GPS as third connection option (Android LocationManager NMEA)
- Satellite skyplot polar chart with constellation colors
- Transform panel with interactive coordinate converter and pipeline inspector
- Haptic and audio feedback on point recorded
- NTRIP disconnect alert vibration
- Persistent fix status pill on map
- Contour lines overlay (vathra.xyz)

### Fixed
- Internal GPS disconnect button and connect state visibility
- Contour lines minzoom, filter compatibility, line thickness
- Coordinate format setting wiring
- About screen version and repository link

## [1.0.0] - 2025-05-15

### Added
- Google Play Store release preparation
- Play Store screenshots and promotional assets
- Material 3 Expressive UI overhaul (MaterialExpressiveTheme, MotionScheme.expressive)
- ContainedLoadingIndicator, FloatingActionButtonMenu, ToggleFloatingActionButton
- Filled text fields, ListItem composables, MaterialShapes

### Changed
- Upgraded to material3 1.5.0-alpha17 for M3 Expressive APIs
- Surfaces, fields, and shapes updated to M3 Expressive guidelines

## [0.3.0] - 2025-05-14

### Added
- Settings tab with recording, connection, and display options
- Photo attachments on survey points
- CSV import for external point data
- Release signing configuration
- Settings persistence via DataStore

### Changed
- M3 polish pass across all panels
- Wired all settings to actual app behavior

### Fixed
- Release workflow to use debug APK when no keystore is available on CI

## [0.2.0] - 2025-05-13

### Added
- Survey points displayed on map with fix-quality colors and labels
- Auto-reconnect for Bluetooth, USB, and NTRIP on startup
- Point edit and delete with confirmation dialogs
- Contour lines layer on map
- Settings screen (averaging duration, accuracy thresholds)

## [0.1.0] - 2025-05-12

### Added
- HEPOS transformation engine (`lib-transform`) with full 6-step pipeline: geographic-to-Cartesian, 7-parameter Helmert, Cartesian-to-geographic, Transverse Mercator, grid interpolation, correction application
- 21 unit tests for transformation engine validated against Python reference implementation across 10 Greek cities
- NMEA 0183 parser supporting GGA, RMC, GSA, GSV, GST sentences with multi-constellation support (GPS, GLONASS, Galileo, BeiDou)
- 12 unit tests for NMEA parser with streaming byte feed support
- Bluetooth Classic (SPP) GNSS receiver connection
- USB-OTG serial GNSS receiver connection via usb-serial-for-android with SerialInputOutputManager
- NTRIP client rewritten with raw TCP sockets, NTRIP v1 (ICY 200 OK) support
- GGA forwarding for VRS with synthetic GGA generation
- Preset NTRIP casters: HEPOS (Ktimatologio), CivilPOS, Hexagon SmartNet Greece, plus custom server option
- Room database for survey projects and points
- Point recording with configurable epoch averaging (1-60 s) and quality filtering
- Stakeout navigation with live distance, bearing, and delta E/N computation
- Map-centric UI with full-screen MapLibre map and bottom sheet tool panels
- Vathra.xyz Protomaps vector tiles with Greek labels
- Ktimatologio orthophoto WMS overlay
- User location marker on map
- Export to CSV (both WGS84 and EGSA87), GeoJSON (EPSG:2100), and DXF (AutoCAD R14)
- Android share intent for file export
- Connection settings persistence via DataStore
- Material 3 Expressive UI with 6 scrollable tabs
- GitHub Actions CI/CD pipeline (test + lint + build)
- Release workflow (tag v* triggers build + GitHub Release with APK)
- F-Droid metadata
- Project documentation

### Changed
- Upgraded toolchain to AGP 9.1.0, Gradle 9.4.1, Kotlin 2.3.0, KSP 2.3.6, Room 2.8.4
- Migrated to AGP 9 built-in Kotlin support, removed deprecated plugin
- Replaced demo tiles with vathra.xyz Protomaps basemap

### Fixed
- NTRIP VRS GGA forwarding (send immediately, handle ICY 200 OK, synthetic GGA)
- USB serial data corruption by switching to SerialInputOutputManager
- USB PendingIntent crash on Android 14+
- USB permission race condition on connection

[Unreleased]: https://github.com/ppapadeas/opentopo/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/ppapadeas/opentopo/compare/v0.3.0...v1.0.0
[0.3.0]: https://github.com/ppapadeas/opentopo/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/ppapadeas/opentopo/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/ppapadeas/opentopo/releases/tag/v0.1.0
