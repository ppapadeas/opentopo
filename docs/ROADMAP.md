# Development Roadmap

## Completed

### v0.1.0 -- Foundation
- [x] HEPOS transformation engine, BT/USB GNSS, NMEA parser
- [x] NTRIP v1 raw TCP client with VRS/GGA forwarding
- [x] Survey projects, point recording, stakeout navigation
- [x] vathra.xyz basemap, Ktimatologio orthophoto, export CSV/GeoJSON/DXF

### v0.2.0 -- Map and Points
- [x] Survey points on map, auto-reconnect, point edit/delete, contours

### v0.3.0 -- Settings and Import
- [x] Settings tab, photo attachments, CSV import, release signing

### v1.0.0 -- Public Release
- [x] Play Store assets, M3 Expressive UI, privacy policy, signed builds

### v1.1.0 -- Field Tools
- [x] Internal GPS, satellite skyplot, transform panel
- [x] Haptic/audio feedback, NTRIP disconnect alert, fix status pill

### v1.2.0 -- Project-First UX
- [x] Persistent project header with switcher, 4-tab consolidation
- [x] ToolsPanel (Export + Transform + Settings), F-Droid metadata

### v1.3.0 -- M3 Expressive Theme
- [x] Teal-green palette, component library, Quick Mark, inverse countdown

### v1.4.0 -- Measurement & Layers
- [x] Line/polygon recording with map rendering
- [x] Point/Line/Area mode switcher, brand logo

### v1.5.0 -- Data & Measurement (current)
- [x] Shapefile export (SHP/DBF/SHX/PRJ as ZIP)
- [x] Import stakeout targets from CSV
- [x] Real-time distance and area during recording
- [x] Undo last vertex
- [x] DXF R12 export fixed for AutoCAD
- [x] F-Droid fdroiddata submission (MR #36364)

---

## Development Path Forward

### v1.6.0 -- Geoid & Heights (current)
- [x] Geoid undulation from GGA receiver used for orthometric height computation
- [x] Dual height display: H (orthometric/MSL) and h (ellipsoidal) in status bar
- [x] Geoid separation and orthometric height stored in PointEntity (DB migration v5)
- [x] Geoid undulation inspector in Transform panel (step 8 in pipeline)
- [x] Orthometric height exported in CSV, GeoJSON, DXF, Shapefile
- [x] CSV import backward compatible with old format

### v1.7.0 -- Vathra.xyz Integration & Trig Point Verification (current)
- [x] Trig point map layer — 25,259 GYS points from vathra.xyz API with status-colored markers
- [x] Trig point details — tap for name, EGSA87 coords, elevation, GYS sheet, status
- [x] Stakeout to trig point — nearby points list, one-tap to set as target
- [x] Layer toggle in map switcher
- [x] Project credits in About section
- [ ] Verification workflow — record at mark, compare measured vs published, compute residuals
- [ ] Verification report — summary with deltas, fix quality, timestamp
- [ ] Submit condition update to vathra.xyz
- [ ] Offline trig point cache

### v1.8.0 -- Direction C: M3 Expressive Showcase (current)
- [x] Compressed single-row status bar (fix + accuracy only in peek)
- [x] FAB always visible (dimmed when disabled)
- [x] DGPS color distinguished from Float
- [x] 12sp minimum font sizes for field readability
- [x] Active layer indicators in layer switcher
- [x] Stakeout immersive full-screen mode (dark, huge numbers, 240dp compass)
- [x] Record button pulse ring animation
- [ ] Glassmorphism bottom sheet (semi-transparent, blur)
- [ ] Spring-based motion on all sheet/tab transitions
- [ ] Shape morphing record button (circle → squircle when recording)

### v1.9.0 -- Quick Win Innovations
**Goal:** Implement the easiest high-impact innovative features.

- [ ] **Accuracy convergence ring** — animated shrinking circle as RTK converges (difficulty: 3, impact: 8)
- [ ] **Glove mode** — single toggle: 64dp targets, volume buttons for store/undo, +4sp fonts (difficulty: 3, impact: 9)
- [ ] **PiP floating stakeout** — picture-in-picture compass visible over other apps (difficulty: 4, impact: 8)
- [ ] **Ambient-adaptive theme** — auto light/dark from ambient light sensor with hysteresis (difficulty: 5, impact: 7)

### v1.10.0 -- Advanced Innovations
**Goal:** Differentiating features no competitor has.

- [ ] **Haptic stakeout guidance** — directional vibration patterns for hands-free navigation (difficulty: 5, impact: 9)
- [ ] **Radial/pie context menu** — long-press on map features for circular action menu (difficulty: 6, impact: 7)
- [ ] **Home screen widget** — Glance API widget with live survey status (difficulty: 5, impact: 5)
- [ ] **Survey coverage heatmap** — density overlay revealing spatial gaps (difficulty: 7, impact: 6)

### v2.0.0 -- Extensible Geodesy
**Goal:** Support custom coordinate systems, grids, and geoid models beyond Greece.

- [ ] **Custom correction grids** — import user-provided dE/dN grids (same .grd format)
- [ ] **Custom geoid models** — import geoid undulation grids for any country
- [ ] **Custom Helmert parameters** — user-defined 7-parameter datum transformations
- [ ] **Custom TM projection** — configurable central meridian, scale factor, false E/N
- [ ] **Kastellorizo zone** — separate TM07 parameters (central meridian 30°, scale 1.0)
- [ ] **Predefined coordinate systems** — dropdown with EGSA87, HTRS07, UTM zones, HATT zones
- [ ] **Grid manager** — list loaded grids, import/delete, show coverage and metadata
- [ ] **Inverse transformation** — EGSA87 → WGS84 (iterative grid interpolation)

### v2.1.0 -- Receiver Intelligence
**Goal:** Better hardware integration and receiver management.

- [ ] **Receiver profiles** — presets for ArduSimple, u-blox ZED-F9P, Emlid Reach
- [ ] **Auto-detect baud rate** — try common rates on USB connect
- [ ] **RTCM message inspector** — show correction types, ages, base station ID
- [ ] **Raw observation logging (RINEX)** for post-processing
- [ ] **u-blox UBX protocol** — configure receiver message rates, dynamic model
- [ ] **Custom attribute fields** per project (user-defined columns per geometry type)

### v2.2.0 -- Platform Expansion
**Goal:** Professional-grade multi-country platform with advanced innovations.

- [ ] Tablet layout (NavigationRail dual-pane)
- [ ] Foldable dual-pane mode (Samsung/Pixel Fold)
- [ ] Country packs (downloadable grid + geoid + Helmert bundles)
- [ ] Voice commands for hands-free field operation
- [ ] Wear OS companion (RTK status on wrist)
- [ ] Cross-section profile (swipe for elevation profile)
- [ ] Offline map tiles (PMTiles download)
- [ ] Localization (Greek, English)
- [ ] F-Droid stable publication
- [ ] Cloud project sync (WebDAV/S3)
- [ ] Onboarding tutorial
