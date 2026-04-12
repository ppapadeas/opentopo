# Development Roadmap

## Completed

### v0.1.0 -- Foundation
- [x] HEPOS transformation engine (lib-transform)
- [x] Bluetooth & USB-OTG GNSS receiver connection
- [x] NMEA parser (GGA, RMC, GSA, GSV, GST)
- [x] NTRIP v1 raw TCP client with VRS/GGA forwarding
- [x] Survey projects, point recording with epoch averaging
- [x] Stakeout navigation
- [x] vathra.xyz vector basemap + Ktimatologio orthophoto
- [x] Export CSV/GeoJSON/DXF, Material 3 UI, CI/CD

### v0.2.0 -- Map and Points
- [x] Survey points on map with fix-quality colors
- [x] Auto-reconnect BT/USB/NTRIP on startup
- [x] Point edit/delete, contour lines

### v0.3.0 -- Settings and Import
- [x] Settings tab, photo attachments, CSV import, release signing

### v1.0.0 -- Public Release
- [x] Play Store assets, M3 Expressive UI overhaul

### v1.1.0 -- Field Tools
- [x] Internal GPS (Android LocationManager)
- [x] Satellite skyplot, transform panel + coordinate converter
- [x] Haptic/audio feedback, NTRIP disconnect alert
- [x] Persistent fix status pill, coordinate format setting

### v1.2.0 -- Project-First UX (current)
- [x] Project header with active project indicator
- [x] 4-tab consolidation (GNSS/Survey/Stakeout/Tools)
- [x] Export/Import from project overflow menu
- [x] Layer data model foundation
- [x] F-Droid metadata

---

## Development Path Forward

### v1.3.0 -- Measurement & Layers (next)
**Goal:** Enable field measurements beyond point collection.

- [ ] **Line recording** -- tap vertices on map, show cumulative distance
- [ ] **Polygon recording** -- close line to polygon, compute area (m2, stremma)
- [ ] **Layer toggles on map** -- show/hide point, line, polygon layers
- [ ] **Context-sensitive record button** -- switch between point/line/polygon mode

### v1.4.0 -- F-Droid & Data
**Goal:** F-Droid publication and professional data handling.

- [ ] **F-Droid publication** -- submit to fdroiddata
- [ ] **Shapefile export** (SHP/DBF/SHX/PRJ) for CAD/GIS interop
- [ ] **Import stakeout points** from CSV or GeoJSON file
- [ ] **Custom attribute fields** per project (user-defined columns)

### v1.5.0 -- Receiver Intelligence
**Goal:** Better hardware integration and receiver management.

- [ ] **Receiver profiles** -- presets for ArduSimple, u-blox ZED-F9P, Emlid Reach
- [ ] **Auto-detect baud rate** -- try common rates on USB connect
- [ ] **RTCM message inspector** -- show correction types and ages
- [ ] **Raw observation logging (RINEX)** for post-processing
- [ ] **u-blox UBX protocol** -- configure receiver settings directly

### v1.6.0 -- Offline & Performance
**Goal:** Full field capability without connectivity.

- [ ] **Offline map tiles** -- download PMTiles for Greece to device storage
- [ ] **Offline contours** -- bundled or downloadable contour PMTiles
- [ ] **Tile cache management** -- download/delete in Settings
- [ ] **Background GNSS service** -- keep tracking when app is backgrounded
- [ ] **Battery optimization** -- reduce GPS polling when stationary

### v2.0.0 -- Platform Expansion
**Goal:** Professional-grade surveying platform.

- [ ] **Tablet layout** -- side-by-side map and panels (NavigationRail)
- [ ] **Kastellorizo grid** -- separate TM07 parameters for Kastellorizo zone
- [ ] **Custom Helmert parameters** -- support other national datums
- [ ] **User-provided correction grids** -- import grids for other countries
- [ ] **Localization** -- Greek and English UI
- [ ] **Onboarding tutorial** -- first-run walkthrough
- [ ] **Dark/light theme toggle** -- per-user preference instead of system
