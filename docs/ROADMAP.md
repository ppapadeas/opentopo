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

### v1.7.0 -- Extensible Geodesy
**Goal:** Support custom coordinate systems, grids, and geoid models beyond Greece.

- [ ] **Custom correction grids** -- import user-provided dE/dN grids (same .grd format)
- [ ] **Custom geoid models** -- import geoid undulation grids for any country
- [ ] **Custom Helmert parameters** -- user-defined 7-parameter datum transformations
- [ ] **Custom TM projection** -- configurable central meridian, scale factor, false E/N
- [ ] **Kastellorizo zone** -- separate TM07 parameters (central meridian 30°, scale 1.0)
- [ ] **Predefined coordinate systems** -- dropdown with EGSA87, HTRS07, UTM zones, HATT zones
- [ ] **Grid manager** -- list loaded grids, import/delete, show coverage and metadata
- [ ] **Inverse transformation** -- EGSA87 → WGS84 (iterative grid interpolation)

### v1.8.0 -- Receiver Intelligence
**Goal:** Better hardware integration and receiver management.

- [ ] **Receiver profiles** -- presets for ArduSimple, u-blox ZED-F9P, Emlid Reach
- [ ] **Auto-detect baud rate** -- try common rates on USB connect
- [ ] **RTCM message inspector** -- show correction types, ages, base station ID
- [ ] **Raw observation logging (RINEX)** for post-processing
- [ ] **u-blox UBX protocol** -- configure receiver message rates, dynamic model
- [ ] **Custom attribute fields** per project (user-defined columns per geometry type)

### v1.9.0 -- Offline & Performance
**Goal:** Full field capability without connectivity.

- [ ] **Offline map tiles** -- download PMTiles for Greece to device storage
- [ ] **Background GNSS service** -- keep tracking when app is backgrounded
- [ ] **Battery optimization** -- reduce GPS polling when stationary
- [ ] **Map tile cache** -- aggressive caching for field areas

### v2.0.0 -- Platform Expansion
**Goal:** Professional-grade multi-country surveying platform.

- [ ] **Tablet layout** -- side-by-side map and panels (NavigationRail)
- [ ] **Country packs** -- downloadable bundles (grid files + geoid + Helmert + TM params) for:
  - Greece (HEPOS/GGRS87/EGSA87) -- built-in
  - Cyprus (LTM, OSGB-like)
  - Other Balkan countries
  - Any user-submitted pack
- [ ] **Localization** -- Greek, English, and community translations
- [ ] **Dark/light theme toggle** -- per-user preference
- [ ] **Onboarding tutorial** -- first-run walkthrough with grid/CRS selection
- [ ] **F-Droid stable publication** -- official inclusion
- [ ] **Cloud project sync** -- optional backup/share via WebDAV or S3-compatible storage
