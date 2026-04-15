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

### v1.5.0 -- Data & Measurement
- [x] Shapefile export (SHP/DBF/SHX/PRJ as ZIP)
- [x] Import stakeout targets from CSV
- [x] Real-time distance and area during recording
- [x] Undo last vertex
- [x] DXF R12 export fixed for AutoCAD
- [x] F-Droid fdroiddata submission (MR #36364)

### v1.6.0 -- Geoid & Heights
- [x] Geoid undulation from GGA receiver used for orthometric height computation
- [x] Dual height display: H (orthometric/MSL) and h (ellipsoidal) in status bar
- [x] Geoid separation and orthometric height stored in PointEntity (DB migration v5)
- [x] Geoid undulation inspector in Transform panel (step 8 in pipeline)
- [x] Orthometric height exported in CSV, GeoJSON, DXF, Shapefile
- [x] CSV import backward compatible with old format

---

## Development Path Forward

### v1.7.0 -- Vathra.xyz Integration & Trig Point Verification
- [x] Trig point map layer — 25,259 GYS points from vathra.xyz API with status-colored markers
- [x] Trig point details — tap for name, EGSA87 coords, elevation, GYS sheet, status
- [x] Stakeout to trig point — nearby points list, one-tap to set as target
- [x] Layer toggle in map switcher
- [x] Project credits in About section
- [x] Verification workflow — record at mark, compare measured vs published, compute residuals
- [x] Verification report — summary with deltas, fix quality, timestamp
- [ ] Submit condition update to vathra.xyz
- [x] Offline trig point cache

### v1.8.0 -- Direction C: M3 Expressive Showcase
- [x] Compressed single-row status bar (fix + accuracy only in peek)
- [x] FAB always visible (dimmed when disabled)
- [x] DGPS color distinguished from Float
- [x] 12sp minimum font sizes for field readability
- [x] Active layer indicators in layer switcher
- [x] Stakeout immersive full-screen mode (dark, huge numbers, 240dp compass)
- [x] Record button pulse ring animation

### v1.8.1 -- Google Play CI/CD
- [x] Signed release builds via GitHub Actions (bundleRelease + assembleRelease)
- [x] Automated Google Play upload on `v*` tag push (closed testing track)
- [x] GitHub Release with signed APK + AAB artifacts
- [x] Play Store descriptions updated with all v1.0–v1.8 features
- [x] Feature graphic (1024x500) for store listing

### v1.9.0 -- M3E Compliance & Quick Win Innovations
**Goal:** Full M3 Expressive compliance and high-impact innovative features.

#### M3E Compliance
- [x] ShortNavigationBar replacing SecondaryTabRow (pill-shaped indicators, compact height)
- [x] ButtonGroup with ToggleButton replacing SegmentedButton (ConnectionPanel, SurveyPanel)
- [x] Emphasized typography variants (all 15 M3E `*Emphasized` text styles)
- [x] Shape system aligned to M3E spec (4/8/12/16/28dp)
- [x] Dark mode surface container hierarchy (surfaceContainerLow through surfaceContainerHighest)
- [x] AMOLED dark mode option (pure black surfaces for field battery conservation)
- [x] MotionScheme.expressive() tokens for panel transitions (replacing hardcoded spring specs)

#### Innovations
- [ ] Glassmorphism bottom sheet (semi-transparent, blur)
- [ ] Shape morphing record button (circle -> squircle when recording)
- [x] **Accuracy convergence ring** — animated shrinking circle as RTK converges (difficulty: 3, impact: 8)
- [x] **Glove mode** — single toggle: 64dp targets, volume buttons for store/undo, +4sp fonts (difficulty: 3, impact: 9)
- [x] **PiP floating stakeout** — picture-in-picture compass visible over other apps (difficulty: 4, impact: 8)
- [ ] **Ambient-adaptive theme** — auto light/dark from ambient light sensor with hysteresis (difficulty: 5, impact: 7)

### v1.9.1 -- Trig Point Datum Fix
- [x] Initial fix for ~1.3 m verification residuals (superseded by v1.9.2)
- [x] Disabled misleading ΔH in verification (EGM96 vs Greek geoid mismatch)

### v1.9.2 -- Published EGSA87 Coordinates
- [x] **vathra.xyz API** now returns `egsa87_x`, `egsa87_y`, `egsa87_z` — exact published coordinates
- [x] **Verification uses published E/N directly** — eliminates round-trip Helmert parameter mismatch
- [x] **Root cause identified** — PROJ 3-param vs HEPOS 7-param Helmert incompatibility
- [x] TrigPoint data model and offline cache extended with EGSA87 fields (DB migration v7)
- [x] Fallback to HeposTransform when API doesn't provide EGSA87 fields

### v1.9.3 -- Greek Geoid Grid
**Goal:** Accurate orthometric heights for Greece, re-enable ΔH in trig point verification.

- [x] **Greek geoid grid** — bundled HEPOS07 geoid (geoid_hepos07.grd, 985 KB, Ktimatologio/NTUA, CC BY-NC-SA 3.0 + GPLv3)
- [x] **Re-enable ΔH verification** — H = h_ellipsoidal - N_greek; eliminates 6.2 m EGM96 vs Greek geoid mismatch
- [x] **HeposTransform geoid API** — geoidUndulation() interpolation, hasGeoidGrid property, forwardDetailed() prefers Greek geoid
- [ ] **Geoid source toggle** — switch between receiver EGM96 and Greek geoid for orthometric display
- [ ] **Geoid grid metadata** — show version, coverage, and source in Tools panel

### v1.10.0 -- Advanced Innovations
**Goal:** Differentiating features no competitor has.

- [ ] **Haptic stakeout guidance** — directional vibration patterns for hands-free navigation (difficulty: 5, impact: 9)
- [ ] **Radial/pie context menu** — long-press on map features for circular action menu (difficulty: 6, impact: 7)
- [ ] **Home screen widget** — Glance API widget with live survey status (difficulty: 5, impact: 5)
- [ ] **Survey coverage heatmap** — density overlay revealing spatial gaps (difficulty: 7, impact: 6)

### v2.0.0 -- Country Packs & Extensible Geodesy
**Goal:** Multi-country support via downloadable country packs, each bundling datum, projection, grids, geoid, trig points, and NTRIP presets.

#### Country Pack Architecture
- [ ] **CountryProfile abstraction** — bundles Helmert params, projection config, grid files, geoid, NTRIP presets, trig point API, export CRS, UI labels
- [ ] **NTv2 grid reader** — industry-standard grid format (used by UK OSTN15, Australia, Germany BeTA2007, Spain, France, etc.)
- [ ] **Lambert Conformal Conic projection** — required for France (Lambert-93) and some US State Plane zones
- [ ] **Grid manager** — list loaded grids, import/delete, show coverage and metadata
- [ ] **Downloadable packs** — country grids + geoid fetched on demand (not bundled in APK)
- [ ] **Profile switcher** — select active country/CRS in settings, UI labels adapt dynamically
- [ ] **Export CRS from profile** — PRJ/WKT, GeoJSON CRS, CSV headers derived from active profile

#### Predefined Country Packs (priority order)
- [ ] **Greece (built-in)** — current HEPOS pipeline, refactored into CountryProfile
- [ ] **Cyprus** — CGRS93/LTM, CYPOS NTRIP (near-identical to Greece, smallest effort)
- [ ] **Spain** — ETRS89/UTM, ERGNSS free NTRIP, EGM08-REDNAP geoid, IGN trig API
- [ ] **Poland** — PL-ETRF2000/PL-2000, ASG-EUPOS free NTRIP, PL-geoid2021
- [ ] **France** — RGF93/Lambert-93, Centipede free RTK, RAF20 geoid
- [ ] **UK** — OSGB36/BNG via OSTN15+OSGM15 grids, trigpointing.uk data

#### Custom/Advanced
- [ ] **Custom Helmert parameters** — user-defined 7-parameter datum transformations
- [ ] **Custom TM projection** — configurable central meridian, scale factor, false E/N
- [ ] **Custom correction grids** — import user-provided dE/dN or NTv2 grids
- [ ] **Custom geoid models** — import geoid undulation grids for any country
- [ ] **Kastellorizo zone** — separate TM07 parameters (central meridian 30°, scale 1.0)
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
