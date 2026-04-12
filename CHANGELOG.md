# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.0] - 2026-04-12

### Added
- **Line recording** — tap vertices sequentially, stored with featureId grouping
- **Polygon recording** — close polygon from vertices, compute area via Shoelace formula
- **Mode switcher** — 3-way segmented control: Point / Line / Area in Survey panel
- Start/Add Vertex/Finish workflow for line and polygon features
- Haversine distance computation for lines
- Area computation in square meters using EGSA87 projected coordinates
- LineLayer and FillLayer rendering on MapLibre map (teal color)
- New DAO queries for feature-based vertex management
- **OpenTopo brand logo** — app icon at all mipmap densities, Play Store icon, README header

### Changed
- Quick Mark now uses dedicated `quickMark()` method (single epoch, no averaging)
- Record FAB countdown shows remaining seconds (inverse counting)
- Database migration v3→v4: added layerType and featureId columns

### Fixed
- **VIBRATE permission** missing from manifest (haptic feedback crash)
- Vibration calls wrapped in try-catch for safety

## [1.3.0] - 2026-04-12

### Added
- **M3 Expressive teal-green theme** — seed #006B5E, light-first branded palette
- **Component library** (ui/components/): TonalCard, SectionLabel, FixStatusPill (pulsing dot + semantic container colors), ConstellationChip (GPS/GLO/GAL/BDS categorical colors), CoordinateBlock, InputRow
- FixStatusPill with animated pulsing dot for fix state indication
- ConstellationChip with purple Galileo, orange BeiDou colors
- Skyplot legend row showing constellation counts

### Changed
- Compact data-dense typography scale (body 13sp, labels 11sp)
- Shapes tightened: XS 4dp, S 8dp, M 12dp, L 16dp, XL 28dp
- Dynamic color disabled by default — branded palette takes priority
- ConnectionPanel restyled with TonalCard and SectionLabel
- StatusBar uses FixStatusPill, ConstellationChips, CoordinateBlock

## [1.2.0] - 2026-04-12

### Added
- **Project-first UX** — persistent project header above tabs with project name, point count, and dropdown switcher
- **4-tab consolidation** — GNSS, Survey, Stakeout, Tools (merged Export + Transform + Config)
- **ToolsPanel** combining export/import, coordinate tools, and settings
- **F-Droid Fastlane metadata** — descriptions, screenshots, changelog
- Layer data model foundation (DB migration v3→v4)

### Changed
- Export/Import now operates on active project implicitly
- SurveyPanel shows active project directly (no project list navigation)

## [1.1.0] - 2026-04-12

### Added
- **Internal GPS** as third connection option (Android LocationManager NMEA)
- **Satellite skyplot** — polar chart with constellation colors and legend
- **Transform panel** — WGS84→EGSA87 coordinate converter with full pipeline visualization
- **Haptic/audio feedback** — vibration + chime on point recorded
- **NTRIP disconnect alert** — vibration pattern + warning tone
- **Persistent fix status pill** on map (colored dot + label)
- **Coordinate format setting** — EGSA87 / WGS84 decimal / WGS84 DMS
- Contour lines overlay with layer toggle
- Scrollable tabs with labels (6 tabs)

### Fixed
- Internal GPS runtime permission check and GPS provider validation
- Contour lines minzoom, filter compatibility, line thickness
- Status bar overflow from constellation chips + accuracy values

## [1.0.0] - 2026-04-11

### Added
- Google Play Store release preparation (privacy policy, store listing, signed AAB)
- Play Store screenshots and promotional assets
- Material 3 Expressive UI overhaul (MaterialExpressiveTheme, MotionScheme.expressive)

### Changed
- Upgraded to material3 1.5.0-alpha17 for M3 Expressive APIs

## [0.3.0] - 2026-04-11

### Added
- Settings tab with recording, connection, and display options
- Photo attachments on survey points
- CSV import for external point data
- Release signing configuration

## [0.2.0] - 2026-04-11

### Added
- Survey points displayed on map with fix-quality colors and labels
- Auto-reconnect for Bluetooth, USB, and NTRIP on startup
- Point edit and delete with confirmation dialogs
- Contour lines layer on map

## [0.1.0] - 2026-04-11

### Added
- HEPOS transformation engine (lib-transform) with full 6-step pipeline
- NMEA 0183 parser (GGA, RMC, GSA, GSV, GST) with multi-constellation support
- Bluetooth Classic and USB-OTG serial GNSS receiver connection
- NTRIP v1 raw TCP client with VRS/GGA forwarding
- Survey project and point management (Room database)
- Point recording with configurable epoch averaging and quality filtering
- Stakeout navigation with distance, bearing, delta E/N
- Map-centric UI with MapLibre, vathra.xyz vector tiles, Ktimatologio orthophoto
- Export CSV/GeoJSON/DXF with Android share intent
- Connection settings persistence via DataStore
- GitHub Actions CI/CD with release workflow

### Changed
- Toolchain: AGP 9.1.0, Gradle 9.4.1, Kotlin 2.3.0, Room 2.8.4

### Fixed
- NTRIP VRS GGA forwarding, USB serial data corruption, PendingIntent crash on Android 14+

[Unreleased]: https://github.com/ppapadeas/opentopo/compare/v1.4.0...HEAD
[1.4.0]: https://github.com/ppapadeas/opentopo/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/ppapadeas/opentopo/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/ppapadeas/opentopo/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ppapadeas/opentopo/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/ppapadeas/opentopo/compare/v0.3.0...v1.0.0
[0.3.0]: https://github.com/ppapadeas/opentopo/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/ppapadeas/opentopo/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/ppapadeas/opentopo/releases/tag/v0.1.0
