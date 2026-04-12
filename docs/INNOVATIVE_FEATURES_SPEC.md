I now have a thorough understanding of the entire OpenTopo codebase, its architecture, theme system, existing UI components, state management, and technical foundations. Let me produce the comprehensive specifications.

---

# OpenTopo Innovative Feature Specifications

## 1. AMBIENT-ADAPTIVE THEME

**Ratings:** User Impact: 7/10 | Implementation Difficulty: 5/10 | Uniqueness: 9/10

No existing GNSS survey app adapts its theme in real time based on ambient light. Surveyors constantly move between bright open fields and shaded interiors; a manual toggle is a nuisance when your hands hold a pole.

### User Flow

1. User enables "Ambient-Adaptive" in SettingsPanel (a new `SegmentedButton` with three states: Light / Dark / Ambient).
2. As the surveyor walks outdoors, the light sensor reads high lux and the app presents the light theme with the existing teal-green `LightColorScheme`.
3. When the surveyor enters a building, tunnel, or dense tree canopy, lux drops below the low threshold and the app transitions smoothly to `DarkColorScheme`.
4. If the user toggles "Field Mode" (a secondary switch below the theme selector), the app additionally: increases all body/label text by 4sp, enlarges all touch targets to a 64dp minimum, enables haptic feedback on every interactive element, and thickens outline strokes by 1dp.
5. The transition is never abrupt: a crossfade animation of 600ms (using the existing `MotionScheme.expressive()` spring spec) blends color schemes without jarring the map view.
6. A small indicator in the FixStatusPill area shows a tiny sun or moon icon reflecting the currently active mode, so the user always knows which mode is engaged.

### Technical Implementation

**Sensor Reading:**
- Register `SensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)` with `SENSOR_DELAY_NORMAL` (200ms sampling). This is the standard ambient light sensor on every Android device since API 3.
- Wrap the listener in a `SensorEventListener` that posts lux values to a `MutableStateFlow<Float>` inside a new `AmbientLightMonitor` class.
- Use `SensorManager.SENSOR_DELAY_UI` (60ms) only during transition windows to get faster reads; drop back to `SENSOR_DELAY_NORMAL` in steady state to save battery.

**Lux Thresholds with Hysteresis:**
- Switch to dark when lux drops below **30 lux** and remains below for **3 consecutive seconds** (debounce).
- Switch to light when lux rises above **100 lux** and remains above for **3 consecutive seconds**.
- The gap between 30 and 100 is the hysteresis band -- prevents flicker when standing at a doorway or under dappled tree shade.
- These values are configurable in UserPreferences via two `intPreferencesKey` entries (`ambient_dark_threshold` and `ambient_light_threshold`), exposed as a collapsed "Advanced" section.

**What Changes Between Modes:**

| Property | Light Mode | Dark Mode | Field Mode (additive) |
|---|---|---|---|
| Color scheme | `LightColorScheme` | `DarkColorScheme` | No change |
| Survey colors | `LightSurveyColors` | `DarkSurveyColors` | No change |
| Map style URL | Protomaps light | Protomaps dark variant | No change |
| Body font size | 14sp | 14sp | +4sp (18sp) |
| Label font size | 12sp | 12sp | +4sp (16sp) |
| Font weight (body) | W400 | W400 | W500 (medium) |
| Touch targets | 48dp minimum | 48dp minimum | 64dp minimum |
| Button height | M3 default | M3 default | 56dp minimum |
| Outline stroke | 1dp | 1dp | 2dp |
| Haptic on press | Only FAB | Only FAB | All buttons/tabs |
| Contrast ratio | 4.5:1 (WCAG AA) | 4.5:1 (WCAG AA) | 7:1 (WCAG AAA) |

**Transition Animation:**
- Use `animateColorAsState` with `tween(durationMillis = 600)` for all `MaterialTheme.colorScheme` values by passing a `darkTheme` state derived from the lux monitor into `OpenTopoTheme`.
- The map style URL swap is instant (MapLibre handles its own crossfade), but trigger it with a 200ms delay after color scheme swap begins so they finish together.
- For Field Mode typography changes, use `animateDpAsState` for sizes and a custom `Modifier.animateContentSize()` on panels.

**Battery Impact:**
- `SENSOR_DELAY_NORMAL` draws approximately 0.5mA continuous. Over 8 hours of fieldwork, this adds roughly 4mAh (negligible on a 4000mAh battery).
- The debounce logic means the app only processes theme changes a few times per day, not every sensor event.
- When survey is inactive (no active project), unregister the sensor entirely.

### UI Mockup Description

In SettingsPanel, below the existing "Coordinate Format" section:

**Section: "Appearance"**
- `SectionLabel` reading "Theme"
- `SegmentedButton` (M3 Expressive) with three segments: a sun icon ("Light"), a moon icon ("Dark"), a combined sun-moon icon with circular arrows ("Ambient"). The active segment uses `primaryContainer` fill.
- When "Ambient" is selected, a new row slides in below (AnimatedVisibility, expandVertically): a `Slider` labeled "Sensitivity" controlling the hysteresis band (30-100 lux range), with tick marks at 30, 50, 70, 100. Default at the midpoint.
- Below the slider: a `FilledTonalButton` labeled "Field Mode" with an icon of a hard hat. When active, its container uses `tertiaryContainer` color. Below it, small helper text: "Larger text, bigger targets, always-on haptic."
- A tiny live lux readout in `labelSmall` / `onSurfaceVariant` at the bottom right: "Ambient: 342 lux" -- useful for debugging/calibration but unobtrusive.

### Edge Cases and Error Handling

- **Devices without light sensor:** `getDefaultSensor(TYPE_LIGHT)` returns null. In this case, hide the "Ambient" segment entirely and show only Light/Dark. No crash path.
- **Sensor returns 0 continuously:** Some devices report 0 lux when the sensor is covered by a case. Treat sustained 0 as "very dark" and switch to dark theme. Add a 10-second timeout: if lux is exactly 0 for 10 seconds, assume sensor is blocked and stop reacting to sensor data (fall back to system dark/light).
- **Race condition during theme switch while recording:** The theme transition must not interrupt SurveyManager. All theme state is UI-only; GnssState and SurveyManager do not depend on theme. The 600ms animation is purely cosmetic.
- **Map style URL swap flicker:** Cache both light and dark Protomaps styles in memory. Pre-load the inactive style's JSON string so the swap is near-instant.

### Accessibility Considerations

- Field Mode's 7:1 contrast ratio meets WCAG AAA, benefiting users with low vision.
- All touch targets in Field Mode (64dp) exceed the 48dp Android accessibility minimum.
- The mode indicator (sun/moon) also has a content description: "Light theme active" or "Dark theme active."
- `TalkBack` announces theme changes: "Switched to dark theme" via `announceForAccessibility`.

### Performance Impact

- Sensor listener adds negligible CPU load (one float comparison per 200ms).
- Theme recomposition on switch: limited to color/typography changes in the Compose tree. The MapLibre map (AndroidView) is unaffected by recomposition; only the style URL changes.
- Field Mode typography changes cause a one-time measure/layout pass. Since the app is single-screen with a bottom sheet, this affects only the visible panel.

### M3 Expressive Components

- `SegmentedButton` (theme selector)
- `Slider` (sensitivity control)
- `FilledTonalButton` (Field Mode toggle)
- `SectionLabel` (reuse existing component)
- `AnimatedVisibility` with `MotionScheme.expressive()` spring for reveal

---

## 2. HAPTIC STAKEOUT GUIDANCE

**Ratings:** User Impact: 10/10 | Implementation Difficulty: 6/10 | Uniqueness: 10/10

No GNSS survey app encodes directional guidance into vibration patterns. Surveyors currently must look at their phone screen while walking to a target, which is awkward and dangerous on construction sites.

### User Flow

1. User sets a stakeout target as they currently do (StakeoutPanel -- enter EGSA87 coordinates or select a trig point).
2. User taps a new toggle button "Haptic Guide" at the top of the active stakeout display. Three settings appear in a `SegmentedButton`: Off / Minimal / Full.
3. On "Minimal": only distance pulses. The phone vibrates with increasing frequency as the user approaches the target -- like a Geiger counter. No directional encoding.
4. On "Full": directional vibration patterns plus distance pulses plus optional audio cues.
5. The user puts the phone in their pocket or clips it to their belt. Holding the GNSS pole, they walk. Distinct vibration patterns communicate: "go left", "go right", "go forward", "go back", "on target."
6. As the user gets closer, pulse frequency accelerates from 1 Hz (far) to 5 Hz (close) to a sustained buzz (on target).
7. A final sustained 800ms vibration signals "on target" (within 0.10m tolerance).

### Pattern Design -- Direction Encoding

Direction is encoded using pulse count and rhythm, not vibration motor position (since most phones have a single motor):

| Direction | Pattern | Duration | Human Mnemonic |
|---|---|---|---|
| Forward (North) | Single long pulse | 200ms on | "Go straight" |
| Left (West) | Two short pulses | 80ms on, 60ms off, 80ms on | "Tap-tap = turn" |
| Right (East) | Three short pulses | 80ms on, 40ms off, 80ms on, 40ms off, 80ms on | "Tap-tap-tap = turn more" |
| Back (South) | One long, one short | 200ms on, 80ms off, 80ms on | "Long-short = reverse" |
| On target | Sustained buzz | 800ms on | "You're here" |

The direction is computed from `StakeoutResult.bearingDeg` relative to the device's compass heading. The raw bearing from the stakeout gives "which direction should I go?" and the device orientation sensor gives "which direction am I facing." The difference is the corrective heading.

**Distance Encoding (pulse frequency):**

| Distance to Target | Repeat Interval | Feel |
|---|---|---|
| > 10m | 2000ms between direction pulses | Slow, relaxed |
| 5-10m | 1200ms | Quickening |
| 2-5m | 700ms | Alert |
| 0.5-2m | 400ms | Urgent |
| 0.1-0.5m | 200ms | Rapid-fire |
| < 0.1m | Sustained 800ms buzz | On target |

### Technical Implementation

**VibrationEffect Patterns:**

```
Forward:  VibrationEffect.createWaveform(longArrayOf(0, 200), intArrayOf(0, 200), -1)
Left:     VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80), intArrayOf(0, 200, 0, 200), -1)
Right:    VibrationEffect.createWaveform(longArrayOf(0, 80, 40, 80, 40, 80), intArrayOf(0, 200, 0, 200, 0, 200), -1)
Back:     VibrationEffect.createWaveform(longArrayOf(0, 200, 80, 80), intArrayOf(0, 200, 0, 200), -1)
OnTarget: VibrationEffect.createOneShot(800, 255)
```

For API 26+ (which matches the minSdk 26): use `VibrationEffect.createWaveform()`. The `amplitudes` array uses 200 (out of 255) for a strong but not jarring feel.

For devices with rich haptic engines (Pixel 6+, Samsung S21+): use `VibrationEffect.createComposition()` with `PRIMITIVE_TICK` and `PRIMITIVE_CLICK` for crisper, more distinguishable patterns. Detect capability via `Vibrator.areAllPrimitivesSupported()`.

**Direction Computation:**

The corrective direction requires fusing the stakeout bearing with the device's compass heading:
1. Use `SensorManager` with `TYPE_ROTATION_VECTOR` (fused accelerometer + magnetometer + gyro) to get the device's azimuth (heading).
2. Compute `correctionAngle = stakeoutBearing - deviceHeading`. Normalize to -180..+180.
3. Map angle ranges:
   - -30 to +30: Forward
   - +30 to +150: Right
   - -30 to -150: Left
   - +150 to +180 or -150 to -180: Back

**Audio Cues (Optional, paired with Full mode):**

- Use `ToneGenerator` (already imported in MainMapScreen) for simple audio tones.
- Forward: 800Hz beep (100ms)
- Left: two 600Hz beeps
- Right: three 1000Hz beeps
- On target: 1200Hz sustained 500ms
- Audio can be toggled independently. Useful in noisy construction environments where haptic is felt through gloves but audio carries over machinery noise.

**Power Consumption:**

- Vibration motor: ~100mA per active vibration. With a 200ms pulse every 1200ms (typical mid-range), duty cycle is ~17%, averaging ~17mA. Over a 30-minute stakeout session, this consumes about 8.5mAh. Acceptable.
- Compass sensor: ~3mA continuous. Registered only while haptic stakeout is active.
- Optimization: when distance > 50m, reduce update rate to every 5 seconds (the user is far away and direction doesn't matter at that resolution).

### UI Mockup Description

When a stakeout target is active, a new row appears between the compass arrow and the delta card:

- A `SegmentedButton` (3 segments, small): "Off" | "Minimal" | "Full"
  - "Off": no haptic, grey icon
  - "Minimal": pulse icon in `secondary` color
  - "Full": wave icon in `primary` color
- When "Full" is active, an additional row below with a `Switch` labeled "Audio cues" (default off). Uses `onSurfaceVariant` text.
- A live visualization below the compass arrow: a small horizontal bar (120dp wide, 4dp tall) showing a gradient from the last pulse pattern. Left side glows for "left" direction, right for "right", full bar for "forward", center dot for "on target." Uses `recordingActive` color with animated alpha pulses. This gives visual confirmation that haptic is working, even when the phone is briefly visible.

### Edge Cases and Error Handling

- **No compass sensor:** Some cheap devices lack a magnetometer. Detect via `SensorManager.getDefaultSensor(TYPE_ROTATION_VECTOR) == null`. Fall back to distance-only mode (Minimal) and grey out the "Full" option with a tooltip: "Direction not available: device has no compass."
- **Magnetic interference near steel structures:** Compass heading can be wildly inaccurate near rebar/steel. Detect erratic heading (variance > 30 degrees over 2 seconds) and automatically fall back to distance-only mode. Show a snackbar: "Compass unreliable -- using distance only."
- **Phone in pocket (screen off):** The vibrator works regardless of screen state. Use a `WakeLock` (PARTIAL_WAKE_LOCK) to keep the CPU active during haptic stakeout. Release it when the user taps "Clear Target."
- **User is at target but accuracy is poor:** The "on target" sustained vibration should only fire when `distance < 0.10m AND horizontalAccuracy < 0.05m`. If accuracy is poor, pulse normally but add a distinct pattern (a "sputtering" feel): `longArrayOf(0, 50, 30, 50, 30, 50)` to indicate uncertainty.
- **Simultaneous NTRIP disconnect alert:** The NTRIP disconnect vibration pattern (already implemented: `longArrayOf(0, 100, 100, 100, 100, 400)`) must not collide with haptic guidance. Queue-based approach: if NTRIP alert fires, pause haptic guidance for 2 seconds, play the alert, resume guidance.

### Accessibility Considerations

- Haptic patterns are inherently accessible -- they don't require vision.
- For hearing-impaired users, haptic mode provides directional guidance that audio-only apps cannot.
- The on-screen visualization bar is a fallback for users who cannot feel vibrations (thick gloves, prosthetic hands).
- Content descriptions on the SegmentedButton segments: "Haptic stakeout off," "Distance pulses only," "Full directional haptic."

### Performance Impact

- The haptic engine runs on a background coroutine with a 200ms-2000ms loop. CPU impact is minimal.
- Compass sensor fusion is handled by the OS; the app only reads the output.
- StakeoutResult is already computed at 1Hz in the existing `Stakeout` class; no additional computation needed.

### M3 Expressive Components

- `SegmentedButton` (mode selector)
- `Switch` (audio cues toggle)
- `Surface` with custom `Canvas` (pulse visualization bar)
- `AnimatedVisibility` for revealing audio toggle when "Full" is selected

---

## 3. RADIAL / PIE CONTEXT MENU

**Ratings:** User Impact: 8/10 | Implementation Difficulty: 7/10 | Uniqueness: 9/10

Professional survey apps use boring dropdown context menus. A radial menu centered on the long-press point keeps the user's finger near the action and the map feature, requires less eye movement, and feels spatial.

### User Flow

1. User long-presses (500ms) on a survey point marker on the map.
2. A circular menu fans out from the touch point, with 6 wedge-shaped slices radiating outward.
3. Without lifting their finger, the user drags toward the desired action. The highlighted slice scales up slightly and shows its label. Haptic click on each slice boundary crossing.
4. Releasing the finger on a slice triggers the action. Releasing at the center (dead zone) dismisses the menu.
5. For trig points: a different set of 3 actions. For lines/polygons: another set of 4 actions.

### Action Sets

**Survey Points (6 slices):**

| Position | Icon | Action | Description |
|---|---|---|---|
| 12 o'clock (top) | pencil | Edit | Opens edit dialog for pointId, remarks |
| 2 o'clock | crosshair | Stakeout-to | Sets this point as stakeout target |
| 4 o'clock | ruler | Measure-from | Starts distance measurement from this point |
| 6 o'clock (bottom) | info | Details | Shows full point metadata card |
| 8 o'clock | camera | Photo | Opens camera to attach photo |
| 10 o'clock | trash | Delete | Confirms and deletes with undo snackbar |

**Trig Points (3 slices, wider):**

| Position | Icon | Action |
|---|---|---|
| 12 o'clock | navigation arrow | Navigate-to (stakeout) |
| 5 o'clock | info | Details (name, coords, elevation, GYS sheet, status) |
| 9 o'clock | flag | Report condition (opens vathra.xyz submission) |

**Lines/Polygons (4 slices):**

| Position | Icon | Action |
|---|---|---|
| 12 o'clock | nodes | Edit vertices |
| 3 o'clock | ruler | Measure (length/area) |
| 6 o'clock | share | Export this feature |
| 9 o'clock | trash | Delete with undo |

### Visual Design

- **Outer radius:** 120dp from touch center.
- **Inner dead zone:** 30dp radius. Finger inside this zone = no selection.
- **Slice rendering:** Each slice is a sector of the circle. Inactive slices: `surfaceContainerHigh` at 92% opacity with 1dp `outlineVariant` borders. Active/hovered slice: `primaryContainer` fill with `primary` icon tint, scaled to 110%.
- **Icons:** 24dp M3 outlined icons in `onSurfaceVariant` (inactive) or `onPrimaryContainer` (active).
- **Labels:** `labelMedium` text outside each slice's outer edge, appearing only when the slice is hovered. Uses `AnimatedVisibility` with `fadeIn + scaleIn`.
- **Background:** A semi-transparent scrim (`scrim.copy(alpha = 0.32f)`) covers the rest of the screen, blurring the map slightly.
- **Animation:** Menu appears with a `spring(dampingRatio = 0.7f, stiffness = 400f)` scaling from 0 to 1 around the touch point. Each slice fans in with a 30ms stagger. Dismissal is a quick `tween(150ms)` scale to 0.

### Compose Implementation Approach

The radial menu is a full-screen overlay composable rendered on top of the map:

1. **Gesture detection:** On the MapLibre `AndroidView`, use `onMapLongClick` (already available) to detect the feature. When a feature is hit-tested (using `mapRef.queryRenderedFeatures(screenPoint, "survey-points-layer")`), capture the screen coordinates and feature type.
2. **Overlay composable:** A `Box(Modifier.fillMaxSize())` placed in the `BottomSheetScaffold` content above the map. Uses `pointerInput(Unit)` with `detectDragGestures` to track the finger position relative to the menu center.
3. **Canvas drawing:** A single `Canvas(Modifier.fillMaxSize())` draws all slices as `Path` arcs using `drawPath`. Each slice path is constructed with `Path.arcTo` and two radial lines.
4. **Hit testing:** On drag, compute the angle from the center and the distance. If distance > 30dp and < 120dp, the sector at that angle is "hovered." Use `atan2(dy, dx)` to find the angle, then map to slice index: `sliceIndex = ((angle + PI) / (2 * PI / sliceCount)).toInt()`.
5. **Haptic on boundary crossing:** Track the previous hovered slice index. When it changes, fire `HapticFeedback.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)`.
6. **Action dispatch:** On finger lift, if a slice is hovered, invoke the corresponding action lambda passed to the menu composable.

### Edge Cases and Error Handling

- **Near screen edge:** If the touch point is within 120dp of any screen edge, shift the menu center inward so all slices remain fully visible. Compute: `adjustedCenter = center.coerceIn(120dp, screenWidth - 120dp)` for both axes.
- **Small screen (< 360dp width):** Reduce outer radius to 90dp and icon size to 20dp. Detect via `LocalConfiguration.current.screenWidthDp`.
- **Accidental long-press on empty map:** If `queryRenderedFeatures` returns empty, do not show the menu. Show nothing -- not even a toast. The long-press should feel natural.
- **Multiple overlapping features:** If the query returns multiple features (a survey point on top of a line), prefer the point (higher Z-order). If multiple points overlap, show the menu for the topmost one and include a "Next feature" action in the dead zone area (a small icon).
- **Bottom sheet partially expanded:** The radial menu should appear above the bottom sheet scrim. Set `zIndex` on the overlay to ensure it layers above `BottomSheetScaffold`'s sheet content.

### Accessibility Considerations

- The radial menu is inherently pointer-based. For TalkBack users, provide an equivalent `DropdownMenu` (standard linear menu) that activates on double-tap-and-hold. Detect TalkBack via `AccessibilityManager.isTouchExplorationEnabled`.
- Each slice has a content description: "Edit point P001," "Delete point P001," etc.
- The dead zone (dismiss) is announced as "Cancel."
- Sufficient contrast: slice borders are `outlineVariant` (passes 3:1 for non-text elements).

### Performance Impact

- The overlay Canvas redraws only during drag gestures (16ms frames during active touch). When the menu is hidden, the composable is removed from the tree entirely (`if (showMenu)` guard).
- `queryRenderedFeatures` is synchronous on the MapLibre render thread; it returns in < 5ms for typical point densities (< 1000 features).
- No impact on GNSS processing or survey recording.

### M3 Expressive Components

- Custom `Canvas` composable (no standard M3 radial menu exists)
- `Icon` (M3 icons for each action)
- `AnimatedVisibility` with `MotionScheme.expressive()` for label reveal
- `DropdownMenu` (accessibility fallback)

---

## 4. ACCURACY CONVERGENCE ANIMATION

**Ratings:** User Impact: 9/10 | Implementation Difficulty: 4/10 | Uniqueness: 8/10

Some apps show a static accuracy number. None show a live contracting ring around the position dot that visually communicates "your accuracy is converging" in a way that's instinctively understood by any surveyor.

### User Flow

1. User connects to GNSS receiver and obtains a fix. An accuracy ring appears around the blue/green/red position dot on the map.
2. Initially, the ring is large (proportional to the reported accuracy -- e.g., 5m radius maps to ~20px at zoom 18) and red.
3. As NTRIP corrections flow in and accuracy improves, the ring smoothly contracts and transitions through amber to green.
4. When RTK Fix is achieved at < 2cm accuracy, the ring contracts to a tight green halo barely larger than the dot itself. The ring pulses gently (slow alpha oscillation between 0.3 and 0.6) to indicate "stable, high precision."
5. If accuracy suddenly degrades (fix lost, NTRIP disconnect), the ring expands abruptly with a spring overshoot animation, turning red. This immediately catches the eye.

### Color Transition

The color is driven by `SurveyColors.accuracyColor(meters)`, already defined:
- Red (`AccuracyPoor`): accuracy > 0.05m
- Amber (`AccuracyOk`): 0.02m - 0.05m
- Green (`AccuracyGood`): < 0.02m

For the ring specifically, use a continuous interpolation rather than discrete steps:
- Interpolate between red and amber from 5m down to 0.05m.
- Interpolate between amber and green from 0.05m down to 0.02m.
- Solid green below 0.02m.

Use `lerp(colorA, colorB, fraction)` where `fraction` is the normalized position within each band.

### Scale Mapping

The ring radius in screen pixels is computed from the accuracy value and the map's current meters-per-pixel ratio:

```
ringRadiusPx = accuracyMeters / metersPerPixel
```

MapLibre provides `metersPerPixelAtLatitude(lat, zoom)`. At zoom 18, 1 meter is roughly 4 pixels (at equator). So 5m accuracy = 20px ring, 0.02m = 0.08px (clamped to minimum 6dp to remain visible).

Clamp the ring between 6dp (minimum visible) and 80dp (maximum so it doesn't overwhelm the screen at zoom-out).

### Technical Implementation

**MapLibre Layer Approach:**

The accuracy ring is drawn as a MapLibre `CircleLayer` on top of the existing `user-location-glow` layer:

1. Add a new GeoJsonSource `"user-accuracy-ring"` with a single Point (same as user location).
2. Add a `CircleLayer` `"accuracy-ring"` with:
   - `circleRadius`: dynamically updated from accuracy value
   - `circleColor`: dynamically updated from accuracy-to-color interpolation
   - `circleOpacity`: 0.25 for fill, 0.8 for stroke
   - `circleStrokeWidth`: 2dp
   - `circleStrokeColor`: same as fill but full opacity
3. Update every 1 second (matching GNSS update rate) via `setProperties()`.

**Smooth Animation (Spring Physics):**

MapLibre property updates are immediate (no built-in animation). To achieve smooth contraction:

- Maintain a `currentDisplayRadius` in a `MutableStateFlow<Float>`.
- On each accuracy update, compute `targetRadius`. Instead of jumping, use a Kotlin coroutine that applies a spring formula over ~300ms:
  - `damping = 0.8`, `stiffness = 300f`
  - Each frame (16ms), compute the next radius value using the spring equation and call `setProperties()`.
- For sudden accuracy jumps (Float to Fix), the spring will overshoot slightly (ring contracts past target then bounces back), which visually communicates "big improvement."

**Handling Sudden Jumps:**

When `fixQuality` transitions from 5 (Float) to 4 (Fix), the accuracy typically jumps from ~0.5m to ~0.015m. The spring physics handles this naturally:
- The ring contracts rapidly with overshoot.
- Add a one-time "celebration" effect: a brief pulse (expand ring by 20% then contract) using a custom spring with lower damping (0.5).

**Display on Different Map Styles:**

- On street map (light background): ring stroke uses the accuracy color at full opacity. Ring fill at 20% opacity.
- On satellite imagery (dark background): ring stroke at full opacity but with a 1px white outer stroke for contrast. Ring fill at 30% opacity. Detect map style from user selection and adjust opacities accordingly.

### UI Mockup Description

The accuracy ring is centered on the existing user-location dot (a 12dp teal-green filled circle with a 4dp white border). The ring:
- Appears as a translucent colored circle with a solid colored border.
- At 5m accuracy: ~40dp diameter, red, clearly visible "bubble" around the dot.
- At 0.5m accuracy: ~16dp diameter, amber, a close halo.
- At 0.02m: ~8dp diameter, green, barely extends beyond the dot itself. Pulses gently.
- The FixStatusPill at the top of the screen already shows the numeric accuracy. The ring provides the spatial, at-a-glance equivalent.

### Edge Cases and Error Handling

- **No GST data (no accuracy estimate):** Some receivers don't output GST sentences. In this case, `horizontalAccuracyM` is derived from HDOP * 2.5, which is a rough estimate. Show the ring but with a dashed stroke (`circleStrokeDasharray` is not supported in MapLibre GL Native, so use an alternating-opacity animation instead) to indicate "estimated."
- **Accuracy oscillating rapidly:** Apply a simple exponential moving average to the accuracy value: `smoothed = 0.7 * smoothed + 0.3 * new`. This dampens jitter without hiding real changes.
- **Zoom level too high or too low:** At very low zoom (whole city visible), the ring is sub-pixel. Clamp minimum radius to 6dp. At very high zoom, the ring fills the screen. Clamp maximum to 80dp.
- **Internal GPS (phone GPS):** Accuracy is typically 3-10m. The ring will be large and red, which is correct. No special handling needed.

### Accessibility Considerations

- The ring color transition is supplemented by the FixStatusPill text ("RTK Fix, 0.015m"). Color-blind users get the same information from text.
- TalkBack: announce accuracy changes when they cross a threshold. E.g., "Accuracy improved to centimeter level" when crossing below 0.02m.
- The pulsing animation respects `Settings.Global.ANIMATOR_DURATION_SCALE`. If animations are disabled system-wide, show a static ring.

### Performance Impact

- One GeoJsonSource update per second (trivial).
- Spring animation: ~18 frames of `setProperties()` calls per accuracy change (300ms / 16ms). Each call is O(1) on the render thread.
- No impact on GNSS data flow or survey recording.

### M3 Expressive Components

- This feature is purely map-layer based (MapLibre), not Compose UI.
- The FixStatusPill already uses `Surface`, `Text`, and `Canvas` (the fix quality dot).
- The color values reuse `LocalSurveyColors.current.accuracyColor()`.

---

## 5. SURVEY COVERAGE HEATMAP

**Ratings:** User Impact: 7/10 | Implementation Difficulty: 6/10 | Uniqueness: 9/10

Topographic surveyors need even point distribution. No existing GNSS app shows a density overlay revealing spatial gaps in collection.

### User Flow

1. User has recorded 50+ points in the current project.
2. In the map layer switcher (existing layer toggle), a new option appears: "Coverage Heatmap."
3. Toggling it on overlays a transparent colored density field on the map.
4. Dense areas glow yellow-red. Sparse areas are blue. Un-surveyed areas remain transparent.
5. The surveyor immediately sees gaps (transparent zones between surveyed strips) and walks there to collect more points.
6. The heatmap updates live as new points are recorded. When a point is stored, the heatmap smoothly gains a new warm spot at that location.

### Algorithm: Kernel Density Estimation

For each pixel on the heatmap, compute the density as the sum of kernel contributions from all nearby points:

```
density(x, y) = SUM_i( K( ||p_i - (x,y)|| / bandwidth ) )
```

Where `K` is a Gaussian kernel and `bandwidth` controls the smoothing radius.

**Practical approach using MapLibre HeatmapLayer:**

MapLibre GL Native supports `HeatmapLayer` natively. This is the ideal approach:

1. Create a `GeoJsonSource` named `"coverage-heatmap"` from the same survey points.
2. Add a `HeatmapLayer` with:
   - `heatmapRadius`: 30 (pixels, adjustable) -- this is the kernel bandwidth.
   - `heatmapWeight`: 1.0 for all points (equal weighting).
   - `heatmapIntensity`: interpolated by zoom level (higher at low zoom, lower at high zoom).
   - `heatmapColor`: a multi-stop color ramp:
     - 0.0: transparent
     - 0.2: `Color(0x330000FF)` (blue, low alpha)
     - 0.4: `Color(0x6600FF00)` (green)
     - 0.6: `Color(0x99FFFF00)` (yellow)
     - 0.8: `Color(0xCCFF8800)` (orange)
     - 1.0: `Color(0xFFFF0000)` (red)
   - `heatmapOpacity`: 0.6 (controllable via a slider).

3. Update the GeoJsonSource when `activePoints` changes (reuse the existing `LaunchedEffect(activePoints)` block).

**Even-Spacing Utility for Topographic Surveys:**

Beyond visualization, add an "Ideal Grid" overlay toggle that shows a regular grid of dots at a user-specified spacing (e.g., 10m x 10m) over the survey area's bounding box. Points within the grid cell radius turn from grey to green. This makes it trivial to see "which grid cells still need a point."

### UI Mockup Description

In the map layer switcher (the `IconButton` with `Icons.Outlined.Layers` in the top-right of the map):
- Below existing layers (Street Map, Satellite, Contours, Trig Points), add:
  - `Switch` labeled "Coverage Heatmap" with a gradient-bar icon
  - When enabled, an `Slider` appears below it: "Radius" (10-60 pixels, default 30). Uses `secondaryContainer` track color.
  - Below that, another `Switch`: "Point Grid" with a grid icon. When enabled, shows:
    - `TextField` labeled "Spacing (m)" with default "10"
    - A legend: grey dot = "Uncovered", green dot = "Covered"

The heatmap itself appears on the map between the base tiles and the survey point markers. This ensures point labels remain readable on top of the heatmap.

### Performance with 1000+ Points

MapLibre's `HeatmapLayer` uses GPU-accelerated fragment shaders. Performance characteristics:
- **100 points:** < 1ms per frame. Negligible.
- **1,000 points:** ~3ms per frame on mid-range GPU (Adreno 618). Smooth.
- **5,000 points:** ~12ms per frame. Still below the 16ms budget for 60fps.
- **10,000+ points:** May exceed 16ms. Mitigation: downsample the GeoJsonSource by spatial binning (aggregate points within 5m cells) when count exceeds 5,000.

The GeoJsonSource update when a new point is added is O(n) serialization. For 1,000 points, this takes ~5ms. For 5,000+, use incremental updates (add a Feature rather than replacing the entire FeatureCollection). MapLibre supports this via `source.addFeature()` although it requires tracking what's already in the source.

### Edge Cases and Error Handling

- **< 3 points:** The heatmap is meaningless with very few points. Show a tooltip: "Record more points to see coverage." Minimum threshold: 3 points.
- **Points clustered in one spot:** The heatmap will show a single hot spot. This is correct behavior -- it reveals that coverage is concentrated.
- **Mixed layer types:** Only include "point" layer type, not "line_vertex" or "polygon_vertex," since vertices are structural, not survey coverage.
- **Zoom level changes:** `heatmapIntensity` and `heatmapRadius` should scale with zoom to maintain consistent apparent density. Use MapLibre expressions: `interpolate(linear, zoom, 10, 0.5, 18, 1.0)`.

### Accessibility Considerations

- The heatmap's color ramp should be perceivable by color-blind users. The blue-green-yellow-red ramp is problematic for deuteranopia. Alternative ramp for color-blind mode: blue-purple-white-orange-brown. Detect via a user preference in SettingsPanel.
- The heatmap is supplementary visualization; the point list in SurveyPanel shows all data in text form.
- For TalkBack, the heatmap toggle announces "Survey coverage heatmap enabled. Shows density of collected points."

### M3 Expressive Components

- `Switch` (layer toggles)
- `Slider` (radius control)
- `TextField` (grid spacing input)
- `Surface` (layer switcher dropdown container)

---

## 6. VOICE COMMANDS FOR FIELD OPERATION

**Ratings:** User Impact: 9/10 | Implementation Difficulty: 8/10 | Uniqueness: 10/10

No GNSS survey app offers voice-controlled point collection. Surveyors holding a GNSS pole with both hands cannot easily tap a screen. Voice commands are the natural interaction model for field survey.

### User Flow

1. User enables "Voice Control" in SettingsPanel. Chooses language: English or Greek.
2. A small pulsing microphone icon appears in the top-left of the map (next to the FixStatusPill).
3. User activates voice input by pressing the Volume Up button (push-to-talk). The mic icon fills with `primaryContainer` color and a listening animation radiates outward.
4. User speaks: "Store point" (or in Greek: "Αποθηκευση σημειου").
5. The app recognizes the command, confirms via TTS: "Point P042 stored, accuracy 1.5 centimeters," and records the point using `SurveyManager.quickMark()`.
6. The mic icon returns to idle state.

### Command Vocabulary

| English Command | Greek Command | Action | Feedback |
|---|---|---|---|
| "Store point" | "Αποθηκευση σημειου" | `surveyManager.startRecording()` | "Point [ID] stored" |
| "Quick mark" | "Γρηγορο σημαδι" | `surveyManager.quickMark()` | "Quick mark [ID]" |
| "Start line" | "Αρχη γραμμης" | `surveyManager.startFeature()` (line mode) | "Line started" |
| "Add vertex" | "Προσθηκη κορυφης" | `surveyManager.recordVertex()` | "Vertex [N] added" |
| "Finish line" | "Τελος γραμμης" | `surveyManager.finishFeature()` | "Line finished, [N] vertices" |
| "Start polygon" | "Αρχη πολυγωνου" | `surveyManager.startFeature()` (polygon mode) | "Polygon started" |
| "Finish polygon" | "Τελος πολυγωνου" | `surveyManager.finishFeature()` | "Polygon closed, area [X] sq meters" |
| "Code [name]" | "Κωδικος [ονομα]" | Sets remarks on next point | "Code set to [name]" |
| "Stakeout to [ID]" | "Χαραξη σε [ID]" | Activates stakeout to point | "Stakeout to [ID] active" |
| "Undo" | "Αναιρεση" | `surveyManager.undoLastVertex()` | "Last vertex removed" |
| "What's my accuracy?" | "Τι ακριβεια εχω;" | Reads accuracy aloud | "Horizontal accuracy [X] centimeters, [fix type]" |
| "How many points?" | "Ποσα σημεια;" | Reads point count | "[N] points in project [name]" |

### Technical Implementation

**Android SpeechRecognizer for Offline Recognition:**

- Use `SpeechRecognizer.createOnDeviceSpeechRecognizer(context)` (API 31+) for fully offline recognition. This uses the on-device speech model.
- For API 26-30: use `SpeechRecognizer.createSpeechRecognizer(context)` with `EXTRA_PREFER_OFFLINE = true` in the `RecognizerIntent`. This works offline if the user has downloaded the offline language pack.
- Check availability: `SpeechRecognizer.isOnDeviceRecognitionAvailable(context)` for API 31+, or `SpeechRecognizer.isRecognitionAvailable(context)` for older APIs.
- Language setting: set `EXTRA_LANGUAGE` to `"en-US"` or `"el-GR"` based on user preference.

**Push-to-Talk via Volume Button:**

- Override `onKeyDown(KeyEvent.KEYCODE_VOLUME_UP)` in `MainActivity`. When voice control is enabled, intercept the volume button:
  - Volume Up press: start listening.
  - Volume Up release: stop listening (or let the recognizer's end-of-speech detection handle it).
- Use `dispatchKeyEvent` in the Activity to catch the event before the system volume UI appears.
- Important: only intercept when voice control is enabled. Otherwise, normal volume behavior.

**Command Parsing:**

The raw recognized text is matched against the command vocabulary using fuzzy matching:
1. Normalize: lowercase, trim whitespace, remove punctuation.
2. For fixed commands ("store point", "quick mark"), use Levenshtein distance with a threshold of 2 edits (to tolerate slight misrecognitions).
3. For parameterized commands ("code [name]", "stakeout to [ID]"), use regex: `"code\\s+(.+)"` to extract the parameter.
4. For Greek, apply the same approach with Greek normalized text.

**TTS Feedback:**

- Use `TextToSpeech(context, initListener)` with `setLanguage(Locale.US)` or `Locale("el", "GR")`.
- Set speech rate to 1.2x for snappy feedback.
- Queue mode: `QUEUE_FLUSH` so each response replaces any previous ongoing speech.
- TTS works through the phone speaker or connected Bluetooth headset (useful for surveyors wearing earbuds).

### UI Mockup Description

**Idle State:** A 40dp circular `FilledIconButton` in the top-left corner of the map (below the status bar inset), containing a microphone icon (`Icons.Outlined.Mic`) in `onSurfaceVariant` color. Background: `surfaceContainer` at 80% opacity. Subtle drop shadow.

**Listening State:** The button fills with `primaryContainer`. The mic icon turns to `onPrimaryContainer`. Three concentric rings radiate outward from the button at 400ms intervals, using `primary` color at decreasing opacity (0.3, 0.2, 0.1), achieved with `animateFloatAsState` for scale and alpha. Below the button, a small `Surface` pill appears showing the recognized text in real-time (partial results from `onPartialResults`), styled with `labelMedium` and `surfaceContainerHigh` background.

**Processing State:** The mic icon replaces with a `LoadingIndicator` (M3 Expressive) for 200ms while the command is parsed and executed.

**Confirmation State:** The button briefly flashes `tertiaryContainer` (green tint) for success or `errorContainer` for failure, then returns to idle.

**Settings:** In SettingsPanel, a new "Voice Control" section:
- `Switch` labeled "Enable Voice Commands"
- When enabled: `SegmentedButton` for language: "English" | "Greek"
- `Switch` labeled "Audio Feedback (TTS)" (default on)
- Helper text: "Press Volume Up to speak. Commands: Store point, Quick mark, Start line, Undo..."

### Edge Cases and Error Handling

- **Offline model not downloaded:** `SpeechRecognizer` will fail. Detect this and show a dialog: "Download offline speech model for [language] in Android Settings > Google > Languages." Provide a button that opens `Settings.ACTION_INPUT_LANGUAGE_SELECTION`.
- **Ambient noise:** Construction sites are loud. Use `EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS = 1500` to require longer utterances, reducing false triggers. Also, require a confidence score > 0.6 from `RecognitionListener.onResults`.
- **Command not recognized:** TTS responds: "I didn't understand. Try again." Do not execute any action. The mic returns to idle.
- **Voice command during active recording (averaging in progress):** Reject "store point" and "quick mark" commands. TTS responds: "Recording in progress. Please wait."
- **No microphone permission:** Voice control toggle shows a permission rationale dialog. If denied, grey out the toggle.
- **Volume button conflict:** Some devices use Volume Up for accessibility shortcuts. Detect if the system is using Volume Up for another purpose via `AudioManager.isVolumeFixed()` or checking accessibility shortcuts. If conflict detected, offer an alternative: a dedicated on-screen button with long-press activation.

### Privacy

- All speech recognition runs on-device. No audio data leaves the phone.
- Display a clear notice in settings: "All voice processing happens on your device. No audio is sent to the cloud."
- The recognized text is processed ephemerally -- not logged, not stored.

### Accessibility Considerations

- Voice commands are inherently an accessibility feature: they enable hands-free operation.
- The on-screen visualization of recognized text benefits deaf/HoH users who might want to see what was recognized.
- TTS feedback can be routed to a Bluetooth hearing aid.
- The volume-button activation is an alternative to touch-based activation, benefiting users with motor impairments.

### Performance Impact

- `SpeechRecognizer` is system-managed and runs in a separate process. Minimal impact on app process.
- TTS engine initialization: ~200ms on first use. Cache the instance for the session lifetime.
- Command parsing: O(n) string comparison against ~12 commands. Negligible.

### M3 Expressive Components

- `FilledIconButton` (mic button)
- `LoadingIndicator` (processing state)
- `Surface` (partial results pill)
- `Switch` (enable toggle)
- `SegmentedButton` (language selector)
- `AnimatedVisibility` with `MotionScheme.expressive()` for listening state reveal

---

## 7. WEAR OS COMPANION

**Ratings:** User Impact: 8/10 | Implementation Difficulty: 8/10 | Uniqueness: 10/10

No GNSS survey app has a Wear OS companion. A wrist display showing RTK status and a one-tap "Store" button frees the surveyor from pulling out their phone entirely.

### User Flow

1. User installs the OpenTopo Wear OS companion from Play Store (separate APK, same package namespace).
2. On first launch, the watch discovers the phone app via Wearable Data Layer API and pairs automatically.
3. The watch shows a **Tile** on the watch face carousel: a concise display of fix status (green/amber/red dot), accuracy value, and satellite count. Updates every 5 seconds.
4. Tapping the Tile opens the **full watch app**: a single screen with a very large "STORE" button (fills 60% of the round display), the accuracy value above it, and the fix status below.
5. Tapping "STORE" sends a message to the phone, which executes `surveyManager.quickMark()`. The watch vibrates on confirmation.
6. The watch also vibrates when fix status changes (Fix achieved, Fix lost).
7. A **Complication** is available for watch faces: a small dot that shows fix status color (green = RTK Fix, amber = Float, red = no fix).

### Technical Implementation

**Module Structure:**

New Gradle module: `:wear` with `com.google.android.gms:play-services-wearable` dependency. Wear OS 3+ (API 30+), using Compose for Wear OS (`androidx.wear.compose:compose-material3`).

**Wearable Data Layer API:**

Communication between phone and watch uses three mechanisms:

1. **DataItem** (persistent, synced state): The phone writes a `DataMap` to path `/opentopo/gnss-state` containing:
   - `fixQuality` (int)
   - `fixDescription` (String)
   - `horizontalAccuracy` (double)
   - `numSatellites` (int)
   - `lastPointId` (String, for confirmation)
   
   Updated every 1 second when survey is active. The DataItem persists across connection drops and syncs when reconnected.

2. **MessageClient** (one-shot commands): The watch sends messages to path `/opentopo/command`:
   - `"store_point"` -- triggers `quickMark()` on the phone.
   - `"quick_mark"` -- same, alternative trigger.
   
   The phone responds with a message to `/opentopo/response`:
   - `"point_stored:P042"` -- confirmation with point ID.
   - `"error:no_fix"` -- error case.

3. **ChannelClient** (not needed for this use case -- DataItem + Message covers it).

**On the Phone Side:**

In `MainActivity` or a dedicated `WearDataService`, register a `DataClient.OnDataChangedListener` (not needed for this direction) and a `MessageClient.OnMessageReceivedListener`:

```
When message path == "/opentopo/command":
  When payload == "store_point":
    surveyManager.quickMark()
    Send response message with point ID
```

Also, update the DataItem whenever `GnssState` changes:

```
gnssState.position.collect { pos ->
  val dataMap = DataMap().apply {
    putInt("fixQuality", pos.fixQuality)
    putString("fixDescription", pos.fixDescription)
    putDouble("accuracy", accuracy.horizontalAccuracyM ?: -1.0)
    putInt("satellites", pos.numSatellites)
  }
  Wearable.getDataClient(context).putDataItem(
    PutDataMapRequest.create("/opentopo/gnss-state").apply {
      dataMap.putAll(data)
      setUrgent()
    }.asPutDataRequest()
  )
}
```

**Tiles API:**

A `TileService` subclass renders the status tile using Tiles Material components:
- `Text` with accuracy value in large font.
- `Image` resource: a colored dot (generated as a bitmap from the fix quality color).
- `Text` with satellite count.
- Layout: vertical `Column` with center alignment.
- Refresh interval: `TimelineEntry` with 5-second freshness. The Tile re-renders on DataItem change via `TileService.getUpdater().requestUpdate()`.

**Complications API:**

A `ComplicationDataSourceService` provides:
- `SHORT_TEXT` type: fix description ("RTK Fix", "Float", "GPS").
- `RANGED_VALUE` type: accuracy value with min 0, max 5.
- `SMALL_IMAGE` type: colored dot bitmap.

**Watch App Screen:**

Using Compose for Wear OS:
- `ScalingLazyColumn` with a single item:
  - Top: `Text(accuracy, fontSize = 24.sp)` in CoordinateFont (monospace).
  - Center: `Button(onClick = sendStoreCommand, modifier = Modifier.size(120.dp))` -- a large circular green button with a crosshair icon. Uses Wear M3 `FilledTonalButton`.
  - Bottom: `Chip` showing fix status with colored icon.

**Haptic on Watch:**

- Fix achieved (transition to quality 4): `VibrationEffect.createOneShot(500, 200)` -- firm, sustained.
- Fix lost (transition to quality < 4 from 4): `VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1)` -- three quick pulses (alarm).
- Point stored: `VibrationEffect.createOneShot(200, 150)` -- light tap.

### UI Mockup Description

**Tile (round watch face, 384x384 viewport):**
- Background: `surface` color (dark, since most watch faces are dark).
- Top third: "RTK Fix" in `titleMedium`, green (`AccuracyGoodDark`).
- Center: "0.015 m" in `displaySmall`, `CoordinateFont`, white.
- Bottom third: satellite icon + "14 sats" in `bodySmall`.
- Left edge: colored vertical bar (4dp wide, full height) indicating fix quality color.

**Full App (round display):**
- Large circular button centered, 120dp diameter, `primaryContainer` fill. Crosshair icon inside, 48dp. Text "STORE" below the icon in `labelLarge`.
- Above button: "0.015 m" in `headlineMedium`, `CoordinateFont`.
- Below button: Fix status chip: colored dot + "RTK Fix" in `labelMedium`.
- All text uses high contrast for outdoor readability on AMOLED.

**Complication:**
- Short Text: "Fix" or "Float" or "GPS" or "--"
- Icon: 16dp colored circle bitmap.

### Edge Cases and Error Handling

- **Watch not connected to phone:** DataItem will contain stale data. Detect staleness by including a `timestamp` field in the DataMap. If age > 30 seconds, show "Disconnected" on the watch in `errorContainer` color.
- **Phone app not running:** MessageClient will fail to deliver. Catch `ApiException` and show "Start OpenTopo on phone" on the watch.
- **Multiple watches:** The Wearable Data Layer broadcasts to all connected nodes. This is fine -- all watches show the same status.
- **Battery on watch:** Only send DataItem updates when survey is active (project open). When no survey, stop updates. The Tile shows last known state with a "Paused" indicator.
- **Round vs square watch displays:** Use Wear OS shape detection (`isRound`) and adjust layout with `Modifier.padding` to avoid clipping on round screens.

### Battery Optimization

- DataItem updates every 1 second during active survey. Cost: ~5mA on phone (BLE transmission), ~3mA on watch (BLE reception). Over 4 hours: ~20mAh phone, ~12mAh watch.
- When survey is not active: no updates at all. Complication uses cached data.
- Tile refresh: 5 seconds is the minimum practical interval. The Tile system batches updates.

### M3 Expressive Components (Wear OS variants)

- Wear M3 `Button` (large store button)
- Wear M3 `Chip` (fix status display)
- Wear M3 `Text` (accuracy display)
- Tiles Material `Text`, `Image`, `Column`
- `ComplicationDataSourceService`

---

## 8. PiP FLOATING STAKEOUT

**Ratings:** User Impact: 8/10 | Implementation Difficulty: 5/10 | Uniqueness: 9/10

When a surveyor is navigating to a stakeout target but needs to check another app (messaging, reference PDF, calculator), they lose the stakeout guidance. PiP keeps it visible as a floating overlay.

### User Flow

1. User has an active stakeout target and navigates to another app (Home button, recents, or opens a notification).
2. Instead of the app disappearing, the stakeout display shrinks into a small 160x160dp floating window in the corner of the screen.
3. The PiP window shows: a compass arrow pointing toward the target, the distance in large text, and a colored dot for fix status.
4. The window updates live (1Hz) as the surveyor walks.
5. Tapping the PiP window returns to the full OpenTopo app.
6. The PiP window can be dragged to any screen edge.

### Technical Implementation

**Android PiP API:**

PiP mode is supported on API 26+ (matching minSdk). Implementation:

1. In `AndroidManifest.xml`, add to the `MainActivity` declaration:
   ```
   android:supportsPictureInPicture="true"
   android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
   ```

2. In `MainActivity`, when user leaves during active stakeout:
   ```
   override fun onUserLeaveHint() {
     if (hasActiveStakeout) enterPipMode()
   }
   
   private fun enterPipMode() {
     val params = PictureInPictureParams.Builder()
       .setAspectRatio(Rational(1, 1))  // square
       .setActions(listOf(/* custom actions */))
       .build()
     enterPictureInPictureMode(params)
   }
   ```

3. Detect PiP state change:
   ```
   override fun onPictureInPictureModeChanged(isInPipMode: Boolean, config: Configuration) {
     _isInPipMode.value = isInPipMode
   }
   ```

4. When `isInPipMode` is true, swap the Compose content to a simplified stakeout-only layout.

**Custom PiP Layout:**

In PiP mode, the full `MainMapScreen` is replaced by a compact composable:

```
@Composable
fun PipStakeoutDisplay(stakeoutResult: StakeoutResult?, fixQuality: Int) {
    Box(Modifier.fillMaxSize().background(surfaceContainer)) {
        // Compass arrow (Canvas, same logic as StakeoutArrow but simplified)
        // Distance text (displaySmall, CoordinateFont)
        // Fix status dot (8dp colored circle in corner)
    }
}
```

The PiP layout is intentionally minimal:
- No interactive elements (PiP mode doesn't support touch interactions except tap-to-expand and custom remote actions).
- No text input, no scrolling.
- Large, legible text (the 160dp window at 1.5x density is ~107 actual dp visible).

**Custom PiP Actions (Remote Actions):**

PiP supports up to 3 custom actions (shown as overlay icons when the user taps the PiP window):

1. **Store Point** (crosshair icon): `RemoteAction` with `PendingIntent` that triggers `surveyManager.quickMark()` via a BroadcastReceiver.
2. **Clear Stakeout** (X icon): `RemoteAction` that clears the stakeout target.

```
val storeAction = RemoteAction(
    Icon.createWithResource(this, R.drawable.ic_crosshair),
    "Store Point",
    "Record a quick mark",
    PendingIntent.getBroadcast(this, 0, Intent("org.opentopo.STORE_POINT"), PendingIntent.FLAG_IMMUTABLE)
)
```

**Triggering PiP:**

- Automatic: When the user presses Home or switches apps during active stakeout, `onUserLeaveHint()` fires and enters PiP. This is the default Android behavior for PiP-enabled activities.
- The user can also trigger PiP explicitly: a small "minimize" icon button appears in the StakeoutPanel header when a target is active.
- PiP does NOT activate when: no stakeout target is set, or the user presses Back (which should exit the app, not PiP).

### UI Mockup Description

**PiP Window (160x160dp effective, square):**
- Background: `surface` color (adapts to light/dark theme).
- Center: Simplified compass arrow, 80dp diameter. Same arrow path as `StakeoutArrow` but without cardinal labels or outer ring. Just the triangular arrow in the stakeout distance color.
- Below arrow: Distance in `headlineMedium`, `CoordinateFont`, centered. E.g., "12.345 m" in `onSurface` color.
- Top-left corner: 10dp circle in fix quality color (green/amber/red).
- Top-right corner: small target name in `labelSmall`, truncated to 6 characters.
- No borders, no padding waste. Every pixel matters.

**Transition Animation:**
- Android handles the shrink animation automatically (activity window scales down to PiP bounds).
- On expand (tap PiP), the activity restores to full screen with the standard scale-up animation.

### Edge Cases and Error Handling

- **Stakeout cleared while in PiP:** The PiP window should show "No Target" and the arrow disappears. Replace the arrow with a dash icon. After 5 seconds, exit PiP mode automatically by calling `finish()` or setting `setAutoEnterEnabled(false)`.
- **Fix lost while in PiP:** The fix dot turns red. The distance text shows "---" instead of a number. The arrow freezes at last known bearing.
- **Screen rotation in PiP:** PiP windows are always in landscape/portrait based on aspect ratio. Since we set `Rational(1,1)`, it's square and rotation-invariant.
- **Device doesn't support PiP:** Some Go Edition devices disable PiP. Check `packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)` before showing the minimize button. If unsupported, hide the option entirely.
- **GNSS service killed in background:** The GNSS Bluetooth/USB connection runs in a foreground service (or should). If the service is killed, the PiP window shows stale data. Detect via `connectionStatus` flow and show "Disconnected" in the PiP.

### Accessibility Considerations

- PiP mode is inherently visual. For TalkBack users, PiP is less useful since they can't see the floating window. However, TalkBack does announce PiP content if content descriptions are set.
- Set content description on the PiP root view: "Stakeout to [target name], [distance] meters, [direction]."
- The RemoteAction buttons have content descriptions ("Store point", "Clear stakeout") that TalkBack reads.

### Performance Impact

- PiP mode reduces the rendering surface to 160x160dp, massively reducing GPU load compared to full-screen.
- The simplified composable has ~5 composable nodes (vs. hundreds for the full screen). Recomposition is trivial.
- The stakeout computation (already 1Hz) continues in the background via the coroutine scope. No additional computation.
- MapLibre is NOT rendered in PiP (it would be too small to be useful and would waste GPU). Only the custom compass+text layout renders.

### M3 Expressive Components

- The PiP layout uses raw `Canvas` and `Text` rather than M3 components (limited space).
- The trigger button in StakeoutPanel uses `FilledIconButton` (minimize icon).
- The `RemoteAction` icons are standard Android icons, not Compose.

---

## 9. CROSS-SECTION PROFILE

**Ratings:** User Impact: 8/10 | Implementation Difficulty: 7/10 | Uniqueness: 8/10

Generating elevation profiles is typically a desktop GIS operation. Doing it in the field, from surveyed points, lets the surveyor verify terrain coverage and detect anomalies immediately.

### User Flow

1. User has recorded points with elevation data in the current project.
2. User enters "Profile" mode via a new button in the SurveyPanel's point/line/polygon mode switcher (extending the existing Row of mode buttons).
3. The map enters a crosshair mode: a grey horizontal line appears across the map center.
4. User pans the map to position the line across the desired section. The line is anchored to the screen center and extends edge-to-edge. User rotates the line by placing two fingers and twisting (rotation gesture).
5. Alternatively, user taps two endpoints on the map to define the profile line.
6. Points within a configurable buffer distance (default 5m) of the line are projected onto it.
7. A mini elevation chart slides up from the bottom of the screen (below the map, above the bottom sheet), showing elevation (Y-axis) vs. distance along the line (X-axis).
8. The chart plots projected survey points as dots, connected by linear interpolation.
9. User can pinch to zoom the chart, tap a point on the chart to highlight it on the map.

### Data Source and Projection

For each survey point within the buffer:
1. Project the point's WGS84 coordinates to EGSA87 (easting, northing) using the existing `HeposTransform`.
2. Project each EGSA87 coordinate onto the profile line using perpendicular projection:
   - Line defined by two endpoints: `A(Ea, Na)` and `B(Eb, Nb)`.
   - For point `P(Ep, Np)`, compute: `t = dot(AP, AB) / dot(AB, AB)`.
   - Distance along line: `d = t * length(AB)`.
   - Perpendicular distance: `perp = |cross(AP, AB)| / length(AB)`.
   - Include point if `perp < bufferDistance` and `0 <= t <= 1`.
3. Sort projected points by `d` (distance along line).
4. The chart X-axis is `d` (in meters), Y-axis is orthometric height `H` (in meters, from `orthometricHeight` field).

### Visualization: Mini Chart

**Layout:**
- A `Surface` anchored to the bottom of the map area, 180dp tall, full width.
- Drawn with a Compose `Canvas`.
- Background: `surfaceContainerLow` at 95% opacity (slightly transparent so the map peeks through).
- Y-axis: thin vertical line on the left with tick marks and labels (elevation in meters, `CoordinateFont`, `labelSmall`).
- X-axis: thin horizontal line at the bottom with distance labels.
- Data line: `drawPath` with 2dp stroke in `primary` color, connecting projected points.
- Data points: 6dp filled circles at each point's position, colored by fix quality (`surveyColors.fixColor(quality)`).
- Grid: horizontal dashed lines at Y-axis tick marks in `outlineVariant`.

**Interactivity:**
- Horizontal `scrollState` allows panning when the profile is wider than the screen.
- `transformableState` allows pinch zoom on X-axis.
- Tapping a point on the chart: highlight it with a larger circle (10dp) and an annotation showing "P042: H=123.456m". On the map, the corresponding point gets a pulsing ring.
- Cross-hair cursor: a vertical line follows the user's horizontal finger position, showing interpolated elevation at that distance.

### Technical Implementation

**Profile Line Definition:**

Two approaches:

1. **Screen-center line (quick):**
   - When profile mode activates, overlay a line graphic on the map at screen center.
   - Use `mapRef.projection.fromScreenLocation(PointF(0, height/2))` and `fromScreenLocation(PointF(width, height/2))` to get the line's lat/lng endpoints.
   - A rotation handle (small circle at one end) allows the user to drag and rotate the line.

2. **Two-tap endpoints (precise):**
   - User taps the map twice. First tap places point A (red dot), second tap places point B (green dot). A line connects them.
   - `mapRef.addOnMapClickListener` captures the taps while in profile mode.

**Chart Rendering:**

The chart is a custom Compose `Canvas` composable:

1. Compute the data space bounds: `minD`, `maxD`, `minH`, `maxH` from the projected points. Add 10% padding.
2. Map data coordinates to pixel coordinates: `px = (d - minD) / (maxD - minD) * canvasWidth`, `py = canvasHeight - (h - minH) / (maxH - minH) * canvasHeight`.
3. Draw axes, grid lines, labels using `drawText` and `drawLine`.
4. Draw the profile path using `Path.lineTo` between consecutive projected points.
5. Draw point markers using `drawCircle`.

**Handling Sparse Data:**

If fewer than 3 points fall within the buffer:
- Show a message: "Too few points along profile line. Try widening the buffer or positioning the line over surveyed areas."
- Provide a slider to increase buffer distance (5m to 50m).

If points are unevenly distributed (clusters with gaps):
- Draw the linear interpolation as a dashed line where the gap between consecutive points exceeds 3x the average spacing. This visually communicates "interpolation is uncertain here."

### Export

- **CSV:** Columns: `Distance_m, Easting, Northing, Elevation_m, PointID, FixQuality`. Button: "Export Profile CSV" in the chart header.
- **Image:** Capture the chart Canvas as a `Bitmap` using `drawToBitmapAsync()` from Compose. Button: "Export Image" generates a PNG and opens the share sheet.

### Edge Cases and Error Handling

- **No elevation data:** Points recorded without altitude (internal GPS with no altitude fix) have `orthometricHeight == null`. Skip them and show a warning: "N points excluded (no elevation)."
- **All points at similar elevation:** The chart Y-axis will be very compressed. Auto-scale with at least 1m of vertical range to avoid a flat line.
- **Very long profile (> 1km):** The chart scrolls horizontally. Show a minimap indicator at the bottom of the chart showing the current viewport within the full profile.
- **Profile line entirely off surveyed area:** Show "No survey points found along this line."

### Accessibility Considerations

- Chart points have content descriptions: "Point P042, distance 45.2 meters, elevation 123.4 meters."
- The chart supports TalkBack touch exploration: swiping left/right moves between data points, announcing each.
- High-contrast mode: data line uses 3dp stroke and points use 8dp circles.

### Performance Impact

- Point projection: O(n) for n survey points, with a simple vector math operation per point. For 1000 points: < 1ms.
- Canvas rendering: redraws only on pan/zoom gestures. Typically < 5ms per frame.
- No impact on GNSS or map rendering (the chart is a separate composable, not a map layer).

### M3 Expressive Components

- `Surface` (chart container)
- `Slider` (buffer distance control)
- `FilledTonalButton` (export buttons)
- `SegmentedButton` (mode switcher extension, adding "Profile" mode)
- `AnimatedVisibility` (chart slide-up animation)

---

## 10. GLOVE MODE

**Ratings:** User Impact: 9/10 | Implementation Difficulty: 3/10 | Uniqueness: 8/10

Surveyors wear gloves in rain, cold, and rough terrain. No survey app has a single toggle that adapts the entire UI for gloved operation, including hardware button mapping.

### User Flow

1. User enables "Glove Mode" via a prominent toggle in SettingsPanel, or via a quick-access button in the FixStatusPill area (long-press the status pill to toggle).
2. Instantly, all touch targets grow, text enlarges, secondary information hides, and hardware volume buttons map to survey actions.
3. The surveyor can now operate the app with thick work gloves without misclicks.
4. Disabling Glove Mode reverts all changes immediately.

### What Changes

| Property | Normal | Glove Mode |
|---|---|---|
| Minimum touch target | 48dp | 64dp |
| Button height | M3 default (40dp) | 56dp |
| Font sizes (all) | As defined in `OpenTopoTypography` | +4sp across the board |
| Font weight (body) | W400 | W500 (medium, for readability) |
| Tab bar | 6 icons with labels | 4 icons only, larger 32dp icons |
| FAB size | 56dp | 72dp |
| Stakeout arrow | 180dp | 220dp |
| FixStatusPill | compact | enlarged, 20dp taller |
| Bottom sheet peek height | 48dp | 64dp |
| Secondary info | Visible | Hidden (HDOP, VDOP, PDOP labels in ConnectionPanel) |
| Volume Up button | System volume | Store point (`surveyManager.quickMark()`) |
| Volume Down button | System volume | Undo last action (`undoLastVertex()` or cancel) |
| Haptic feedback | FAB only | All interactive elements |
| Touch slop | System default (8dp) | Increased to 12dp (reduces accidental drags) |

### Technical Implementation

**Preference Storage:**

```kotlin
// In UserPreferences
private val KEY_GLOVE_MODE = booleanPreferencesKey("glove_mode")
val gloveMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_GLOVE_MODE] ?: false }
suspend fun setGloveMode(value: Boolean) {
    context.dataStore.edit { it[KEY_GLOVE_MODE] = value }
}
```

**Typography Scaling:**

Create a `GloveModeTypography` that adds 4sp to every size in `OpenTopoTypography`:

```kotlin
val GloveModeTypography = Typography(
    displayLarge = OpenTopoTypography.displayLarge.copy(fontSize = 52.sp),
    displayMedium = OpenTopoTypography.displayMedium.copy(fontSize = 40.sp),
    // ... all 13 text styles
)
```

Provide via a `CompositionLocal`:

```kotlin
val LocalGloveMode = staticCompositionLocalOf { false }

// In OpenTopoTheme:
val typography = if (gloveMode) GloveModeTypography else OpenTopoTypography
```

**Touch Target Sizing:**

A custom `Modifier` extension:

```kotlin
fun Modifier.gloveTarget(gloveMode: Boolean): Modifier =
    if (gloveMode) this.defaultMinSize(minWidth = 64.dp, minHeight = 64.dp)
    else this
```

Apply this to all clickable elements: buttons, tabs, list items.

**Volume Button Mapping:**

In `MainActivity`:

```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (!gloveModeEnabled) return super.onKeyDown(keyCode, event)
    return when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> {
            surveyManager?.quickMark()
            true // consumed
        }
        KeyEvent.KEYCODE_VOLUME_DOWN -> {
            surveyManager?.undoLastVertex()
            true // consumed
        }
        else -> super.onKeyDown(keyCode, event)
    }
}
```

**Touch Sensitivity:**

Android does not expose a "touch sensitivity" API at the app level. `View.setFilterTouchesWhenObscured(false)` only controls obscured touch filtering, not sensitivity. The actual touch sensitivity improvement comes from:
1. Larger touch targets (the primary mechanism).
2. Increased touch slop (reduces misregistered taps):
   ```kotlin
   ViewConfiguration.get(context).scaledTouchSlop // default ~8dp
   // Override via custom gesture detector with increased threshold
   ```
3. Some Samsung devices have a "Touch sensitivity" system setting for screen protectors. Guide the user to enable it: show a one-time tip in Glove Mode settings.

**Hiding Secondary Info:**

When Glove Mode is active, `ConnectionPanel` hides PDOP/VDOP/HDOP detail rows and shows only:
- Fix status (large text)
- Satellite count (large text)
- Accuracy (large text)
- Connection button (large)

This is controlled by `if (!LocalGloveMode.current)` guards around secondary composables.

### UI Mockup Description

**Toggle Location 1 -- SettingsPanel:**
- A `TonalCard` (existing component) at the top of SettingsPanel, before any other setting.
- Large `Switch` labeled "Glove Mode" with a glove icon.
- When enabled, the card turns `tertiaryContainer` color.
- Below the switch: "Larger targets, bigger text, volume buttons map to survey actions."

**Toggle Location 2 -- Quick Access:**
- Long-press the FixStatusPill (top of map). A small menu with a single item: "Glove Mode: On/Off." One tap to toggle.
- The FixStatusPill shows a tiny glove icon when Glove Mode is active.

**Visual Difference:**
- All buttons visibly grow from 40dp to 56dp height.
- Tab icons grow from 24dp to 32dp.
- FAB grows from 56dp to 72dp with a thicker elevation shadow.
- The overall feel is "chunky and confident."

### Edge Cases and Error Handling

- **Volume button conflict with Voice Commands:** If both Glove Mode (Volume Up = store) and Voice Commands (Volume Up = push-to-talk) are enabled, Voice Commands take priority (it's the more complex interaction). Show a dialog: "Volume Up is used for voice commands. Glove Mode point storage will use Volume Down instead."
- **Glove Mode + Field Mode (from Ambient-Adaptive):** They stack. Field Mode adds +4sp and Glove Mode adds another +4sp, for a total of +8sp. Touch targets take the maximum of both (64dp).
- **Screen too small for enlarged UI:** On devices with < 360dp width, the bottom sheet tabs might overflow. Switch to icon-only tabs (no labels) unconditionally when both Glove Mode and small screen are detected.
- **User forgets Glove Mode is on:** The persistent glove icon in FixStatusPill serves as a reminder. Additionally, a subtle `tertiaryContainer` tint on the bottom sheet drag handle indicates an active mode override.

### Accessibility Considerations

- Glove Mode is inherently an accessibility enhancement: larger targets, larger text.
- It benefits users with motor impairments even without gloves.
- Volume button mapping provides an alternative input method for users who cannot use the touchscreen reliably.
- Announce via `LiveRegion`: "Glove Mode enabled. Volume Up stores a point. Volume Down undoes."

### Performance Impact

- Typography swap causes a single full recomposition (all text re-measures). Cost: ~20ms on mid-range device. Happens once on toggle.
- Larger touch targets do not affect rendering performance.
- Volume button interception has zero performance cost.

### M3 Expressive Components

- `Switch` (toggle)
- `TonalCard` (settings card, reusing existing component)
- `Surface` (FixStatusPill enhancement)
- `AnimatedContent` for smooth size transitions when toggling

---

## 11. HOME SCREEN WIDGET

**Ratings:** User Impact: 6/10 | Implementation Difficulty: 5/10 | Uniqueness: 7/10

A glanceable home screen widget showing live survey status lets the surveyor see project state without opening the app.

### User Flow

1. User long-presses their home screen and adds the "OpenTopo Survey" widget.
2. The widget shows: project name, point count, GNSS connection status (connected/disconnected), and fix quality indicator (colored dot).
3. When a survey is active, the widget updates every 30 seconds with fresh data.
4. Tapping the widget opens the app to the last active project.
5. A "Resume Survey" button on the widget launches the app and immediately connects to the last-used GNSS receiver.

### Technical Implementation

**Jetpack Glance (Compose-style widgets):**

Glance uses `@Composable` functions but renders to `RemoteViews`. It's the modern approach for Android widgets.

```kotlin
class SurveyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            SurveyWidgetContent()
        }
    }
}

@Composable
fun SurveyWidgetContent() {
    val prefs = currentState<Preferences>()
    val projectName = prefs[stringPreferencesKey("project_name")] ?: "No project"
    val pointCount = prefs[intPreferencesKey("point_count")] ?: 0
    val fixQuality = prefs[intPreferencesKey("fix_quality")] ?: 0
    val isConnected = prefs[booleanPreferencesKey("is_connected")] ?: false
    
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Project name
            Text(projectName, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
            Spacer(GlanceModifier.height(4.dp))
            // Point count
            Text("$pointCount points", style = TextStyle(fontSize = 14.sp))
            Spacer(GlanceModifier.height(8.dp))
            // Status row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fix quality dot (colored Image)
                Image(provider = fixQualityDotProvider(fixQuality), contentDescription = "Fix status")
                Spacer(GlanceModifier.width(6.dp))
                Text(fixDescription(fixQuality), style = TextStyle(fontSize = 12.sp))
            }
            Spacer(GlanceModifier.height(8.dp))
            // Resume button
            Button(
                text = "Resume Survey",
                onClick = actionStartActivity<MainActivity>(
                    actionParametersOf(ActionParameters.Key<Boolean>("resume_survey") to true)
                )
            )
        }
    }
}
```

**Widget Receiver:**

```kotlin
class SurveyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SurveyWidget()
}
```

**Manifest declaration:**

```xml
<receiver android:name=".widget.SurveyWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/survey_widget_info" />
</receiver>
```

**Widget Info (res/xml/survey_widget_info.xml):**

```xml
<appwidget-provider
    android:minWidth="180dp"
    android:minHeight="110dp"
    android:targetCellWidth="3"
    android:targetCellHeight="2"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen" />
```

**Data Update Strategy:**

The widget state is updated from the app via `GlanceAppWidget.update()`:
1. In `SurveyManager`, whenever a point is recorded or project changes, call `updateWidget()`.
2. In `GnssState`, whenever `connectionStatus` or `fixQuality` changes, call `updateWidget()`.
3. The `updateWidget()` function writes to Glance `DataStore` (separate from app DataStore) and calls `SurveyWidget().update(context, glanceId)`.
4. When the survey is active, a `PeriodicWorkRequest` (WorkManager, every 30 minutes -- the minimum) triggers `updateWidget()`. For more frequent updates during active survey, use a coroutine in the foreground service that calls `update()` every 30 seconds.

### UI Mockup Description

**Widget (3x2 grid cells, approximately 180dp x 110dp):**

- Background: rounded rectangle (16dp corners) with `surface` color. On Android 12+, uses the user's dynamic wallpaper color via `GlanceTheme`.
- **Top row:** Project name in `titleMedium` weight, left-aligned. If name is too long, ellipsize.
- **Middle row:** "47 points" in `bodyLarge`, with a small grid icon to the left.
- **Status row:** A 10dp colored circle (fix quality color), then "RTK Fix" or "Disconnected" in `labelMedium`.
- **Bottom row:** "Resume Survey" button spanning the widget width. `FilledTonalButton` style (rounded, primaryContainer background). 36dp height to fit the constrained space.
- **Dark theme:** Widget automatically uses dark colors when the system is in dark mode. The fix dot colors remain the same (they're already defined for both themes).

### Edge Cases and Error Handling

- **No active project:** Show "No active project" with a "Open App" button instead of "Resume Survey."
- **App not running:** Widget shows last known state (from persisted DataStore). The fix status shows "Unknown" with a grey dot.
- **Widget on lock screen (Android 14+):** Lock screen widgets show limited data. Hide the point count and show only project name + status dot.
- **Multiple widgets:** All instances show the same data. Each gets updated when `update()` is called.

### Battery Optimization

- When no survey is active (no project open, GNSS disconnected): widget updates only on app launch/exit.
- When survey is active: 30-second updates via foreground service coroutine (not WorkManager, since the service is already running for GNSS).
- Glance widget rendering is lightweight (~5ms per update).

### Accessibility Considerations

- All widget elements have content descriptions: "Project: Site A", "47 points recorded", "GNSS status: RTK Fix."
- The "Resume Survey" button is large enough for touch accessibility (36dp height, full width).
- High contrast: fix quality dots use the same semantic colors as the app, which meet WCAG AA.

### M3 Expressive Components (Glance equivalents)

- Glance `Column`, `Row` (layout)
- Glance `Text` (content)
- Glance `Button` (action)
- Glance `Image` (status dot)
- `GlanceTheme` for M3-compatible theming

---

## 12. FOLDABLE DUAL-PANE MODE

**Ratings:** User Impact: 6/10 | Implementation Difficulty: 6/10 | Uniqueness: 7/10

Samsung Fold and Pixel Fold devices offer a large inner display perfect for showing map and data simultaneously. No GNSS survey app takes advantage of this form factor.

### User Flow

1. Surveyor unfolds their Samsung Galaxy Fold to the inner display (7.6 inches).
2. OpenTopo detects the unfolded state and switches to dual-pane layout: map on the left pane (~60% width), data panel on the right pane (~40% width).
3. The bottom sheet is no longer needed -- all panel content (GNSS, Survey, Stakeout, Tools) is displayed in the right pane with a `NavigationRail` for switching.
4. The surveyor can see the map and stakeout compass simultaneously, or the map and point list together.
5. When the device is folded to the outer display (6.2 inches), the layout reverts to the existing single-pane design with the bottom sheet.
6. The transition is seamless: no data loss, no UI state reset. If the user was viewing StakeoutPanel, it remains visible in whatever layout the new form factor uses.

### Technical Implementation

**Detecting Foldable State:**

Use the Jetpack WindowManager library (`androidx.window:window`):

```kotlin
// In MainActivity
val windowInfoTracker = WindowInfoTracker.getOrCreate(this)
lifecycleScope.launch {
    windowInfoTracker.windowLayoutInfo(this@MainActivity).collect { layoutInfo ->
        val foldingFeature = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
        _foldState.value = when {
            foldingFeature == null -> FoldState.FLAT_SINGLE  // not a foldable, or fully flat
            foldingFeature.state == FoldingFeature.State.FLAT -> FoldState.UNFOLDED_DUAL
            foldingFeature.state == FoldingFeature.State.HALF_OPENED -> FoldState.TABLETOP
            else -> FoldState.FLAT_SINGLE
        }
        _hingeBounds.value = foldingFeature?.bounds
    }
}
```

**Dual-Pane Layout:**

```kotlin
@Composable
fun AdaptiveMainScreen(...) {
    val foldState by foldState.collectAsState()
    val hingeBounds by hingeBounds.collectAsState()
    
    when (foldState) {
        FoldState.UNFOLDED_DUAL -> DualPaneLayout(hingeBounds, ...)
        FoldState.TABLETOP -> TabletopLayout(hingeBounds, ...)
        FoldState.FLAT_SINGLE -> MainMapScreen(...)  // existing single-pane
    }
}

@Composable
fun DualPaneLayout(hingeBounds: Rect?, ...) {
    Row(Modifier.fillMaxSize()) {
        // Left pane: map
        Box(Modifier.weight(0.6f).fillMaxHeight()) {
            MapContent(...)
        }
        
        // Hinge gap: avoid placing interactive content over the physical hinge
        if (hingeBounds != null) {
            Spacer(Modifier.width(with(LocalDensity.current) { hingeBounds.width().toDp() }))
        }
        
        // Right pane: data panel with NavigationRail
        Row(Modifier.weight(0.4f).fillMaxHeight()) {
            NavigationRail(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                // 4 items: GNSS, Survey, Stakeout, Tools
            )
            // Panel content
            Box(Modifier.fillMaxSize()) {
                when (selectedTab) {
                    TAB_CONNECTION -> ConnectionPanel(...)
                    TAB_SURVEY -> SurveyPanel(...)
                    TAB_STAKEOUT -> StakeoutPanel(...)
                    TAB_TOOLS -> ToolsPanel(...)
                }
            }
        }
    }
}
```

**Hinge Awareness:**

The `hingeBounds` Rect tells us exactly where the physical fold is in pixels. The `Spacer` between the two panes matches this width (typically 0dp for seamless foldables, but ~4dp for hinge-gap devices). This ensures no button, text, or interactive element is placed over the crease.

**Tabletop Mode:**

When the device is half-folded (like a laptop), the top half shows the map and the bottom half shows the data panel:

```kotlin
@Composable
fun TabletopLayout(hingeBounds: Rect?, ...) {
    Column(Modifier.fillMaxSize()) {
        // Top pane: map
        Box(Modifier.weight(0.5f).fillMaxWidth()) {
            MapContent(...)
        }
        
        // Hinge gap
        if (hingeBounds != null) {
            Spacer(Modifier.height(with(LocalDensity.current) { hingeBounds.height().toDp() }))
        }
        
        // Bottom pane: data panel with tabs at bottom
        Box(Modifier.weight(0.5f).fillMaxWidth()) {
            PanelWithTabs(...)
        }
    }
}
```

**Continuity During Fold/Unfold:**

The key is that all state is held in `StateFlow` objects (`GnssState`, `SurveyManager`, `Stakeout`, DataStore preferences). The layout composables read from these flows. When the layout switches (fold/unfold triggers `onConfigurationChanged`), the composables are rebuilt but the state is preserved because the flows persist in the Activity (not destroyed on config change).

To avoid Activity recreation:
- `android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"` is already required for PiP and is compatible with foldable handling.
- The `WindowInfoTracker` flow survives configuration changes.

### Testing Without a Foldable Device

1. **Android Emulator:** Create a "7.6 Foldable" AVD in Android Studio. Use the emulator's fold/unfold controls to test state transitions.
2. **Foldable API testing library:** `androidx.window.testing:window-testing` provides `FoldingFeature` fakes:
   ```kotlin
   @Test
   fun dualPaneLayout_showsNavigationRail() {
       val foldingFeature = FoldingFeature(
           activity = activityRule.activity,
           state = FoldingFeature.State.FLAT,
           orientation = FoldingFeature.Orientation.VERTICAL,
       )
       // Set up test scenario and assert layout
   }
   ```
3. **Desktop preview:** Compose Preview with `@Preview(device = Devices.FOLDABLE)` shows approximate dual-pane rendering.

### UI Mockup Description

**Unfolded Inner Display (2176x1812 pixels, ~7.6 inches):**

Left pane (60%, ~1300px wide):
- Full MapLibre map, exact same rendering as single-pane.
- FixStatusPill in top-left corner.
- FAB menu in bottom-right corner.
- Layer switcher in top-right corner.
- No bottom sheet.

Right pane (40%, ~470px wide):
- `NavigationRail` on the left edge: 4 icons vertically stacked (GNSS, Survey, Stakeout, Tools), each 64dp tall. Selected icon: `primary` tint with `primaryContainer` background pill. Uses M3 `NavigationRail` composable.
- Content area: the selected panel fills the remaining width (~390dp) and full height. Identical content to the bottom sheet tabs, but with more vertical space (no need for scrolling in most cases).
- Background: `surface` color. Thin `outlineVariant` divider between NavigationRail and content.

**Folded Outer Display (6.2 inches, narrow):**
- Identical to the current single-pane `MainMapScreen` with `BottomSheetScaffold`.

**Transition animation:**
- When folding/unfolding, Android provides a system animation. The layout swap happens during this animation. To the user, it appears as a smooth transformation.
- Use `AnimatedContent` with `fadeIn + fadeOut` on the layout switch for an additional polish layer.

### Edge Cases and Error Handling

- **Device not foldable:** `windowLayoutInfo` emits no `FoldingFeature`. The `foldState` stays `FLAT_SINGLE` forever. Zero impact on non-foldable devices.
- **Foldable without hinge (flex display):** Some foldables (OPPO Find N) have a seamless inner display with no visible crease. `hingeBounds` may be a zero-width Rect. The `Spacer` width is 0dp, which is correct.
- **Rapid fold/unfold (bouncing the device):** The `WindowInfoTracker` flow emits rapidly. Debounce the fold state with a 300ms delay to prevent layout thrashing.
- **Map state during layout switch:** `MapLibreMap` is held in a `remember` block that persists across recompositions (the map view reference doesn't change). The camera position, layers, and sources are maintained. However, the `AndroidView`'s size changes, which triggers a MapLibre `onSizeChanged` -- this is handled automatically by the library.
- **Right pane too narrow for some panels:** On the Pixel Fold's inner display, the right pane is ~390dp. The existing panels are designed for 360dp-wide phones, so they fit. Test that no overflow occurs. Use `Modifier.verticalScroll` as a safety net.

### Accessibility Considerations

- `NavigationRail` items have content descriptions: "GNSS tab," "Survey tab," etc.
- Screen readers announce the pane switch: "Map pane" and "Data pane."
- Large display means more content visible simultaneously, reducing navigation for all users.
- Touch targets remain at standard 48dp (or 64dp in Glove Mode).

### Performance Impact

- Dual-pane renders the map and a panel simultaneously. On the inner display (higher resolution), the GPU works harder for map tiles. MapLibre handles this efficiently; the main cost is tile decoding, which happens on background threads.
- The `NavigationRail` and panel content are lightweight Compose trees, adding < 2ms to frame time.
- No impact on GNSS processing.

### M3 Expressive Components

- `NavigationRail` (panel navigation in dual-pane mode)
- `NavigationRailItem` (individual tab items)
- `AnimatedContent` (layout transition)
- All existing panel components (`ConnectionPanel`, `SurveyPanel`, `StakeoutPanel`, `ToolsPanel`) are reused without modification.

---

## Summary Ratings Table

| Feature | User Impact | Difficulty | Uniqueness | Priority Score |
|---|---|---|---|---|
| 1. Ambient-Adaptive Theme | 7 | 5 | 9 | 21 |
| 2. Haptic Stakeout Guidance | 10 | 6 | 10 | 26 |
| 3. Radial/Pie Context Menu | 8 | 7 | 9 | 24 |
| 4. Accuracy Convergence Animation | 9 | 4 | 8 | 21 |
| 5. Survey Coverage Heatmap | 7 | 6 | 9 | 22 |
| 6. Voice Commands | 9 | 8 | 10 | 27 |
| 7. Wear OS Companion | 8 | 8 | 10 | 26 |
| 8. PiP Floating Stakeout | 8 | 5 | 9 | 22 |
| 9. Cross-Section Profile | 8 | 7 | 8 | 23 |
| 10. Glove Mode | 9 | 3 | 8 | 20 |
| 11. Home Screen Widget | 6 | 5 | 7 | 18 |
| 12. Foldable Dual-Pane | 6 | 6 | 7 | 19 |

**Recommended implementation order by priority score:**
1. Voice Commands (27) -- highest unique value, transformative for field workflow
2. Haptic Stakeout Guidance (26) -- game-changing for stakeout, builds on existing Stakeout class
3. Wear OS Companion (26) -- high unique value but complex; can be deferred
4. Radial/Pie Context Menu (24) -- polished UX differentiator
5. Cross-Section Profile (23) -- high professional utility
6. Survey Coverage Heatmap (22) -- leverages existing MapLibre HeatmapLayer
7. PiP Floating Stakeout (22) -- relatively easy, high daily-use value
8. Accuracy Convergence Animation (21) -- low difficulty, high visual impact
9. Ambient-Adaptive Theme (21) -- medium effort, nice differentiation
10. Glove Mode (20) -- very low difficulty, immediate field utility (could be done first as a quick win)
11. Foldable Dual-Pane (19) -- niche audience but future-proofing
12. Home Screen Widget (18) -- lowest priority but straightforward