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
- [x] Teal-green palette (#006B5E), light-first design
- [x] Component library: FixStatusPill, TonalCard, ConstellationChip, etc.
- [x] Quick Mark (1-epoch), inverse countdown, VIBRATE permission fix

### v1.4.0 -- Measurement & Layers (current)
- [x] Line recording with vertex tapping and distance computation
- [x] Polygon recording with Shoelace area computation
- [x] Point/Line/Area mode switcher in Survey panel
- [x] LineLayer + FillLayer map rendering
- [x] OpenTopo brand logo in all app assets

---

## Development Path Forward

### v1.5.0 -- F-Droid & Data (next)
**Goal:** F-Droid publication and professional data handling.

- [ ] **F-Droid publication** -- submit to fdroiddata (Fastlane metadata ready)
- [ ] **Shapefile export** (SHP/DBF/SHX/PRJ) for CAD/GIS interop
- [ ] **Import stakeout points** from CSV or GeoJSON file
- [ ] **Custom attribute fields** per project (user-defined columns)
- [ ] **Line/polygon distance and area** shown in real-time during recording
- [ ] **Undo last vertex** during line/polygon recording

### v1.6.0 -- Receiver Intelligence
**Goal:** Better hardware integration and receiver management.

- [ ] **Receiver profiles** -- presets for ArduSimple, u-blox ZED-F9P, Emlid Reach
- [ ] **Auto-detect baud rate** -- try common rates on USB connect
- [ ] **RTCM message inspector** -- show correction types and ages
- [ ] **Raw observation logging (RINEX)** for post-processing

### v1.7.0 -- Offline & Performance
**Goal:** Full field capability without connectivity.

- [ ] **Offline map tiles** -- download PMTiles for Greece to device storage
- [ ] **Background GNSS service** -- keep tracking when app is backgrounded
- [ ] **Battery optimization** -- reduce GPS polling when stationary

### v2.0.0 -- Platform Expansion
**Goal:** Professional-grade surveying platform.

- [ ] **Tablet layout** -- side-by-side map and panels (NavigationRail)
- [ ] **Kastellorizo grid** -- separate TM07 parameters
- [ ] **Custom Helmert parameters** -- support other national datums
- [ ] **User-provided correction grids** -- import grids for other countries
- [ ] **Localization** -- Greek and English UI
- [ ] **Dark/light theme toggle** -- per-user preference
- [ ] **Onboarding tutorial** -- first-run walkthrough
