# Development Roadmap

## v0.1.0 — MVP (Current)

- [x] HEPOS transformation engine (lib-transform)
- [x] Bluetooth GNSS receiver connection
- [x] USB-OTG GNSS receiver connection
- [x] NMEA parser (GGA, RMC, GSA, GSV, GST)
- [x] NTRIP v1/v2 client with GGA forwarding
- [x] Survey project and point management (Room database)
- [x] Point recording with epoch averaging
- [x] Stakeout navigation
- [x] Map-centric UI with bottom sheet
- [x] Export: CSV, GeoJSON, DXF
- [x] CI/CD pipeline
- [x] Project documentation

## v0.2.0 — Field Validation

- [ ] Visit 5+ trig points across Greece, validate accuracy
- [ ] Compare with HEPOS_TT Windows tool output
- [ ] Offline map tiles (MBTiles support)
- [ ] Settings screen (averaging duration, accuracy thresholds, units)
- [ ] Receiver profiles (ArduSimple, u-blox, Emlid presets)
- [ ] Skyplot display (satellite constellation view with SNR bars)
- [ ] Auto-reconnect for Bluetooth on connection loss

## v0.3.0 — Advanced Survey

- [ ] Line and polygon recording (vertex collection, auto-close)
- [ ] Area computation for polygons
- [ ] Custom attribute fields per project layer
- [ ] Import stakeout points from CSV or GeoJSON
- [ ] WMS/WMTS layer support (Ktimatologio orthophoto)
- [ ] Shapefile export (SHP/DBF/SHX/PRJ)

## v0.4.0 — Polish

- [ ] Proper app icon and branding
- [ ] Onboarding / first-run tutorial
- [ ] Dark mode optimization
- [ ] Localization (Greek, English)
- [ ] Photo attachment for recorded points
- [ ] Mock location provider option

## v1.0.0 — Public Release

- [ ] Google Play Store listing
- [ ] F-Droid publication
- [ ] GitHub Release with signed APK
- [ ] Complete user documentation
- [ ] Kastellorizo grid support (separate TM07 parameters)

## Future

- [ ] Post-processing support
- [ ] Custom Helmert parameters for other datums
- [ ] User-provided correction grids (other countries)
- [ ] Coordinate calculator (standalone transform tool)
- [ ] Tablet-optimized layout
