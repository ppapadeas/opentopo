# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Geoid source toggle** in Settings → Display — choose between Greek HEPOS07 (default) and receiver EGM96 for orthometric height H = h − N; falls back to the other source when the preferred one is unavailable
- **Geoid grid metadata block** in Transform panel — shows HEPOS07 version, dimensions (408 × 422 nodes), cell size (2000 m), TM07 coverage bounds, source (Ktimatologio / NTUA), licence, and the currently active source
- `HeposTransform.forwardDetailed(preferReceiverGeoid=…)` parameter in `lib-transform` inverts the geoid precedence
- `HeposTransform.geoidGridMetadata` property exposing grid bounds/resolution as a `GridMetadata` data class
- `UserPreferences.preferReceiverGeoid` persisted preference (default `false` = Greek HEPOS07)
- 4 new unit tests covering the toggle and metadata accessor
- `docs/USER_STORIES.md` — catalogue of user stories grouped by persona and functional area, tagged shipped vs planned

### Changed
- `MainMapScreen` Verify button honours the geoid source preference when computing measured orthometric height for trig-point verification

## [1.9.5] - 2026-04-18

### Fixed
- **Basemap rendering fix** — removed `water_label_ocean` and `water_label_lakes` layers whose deeply-nested `is-supported-script` expressions caused the entire style to fail rendering on MapLibre Android 11.8.4, leaving the map blank

### Changed
- **Initial camera uses last known GPS location** — map opens centered on the user's location at zoom 15, falling back to Greece overview (zoom 7) when no GPS fix is available

## [1.9.4] - 2026-04-16

### Fixed
- **Basemap style regenerated** from `@protomaps/basemaps` v5.7.1 to match current tile data (protomaps basemap v4.14.1) served by Cloudflare worker
- **Initial map zoom adjusted** from 6 to 7 so Greece fills the viewport on launch instead of showing mostly gray background

## [1.9.3] - 2026-04-15

### Added
- **Greek geoid grid (HEPOS07)** — bundled `geoid_hepos07.grd` (985 KB, 422x408, 2km spacing) from Ktimatologio/NTUA for accurate orthometric heights in Greece
- **Geoid interpolation in HeposTransform** — `geoidUndulation()` method and `hasGeoidGrid` property
- **ΔH re-enabled in trig point verification** — measured orthometric height computed as H = h_ellipsoidal - N_greek instead of relying on receiver EGM96
- 5 new geoid tests (grid loading, Athens/Arfara undulation checks, Greek geoid overrides receiver)

### Fixed
- **6.2 m vertical residual eliminated** — Greek geoid undulation (~25.7 m at Arfara) replaces receiver EGM96 (~31.9 m), matching the published trig point height datum

## [1.9.2] - 2026-04-14

### Fixed
- **Trig point verification uses published EGSA87 directly** — vathra.xyz API now returns the exact published `egsa87_x`/`egsa87_y`/`egsa87_z` coordinates; the app uses these directly for verification and stakeout instead of recomputing from lat/lon
- **Root cause of 1.3 m verification residuals** — the API's WGS84 lat/lon were transformed from EGSA87 by GDAL using PROJ's 3-parameter Helmert, which differs from the official HEPOS 7-parameter Helmert used by OpenTopo; the round-trip through incompatible parameter sets caused systematic error
- **ΔH disabled in verification** — GGA altitude uses the receiver's EGM96 geoid while published trig point elevations reference the Greek vertical datum; the two geoid surfaces differ by several metres, making ΔH misleading

### Added
- `egsa87Easting`, `egsa87Northing`, `egsa87Z` fields in TrigPoint data model and offline cache
- Database migration v7 for trig point cache EGSA87 columns
- Fallback to HeposTransform when API doesn't provide EGSA87 fields

## [1.9.1] - 2026-04-14

### Fixed
- **Trig point verification datum fix (superseded by v1.9.2)** — initial fix using direct TM87 projection; replaced by using published EGSA87 coordinates from the API
- Coordinate label corrected from "WGS84" to "GGRS87" for trig points (reverted — API lat/lon are WGS84)

### Changed
- Bumped versionCode to 11

## [1.9.0] - 2026-04-14

### Added
- **Accuracy convergence ring** — animated shrinking circle as RTK converges
- **Glove mode** — 64dp targets, volume buttons for store/undo, +4sp fonts
- **PiP floating stakeout** — picture-in-picture compass visible over other apps
- **Trig point verification workflow** — record at mark, compare measured vs published, compute residuals with report dialog
- **Offline trig point cache** — Room-backed cache for trig points with fallback when API unavailable
- ShortNavigationBar, ButtonGroup, emphasized typography, M3E shape system
- AMOLED dark mode option
- MotionScheme.expressive() tokens for panel transitions

### Changed
- Bumped versionCode to 10

## [1.8.1] - 2026-04-13

### Added
- **Google Play CI/CD pipeline** — signed release AAB built and uploaded to Google Play automatically on `v*` tag push via GitHub Actions
- **Feature graphic** (1024x500) for Play Store listing
- **GitHub Release automation** — signed APK and AAB attached to GitHub Releases

### Changed
- **Play Store descriptions** updated with latest features (lines, polygons, shapefile export, trig points, skyplot, coordinate converter, NTRIP presets, immersive stakeout)
- Release workflow now builds signed release variants instead of debug
- Bumped versionCode to 9

## [1.8.0] - 2026-04-13

### Added
- **Stakeout immersive full-screen mode** — dark inverseSurface background, displayLarge distance, 240dp compass, delta E/N, fix pill. Enter via "Full Screen" button during stakeout
- **Record button pulse ring** — animated expanding/fading ring around FAB during point recording
- **Active layer indicators** — checkmarks in layer switcher dropdown showing current state
- **FAB always visible** — dimmed at 40% opacity when disabled instead of hidden

### Changed
- **DGPS color** distinguished from Float — now bright yellow (#F9A825) vs amber (#7B5800)
- **Minimum font sizes** increased for field readability — bodySmall 12sp, labelMedium 12sp, labelSmall 11sp
- **Status bar compressed** — constellation chips removed from peek (shown in GNSS panel instead)
- Roadmap reorganized with M3 Expressive showcase (v1.8.0), Quick Win Innovations (v1.9.0), Advanced Innovations (v1.10.0)

## [1.7.0] - 2026-04-12

### Added
- **Vathra.xyz trig point integration** — 25,259 Greek GYS trigonometric points loaded from vathra.xyz API
- **Trig point map layer** — status-colored markers (green=OK, orange=damaged, red=destroyed, purple=missing, gray=unknown) with toggleable visibility
- **Trig point tap details** — snackbar showing GYS ID, name, status, elevation, EGSA87 coordinates
- **Stakeout to trig point** — "Nearby Trig Points" button fetches points within 10km, tap to auto-fill stakeout target
- **Project credits** — comprehensive About section with developer, transformation engine, map data, library, and license attribution
- Survey points now visible on orthophoto (bright neon colors, thick borders, layer ordering fix)
- Connection method badge (BT/USB/Internal) shown when connected, segmented selector hidden

### Fixed
- Orthophoto layer z-order: now renders below survey features
- Internal GPS no longer auto-connects (requires explicit tap)
- About version double "v" prefix

## [1.6.0] - 2026-04-12

### Added
- **Geoid undulation support** — orthometric height (H = h - N) computed from receiver-reported GGA geoid separation
- **Dual height display** — status bar shows H (MSL) and h (ellipsoidal) when geoid separation available
- **Geoid fields in database** — geoidSeparation and orthometricHeight columns (migration v4→v5)
- **Geoid step in Transform panel** — step 8 showing N, h, and H with user-editable geoid input
- **Height info in point cards** — H and N values shown below coordinates
- **Export orthometric height** — CSV (Ortho_Height, Geoid_N columns), GeoJSON (ortho_height, geoid_n), DXF (Z = orthometric), Shapefile (ORTHO_H, GEOID_N fields)
- CSV import backward compatible with old format (auto-detects column layout)

## [1.5.0] - 2026-04-12

### Added
- **Shapefile export** (SHP/SHX/DBF/PRJ packed as ZIP) with EGSA87 coordinates and EPSG:2100 projection
- **Import stakeout targets** from CSV files
- **Real-time measurements** — line distance and polygon area shown during vertex recording
- **Undo last vertex** during line/polygon recording
- **F-Droid submission** — fdroiddata MR #36364

### Fixed
- DXF export rewritten as R12 (AC1009) with all required tables for AutoCAD compatibility
- Dynamic versionName from `git describe --tags --always --dirty`

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
- M3E ShortNavigationBar with 4 tabs (pill-shaped active indicators)

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

[Unreleased]: https://github.com/ppapadeas/opentopo/compare/v1.8.1...HEAD
[1.8.1]: https://github.com/ppapadeas/opentopo/compare/v1.8.0...v1.8.1
[1.8.0]: https://github.com/ppapadeas/opentopo/compare/v1.7.0...v1.8.0
[1.7.0]: https://github.com/ppapadeas/opentopo/compare/v1.6.0...v1.7.0
[1.6.0]: https://github.com/ppapadeas/opentopo/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/ppapadeas/opentopo/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/ppapadeas/opentopo/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/ppapadeas/opentopo/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/ppapadeas/opentopo/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ppapadeas/opentopo/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/ppapadeas/opentopo/compare/v0.3.0...v1.0.0
[0.3.0]: https://github.com/ppapadeas/opentopo/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/ppapadeas/opentopo/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/ppapadeas/opentopo/releases/tag/v0.1.0
