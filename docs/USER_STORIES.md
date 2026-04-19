# OpenTopo User Stories

A living catalogue of user stories, grouped by functional area. Each story follows the form
"**As a** `<persona>`, **I want** `<capability>` **so that** `<benefit>`." Acceptance criteria are
kept as short bullets under each story. Items already in the app are marked **[shipped]**; items on
the roadmap are **[planned]**.

## Personas

- **Surveyor** — licensed professional doing cadastral, engineering or topographic work in Greece with a dual-frequency GNSS receiver and an NTRIP subscription (HEPOS / CivilPOS / SmartNet).
- **Field Technician** — works alongside a surveyor, collects points/stakes out, uses the app but not necessarily its internals.
- **Trig Point Volunteer** — contributes to the vathra.xyz crowdsourced inventory of Greek geodetic benchmarks; uses the app to locate, verify and document the 25,259 GYS pillars.
- **Geodesist / Power User** — inspects the coordinate pipeline (Helmert, TM, grid corrections, geoid) and trusts-but-verifies transformation results.
- **Admin / Installer** — configures a device for a crew, pairs receivers, sets defaults, and deploys projects.

---

## 1. GNSS Connection & Receiver Setup

### 1.1 Connect to a Bluetooth receiver **[shipped]**
**As a** Surveyor, **I want** to pair with a Bluetooth GNSS receiver from a list of bonded devices **so that** I can use my professional receiver without typing addresses.
- Connection type picker offers `Bluetooth / USB / Internal GPS`.
- A dropdown lists currently-paired devices.
- The last chosen device is remembered across restarts.
- A red "Disconnect" button is shown while connected.

### 1.2 Connect over USB OTG **[shipped]**
**As a** Surveyor, **I want** to plug my receiver into USB-C and have the app detect it **so that** I am not dependent on Bluetooth.
- USB serial devices are enumerated on demand via a "Refresh" action.
- Baud rate can be set (default 115,200).
- The app requests USB permission when attaching.

### 1.3 Use the phone's internal GPS **[shipped]**
**As a** Trig Point Volunteer, **I want** to collect positions with just the phone **so that** I can work without dedicated hardware when centimetre accuracy is not required.
- A single button enables Internal GPS via `LocationManager.GPS_PROVIDER`.
- NMEA from the Android location service feeds the same position/accuracy pipeline.
- The UI surfaces that RTK is unavailable in this mode.

### 1.4 Restore the last connection automatically **[shipped]**
**As a** Field Technician, **I want** the app to remember which receiver and settings I used **so that** I do not reconfigure every morning.
- Connection type, BT device address, baud rate, NTRIP settings are persisted in DataStore.

### 1.5 Hot-swap between connection types **[planned]**
**As a** Surveyor, **I want** to switch from Internal GPS to RTK receiver mid-session **so that** I can start work quickly and upgrade accuracy when gear is ready.
- Switching connection type cleanly disconnects the previous session.
- Previously-recorded points keep their original fix-quality stamps.

---

## 2. NTRIP / RTK Corrections

### 2.1 Pick a preset NTRIP caster **[shipped]**
**As a** Surveyor, **I want** a one-tap preset for HEPOS, CivilPOS and SmartNet Greece **so that** I don't need to memorise hostnames and ports.
- Presets populate host and port automatically.
- Custom preset allows arbitrary host/port for other casters.

### 2.2 Fetch the mountpoint sourcetable **[shipped]**
**As a** Surveyor, **I want** to fetch the list of mountpoints from my caster **so that** I pick the correct stream (VRS, MAC, PRS, RTCM 3.2 etc.) without a paper list.
- "Get List" issues a sourcetable request with my credentials.
- Returned entries show mountpoint name and data format.
- I can also type the mountpoint manually.

### 2.3 Store NTRIP credentials securely across restarts **[shipped]**
**As a** Surveyor, **I want** my NTRIP username/password saved **so that** I reconnect with one tap each morning.
- Password field is masked in the UI.
- Credentials persist in DataStore for the selected preset.

### 2.4 See correction age and bandwidth **[shipped]**
**As a** Surveyor, **I want** to see how stale the RTCM corrections are **so that** I notice when the link drops before my accuracy degrades.
- A live "Correction age" (seconds) is displayed.
- Values > 5 s turn red as a warning.
- Data rate (bytes/s) is shown alongside.

### 2.5 Configure GGA interval **[shipped]**
**As a** Surveyor, **I want** to control how often my receiver's GGA position is sent back to the caster **so that** I balance VRS accuracy against traffic and battery.
- Interval is selectable (e.g. 1/5/10/30 s) in Settings.
- The app suppresses extra GGA sentences to match.

---

## 3. Live Position & Quality Display

### 3.1 Always-visible fix status pill **[shipped]**
**As a** Field Technician, **I want** a persistent top-of-screen fix indicator **so that** I always know whether my next recorded point will be RTK-grade.
- Label (`RTK Fix`, `RTK Float`, `DGPS`, `GPS`, `No Fix`) with fix-quality colour.
- Visible on the map regardless of which panel is open.

### 3.2 Satellite count by constellation **[shipped]**
**As a** Surveyor, **I want** separate sat counts for GPS/GLONASS/Galileo/BeiDou **so that** I can diagnose why a fix is weak.
- Per-system chips update live from GSV/GSA NMEA.

### 3.3 Horizontal and vertical accuracy **[shipped]**
**As a** Surveyor, **I want** σH, σV and HDOP with colour bands **so that** I can tell at a glance whether I'm in cm territory.
- Green < 2 cm, amber < 5 cm, red > 5 cm thresholds.

### 3.4 Skyplot of satellite geometry **[shipped]**
**As a** Geodesist, **I want** a polar skyplot of elevation/azimuth **so that** I can understand multipath and mask problems under trees or against buildings.

### 3.5 Raw NMEA console **[shipped]**
**As a** Geodesist, **I want** a live NMEA feed **so that** I can diagnose parser or receiver issues on site.

### 3.6 Accuracy-convergence ring around the location dot **[shipped]**
**As a** Surveyor, **I want** a visible ring that shrinks as RTK converges **so that** I *feel* the fix tightening and know when to press Record.
- Ring radius = σH rendered in screen pixels at current zoom.
- Ring colour follows the accuracy bands.

---

## 4. Survey Recording

### 4.1 Record a single point with epoch averaging **[shipped]**
**As a** Surveyor, **I want** to average N seconds of epochs for one point **so that** I reduce noise and get a defensible measurement.
- Averaging duration is selectable (1/3/5/10/15/30/60 s).
- Progress bar advances 1 epoch per second.
- Mean lat/lon/h, σH, σV are stored.

### 4.2 Reject points that do not meet accuracy **[shipped]**
**As a** Surveyor, **I want** to abort recording if σH exceeds my tolerance **so that** I never commit a bad point.
- Tolerance set in Settings (default 0.05 m).
- If exceeded mid-averaging, the session aborts with a clear message.

### 4.3 Require RTK Fix before recording **[shipped]**
**As a** Surveyor, **I want** a hard gate that blocks recording below RTK Fix **so that** junior crew cannot accidentally record DGPS or Float.
- Toggle in Settings; if on and fix quality < 4, the Record button disables with explanation.

### 4.4 Apply antenna height automatically **[shipped]**
**As a** Surveyor, **I want** to set AH once and have it subtracted from h **so that** every point reports ground elevation, not antenna-phase-centre.
- AH stored per recording session; also editable per point after the fact.

### 4.5 Record a polyline **[shipped]**
**As a** Field Technician, **I want** to record a line by adding vertices while walking **so that** I capture edges, fences and paths.
- Start / Add Vertex / Undo / Finish controls.
- Live cumulative distance displayed.

### 4.6 Record a polygon with live area **[shipped]**
**As a** Surveyor, **I want** to close a polygon and see its area in m² and στρέμματα **so that** I can confirm a parcel without post-processing.
- Stremma conversion (1 στρ. = 1000 m²) is shown alongside m².

### 4.7 Audible and haptic confirmation on each point **[shipped]**
**As a** Field Technician, **I want** a strong vibration and tone on successful record **so that** I know it worked without taking my eyes off the pole.

### 4.8 Remarks / note per point **[shipped]**
**As a** Surveyor, **I want** a free-text field on each point **so that** I capture `"NW corner"`, `"fence post"`, `"suspect leaf drift"` etc.

### 4.9 Auto-numbering with manual override **[shipped]**
**As a** Surveyor, **I want** automatic `P001, P002…` naming that I can override **so that** I stay consistent with my firm's naming scheme.

### 4.10 Volume-button quick record in glove mode **[shipped]**
**As a** Field Technician, **I want** the volume-up key to record a point **so that** I can work with gloves or hold a pole without touching the screen.

---

## 5. Project & Point Management

### 5.1 Multiple projects **[shipped]**
**As a** Surveyor, **I want** separate projects for each job **so that** export files don't mix sites.
- Create, rename, delete project.
- One project is "active" at a time.

### 5.2 Per-point browser **[shipped]**
**As a** Surveyor, **I want** to scroll the list of all points in the project **so that** I can audit what I've recorded today.
- Shows ID, EGSA87 E/N, H, fix badge, σH.

### 5.3 Edit an existing point **[shipped]**
**As a** Surveyor, **I want** to correct remarks or AH on a stored point **so that** typos don't live forever in the export.

### 5.4 Delete a point with confirmation **[shipped]**
**As a** Surveyor, **I want** destructive delete to require confirmation **so that** I never lose a point by thumb-tap.

### 5.5 Attach a photo to a point **[shipped]**
**As a** Surveyor, **I want** to snap a photo that is stored alongside the point **so that** the office has visual context for each measurement.
- Uses FileProvider; photo path stored with the point.

### 5.6 Bulk-delete / bulk-export planned **[planned]**
**As a** Surveyor, **I want** to select multiple points and delete or export them **so that** I can split a day's work into deliverables.

---

## 6. Stakeout

### 6.1 Enter a target by coordinates **[shipped]**
**As a** Surveyor, **I want** to type E/N/(H) of a design point **so that** I can stake it out in the field.

### 6.2 Pick a stakeout target from a trig point on the map **[shipped]**
**As a** Trig Point Volunteer, **I want** to tap a GYS benchmark and hit "Stakeout" **so that** I can walk to it without copying numbers.

### 6.3 Live ΔE / ΔN / ΔH, distance, and bearing **[shipped]**
**As a** Field Technician, **I want** big readable deltas and a compass arrow **so that** I converge on the target efficiently.
- ΔE/ΔN/ΔH bands green/orange/red by magnitude.

### 6.4 Immersive full-screen stakeout HUD **[shipped]**
**As a** Field Technician, **I want** a distraction-free full-screen compass **so that** I don't have to squint at a cluttered map in the sun.

### 6.5 Picture-in-Picture floating stakeout **[shipped]**
**As a** Surveyor, **I want** to keep stakeout visible while using another app (e.g. a plan in a PDF viewer) **so that** I don't lose guidance when switching tools.

### 6.6 Nearby-target picker **[shipped]**
**As a** Surveyor, **I want** a "nearby targets" list when stakeout is open **so that** I can move to the next pin without going back to the map.

### 6.7 Haptic stakeout guidance **[planned]**
**As a** Field Technician, **I want** directional vibration (stronger in the target direction) **so that** I can navigate without looking at the phone at all.

---

## 7. Coordinate Transformation Panel

### 7.1 Convert a WGS84 lat/lon/h to EGSA87 manually **[shipped]**
**As a** Geodesist, **I want** to type coordinates and see the EGSA87 output **so that** I can verify numbers from external sources.

### 7.2 Inspect the full pipeline step-by-step **[shipped]**
**As a** Geodesist, **I want** to see every intermediate stage (HTRS07 XYZ → Helmert → EGSA87 XYZ → TM87 → grid corrections → output) **so that** I trust the result.

### 7.3 See the active Helmert parameters **[shipped]**
**As a** Geodesist, **I want** the published HEPOS 7-parameter Helmert displayed **so that** I can confirm the app uses the official datum shift.

### 7.4 See the active projection parameters **[shipped]**
**As a** Geodesist, **I want** the TM87/TM07 parameters visible **so that** I can compare against the official specification.

### 7.5 See the loaded correction grid info **[shipped]**
**As a** Geodesist, **I want** version, resolution and coverage of the dE/dN correction grids **so that** I know which HEPOS grid version is active.

---

## 8. Geoid & Orthometric Heights

### 8.1 Greek HEPOS07 geoid is used by default **[shipped]**
**As a** Surveyor, **I want** orthometric height H computed with the Greek geoid, not receiver EGM96 **so that** my H matches GYS benchmark publications.

### 8.2 Toggle geoid source in Settings **[shipped]**
**As a** Surveyor, **I want** to switch between Greek HEPOS07 and receiver EGM96 for H **so that** I can match the datum expected by a specific deliverable.
- Defaults to Greek HEPOS07.
- Fall back to the other source when the preferred one is unavailable.

### 8.3 See geoid grid metadata in the Transform panel **[shipped]**
**As a** Geodesist, **I want** the geoid grid version, bounds, resolution and source visible **so that** I can document my vertical datum in reports.
- Shows `HEPOS07`, `408 × 422 nodes`, `2000 m cell`, TM07 coverage, Ktimatologio/NTUA source, licence.
- Also shows which source (Greek vs receiver) is active for H.

### 8.4 Geoid coverage warning outside Greece **[planned]**
**As a** Surveyor working near the border, **I want** a visible warning when I step outside the HEPOS07 grid bounds **so that** I don't unknowingly rely on receiver EGM96 there.

---

## 9. Trig Points (GYS / vathra.xyz Layer)

### 9.1 Browse 25,259 Greek trig points on the map **[shipped]**
**As a** Trig Point Volunteer, **I want** all GYS pillars drawn on the map **so that** I can find nearby ones without a separate browser.
- Loaded lazily by map viewport via api.vathra.xyz.

### 9.2 Colour-code trig points by status **[shipped]**
**As a** Trig Point Volunteer, **I want** colours for OK / DAMAGED / DESTROYED / MISSING / UNKNOWN **so that** I can prioritise field trips.

### 9.3 Tap a trig point for details **[shipped]**
**As a** Trig Point Volunteer, **I want** name, GYS ID, elevation and published EGSA87 to appear on tap **so that** I have everything I need before I move.

### 9.4 Verify measured vs published **[shipped]**
**As a** Trig Point Volunteer, **I want** a "Verify" action that compares my current position to the published coordinates **so that** I can report residuals back to the database.
- Residuals dialog shows ΔE, ΔN, ΔH, horizontal residual, fix quality, σH, sat count.

### 9.5 Offline cache of trig points **[shipped]**
**As a** Trig Point Volunteer, **I want** the points I've loaded once to persist **so that** I can work in areas without data coverage.
- Backed by Room DB; re-syncs when online.

### 9.6 Filter trig points by status **[planned]**
**As a** Trig Point Volunteer, **I want** to show only `MISSING / UNKNOWN` **so that** I focus on points that still need a survey.

---

## 10. Basemap & Map Interaction

### 10.1 Vector basemap localised in Greek **[shipped]**
**As a** Surveyor, **I want** Greek place names and labels **so that** the map reads naturally in the field.
- Style derived from `@protomaps/basemaps` LIGHT with `lang: 'el'`.

### 10.2 Orthophoto overlay (Ktimatologio WMS) **[shipped]**
**As a** Surveyor, **I want** to switch to aerial imagery **so that** I can see real features, boundaries and structures.

### 10.3 Contour lines overlay **[shipped]**
**As a** Surveyor, **I want** elevation contours as an optional overlay **so that** I can plan stakeout across sloping terrain.

### 10.4 Initial camera on my current GPS location **[shipped]**
**As a** Field Technician, **I want** the map to open where I am **so that** I don't have to pan from the middle of the Aegean on every launch.
- Uses last-known GPS at zoom 15, falls back to Greece overview if no fix.

### 10.5 Layer toggle menu **[shipped]**
**As a** Surveyor, **I want** one place to toggle trig points, orthophoto and contours **so that** I can declutter the map as needed.

### 10.6 Compass, zoom in/out, follow-me **[shipped]**
**As a** Field Technician, **I want** standard map controls **so that** the app behaves predictably.

### 10.7 Offline basemap packs **[planned]**
**As a** Surveyor, **I want** to pre-download tiles for a job area **so that** I have a basemap in the mountains without signal.

---

## 11. Data Export

### 11.1 CSV export with fix-quality metadata **[shipped]**
**As a** Surveyor, **I want** a CSV that includes coordinates, accuracies, fix type, sats, HDOP, averaging, timestamps and remarks **so that** I can evaluate point quality in the office.

### 11.2 GeoJSON export **[shipped]**
**As a** Surveyor, **I want** a GeoJSON with geometry + properties **so that** I can open the result directly in QGIS.

### 11.3 DXF export **[shipped]**
**As a** Surveyor, **I want** a DXF of points (2D or 3D) **so that** I can import directly into AutoCAD.

### 11.4 Shapefile export **[shipped]**
**As a** Surveyor, **I want** an ESRI Shapefile including points, lines and polygons **so that** I can hand off deliverables to clients that only accept .shp.

### 11.5 Share via Android share sheet **[shipped]**
**As a** Field Technician, **I want** a Share button after export **so that** I can email or cloud-upload the file without a file manager.

### 11.6 Choose output coordinate system **[shipped]**
**As a** Surveyor, **I want** to pick EGSA87 or WGS84 before export **so that** I match each client's expectation.

### 11.7 KML / GPX export **[planned]**
**As a** Trig Point Volunteer, **I want** KML/GPX output **so that** I can load into Google Earth or handheld GPS units.

---

## 12. Data Import

### 12.1 Import a CSV into an existing project **[shipped]**
**As a** Surveyor, **I want** to bring in a point list from a previous session or from a colleague **so that** I can stake out against it.
- Detects legacy vs current CSV format automatically.
- Invalid rows are skipped with a count in the summary.

### 12.2 Import GeoJSON / DXF / Shapefile **[planned]**
**As a** Surveyor, **I want** to import design files from the office **so that** I can stake out a plan the engineer sent.

---

## 13. Display & Accessibility

### 13.1 Coordinate display format **[shipped]**
**As a** Surveyor, **I want** EGSA87 E/N, WGS84 decimal, or WGS84 DMS formats **so that** numbers match my deliverable.

### 13.2 Dark mode with AMOLED option **[shipped]**
**As a** Field Technician, **I want** a true-black dark mode **so that** I save battery on OLED phones during all-day field work.

### 13.3 Glove mode **[shipped]**
**As a** Field Technician, **I want** 64 dp tap targets and larger type **so that** I can operate the app with gloves on.

### 13.4 Volume-button triggers **[shipped]**
**As a** Field Technician, **I want** volume up/down to trigger record/undo **so that** I can work without touching the screen.

### 13.5 High-contrast point labels **[shipped]**
**As a** Surveyor, **I want** white labels with black halo on map points **so that** they stay legible over orthophoto.

### 13.6 Ambient-adaptive theme **[planned]**
**As a** Field Technician, **I want** auto light/dark switching from the ambient sensor **so that** the screen is always comfortable without thinking about it.

---

## 14. Transformation Engine (lib-transform)

### 14.1 Reusable JVM library **[shipped]**
**As a** Geodesist integrating HEPOS elsewhere, **I want** `lib-transform` with no Android dependencies **so that** I can use it in desktop tools or tests.

### 14.2 Deterministic, testable API **[shipped]**
**As a** Geodesist, **I want** `HeposTransform.forward()` / `forwardDetailed()` covered by unit tests against published anchor points **so that** I can trust the output.

### 14.3 Pluggable correction grids **[shipped]**
**As a** Geodesist, **I want** the library to accept `InputStream` for dE/dN/geoid grids **so that** the app layer controls where they come from (assets, download, user file).

### 14.4 Swappable geoid precedence **[shipped]**
**As a** Geodesist, **I want** `forwardDetailed(preferReceiverGeoid = true)` **so that** I can drive the Greek-vs-EGM96 choice from my own UI.

### 14.5 Grid metadata accessor **[shipped]**
**As a** Geodesist, **I want** `HeposTransform.geoidGridMetadata` **so that** I can show coverage/resolution in my app without reparsing the .grd file.

---

## 15. App-Level

### 15.1 Offline operation **[shipped]**
**As a** Surveyor, **I want** everything to work with no internet once I've cached tiles and trig points **so that** I can survey on mountain ridges.

### 15.2 About + licence + source code link **[shipped]**
**As an** Admin, **I want** version, licence (AGPLv3) and GitHub link visible **so that** I can audit what's deployed and update responsibly.

### 15.3 No telemetry / no cloud sync **[shipped]**
**As a** Surveyor, **I want** assurance that survey data never leaves my device unless I export **so that** I meet client confidentiality requirements.

### 15.4 Privacy & Terms screens **[shipped]**

### 15.5 Crash reporting opt-in **[planned]**
**As an** Admin, **I want** an explicit opt-in to crash reports **so that** I can help upstream without being surveilled by default.

---

## 16. Roadmap-only (larger bets, not yet scheduled)

### 16.1 Radial / pie context menu on map features **[planned]**
**As a** Surveyor, **I want** a long-press pie menu on map features **so that** I can edit/delete/stakeout/verify in one gesture.

### 16.2 Home-screen widget with live survey status **[planned]**
**As a** Field Technician, **I want** a Glance widget showing fix quality and today's point count **so that** I can check status without opening the app.

### 16.3 Survey coverage heatmap **[planned]**
**As a** Surveyor, **I want** a density overlay of recorded points **so that** I can see which areas still need field work.

### 16.4 Multi-country expansion **[planned]**
**As a** Surveyor in Cyprus / Bulgaria / … , **I want** swappable correction grids and projections **so that** I can use OpenTopo in my country.
