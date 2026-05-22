# ToF Sensor — Deep Well & Shaft Depth Capture

**Status:** Draft spec, no code yet.
**Target release:** v1.10.x (sits under "Advanced Innovations" in the roadmap).
**Owner workflow:** rural cadastre + utilities — irrigation wells, septic tanks,
manholes, archaeological pits, retaining-wall toe surveys.

---

## 1. Problem

A surveyor working a rural Greek site routinely encounters features where the
point the client cares about — the bottom of an irrigation well, the invert of a
manhole, the base of a sump — is **vertically offset from where GNSS can fix**.
GNSS cannot see sky from 6 m underground. Today the workflow is:

1. Stand at the rim, record a GNSS point.
2. Drop a tape, read the depth, write it on paper.
3. Back at the office, hand-edit the CSV to subtract depth from `H`.

This is slow, error-prone, and the depth never reaches the export columns.

The "6+2 m" framing comes from the most common Greek field case: a **6 m
masonry shaft plus a ~2 m sump**, so a sensor with usable range to **~8 m** and
**±2 cm at 6 m** covers the long tail of real wells. Anything shallower (street
manholes at 1–3 m) is comfortably inside that envelope.

## 2. Proposed solution

Add **Time-of-Flight depth capture** as a first-class survey mode. The
surveyor holds the rover plumb over the well opening with the ToF module
clipped to the pole pointing straight down. When the GNSS point is recorded,
the live ToF depth is captured in the **same atomic record** and OpenTopo
stores **two linked points**:

- **Rim point** — full GNSS attributes, `H_rim` from the geoid pipeline.
- **Bottom point** — same `lat/lon/E/N` (the well is assumed vertical), with
  `H_bot = H_rim − depth − antennaHeight`.

The two points are tied by a shared `featureId`, drawn on the map as a single
"well" feature, and exported as a 2-vertex vertical line in DXF/Shapefile (or
as paired rows in CSV with a `depth_m` column).

This avoids inventing a new geometry type — wells reuse the existing
`layerType = "line_vertex"` machinery with `featureId` grouping that already
ships in `PointEntity`.

## 3. Sensor selection

The Android device cannot reach 8 m with its own ToF (the rear-camera dToF on
phones that have one tops out around 5 m and is not exposed as a metric
distance). An **external module** is required.

| Module | Range | Beam | Accuracy @ 6 m | Interface | Notes |
|---|---|---|---|---|---|
| Benewake **TF-Luna** | 0.2–8 m | 2° | ±6 cm | UART/I²C | Default recommendation. €18, 5 g, 5 V. |
| Benewake **TF02-Pro** | 0.1–40 m | 3° | ±6 cm | UART | Overkill range, IP65, €60. Pick when also surveying cliffs or silos. |
| **VL53L1X** breakout | 0.04–4 m | 27° | ±3 cm | I²C | **Insufficient** for 6+2 m. Document explicitly so users don't try. |
| Garmin **LIDAR-Lite v4** | 0.05–10 m | 3° | ±2 cm | I²C/UART | Premium option, €100. |

Reference implementation targets the **TF-Luna** because (a) the 8 m range is
exactly the design envelope, (b) the 2° beam doesn't hit the well wall before
the water surface at typical 0.6–1.0 m shaft diameter, and (c) the 5 V/UART
breakout is trivial to wedge into the same USB-OTG hub as the GNSS receiver.

### Beam geometry sanity check

A 2° full-angle cone subtends a circle of diameter `2 · d · tan(1°) ≈ d/28.6`
at distance `d`. At 8 m the footprint is **~28 cm**, which fits inside any
well wider than ~40 cm. For narrower shafts (drainage pipes, borewells) we
display a warning when the configured `wellDiameter` field is set below the
footprint.

## 4. Transport

Match the existing GNSS dual-transport pattern from `CLAUDE.md`:

> NTRIP RTCM data routed to both BT and USB services (whichever is connected
> accepts)

ToF inverts that direction (data flows **device-bound** instead of
sensor-bound), but the topology is the same:

- **USB serial** — TF-Luna at 115200 bps via FTDI/CP2102 on the same OTG hub
  that already serves the GNSS receiver. `UsbToFService` mirrors
  `UsbGnssService`.
- **Bluetooth SPP** — for users who put the ToF in its own enclosure with a
  HC-05/HM-10 module. `BtToFService` mirrors `BtGnssService`.
- **Manual fallback** — a numeric `Depth (m)` field in the survey panel for
  surveyors without a sensor. This is the only ToF path that ships in the
  first iteration; hardware drivers land in a follow-up minor.

A new repository, `ToFRepository`, exposes a single `depth: StateFlow<ToFReading?>`
that is the union of "whichever transport is live". This is the exact pattern
`NtripProfileRepository` uses to merge transport-specific state.

## 5. Protocol: TF-Luna UART frame

The TF-Luna emits 9-byte little-endian frames at 100 Hz by default:

```
0x59 0x59  dist_lo dist_hi  amp_lo amp_hi  temp_lo temp_hi  checksum
```

- `dist` = centimetres, `0–800` valid, `0xFFFF` = no return.
- `amp` = signal strength; below `100` the reading is junk (drop it).
- `temp` = `(raw / 8) − 256` °C — used only for cold-weather sanity logging.
- `checksum` = low byte of the sum of bytes 0–7.

`ToFFrameParser` is a pure-Kotlin/JVM class in `lib-transform` (no Android
deps) so it's unit-testable on JVM, matching the project's convention.

A 10-sample median filter runs in front of the StateFlow to suppress water-
surface ripple. Tap the rim; the surface settles in well under a second, so
a 10-sample window at 100 Hz = 100 ms latency is invisible to the surveyor.

## 6. Data model

Extend `PointEntity` (DB v8 → v9 migration). Both rim and bottom rows live
in the existing `points` table; the new columns are nullable to stay
backward-compatible with v1–v8 captures.

```kotlin
@ColumnInfo(name = "wellRole") val wellRole: String? = null,
    // null = ordinary point, "rim", "bottom"
@ColumnInfo(name = "depthMeters") val depthMeters: Double? = null,
    // populated on rim row; null on bottom (computed)
@ColumnInfo(name = "depthSigma") val depthSigma: Double? = null,
    // 1-σ from the 10-sample window
@ColumnInfo(name = "wellDiameter") val wellDiameter: Double? = null,
    // user-entered, drives the beam-footprint warning
```

`layerType` stays `"line_vertex"` for both rows; the well is just a 2-vertex
vertical line. `featureId` groups them. Drawing code in `MapLayer.kt`
recognises the well by checking `wellRole != null` on either endpoint and
renders the feature as a single circled-cross glyph at the rim (not as a
polyline) since the two endpoints share E/N.

## 7. UI

### 7.1 Survey panel — new mode

The existing `Point | Line | Polygon` `ButtonGroup` in `SurveyPanel`
acquires a fourth entry: **`Well`**. Selecting it:

1. Reveals a **Depth** card above the record button:
   - Live numeric readout (54 sp `MonoCoord`, matching the immersive
     stakeout style from v2): `6.42 m`.
   - 1-σ chip below the value: `σ 0.8 cm`.
   - Sensor-status pill on the right: `Live` (mint), `Stale` (ochre, after
     2 s without a frame), `Manual` (slate, when the sensor is absent and
     the user is typing).
2. Adds a small `Well diameter` text field beneath it (defaults to the
   last value used in this project). Sub-footprint diameters paint the
   pill red with `Beam may clip wall — readings unreliable`.
3. Repurposes the **Record** button: tap → atomic capture of (rim GNSS
   epoch, current `depthMeters`, current `depthSigma`). The bottom row is
   written transactionally in the same `Room` insert.

When no GNSS fix is available the record button stays disabled, exactly
like Point mode — the bottom point is only meaningful if the rim point is.

### 7.2 Connection panel — ToF sourcetable row

The GNSS connect screen gains a fourth row beneath the receiver hero card:

```
ToF sensor      ●  TF-Luna @ USB        Live  6.42 m
                ○  No sensor — using manual entry
```

The dot pulses mint on live frames. Tapping the row opens a small sheet
with the same BT/USB/Manual `ButtonGroup` and a one-shot test reading.

### 7.3 Map glyph

Wells render as a 16 dp circle with a tiny down-arrow inside, in
`tertiary` (HEPOS ochre `#B0522C`). Tap → bottom sheet with rim coords,
bottom `H`, depth, σ, sensor model, water/dry tag.

## 8. Exports

| Format | Columns added | Notes |
|---|---|---|
| CSV  | `well_role`, `depth_m`, `depth_sigma_m`, `well_diameter_m` | Existing CSV importer ignores unknown columns, so v1–v2 captures still round-trip. |
| GeoJSON | Same fields under `properties` | Geometry stays `Point` (rim) + `Point` (bottom); link via `featureId`. |
| DXF R12 | One vertical `LINE` per well between rim and bottom, on layer `WELLS` | Plus two `POINT` entities on `WELL_RIM` / `WELL_BOTTOM`. |
| Shapefile | New `WELLS` layer of `PointZ` rim + bottom, `featureId` as foreign key | Keeps the SHP exporter's "one geometry type per file" invariant. |

The shapefile PRJ file is the same EGSA87 WKT the project already emits.

## 9. Computation

```
H_rim    = orthometricHeight from the geoid pipeline (Greek HEPOS07)
H_bot    = H_rim − depthMeters − antennaHeight
σ_H_bot  = sqrt(σ_H_rim² + depthSigma² + σ_antennaHeight²)
```

The bottom-row `geoidSeparation` column copies the rim's value (the well
is vertical and far smaller than the 2 km geoid cell). E/N copy verbatim.

**Plumb assumption.** We are assuming a vertical shaft. A 1° tilt at 8 m
introduces 14 cm of horizontal offset and 0.1% of depth shortening — both
inside the published accuracy of the sensor. We display a warning if the
phone's `TYPE_ROTATION_VECTOR` pitch deviates by more than 3° from
vertical when the record button is pressed. No correction is applied;
the tilt is logged in `remarks` and the surveyor decides whether to redo.

## 10. Edge cases

- **Water surface vs dry bottom.** The ToF cannot tell. We add a single
  `water | dry` toggle next to the depth readout that the surveyor sets
  by eye; it stores into `remarks` as `bottom:water` or `bottom:dry`.
  Exports get a `bottom_type` CSV column.
- **No return (`0xFFFF`).** Sensor sees only sky, or the bottom is below
  range. Pill turns red, record button disables, helper text reads
  *"No echo — depth out of range or beam clear of bottom"*.
- **Reflective water at oblique angle.** Calm water reflects the beam
  away from the sensor; we may see `amp < 100` even when depth is
  in-range. The median filter rejects this; if the rolling-window failure
  rate exceeds 50% for 2 s we show *"Weak return — try tilting 5° off
  vertical"*. (Tilt correction handled by the rotation-vector warning
  above.)
- **Sensor unplugged mid-survey.** State falls to `Stale` after 2 s and
  the record button re-disables for Well mode (other modes unaffected).
- **Cold weather.** TF-Luna spec: −10 °C to +60 °C. We don't refuse to
  operate below that, but we paint the temperature chip red when the
  reported `temp_c < −10` and add `low_temp_warning` to `remarks`.
- **DB downgrade.** `fallbackToDestructiveMigration` already covers it.
  The forward migration is additive (`ALTER TABLE points ADD COLUMN …`),
  so v8 captures open fine.

## 11. Battery

The TF-Luna draws ~70 mA continuous over the USB-OTG hub. Over an 8 h
day that's 560 mAh on top of the GNSS receiver's draw. On a Pixel 7 with
a 4355 mAh battery the rover-power budget shrinks by ~13%. The sensor
service unregisters whenever the survey mode leaves Well, so the cost is
only paid while actively logging wells.

## 12. Testing

JVM unit tests in `lib-transform`:

- `ToFFrameParserTest` — checksum, range clamp, `0xFFFF` rejection,
  amplitude floor, malformed frame resync, partial-frame buffering.
- `WellDepthComputeTest` — `H_bot` arithmetic, σ propagation, copy-down
  of geoid separation and E/N, antenna-height application.
- `BeamFootprintTest` — diameter-vs-footprint comparison at 1, 4, 8 m.

Instrumented test:

- Record a Well with sensor mocked → assert two rows inserted in one Room
  transaction with shared `featureId`, correct `wellRole` values, and
  `H_bot` matching the computation above to within 1 mm.

## 13. Release plan

1. **v1.10.x-alpha** — DB v9 migration, `wellRole`/`depthMeters` columns,
   `SurveyPanel` Well mode with **manual depth entry only**, CSV/GeoJSON
   exports. No drivers yet. Lowest-risk slice; unblocks the workflow on
   day one.
2. **v1.10.x-beta** — `ToFRepository`, `UsbToFService`, TF-Luna driver,
   live depth readout, Connection-panel row, tilt warning.
3. **v1.10.x** — Bluetooth transport, DXF and Shapefile exports,
   beam-footprint warning, water/dry toggle, map glyph.

Each slice is shippable on its own.

## 14. Out of scope (deliberately)

- **Inclined-borehole geometry.** Wells are assumed vertical. Mining
  shafts and angled drains need a different model (a 3-vertex
  polyline with explicit (Δx, Δy, Δz) at the bottom) — that's a v3.x
  conversation, not this one.
- **Continuous depth logging.** We only capture depth at the moment of
  the record press, not as a time series. Profiling silos / reservoirs
  would need a streaming mode and a different export.
- **Atmospheric / sound-speed correction.** TF-Luna is laser ToF, not
  ultrasonic — temperature and humidity affect it well below the
  ±2 cm spec, so no correction is applied.
- **Phone-native dToF.** Even on Pixel 8 Pro the ToF is camera-bound, not
  exposed as a metric distance, and tops out around 5 m. Document that
  it's *not* a substitute and move on.
