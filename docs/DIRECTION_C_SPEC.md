I now have a comprehensive understanding of the entire OpenTopo codebase. Let me compile the complete design specification.

---

# Direction C: M3 Expressive Showcase -- Complete Design Specification

## 0. Design System Foundation

### 0.1 Color System

**Seed color:** `#006B5E` (teal-green)

**Light scheme (current, preserved):**

| Role | Hex | Usage |
|---|---|---|
| primary | `#006B5E` | App bar icons, active tab indicator, section labels, record button |
| onPrimary | `#FFFFFF` | Text on primary surfaces |
| primaryContainer | `#6FF7DD` | NTRIP connected surface, transform result card |
| onPrimaryContainer | `#00201B` | Text on primaryContainer |
| secondary | `#4A635C` | Secondary actions |
| secondaryContainer | `#CCE8DF` | Inactive toggle states |
| tertiary | `#436278` | Info elements, skyplot background |
| tertiaryContainer | `#C7E7FF` | Info chips |
| error | `#BA1A1A` | No-fix state, delete actions |
| errorContainer | `#FFDAD6` | Stale correction chip background |
| surface | `#F5FBF7` | Sheet background, dialog background |
| surfaceContainerLow | `#F5FBF7` | Target input card, settings sections |
| surfaceContainer | `#EFF5F1` | Default container |
| surfaceContainerHigh | `#E9EFEB` | TonalCard background, trig point list items |
| surfaceContainerHighest | `#E3EAE6` | Connected state surface, coordinate block, input row bg |
| onSurface | `#171D1B` | Primary text |
| onSurfaceVariant | `#3F4945` | Secondary text, labels |
| outline | `#6F7975` | Dividers, credit text |
| outlineVariant | `#BFC9C4` | Hairline dividers, skyplot grid, stakeout compass ring |
| inverseSurface | `#2B322F` | Snackbar background |
| inversePrimary | `#50DBC2` | Snackbar action text |
| scrim | `#000000` | Modal scrim at 32% opacity |

**Dark scheme (current, preserved):**

| Role | Hex |
|---|---|
| primary | `#50DBC2` |
| onPrimary | `#003830` |
| primaryContainer | `#005046` |
| onPrimaryContainer | `#6FF7DD` |
| surface | `#0F1512` |
| onSurface | `#DEE4E0` |
| surfaceVariant | `#3F4945` |
| onSurfaceVariant | `#BFC9C4` |
| outline | `#89938F` |
| outlineVariant | `#3F4945` |

**Semantic survey colors (light / dark):**

| Semantic | Light | Dark | Usage |
|---|---|---|---|
| rtkFix | `#1B6D2F` | `#A1F5A3` | Fix quality 4 |
| rtkFloat | `#7B5800` | `#FFDEA6` | Fix quality 5 |
| dgps | `#7B5800` | `#FFDEA6` | Fix quality 2 |
| gps | `#1565C0` | `#90CAF9` | Fix quality 1 |
| noFix | `#BA1A1A` | `#FFB4AB` | Fix quality 0 |
| onFix | `#FFFFFF` | `#FFFFFF` | Text on fix badges |
| accuracyGood | `#1B6D2F` | `#A1F5A3` | <0.02m |
| accuracyOk | `#7B5800` | `#FFDEA6` | 0.02-0.05m |
| accuracyPoor | `#BA1A1A` | `#FFB4AB` | >0.05m |
| correctionFresh | `#1B6D2F` | `#A1F5A3` | <2s age |
| correctionStale | `#7B5800` | `#FFDEA6` | 2-5s age |
| correctionDead | `#BA1A1A` | `#FFB4AB` | >5s age |
| stakeoutOnPoint | `#1B6D2F` | `#A1F5A3` | <0.1m |
| stakeoutClose | `#7B5800` | `#FFDEA6` | 0.1-1.0m |
| stakeoutFar | `#BA1A1A` | `#FFB4AB` | >1.0m |
| recordingActive | `#D1416A` | `#FFD9E0` | Active recording pulse |
| recordingProgress | `#006B5E` | `#50DBC2` | Progress indicator fill |

**Fixed status containers (light only, used in FixStatusPill):**

| Container | Hex | On-container | Hex |
|---|---|---|---|
| SuccessContainer | `#A1F5A3` | OnSuccessContainer | `#002108` |
| WarningContainer | `#FFDEA6` | OnWarningContainer | `#271900` |

**Constellation categorical colors (fixed, not theme-dependent):**

| Constellation | Dot | Container |
|---|---|---|
| GPS | `#558B2F` | `#DCEDC8` |
| GLONASS | `#1565C0` | `#E3F2FD` |
| Galileo | `#6A1B9A` | `#F3E5F5` |
| BeiDou | `#E65100` | `#FFF3E0` |

### 0.2 Shape System

| Token | Value | Usage |
|---|---|---|
| extraSmall | `RoundedCornerShape(4.dp)` | Fix type badges in point cards |
| small | `RoundedCornerShape(8.dp)` | ConstellationChip, text field shapes, baud rate dropdowns |
| medium | `RoundedCornerShape(12.dp)` | TonalCard, settings sections, coordinate block, delta cards, connected surfaces, pipeline surfaces |
| large | `RoundedCornerShape(16.dp)` | TonalCard inner card, stakeout target input form |
| extraLarge | `RoundedCornerShape(28.dp)` | Sheet top corners, drag handle shape, FAB recording circle, FixStatusPill (50%) |

The sheet top corners use a custom `RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`.

The FixStatusPill uses `RoundedCornerShape(percent = 50)` for a full capsule.

### 0.3 Typography Map

The custom `OpenTopoTypography` scale (all system default sans-serif except where CoordinateFont is applied):

| Role | Size | Weight | Spacing | Where used |
|---|---|---|---|---|
| displayLarge | 48sp | W300 | -2sp | Stakeout immersive distance (proposed) |
| displayMedium | 36sp | W300 | -1sp | Stakeout immersive deltas (proposed) |
| displaySmall | 28sp | W400 | 0sp | Stakeout distance in panel (`StakeoutPanel`) |
| headlineLarge | 24sp | W600 | 0sp | Section headers (proposed full-screen states) |
| headlineMedium | 16sp | W600 | 0sp | (Reserved) |
| headlineSmall | 14sp | W600 | 0sp | FAB recording countdown number |
| titleLarge | 18sp | W600 | 0sp | `SectionHeader` title ("Points (N)"), about panel title |
| titleMedium | 14sp | W600 | 0.1sp | Point/Line/Polygon recording status, stakeout bearing text, feature measurement, pipeline step labels, project prompt |
| titleSmall | 12sp | W600 | 0.1sp | Project name in header, trig point GYS ID, point card point ID, converter title, export project name, target form header |
| bodyLarge | 14sp | W400 | 0sp | Fix description in connected state, "Waiting for fix", NTRIP status labels |
| bodyMedium | 13sp | W400 | 0sp | Accuracy row labels, stakeout delta labels, NTRIP data rows, settings headlineContent, general descriptions, coordinate text fields (with CoordinateFont overlay) |
| bodySmall | 11sp | W400 | 0sp | Point card coordinates, trig point name, pipeline coordinate values, help/credit text, remarks text |
| labelLarge | 12sp | W600 | 0.2sp | Section label text ("EXPORT / IMPORT"), fix badge label, settings section headers |
| labelMedium | 11sp | W600 | 0.3sp | Tab text, constellation chip text, NTRIP data rate text, accuracy badge text, fix status pill label, point count badge |
| labelSmall | 10sp | W600 | 0.8sp | `SectionLabel` component text, pipeline section subtitles, credits label headings |

**CoordinateFont (monospace) usage rule:** All numeric coordinate values, accuracy numbers, epoch counts, antenna height inputs, NTRIP data rates, point IDs in cards, constellation chip counts, baud rate values, altitude displays, and any measurement readout. Never use monospace for UI labels, descriptions, or button text.

**Minimum outdoor readability rule:** No text element in the field-use screens (map overlay, status bar, stakeout compass, FAB) should be below 11sp. The FixStatusPill at 11sp (labelMedium) is the floor. The stakeout immersive mode uses displayLarge (48sp) for the primary distance -- readable at arm's length in direct sunlight.

### 0.4 Motion System

The theme applies `MotionScheme.expressive()` globally. This provides pre-configured spring and easing values for M3 Expressive components. Custom animations should use these reference values:

| Spec name | Spring damping ratio | Spring stiffness | Usage |
|---|---|---|---|
| Expressive spatial fast | 0.7 | 600 | Sheet expand/collapse |
| Expressive spatial medium | 0.8 | 380 | Tab content crossfade container |
| Expressive spatial slow | 0.9 | 200 | Full-screen stakeout enter/exit |
| Expressive effects fast | 0.6 | 800 | Fix status color change |
| Expressive effects bounce | 0.4 | 500 | Point appear on map |

Durations (for tween-based where spring is not appropriate):

| Timing | Duration | Easing | Usage |
|---|---|---|---|
| Short 1 | 100ms | EaseInOut | Micro-feedback (tap ripple) |
| Short 2 | 150ms | FastOutSlowIn | Icon swap (FAB + to X) |
| Medium 1 | 250ms | EmphasizedDecelerate | Tab crossfade |
| Medium 2 | 350ms | EmphasizedDecelerate | Sheet partial expand |
| Long 1 | 450ms | Emphasized | Sheet full expand |
| Long 2 | 600ms | EmphasizedAccelerate then Decelerate | Stakeout immersive enter |
| Extra long | 1000ms | CubicBezier(0.2, 0, 0, 1) | Accuracy ring convergence |

---

## 1. Screen-by-Screen Specification

### 1.1 Map Idle State (No Survey Active)

**Layout:**
- Full-screen MapLibre GL map fills the entire viewport from edge to edge, including behind system bars
- Status bars transparent, content draws behind them with `WindowInsets.statusBars` padding applied only to overlay elements

**Map overlay elements:**

1. **Fix Status Pill** (top-left)
   - Position: `Alignment.TopStart`, padded `windowInsetsPadding(WindowInsets.statusBars)` + 12dp all sides
   - Component: `Surface` with `RoundedCornerShape(percent = 50)`
   - Background: fix-quality container color (see color table)
   - Content: 6dp pulsing dot + labelMedium (CoordinateFont) fix label
   - Dot animation: `infiniteRepeatable`, `tween(750ms, EaseInOut)`, alpha 1.0 to 0.4, `RepeatMode.Reverse`
   - Minimum touch target: intrinsic (not interactive -- display only)
   - Height: approximately 28dp (5dp vertical padding + 11sp text + 6dp dot)

2. **Layer Switcher Button** (top-right)
   - Position: `Alignment.TopEnd`, same inset padding as pill + 12dp
   - Component: `FilledIconButton` with semi-transparent surface (`surface.copy(alpha = 0.92f)`)
   - Icon: `Icons.Outlined.Layers`, 22dp
   - Size: 48dp x 48dp (default FilledIconButton)
   - On tap: opens `DropdownMenu` with 3 items + trig points toggle (Street Map, Orthophoto, Toggle Contours, Trig Points)

3. **User Location Dot** (on map)
   - Outer glow: CircleLayer radius 18px, fix-quality color, opacity 0.15
   - Inner dot: CircleLayer radius 8px, fix-quality color, opacity 1.0, white stroke 2.5px
   - Color updates reactively from `surveyColors.fixColor(fixQuality)` as hex

4. **Survey Point Markers** (on map, when project has points)
   - Circle radius 8px, color by fixQuality expression (RTK=#00E676, Float=#FFAB00, DGPS=#FFD600, GPS=#448AFF, None=#FF1744)
   - White stroke 3px for orthophoto visibility
   - Labels: SymbolLayer, Noto Sans Medium 12px, white text with black halo width 2px, offset [0, -1.8]

5. **FAB Menu** (bottom-right) -- HIDDEN when no project is active or no fix
   - `AnimatedVisibility` with `scaleIn() + fadeIn()` enter, `scaleOut() + fadeOut()` exit
   - Position: `Alignment.BottomEnd`, padding end=16dp, bottom=8dp
   - Component: `FloatingActionButtonMenu` + `ToggleFloatingActionButton`
   - The FAB shows `Icons.Filled.Add` when collapsed, morphs to `Icons.Filled.Close` when expanded (crossover at `checkedProgress > 0.5f`)
   - Menu items: "Record Point" (RadioButtonChecked icon), "Quick Mark" (Speed icon), "Stakeout" (NearMe icon)
   - Each `FloatingActionButtonMenuItem` is a `Surface` with icon + text

6. **MapLibre Compass** -- repositioned to top-left below fix pill
   - Gravity: `TOP | START`
   - Margins: left=40, top=380, right=0, bottom=0

**Bottom Sheet (collapsed/peek state):**
- `BottomSheetScaffold` with `sheetPeekHeight = 148.dp`
- Sheet top corners: `RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`
- Container color: `MaterialTheme.colorScheme.surface`
- Shadow elevation: 8.dp
- Drag handle: 32dp x 4dp `Surface`, color `onSurfaceVariant.copy(alpha = 0.4f)`, shape extraLarge, vertical padding 12dp

**Status bar content (visible in peek, 148dp):**

Row 1 (top): Fix Status Pill (left) + Accuracy badges (right)
- `FixStatusPill` composable (identical to map overlay pill but smaller context)
- Two `AccuracyBadge` texts: "H+/-0.012m" and "V+/-0.018m" with `labelMedium`, CoordinateFont, bold, colored by accuracy threshold
- If no fix: text "Swipe up to connect" in bodyMedium, onSurfaceVariant
- Horizontal padding: 16dp, bottom padding: 12dp

Row 2: Constellation chips (only if fix and satellites visible)
- `Spacer(4.dp)` then `Row(horizontalArrangement = spacedBy(4.dp))`
- Each `ConstellationChip`: Surface with small shape (8dp corners), constellation bg/fg colors, labelMedium text "GPS 12" / "GAL 8" etc.

Row 3: Coordinate block (only if fix)
- `Spacer(8.dp)` then `CoordinateBlock` composable
- Surface with surfaceContainerHighest bg, medium shape (12dp corners), 12dp padding
- Label in labelSmall, primary color, uppercase ("EGSA87" or "WGS84" or "WGS84 DMS")
- E/N values in bodyMedium, CoordinateFont, W600 weight, row with SpaceBetween arrangement

Row 4: Altitude + NTRIP indicator
- `Spacer(4.dp)`
- Left: "H:38.52m h:76.12m" (dual height) or "Alt: 38.52m" in labelMedium, CoordinateFont, onSurfaceVariant
- Right: 8dp green/yellow/red circle + "NTRIP 1KB/s" in labelMedium, CoordinateFont, colored by correction age

Then: `HorizontalDivider(color = outlineVariant)`

### 1.2 Map During Point Recording (Averaging in Progress)

**Map changes:**
- The FAB area transforms from the `FloatingActionButtonMenu` to a recording indicator
- The `ToggleFloatingActionButton` + menu is replaced by a standalone recording FAB

**Recording FAB (bottom-right, same position as normal FAB):**
- Outer ring: `LoadingIndicator` with `progress = { recordingState.progress }`, 80dp size, color = recordingProgress (`#006B5E` light / `#50DBC2` dark)
- Inner circle: `Surface` with recordingActive color (`#D1416A` light / `#FFD9E0` dark), extraLarge shape (28dp corners -- effectively circular at 64dp), 64dp size, shadowElevation 6dp
- Center text: countdown number (totalEpochsTarget - epochsCollected), headlineSmall (14sp W600), CoordinateFont, bold, white
- On tap: cancels recording (`surveyManager?.cancelRecording()`)

**Transition animation (FAB idle to recording):**
- The entire `AnimatedVisibility` wrapping uses `scaleIn() + fadeIn()` / `scaleOut() + fadeOut()`
- The swap between menu FAB and recording FAB happens via an `if (recordingState.isRecording)` branch inside the `AnimatedVisibility`
- Additional recommended enhancement: wrap the inner content in `AnimatedContent` with `scaleIn(initialScale = 0.8f) + fadeIn()` to `scaleOut(targetScale = 0.8f) + fadeOut()` for the morph between plus-icon-FAB and countdown-circle

**Bottom sheet Survey panel (if visible):**
- The `RecordingControls` card shows:
  - `ContainedLoadingIndicator` with progress, fullWidth, 8dp height, indicatorColor = recordingProgress
  - Text: "Averaging: 3/10" in bodyMedium, CoordinateFont, bold, recordingActive color
  - `LoadingIndicator` spinner at 24dp (indeterminate, for visual activity)
  - Cancel button: `OutlinedButton` full-width, "Cancel" text

**Haptic + audio feedback on completion:**
- Vibration: 300ms one-shot at default amplitude
- Audio: `ToneGenerator.TONE_PROP_ACK` for 200ms
- Snackbar: "Recorded P001: E=456123.456 N=4312567.890 H=38.521m +/-0.012m"

### 1.3 Map During Line Recording (Vertices Being Added)

**Map layer changes:**
- `survey-lines` GeoJsonSource updated with LineString from accumulated vertices
- `survey-lines-layer`: LineLayer, color `#00E5FF`, width 4px, opacity 0.9

**Bottom sheet Survey panel content:**
- Mode switcher: `SingleChoiceSegmentedButtonRow` with 3 segments (Point / Line / Area), "Line" selected
- `TonalCard` (surfaceContainerHigh bg, large shape, 14dp horizontal / 12dp vertical padding) containing:
  - Title: "Recording Line" in titleMedium
  - Vertex count: "5 vertices recorded" in bodyMedium, CoordinateFont
  - Live measurement: "123.45 m" in titleMedium, CoordinateFont, primary color
  - Action row: Undo `IconButton` (20dp icon) + "Add Vertex" `FilledTonalButton` (weight 1f) + "Finish" `OutlinedButton` (weight 1f), spacedBy 8dp

**For polygon mode, identical except:**
- Title: "Recording Polygon"
- Measurement: "1234.5 m^2 (1.235 stremma)" when >= 3 vertices
- Map layers: `survey-polygons` GeoJsonSource + `survey-polygons-layer` (FillLayer, color `#00E5FF`, opacity 0.15) + `survey-polygons-outline` (LineLayer, same color, width 3px, opacity 0.6)
- Polygon auto-closes (first coord appended)

### 1.4 Map During Stakeout (Navigating to Target)

**Map overlay:** No change to standard map overlay elements.

**Bottom sheet Stakeout panel content (when target is set and result available):**

1. **Compass arrow** (`StakeoutArrow` composable):
   - `Canvas(Modifier.size(180.dp))` centered in column
   - Outer ring: 2dp stroke, outlineVariant color
   - Fill: stakeout color at 0.08 alpha
   - Cardinal labels: "N", "E", "S", "W" at radius - 10dp, 12sp medium weight, onSurfaceVariant
   - Tick marks at cardinals: 6dp length, 1.5dp width, outlineVariant
   - Arrow: Path (chevron shape) rotated to bearing, filled with stakeout distance color
   - Arrow tip at 85% of inner radius, sides at 15% of inner radius, notch at 30%, 14dp half-width

2. **Distance display:**
   - `Spacer(12.dp)` below compass
   - Text: "0.234 m" in displaySmall (28sp W400), CoordinateFont, bold, stakeout distance color
   - Below: "NE (34.2 deg)" in titleMedium, onSurfaceVariant

3. **Delta card:**
   - `Surface` with surfaceContainerLow bg, medium shape, tonalElevation 1dp
   - 16dp padding, spacedBy 2dp
   - Three `SurveyStatusRow`: "deltaE" / "deltaN" / "Target" with values right-aligned in bodyMedium, CoordinateFont, bold

4. **Clear Target button:**
   - `OutlinedButton` full-width, small shape (8dp corners)
   - Icon: `Icons.Outlined.Close` 18dp + "Clear Target" text

**When waiting for fix (target set, no result):**
- `ContainedLoadingIndicator` at 48dp, centered
- Text: "Waiting for fix..." in bodyLarge, onSurfaceVariant

**When no target set:**
- Import CSV button: `OutlinedButton` full-width with FileUpload icon
- Nearby Trig Points button: `OutlinedButton` full-width with PinDrop icon (disabled without fix)
- Trig point list: up to 5 items, each a `Surface` with surfaceContainerHigh bg, medium shape, 12dp padding, containing status color dot (10dp) + GYS ID (titleSmall, CoordinateFont) + name (bodySmall) + elevation (labelMedium, CoordinateFont)
- Target input form: `Surface` with surfaceContainerLow bg, large shape, tonalElevation 1dp, containing:
  - Label: "Target (EGSA87)" in titleSmall, onSurfaceVariant
  - Name `TextField` full-width, small shape
  - E/N `TextField` pair in `Row(spacedBy(8.dp))`, each weight(1f), small shape
  - "Start" `FilledTonalButton` full-width, small shape, enabled when both E/N parse as Double

### 1.5 Stakeout Immersive Full-Screen (Proposed New Screen)

**Entry trigger:** Double-tap on stakeout compass in panel, or explicit "Immersive" button.

**Layout:** Full-screen dark overlay replacing the entire app UI.

**Background:** Always `#0F1512` (dark surface) regardless of system theme. This is the dark mode surface color from the existing dark scheme.

**Content (centered vertically):**

1. **Large compass** (top 40% of screen):
   - Same `StakeoutArrow` Canvas but sized to `min(screenWidth - 64.dp, 320.dp)`
   - Colors: outlineVariant uses dark scheme (`#3F4945`), arrow color from dark survey colors
   - Cardinal labels: 16sp (larger for outdoor visibility)

2. **Distance display** (center):
   - Text: "0.234" in displayLarge (48sp W300) with CoordinateFont, bold, dark stakeout distance color
   - Unit: "m" in headlineLarge (24sp W600) to the right, same color
   - Below: bearing "NE (34.2 deg)" in titleLarge (18sp W600), dark onSurfaceVariant (`#BFC9C4`)

3. **Delta display** (below center):
   - Two large readouts side by side:
   - "deltaE: +0.123" and "deltaN: -0.456" each in displayMedium (36sp W300), CoordinateFont, dark onSurface (`#DEE4E0`)

4. **Target name** (bottom):
   - "Target: BM-0042" in titleLarge, dark primary (`#50DBC2`)

5. **Exit button** (top-right):
   - `FilledIconButton` with dark surfaceContainerHigh bg, Close icon, 48dp touch target
   - Position: top-right with status bar inset + 16dp

**Enter animation:**
- Background: color morph from transparent to `#0F1512` over 600ms, `Emphasized` easing
- Content: scale from 0.85 to 1.0 + fade from 0 to 1 over 600ms with `spring(dampingRatio = 0.9, stiffness = 200)`

**Exit animation:**
- Reverse of enter: scale to 0.85 + fade out over 400ms
- Background color morph to transparent over 400ms

### 1.6 Bottom Sheet Collapsed (Single Status Row)

This is the peek state of the `BottomSheetScaffold`, showing at `sheetPeekHeight = 148.dp`.

**Visible content at 148dp:**
- Drag handle (32dp x 4dp, centered, 12dp top padding)
- Status bar (rows 1-4 as described in section 1.1)
- Everything below the HorizontalDivider is clipped/hidden

**Interaction:** Swipe up to half-expand. Tap on status bar does not expand (sheet only responds to drag gesture on handle area and velocity-based fling).

### 1.7 Bottom Sheet Half-Expanded (Tool Panel Visible)

**Trigger:** Swipe up from peek, or programmatic expansion.

**State:** `SheetValue.PartiallyExpanded` (default initial state).

**Visible content:**
- Status bar (always visible)
- Divider
- Project header row: TextButton with folder icon + project name (titleSmall, bold) + dropdown arrow + point count badge (labelMedium, CoordinateFont, onSurfaceVariant), all in surfaceContainerLow bg, 16dp horizontal / 6dp vertical padding
- Divider
- `SecondaryTabRow` with 4 tabs: GNSS (Cable icon), Survey (Straighten icon), Stakeout (NearMe icon), Tools (Build icon)
  - Each tab: icon 20dp + labelMedium text
  - Selected tab indicator: M3 default secondary tab indicator (primary color underline)
- Panel content area: `Box(Modifier.fillMaxWidth().height(420.dp))` with `AnimatedContent` crossfade between panels

**Tab transition:** `fadeIn() togetherWith fadeOut()`, using MotionScheme.expressive() defaults.

### 1.8 Bottom Sheet Fully Expanded (Full Data View)

**Trigger:** Continue swiping up from half-expanded, or fast fling.

**State:** `SheetValue.Expanded`

**Content:** Same as half-expanded but the 420dp panel content area can scroll internally (each panel has its own `verticalScroll`). The sheet fills to screen height minus status bar inset. The map is still visible but mostly occluded.

### 1.9 GNSS Panel (`ConnectionPanel`)

**Layout:** Column, fullWidth, verticalScroll, horizontalPadding 16dp, spacedBy 16dp

**Section 1: GNSS Receiver**
- `SectionLabel("GNSS Receiver")` -- labelSmall, primary color, uppercase, padding start=4dp top=8dp bottom=4dp
- `TonalCard` (surfaceContainerHigh bg, large shape, 0dp elevation, 14dp horizontal / 12dp vertical padding)

**When disconnected:**
- `SingleChoiceSegmentedButtonRow` full-width with 3 segments: BT (Bluetooth icon) / USB (Usb icon) / Internal (PhoneAndroid icon)
- Each `SegmentedButton` with icon 18dp
- Below: device picker dropdown (`ExposedDropdownMenuBox` + `TextField` read-only) for BT or USB
- Connect button: `Button` full-width, medium shape, with matching icon 18dp + text

**When connecting:**
- Method badge: Row with 16dp method icon + labelMedium label, primary color, 8dp bottom padding
- Surface with surfaceContainerHighest bg, large shape
- Row: `ContainedLoadingIndicator` 24dp + "Connecting" in bodyLarge bold onPrimaryContainer | "Disconnect" `OutlinedButton` medium shape

**When connected:**
- Same surface layout as connecting, but fix description text (bodyLarge bold, fixColor) replaces spinner
- Below: satellite constellation chips row (spacedBy 4dp)
- `SurveyStatusRow` items: H-Accuracy, V-Accuracy, HDOP
- `Skyplot` composable (220dp Canvas) with legend row

**Skyplot specification:**
- Canvas 220dp square
- Background circle: surfaceContainerHigh, radius + 8dp
- Elevation rings at 0/30/60 degrees: 1dp stroke, outlineVariant
- Cross-hairs: 0.5dp stroke, outlineVariant
- Cardinal labels: 11sp bold, onSurfaceVariant, positioned at radius + 10dp
- Elevation labels: 8sp, outlineVariant, positioned inside rings
- Satellite dots:
  - SNR >= 35: 6dp radius, alpha 1.0
  - SNR >= 20: 4.5dp radius, alpha 0.7
  - SNR >= 15: 3dp radius, alpha 0.4
  - SNR > 0: 2.5dp radius, alpha 0.25
  - White stroke ring: 0.8dp
  - PRN label: 7sp bold, same color/alpha, offset below dot by dotRadius + 1dp

**Section 2: NTRIP Corrections**
- `SectionLabel("NTRIP Corrections")`
- `TonalCard` containing:

**When disconnected:**
- Caster preset picker (`ExposedDropdownMenuBox`), showing preset name or "Custom server"
- Custom server: Host + Port `OutlinedTextField` pair in Row(spacedBy 8dp), 2:1 weight
- User/Pass `OutlinedTextField` pair in Row(spacedBy 8dp), 1:1 weight, password with PasswordVisualTransformation
- Mountpoint `OutlinedTextField` full-width
- Mountpoint picker dropdown (when sourcetable fetched)
- Action row: "Get List" `OutlinedButton` (weight 1f, medium shape) + "Connect" `Button` (weight 1f, medium shape)
- Get List shows `ContainedLoadingIndicator` 20dp + "Fetching" when active

**When connected:**
- Surface with primaryContainer bg, medium shape, 12dp padding
- Row: "Connected" bodyLarge bold onPrimaryContainer | "Stop" `OutlinedButton` medium shape
- `SurveyStatusRow` items: Mountpoint, Data Rate, Correction Age (colored by age threshold)
- If age > 5s: `SuggestionChip` with errorContainer bg, "Corrections stale!" labelMedium bold, ErrorOutline icon 18dp, error border

### 1.10 Survey Panel (`SurveyPanel`)

**Layout:** Column, fullWidth, horizontalPadding 16dp, verticalScroll

**When no project selected:**
- Centered column with 48dp top/bottom padding
- Inventory2 icon 56dp, onSurfaceVariant at 0.5 alpha
- "Select a project from the header above" titleMedium, onSurfaceVariant
- Help text in bodyMedium, onSurfaceVariant at 0.7 alpha

**When project active:**
- `SingleChoiceSegmentedButtonRow` full-width: Point (LocationOn icon) / Line (Timeline icon) / Area (Pentagon icon)
- `RecordingControls` `OutlinedCard` full-width, 16dp padding, spacedBy 8dp:
  - AH/Remarks input row: two `OutlinedTextField` in Row(spacedBy 8dp), AH at weight(0.35f) with CoordinateFont text style, Remarks at weight(0.65f)
  - "Record Point" `FilledTonalButton` full-width, RadioButtonChecked icon 18dp
  - Or recording progress (see section 1.2)
- Line/polygon feature controls (when in line/polygon mode)
- `SectionHeader` with RadioButtonChecked icon 24dp primary + "Points (N)" titleLarge
- Point list: each `PointCard` as `ElevatedCard` (1dp elevation), 16dp padding, Row layout:
  - Left: `FixTypeBadge` (56dp box) -- Surface with fixColor bg, small shape, labelLarge bold CoordinateFont white text, 12dp horizontal / 4dp vertical padding
  - Center column (weight 1f): point ID (titleSmall, CoordinateFont, bold), E/N coords (bodySmall, CoordinateFont, onSurfaceVariant), height info (bodySmall, CoordinateFont), accuracy badge + remarks row
  - Right: 3 `IconButton` (each 36dp): camera (PhotoCamera 18dp), edit (Edit 18dp), delete (Delete 18dp, error tint)

**Dialogs:**
- Delete: `AlertDialog` with title, destructive text, "Delete" (error color) / "Cancel" buttons
- Edit: `AlertDialog` with Edit icon (primary tint), title, AH (OutlinedTextField, CoordinateFont) + Remarks (OutlinedTextField), "Save" / "Cancel"
- New Project: `AlertDialog` with Folder icon (primary tint), Name + Description OutlinedTextFields, "Create" (enabled when name not blank) / "Cancel"

### 1.11 Stakeout Panel (`StakeoutPanel`)

Fully described in section 1.4 above.

### 1.12 Layer Switcher Modal

**Current implementation:** `DropdownMenu` attached to the layer button, containing:
- "Street Map" -- `DropdownMenuItem`
- "Orthophoto (Ktimatologio)" -- `DropdownMenuItem`
- "Toggle Contours" -- `DropdownMenuItem`
- (Trig points toggle when service available)

**Proposed enhancement for M3 Expressive showcase:**
Replace `DropdownMenu` with a `ModalBottomSheet` or a floating `Surface` card positioned below the layer button, using M3 Expressive shapes and animation:

- Surface: surfaceContainerHigh bg, large shape (16dp corners), 8dp elevation
- Enter: scale from 0.9 + fade, spring(0.7, 600)
- Items: `ListItem` with leading radio indicator, trailing check icon for active layer
- Divider between basemap and overlay sections

### 1.13 Project Switcher Dropdown

**Current implementation:** `DropdownMenu` from project header `TextButton`.

**Content:**
- List of projects: each `DropdownMenuItem` with Folder leading icon + project name text
- `HorizontalDivider`
- "New Project" `DropdownMenuItem` with Add leading icon

**Dimensions:** Standard M3 DropdownMenu width (min 112dp, max 280dp), positioned below anchor.

### 1.14 Settings Sections (`SettingsPanel`)

**Layout:** Column, fullWidth, horizontalPadding 16dp, spacedBy 16dp

**Section: RECORDING**
- Label: labelLarge, primary color, padding start=16dp top=16dp bottom=4dp
- `Surface` with surfaceContainerLow bg, medium shape, containing:
  - "Averaging time" `ListItem` with dropdown trailing (TextButton showing "5s" in CoordinateFont + ArrowDropDown icon, opening DropdownMenu with options 1/3/5/10/15/30/60s)
  - Divider
  - "Min accuracy (m)" `ListItem` with `OutlinedTextField` trailing (120dp width, CoordinateFont)
  - Divider
  - "Require RTK Fix" `ListItem` with `Switch` trailing

**Section: CONNECTION**
- "Baud rate" `ListItem` with dropdown (4800-460800)
- "GGA interval" `ListItem` with dropdown (5-60s)

**Section: DISPLAY**
- "Coordinates" `ListItem` with dropdown (EGSA87 E/N, WGS84 Decimal, WGS84 DMS)

**Section: ABOUT**
- Surface with surfaceContainerLow bg, medium shape, tonalElevation 2dp, 16dp padding
- "About" titleMedium, version bodyMedium, description bodySmall
- GitHub TextButton with Code icon
- Credits block with hierarchical labelSmall headers (primary color) and bodySmall entries (outline color)
- License at bottom

### 1.15 Transform Converter with Pipeline (`TransformPanel`)

**Layout:** Column, fullWidth, horizontalPadding 16dp, spacedBy 16dp

**Section 1: Converter**
- Surface with surfaceContainerLow bg, medium shape, 16dp padding, spacedBy 12dp
- Title: "WGS84 -> EGSA87 (EPSG:2100)" titleSmall, primary
- Lat/Lon `OutlinedTextField` pair (Row spacedBy 8dp, weight 1f each), placeholder "38.0000"/"23.0000", CoordinateFont
- Height and Geoid N fields, each full-width
- "Transform" `FilledTonalButton` full-width, medium shape, SwapVert icon 18dp

**Section 2: Result**
- Surface with primaryContainer bg, medium shape, 16dp padding, spacedBy 4dp
- "EGSA87 Result" titleSmall
- `CoordRow` entries: Easting/Northing in bodySmall with CoordinateFont bold right-aligned values

**Section 3: Pipeline Steps**
- Surface with surfaceContainerLow bg, medium shape, 16dp padding, spacedBy 8dp
- Header: Timeline icon 20dp primary + "Pipeline Steps" titleSmall primary
- 8 steps (or 7 without geoid), each a `PipelineStep` (labelMedium bold label + CoordRow values), separated by `HorizontalDivider(outlineVariant)`
- Steps: Input (WGS84) -> Cartesian (HTRS07) -> Helmert -> Geographic (EGSA87) -> TM87 -> TM07 -> Grid Corrections -> Geoid Undulation

**Section 4: Helmert Parameters (static reference)**
- Surface surfaceContainerLow, medium shape, showing TX/TY/TZ/RX/RY/RZ/Scale

**Section 5: Projection Parameters (static reference)**
- TM87 and TM07 parameters

**Section 6: Grid Info (static reference)**
- GridOn icon + grid metadata
- Warning text if transform is null (error color)

---

## 2. Animation Choreography

### 2.1 Sheet Expand/Collapse

| Transition | Spring damping | Spring stiffness | Notes |
|---|---|---|---|
| Peek to PartiallyExpanded | 0.7 | 600 | Spatial fast -- snappy response to thumb gesture |
| PartiallyExpanded to Expanded | 0.8 | 380 | Spatial medium -- slightly slower, deliberate action |
| Expanded to PartiallyExpanded | 0.7 | 600 | Same snap-back as expand |
| Any to Peek (fling down) | 0.6 | 800 | Effects fast -- strong deceleration for dismissal |
| Overshoot correction | 0.5 | 400 | Visible bounce at peek stop position |

These are the default `MotionScheme.expressive()` values applied to the `BottomSheetScaffold` internal spring. The sheet uses `SheetValue` states managed by `rememberStandardBottomSheetState`.

### 2.2 Tab Switching

| Property | Spec |
|---|---|
| Container | `AnimatedContent` with `fadeIn() togetherWith fadeOut()` |
| Crossfade duration | 250ms (medium 1) |
| Easing | EmphasizedDecelerate |
| Content height | Fixed at 420dp (no content-size animation needed) |

The content inside each panel uses `verticalScroll` for overflow, so the container itself does not animate height.

### 2.3 FAB Menu Open/Close

| Phase | Property | From | To | Duration/Spring | Easing |
|---|---|---|---|---|---|
| Open - toggle icon | rotation | 0deg | 135deg | spring(0.6, 800) | -- |
| Open - toggle icon | icon swap | Add | Close | at checkedProgress 0.5 | -- |
| Open - menu items | scale | 0.0 | 1.0 | spring(0.7, 600) per item, 50ms stagger | -- |
| Open - menu items | alpha | 0.0 | 1.0 | 150ms tween | FastOutSlowIn |
| Open - scrim | alpha | 0.0 | 0.32 | 200ms | Linear |
| Close | Reverse of open | -- | -- | Same springs | -- |

These are the M3 `FloatingActionButtonMenu` built-in animations driven by `MotionScheme.expressive()`.

### 2.4 Record Button Idle to Recording Transition

| Phase | Property | Spec |
|---|---|---|
| FAB menu disappears | scale + fade | `scaleOut(targetScale = 0.8f) + fadeOut()`, 200ms |
| Recording indicator appears | scale + fade | `scaleIn(initialScale = 0.6f) + fadeIn()`, spring(0.5, 500) -- bounce |
| Countdown circle background | color | Animates from primary to recordingActive (`#D1416A`) via `animateColorAsState`, spring(0.8, 380) |
| Outer progress ring | circular sweep | `LoadingIndicator` progress from 0.0 to 1.0, linearly mapped to epoch collection |
| Inner countdown number | crossfade | `AnimatedContent` with `fadeIn(100ms) + scaleIn(0.8)` to `fadeOut(100ms)` on each epoch tick |

### 2.5 Fix Status Change (Float to Fix)

| Property | From | To | Animation |
|---|---|---|---|
| FixStatusPill container color | WarningContainer (`#FFDEA6`) | SuccessContainer (`#A1F5A3`) | `animateColorAsState` with spring(dampingRatio = 0.6, stiffness = 800) -- fast snap with slight overshoot in luminance |
| FixStatusPill content color | OnWarningContainer | OnSuccessContainer | Same spring |
| FixStatusPill scale | 1.0 | 1.15 then 1.0 | spring(dampingRatio = 0.4, stiffness = 500) -- visible bounce |
| Map dot color | Float amber hex | Fix green hex | Immediate (MapLibre property update, no Compose animation) |
| Accuracy badge color | accuracyOk | accuracyGood | `animateColorAsState`, spring(0.8, 380) |

Recommended: wrap `FixStatusPill` in `Modifier.animateContentSize()` and add `Modifier.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }` driven by a `Animatable<Float>` that targets 1.15 then settles to 1.0 on each fixQuality change.

### 2.6 Point Appearing on Map

Map points update via `LaunchedEffect(activePoints)` setting GeoJsonSource. MapLibre does not animate symbol appearance natively. For M3 Expressive showcase, add a Compose overlay animation:

| Property | Spec |
|---|---|
| Overlay circle | Draw a temporary `Canvas` circle at the projected screen position of the new point |
| Scale | 0.0 to 1.0, spring(dampingRatio = 0.4, stiffness = 500) -- strong bounce |
| Fade | 0.0 to 1.0 over 200ms, then sustained 800ms, then fade to 0.0 over 300ms |
| Color | SuccessContainer (`#A1F5A3`) at 0.5 alpha, expanding ring effect |
| Ring | Radius expands from 8dp to 32dp over 1000ms with `EmphasizedDecelerate` then fades |

After the animation completes, the MapLibre native marker is already visible beneath.

### 2.7 Stakeout Immersive Enter/Exit

**Enter:**
| Phase | Property | Spec |
|---|---|---|
| 0-100ms | Scrim overlay | Alpha 0 to 0.5, linear |
| 0-600ms | Background | Color morph to `#0F1512`, `Emphasized` easing |
| 100-600ms | Compass | Scale 0.85 to 1.0 + fade in, spring(0.9, 200) |
| 200-600ms | Distance text | Slide up 24dp + fade in, spring(0.9, 200), 100ms after compass |
| 300-600ms | Delta values | Slide up 24dp + fade in, spring(0.9, 200), 100ms after distance |

**Exit:**
| Phase | Property | Spec |
|---|---|---|
| 0-400ms | All content | Scale to 0.9 + fade out, spring(0.7, 600) |
| 200-400ms | Background | Color morph to transparent |

### 2.8 Accuracy Ring Convergence Animation

When accuracy improves (e.g., from 5.0m to 0.012m during NTRIP acquisition), the user location glow ring on the map should visually converge:

| Property | Spec |
|---|---|
| Ring radius | From `max(accuracy * scaleFactor, 18px)` to `max(newAccuracy * scaleFactor, 18px)` |
| Animation | `Animatable<Float>.animateTo()` with `tween(1000ms, CubicBezier(0.2, 0, 0, 1))` |
| Ring opacity | Increases from 0.15 to 0.25 as accuracy improves below 0.05m |
| Ring color | Transitions with accuracy threshold colors via `animateColorAsState` |

Note: this requires intercepting the MapLibre circle radius as an animated value and updating the layer property each frame. Performance-sensitive -- consider updating at 10fps maximum.

### 2.9 Epoch Averaging Countdown

| Element | Animation |
|---|---|
| Circular progress ring | `LoadingIndicator` with `progress = { epochsCollected / totalEpochsTarget.toFloat() }`, smooth linear progress |
| Countdown number | `AnimatedContent` targeting the remaining count, `fadeIn(100ms) + scaleIn(initialScale = 1.2f)` entering, `fadeOut(100ms) + scaleOut(targetScale = 0.8f)` exiting -- each tick the number scales up slightly then settles |
| Final "0" | Scale to 1.5 + color flash to SuccessContainer, then entire FAB scales down with completion feedback |
| Completion burst | `SuccessContainer` colored ring expands from 64dp to 96dp, alpha 0.8 to 0.0, over 500ms, `EmphasizedDecelerate` |

---

## 3. Micro-Interactions

### 3.1 Long-press on Map Point

**Current:** No long-press handler.

**Specified behavior:**
1. Haptic feedback: 50ms light vibration (VibrationEffect.EFFECT_TICK on API 29+)
2. Display a contextual `ModalBottomSheet` containing:
   - Point ID as titleLarge, CoordinateFont
   - Full coordinates (E/N, lat/lon, heights) in `CoordinateBlock`
   - Fix quality badge
   - Accuracy, timestamp, remarks
   - Action buttons: "Navigate To" (sets as stakeout target), "Edit", "Delete"
3. Map camera: gentle ease to center the tapped point, 300ms `CameraUpdateFactory.newLatLng`, maintaining current zoom

### 3.2 Swipe Down on Expanded Sheet

**Current behavior:** Standard `BottomSheetScaffold` physics -- velocity-based fling detection.

**Specified behavior:**
- Slow drag (< 500dp/s velocity): settles to nearest stable detent (PartiallyExpanded or Hidden based on position)
- Medium fling (500-1500dp/s): moves one detent down
- Fast fling (> 1500dp/s): collapses to peek regardless of current position
- Spring: `MotionScheme.expressive()` spatial fast (damping 0.7, stiffness 600)
- Overshoot: slight bounce at peek position (1-2dp past then correction)
- If already at peek: no further collapse (sheet does not hide completely -- always shows status bar)

### 3.3 Double-tap on Map

**Current behavior:** MapLibre default zoom-in by 1 level.

**Specified behavior:**
- MapLibre handles this natively (zoom in by 1 level, centered on tap point)
- No override needed
- Ensure the double-tap target area excludes the 48dp zones around the FixStatusPill and LayerSwitcher button

### 3.4 Pinch on Stakeout Compass

**Current:** Not handled (compass is a static Canvas).

**Specified behavior:**
- Pinch gesture on the 180dp Canvas should be intercepted with `Modifier.pointerInput`
- Scale the compass from 1.0x to a max of 1.5x
- On release: spring back to 1.0x with spring(0.5, 400)
- While scaled: show additional precision -- display bearing to 2 decimal places, show 0.1-degree tick marks
- This gesture naturally leads into the immersive stakeout mode -- if scale exceeds 1.3x and holds for 500ms, trigger immersive enter

### 3.5 Shake Device

**Current:** No shake handler.

**Specified behavior:**
- Register an `accelerometer` listener via `SensorManager`
- Threshold: 15 m/s^2 acceleration delta within 500ms
- Action: Quick Mark -- records an instant single-epoch point with no averaging
- Feedback: strong haptic (300ms vibration) + `TONE_PROP_ACK` + Snackbar "Quick mark recorded"
- Guard: only active when (a) project is selected, (b) fix is available, (c) not currently averaging
- Cooldown: 3 seconds between shake events

### 3.6 Volume Button Press During Survey

**Current:** No volume button override.

**Specified behavior:**
- Override `Activity.dispatchKeyEvent` for `KEYCODE_VOLUME_UP` and `KEYCODE_VOLUME_DOWN`
- Volume UP: Start point recording (equivalent to tapping "Record Point" in FAB menu)
  - If already recording: no-op (prevent double-trigger)
  - Haptic: 100ms tick
- Volume DOWN: Add vertex (in line/polygon mode) or cancel current recording (in point mode)
  - In point mode while averaging: cancel recording
  - In line/polygon mode: add vertex at current position
  - Haptic: 50ms tick
- Guard: only active when survey panel is the selected tab or a project is active
- Volume buttons return to normal audio control when sheet is collapsed

---

## 4. Glassmorphism Specification

### 4.1 Bottom Sheet Glass Treatment

**Background blur:**
- `Modifier.blur(radius = 24.dp)` applied to the sheet's background layer (not the content)
- Implementation: `RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)` on the sheet `Surface`'s background
- Alternatively: use `Modifier.graphicsLayer { renderEffect = ... }` on API 31+

**Surface opacity:**
- Light mode: `surface.copy(alpha = 0.78f)` -- the `#F5FBF7` surface at 78% opacity
- Dark mode: `surface.copy(alpha = 0.72f)` -- the `#0F1512` surface at 72% opacity
- The lower dark-mode opacity lets more map content bleed through, appropriate for satellite imagery viewing

**Border treatment:**
- Top edge: 1dp hairline border in `outlineVariant.copy(alpha = 0.3f)` -- barely visible frost edge
- No side or bottom borders (sheet extends to screen edges)
- Corner: same 28dp top radius as current

**Sheet shadow:**
- Remove or reduce `sheetShadowElevation` from 8dp to 2dp -- glassmorphism relies on transparency, not drop shadow
- Add a subtle inner shadow via gradient overlay at the top edge: 4dp tall gradient from `scrim.copy(alpha = 0.05f)` to transparent

### 4.2 Appearance Over Different Map Types

**Over street map (vathra.xyz vector tiles):**
- Muted colors bleed through -- the teal-green surfaces pick up subtle map hues
- Text remains highly readable due to the 78% opacity
- The grid of streets creates a subtle texture effect behind the glass

**Over satellite imagery (Ktimatologio orthophoto):**
- Rich earth tones bleed through -- greens, browns, grays from aerial photos
- The 78% opacity ensures readability but the bottom sheet feels grounded in the terrain
- Consider increasing to 82% alpha if specific orthophoto tiles prove too busy (rural areas with dense vegetation)

**Over dark basemap (future dark mode map style):**
- Glass effect is most dramatic -- dark map + semi-transparent dark surface creates depth
- The 72% dark opacity with 24dp blur creates a frosted-glass effect showing map features as silhouettes

### 4.3 Performance Considerations

**API level gating:**
- `RenderEffect` (blur) requires API 31 (Android 12+)
- On API 26-30: fall back to solid surface (current behavior, `alpha = 1.0f`)
- Feature flag: `val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`

**Render performance:**
- Blur is GPU-accelerated on modern SoCs but can cause frame drops on mid-range devices
- Benchmark threshold: if frame time exceeds 16ms (sub-60fps), disable blur
- Use `FrameMetricsAggregator` or `Choreographer.FrameCallback` to detect performance
- Mid-range fallback: reduce blur radius to 12dp, increase surface alpha to 0.88f
- Budget devices (< 4GB RAM or GPU score < threshold): solid surface, no blur

**Composition strategy:**
- The sheet and map should be on separate composition layers (`Modifier.graphicsLayer {}`)
- Blur operates on the layer beneath, not on the sheet content itself
- Sheet content (text, buttons) remains crisp -- only the background is blurred

---

## 5. Component Inventory

### 5.1 `FloatingActionButtonMenu` + `ToggleFloatingActionButton`
- **Where:** Map idle state, bottom-right, when project active + fix available
- **Usage:** Contains "Record Point", "Quick Mark", "Stakeout" actions
- **Behavior:** `ToggleFloatingActionButton` toggles `expanded` state, icon morphs Add to Close at 50% progress

### 5.2 `FloatingActionButtonMenuItem`
- **Where:** Inside `FloatingActionButtonMenu`, 3 items
- **Usage:** Each survey quick-action with icon + text label

### 5.3 `LoadingIndicator` (indeterminate)
- **Where:** (a) Recording FAB outer ring with progress, 80dp (b) Recording controls row, 24dp indeterminate spinner
- **Usage:** Epoch progress visualization and activity indicator

### 5.4 `ContainedLoadingIndicator`
- **Where:** (a) Recording controls, fullWidth x 8dp height, with progress callback (b) GNSS connecting state, 24dp (c) NTRIP connecting state, 24dp (d) Stakeout waiting, 48dp (e) Export in progress, 24dp (f) Sourcetable fetch, 20dp
- **Usage:** Determinate and indeterminate progress within contained surfaces

### 5.5 `BottomSheetScaffold` + `rememberBottomSheetScaffoldState`
- **Where:** Main layout, wrapping the entire screen
- **Peek height:** 148dp
- **Shape:** 28dp top corners
- **States:** PartiallyExpanded (initial), Expanded, Hidden (not used -- always shows peek)

### 5.6 `SecondaryTabRow` + `Tab`
- **Where:** Bottom sheet, below project header
- **Tabs:** 4 (GNSS, Survey, Stakeout, Tools)
- **Style:** icon (20dp) + labelMedium text per tab

### 5.7 `SegmentedButton` + `SingleChoiceSegmentedButtonRow`
- **Where:** (a) GNSS connection type (BT/USB/Internal), 3 segments (b) Recording mode (Point/Line/Area), 3 segments
- **Icons:** 18dp per segment, using `SegmentedButtonDefaults.Icon` for active state

### 5.8 `Surface` (various roles)
- Map overlay pill, cards, connected state backgrounds, settings sections, trig point list items, coordinate blocks
- Used pervasively as the primitive container with shape + color + elevation

### 5.9 `ElevatedCard` + `CardDefaults`
- **Where:** Point cards in survey list
- **Elevation:** 1dp

### 5.10 `OutlinedCard`
- **Where:** Recording controls card

### 5.11 `TonalCard` (custom component)
- **Where:** GNSS receiver section, NTRIP section, line/polygon recording state
- **Implementation:** `Card` with surfaceContainerHigh bg, large shape, 0dp elevation

### 5.12 `AlertDialog`
- **Where:** Delete point confirmation, edit point, new project (2 instances: from survey panel and from header dropdown)
- **Style:** Standard M3 dialog with icon, title, text content area, confirm/dismiss buttons

### 5.13 `DropdownMenu` + `DropdownMenuItem`
- **Where:** Layer switcher, project switcher, settings dropdowns (averaging time, baud rate, GGA interval, coordinate format), NTRIP preset picker

### 5.14 `ExposedDropdownMenuBox`
- **Where:** Bluetooth device picker, USB device picker, NTRIP caster preset, mountpoint picker
- **Style:** `TextField` with read-only flag + trailing icon + anchored dropdown

### 5.15 `Switch`
- **Where:** Settings "Require RTK Fix" toggle

### 5.16 `ListItem`
- **Where:** Settings panel -- each setting row

### 5.17 `SuggestionChip`
- **Where:** NTRIP corrections stale warning
- **Colors:** errorContainer bg, onErrorContainer label/icon, error border

### 5.18 `AssistChip`
- **Where:** Export status feedback ("CSV shared")

### 5.19 `FilledTonalButton`
- **Where:** Record Point, Start Line/Polygon, Add Vertex, Transform, Export CSV
- **Shape:** Medium (12dp) or full-width

### 5.20 `OutlinedButton`
- **Where:** Cancel, Clear Target, Disconnect, Get List, Finish, Import CSV, Undo, export formats
- **Shape:** Medium or small depending on context

### 5.21 `Button` (filled)
- **Where:** Connect Bluetooth, Connect USB, Connect Internal GPS, NTRIP Connect

### 5.22 `TextButton`
- **Where:** Dialog actions (Create, Save, Cancel, Delete), project name in header, settings value display, GitHub link

### 5.23 `FilledIconButton`
- **Where:** Layer switcher button (top-right map overlay)
- **Custom colors:** surface at 0.92 alpha bg, onSurface content

### 5.24 `IconButton`
- **Where:** Point card actions (camera, edit, delete), undo vertex

### 5.25 `TextField` / `OutlinedTextField`
- **Where:** All form inputs throughout the app
- **TextField:** Used inside ExposedDropdownMenuBox (device pickers, NTRIP fields)
- **OutlinedTextField:** Used for free-form input (coordinates, antenna height, remarks, min accuracy, transform inputs)

### 5.26 `Snackbar` + `SnackbarHost`
- **Where:** Point recorded confirmation, NTRIP disconnect alert
- **Position:** Padded bottom=160dp to clear sheet peek

### 5.27 `AnimatedContent` / `AnimatedVisibility`
- **Where:** Tab panel switching (AnimatedContent), FAB show/hide (AnimatedVisibility)

### 5.28 `HorizontalDivider`
- **Where:** Between status bar and project header, between project header and tabs, settings list items, NTRIP form sections, pipeline steps, dropdown menu sections

### 5.29 `Canvas` (custom drawing)
- **Where:** Skyplot (220dp), StakeoutArrow (180dp), FixStatusPill dot (6dp), NtripIndicator (8dp), trig point status dot (10dp), constellation legend dots (8dp)

### 5.30 Components NOT currently used but available in M3 1.5.0-alpha17 for future use

| Component | Proposed usage |
|---|---|
| `HorizontalFloatingToolbar` | Map editing toolbar for line/polygon vertex operations |
| `VerticalFloatingToolbar` | Map zoom/location controls replacing MapLibre native buttons |
| `FlexibleBottomAppBar` | Alternative to current tab row if tabs exceed 5 |
| `DockedToolbar` | Stakeout immersive mode bottom control bar |
| `SplitButtonLayout` | Record button with split dropdown for mode selection |
| `ButtonGroup` | Export format selection (CSV/GeoJSON/DXF/SHP as grouped buttons) |
| `MaterialShapes` / `RoundedPolygon` | Constellation legend icons (pentagon for satellite, hexagon for base station), stakeout target crosshair shape |

---

## 6. Typography Map (Complete Element-by-Element)

### 6.1 Map Overlay
| Element | Role | Font | Size | Weight |
|---|---|---|---|---|
| Fix pill label | labelMedium | CoordinateFont | 11sp | W600 |
| Layer button icon | -- | -- | 22dp | -- |

### 6.2 Status Bar (Sheet Peek)
| Element | Role | Font |
|---|---|---|
| Fix pill (duplicate) | labelMedium | CoordinateFont |
| Accuracy badge "H+/-0.012m" | labelMedium | CoordinateFont |
| "Waiting for fix..." | bodyMedium | System |
| Constellation chip "GPS 12" | labelMedium | CoordinateFont |
| Coordinate label "EGSA87" | labelSmall | System |
| Coordinate value "E 456123.456" | bodyMedium | CoordinateFont |
| Altitude "H:38.52m h:76.12m" | labelMedium | CoordinateFont |
| NTRIP rate "NTRIP 1KB/s" | labelMedium | CoordinateFont |

### 6.3 Project Header
| Element | Role | Font |
|---|---|---|
| Project name "Site Alpha" | titleSmall | System |
| Point count "42 pts" | labelMedium | CoordinateFont |

### 6.4 Tab Row
| Element | Role | Font |
|---|---|---|
| Tab label "GNSS" | labelMedium | System |

### 6.5 GNSS Panel
| Element | Role | Font |
|---|---|---|
| Section label "GNSS RECEIVER" | labelSmall | System |
| Segment label "BT" | bodySmall (default) | System |
| "Connecting" | bodyLarge | System |
| "RTK Fix" (connected state) | bodyLarge | System |
| "Satellites" | bodyMedium | System |
| "H-Accuracy" (label) | bodyMedium | System |
| "0.012 m" (value) | bodyMedium | CoordinateFont |
| Connection method "Bluetooth" | labelMedium | System |
| Skyplot cardinal "N" | custom 11sp | System |
| Skyplot elevation "30 deg" | custom 8sp | System |
| Skyplot PRN "G12" | custom 7sp | System |
| Skyplot legend "GPS (12)" | bodySmall | System |

### 6.6 Survey Panel
| Element | Role | Font |
|---|---|---|
| "Select a project..." prompt | titleMedium | System |
| Help text | bodyMedium | System |
| Segment "Point" | bodySmall (default) | System |
| "AH (m)" label | bodySmall | System |
| AH value | bodyMedium | CoordinateFont |
| "Averaging: 3/10" | bodyMedium | CoordinateFont |
| "Cancel" button | bodyMedium | System |
| "Points (42)" section header | titleLarge | System |
| Point ID "P001" | titleSmall | CoordinateFont |
| Coords "E: 456123.456" | bodySmall | CoordinateFont |
| Height "H=38.521 N=40.12" | bodySmall | CoordinateFont |
| Accuracy badge | labelSmall | CoordinateFont |
| Remarks text | labelSmall | System |
| Fix badge "RTK Fix" | labelLarge | CoordinateFont |
| Vertex count "5 vertices" | bodyMedium | CoordinateFont |
| Measurement "123.45 m" | titleMedium | CoordinateFont |
| "Recording Line" | titleMedium | System |

### 6.7 Stakeout Panel
| Element | Role | Font |
|---|---|---|
| Distance "0.234 m" | displaySmall | CoordinateFont |
| Bearing "NE (34.2 deg)" | titleMedium | System |
| Delta label "deltaE" | bodyMedium | System |
| Delta value "0.123 m" | bodyMedium | CoordinateFont |
| Target name | bodyMedium | CoordinateFont |
| "Import Target from CSV" | bodyMedium | System |
| Trig point ID "GYS 12345" | titleSmall | CoordinateFont |
| Trig point name | bodySmall | System |
| Trig point elevation "456m" | labelMedium | CoordinateFont |
| "Target (EGSA87)" | titleSmall | System |
| "Waiting for fix..." | bodyLarge | System |

### 6.8 Stakeout Immersive
| Element | Role | Font |
|---|---|---|
| Distance "0.234" | displayLarge (48sp) | CoordinateFont |
| Unit "m" | headlineLarge (24sp) | System |
| Bearing "NE (34.2 deg)" | titleLarge (18sp) | System |
| DeltaE "+0.123" | displayMedium (36sp) | CoordinateFont |
| DeltaN "-0.456" | displayMedium (36sp) | CoordinateFont |
| Target "BM-0042" | titleLarge (18sp) | System |

### 6.9 FAB & Recording
| Element | Role | Font |
|---|---|---|
| FAB menu item "Record Point" | bodyMedium (default) | System |
| Countdown "7" | headlineSmall (14sp) | CoordinateFont |

### 6.10 Tools Panel
| Element | Role | Font |
|---|---|---|
| Section label "EXPORT / IMPORT" | labelLarge | System |
| "Project: Site Alpha" | titleSmall | System |
| Button labels "Export CSV" | bodyMedium | System |
| Status chip "CSV shared" | labelMedium | CoordinateFont |
| Transform title "WGS84 -> EGSA87" | titleSmall | System |
| Pipeline step "1. Input (WGS84)" | labelMedium | System |
| Pipeline value "38.00000000 deg" | bodySmall | CoordinateFont |
| Helmert label "TX" | bodySmall | System |
| Helmert value "203.437 m" | bodySmall | CoordinateFont |
| Grid info "408 x 422 nodes" | bodySmall | CoordinateFont |

### 6.11 Settings Panel
| Element | Role | Font |
|---|---|---|
| Section "RECORDING" | labelLarge | System |
| Setting title "Averaging time" | bodyLarge (default ListItem) | System |
| Setting description | bodyMedium (default ListItem) | System |
| Setting value "5s" | bodyMedium | CoordinateFont |
| "About" | titleMedium | System |
| Version "OpenTopo 1.7.0" | bodyMedium | System |
| Credits text | bodySmall | System |
| Credits category "Transformation engine" | labelSmall | System |
| "License: GNU AGPL v3.0" | bodySmall | System |

---

## 7. Dark Mode Specification

### 7.1 Surface Colors at Each Tonal Level

| Light | Hex | Dark | Hex |
|---|---|---|---|
| surface | `#F5FBF7` | surface | `#0F1512` |
| surfaceContainerLow | `#F5FBF7` | surfaceContainerLow | `#151B18` (estimated, 1 step up) |
| surfaceContainer | `#EFF5F1` | surfaceContainer | `#1B211E` |
| surfaceContainerHigh | `#E9EFEB` | surfaceContainerHigh | `#252B28` |
| surfaceContainerHighest | `#E3EAE6` | surfaceContainerHighest | `#303634` |

(Dark mode surface containers follow the M3 tonal palette generated from seed `#006B5E`. The exact values depend on the dynamic scheme generator, but the progression is approximately 4-5% luminance steps from the dark surface base.)

### 7.2 Status Color Adaptation

All semantic survey colors have explicit dark variants defined in `Color.kt`:

| Semantic | Light | Dark |
|---|---|---|
| rtkFix | `#1B6D2F` (dark green) | `#A1F5A3` (bright green) |
| rtkFloat | `#7B5800` (dark amber) | `#FFDEA6` (bright amber) |
| noFix | `#BA1A1A` (dark red) | `#FFB4AB` (bright red) |
| recordingActive | `#D1416A` (muted pink) | `#FFD9E0` (bright pink) |
| recordingProgress | `#006B5E` (teal) | `#50DBC2` (bright teal) |

The `LocalSurveyColors` CompositionLocal provides the correct set based on `isSystemInDarkTheme()`.

In dark mode, status colors become pastel/bright on dark backgrounds (reversed contrast model), following M3 dark theme guidelines where container colors become dark and content colors become light.

The FixStatusPill containers in dark mode:
- RTK Fix: dark primaryContainer-like green bg + bright green text
- Float: dark warningContainer-like amber bg + bright amber text
- No Fix: dark errorContainer bg + bright error text
These should use the same SuccessContainer/WarningContainer values but with inverted on-container colors. Currently the containers (SuccessContainer, WarningContainer) are light-mode only -- dark mode needs dark equivalents:
- Dark SuccessContainer: `#002108` (OnSuccessContainer from light as bg)
- Dark OnSuccessContainer: `#A1F5A3` (light SuccessContainer as text)
- Same inversion for Warning

### 7.3 Map Style Switching

**Current:** Single vathra.xyz vector basemap style loaded from `style_vathra.json` (light).

**Dark mode specification:**
- Provide `style_vathra_dark.json` asset with inverted/dark color palette for roads, buildings, water, land
- Dark basemap colors: land `#1A1C1E`, water `#001849`, roads `#353739`, labels white/light gray
- On theme change: `map.setStyle(Style.Builder().fromJson(darkStyleJson))` -- this reloads the entire style but preserves source data
- Or: use MapLibre runtime style API to update individual layer colors without full reload
- Transition: Immediate swap (no crossfade -- MapLibre does not support map crossfade)

**Orthophoto layer:** Unchanged in dark mode (aerial imagery is inherently "light" -- applying darkness would distort colors). But consider adding a slight dark tint overlay:
- Add a semi-transparent FillLayer above the raster with color `#000000` at 0.15 opacity to slightly darken the orthophoto for eye comfort

**Survey markers:** Use brighter colors for dark basemap visibility:
- Already handled -- MapLibre markers use bright absolute colors (#00E676, #FFAB00, etc.) with white 3px stroke, readable on both light and dark basemaps
- Point labels: already white text with black halo -- perfect for both modes

### 7.4 Stakeout Immersive: Always Dark

**Rule:** The stakeout immersive full-screen mode ALWAYS renders with dark scheme colors, regardless of the system theme setting.

**Implementation:** Wrap the immersive composable in:
```
MaterialExpressiveTheme(colorScheme = DarkColorScheme, ...) {
    CompositionLocalProvider(LocalSurveyColors provides DarkSurveyColors) {
        StakeoutImmersiveScreen(...)
    }
}
```

This forces all `MaterialTheme.colorScheme.*` references inside the immersive screen to use dark values. The dark background (`#0F1512`) ensures:
- Maximum contrast for the stakeout distance number (bright teal/green/red on near-black)
- Reduced screen brightness for battery conservation during extended stakeout operations
- Glare reduction when used outdoors in variable lighting
- Consistent appearance regardless of the user's system theme preference

**Status bar in immersive:** Set system bars to transparent with light icons (white icons on dark background) using `enableEdgeToEdge()` or `WindowInsetsController.setAppearanceLightStatusBars(false)`.

---

This specification covers every screen, component, color, typography role, animation parameter, and interaction in the OpenTopo app. It is directly implementable using the existing M3 Expressive 1.5.0-alpha17 component library and the `MotionScheme.expressive()` motion system. All dp measurements, hex colors, spring constants, and animation durations are concrete values ready for Compose code.