# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- HEPOS transformation engine (`lib-transform`) with full 6-step pipeline: geographic-to-Cartesian, 7-parameter Helmert, Cartesian-to-geographic, Transverse Mercator, grid interpolation, correction application
- 21 unit tests for transformation engine validated against Python reference implementation across 10 Greek cities
- NMEA 0183 parser supporting GGA, RMC, GSA, GSV, GST sentences with multi-constellation support (GPS, GLONASS, Galileo, BeiDou)
- 12 unit tests for NMEA parser with streaming byte feed support
- Bluetooth Classic (SPP) GNSS receiver connection
- USB-OTG serial GNSS receiver connection via usb-serial-for-android
- NTRIP v1/v2 client with GGA forwarding, sourcetable parsing, auto-reconnect with exponential backoff
- Preset NTRIP casters: HEPOS (Ktimatologio), CivilPOS, Hexagon SmartNet Greece
- Room database for survey projects and points
- Point recording with configurable epoch averaging (1-60s) and quality filtering
- Stakeout navigation with live distance, bearing, and delta E/N computation
- Map-centric UI with full-screen MapLibre map, bottom sheet tool panels, and status chips
- Export to CSV (both WGS84 and EGSA87), GeoJSON (EPSG:2100), and DXF (AutoCAD R14)
- Android share intent for file export
- GitHub Actions CI/CD pipeline
- F-Droid metadata

[Unreleased]: https://github.com/ppapadeas/opentopo/commits/main
