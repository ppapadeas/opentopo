# OpenTopo UX/UI Evaluation & Design Directions

> Comprehensive analysis based on deep research of Emlid Flow, Trimble Access, Leica Captivate, SW Maps, QField, MapIt GIS, Google Maps, Strava, Material 3 Expressive, and 50+ Dribbble/Behance concepts.

---

## Current State Assessment: 7/10

### What works well
- Map-centric layout with persistent bottom sheet (Google Maps pattern)
- FixStatusPill with pulsing dot — clear, accessible, beautiful
- ConstellationChips — categorical colors instantly readable
- Point/Line/Area mode switcher — intuitive segmented control
- Haptic + audio feedback on recording — professional touch
- CoordinateBlock component — clean monospace display
- Teal-green brand identity (#006B5E) — unique in the survey app space

### Critical issues
1. **Status bar too dense** — 5 rows in 100dp peek. Hard to parse at a glance
2. **FAB menu hidden** — only appears when conditions met; users don't know it exists
3. **DGPS and Float use same color** — indistinguishable in lists
4. **Typography too small for field** — 10-11sp labels illegible in sunlight
5. **Large point lists not paginated** — 100+ points kills scrolling
6. **ToolsPanel is a catch-all** — Export + Transform + Settings feel incoherent
7. **Layer switcher shows no active state** — no feedback on current map layer
8. **Stakeout compass too small** — 180dp hard to read on mobile

---

## Three Design Directions

### Direction A: "Emlid Flow Minimal"
**Philosophy:** Ruthless simplicity. Remove everything not needed in the next 3 seconds.

**Key patterns:**
- Global status strip (single row): `[●RTK Fix] [12 sats] [H:0.014m] [NTRIP ●]`
- Map fills 90% of screen. Sheet collapsed to single status line
- FAB is always visible (no conditions). Tap = record point. Long-press = mode menu
- No tabs in sheet. Sheet content is contextual (changes based on current workflow)
- Stakeout is full-screen immersive (dark background, huge distance display)
- Settings accessed via gear icon on map, opens separate screen
- Maximum 3 taps to any action

**Color:** Light theme only, high-contrast. Green primary (#006B5E), white surfaces, black text. Neon markers on map.

**Best for:** New users, quick field checks, simple topo surveys.

**Trade-off:** Power users lose quick access to advanced features.

---

### Direction B: "Professional Surveyor"
**Philosophy:** Data density with clear hierarchy. Every piece of information a surveyor needs, organized by importance.

**Key patterns:**
- Status strip (2 rows): Row 1 = fix + accuracy + NTRIP. Row 2 = coordinates
- Three-stop bottom sheet: collapsed (status), half (active tool), full (data tables)
- 4-5 tabs inside the sheet for tool switching (current pattern, refined)
- Context-sensitive softkeys at bottom of sheet (Trimble Access pattern)
- Split button for recording: primary = record, secondary = settings/code
- Stakeout with bull's-eye threshold indicator (Emlid Flow pattern)
- Point list with search, sort, filter (LazyColumn with pagination)
- Vertical floating toolbar on right edge for map controls

**Color:** Adaptive (ambient light sensor). Light mode for outdoor, dark for indoor. High-contrast semantic colors. Yellow text with black halo on satellite imagery.

**Best for:** Professional surveyors doing cadastral, engineering, and topographic surveys.

**Trade-off:** Steeper learning curve, more UI elements on screen.

---

### Direction C: "M3 Expressive Showcase"
**Philosophy:** Push Material 3 Expressive to its limits. Every interaction is animated, every surface is tonal, every transition is spring-based.

**Key patterns:**
- Docked toolbar (M3E FlexibleBottomAppBar) for global actions: record, my location, layers
- FAB Menu (M3E FloatingActionButtonMenu) for survey mode: Point, Line, Polygon, Stakeout
- Tonal surface hierarchy: map (level 0) → sheet (level 1) → cards (level 2) → inputs (level 3)
- Shape morphing: record button morphs from circle to rounded square when active
- Spring-based motion on all transitions (sheet expand, tab switch, point appear)
- Loading Indicator (M3E shape-morph animation) for averaging and NTRIP connection
- Glassmorphism: semi-transparent sheet lets map bleed through
- Pill-shaped everything: status, buttons, chips, badges
- Constellation pie chart in status area (not full skyplot)

**Color:** Dynamic color (Material You) with teal-green fallback. Expressive tonal surfaces. Neon accents for map markers. Pink accent for recording active state.

**Best for:** Showcasing modern Android design. Appeal to tech-forward users.

**Trade-off:** May feel over-designed for surveyors who want raw data, not animations.

---

## Recommended Hybrid: "Professional Expressive"

Take the best from each direction:

### From Direction A (Minimal):
- Single-row status strip for collapsed state
- FAB always visible (no conditions)
- Stakeout immersive full-screen mode

### From Direction B (Professional):
- Three-stop bottom sheet with tabs
- Context-sensitive softkeys
- Point list with search/sort/filter
- Adaptive light/dark based on ambient sensor

### From Direction C (M3 Expressive):
- FAB Menu for mode switching
- Spring-based motion on transitions
- Tonal surface hierarchy
- Loading Indicator for averaging
- Shape morphing on record button

---

## Specific Improvements (Priority Order)

### P0 — Fix immediately
1. **Distinguish DGPS from Float** — use distinct yellow (#FFD600) for DGPS vs amber (#FFAB00) for Float
2. **Increase minimum font size** — 12sp minimum for all text, 14sp for field values
3. **Compress status bar** — single row when collapsed: `[●Fix] [12sv] [H:0.014] [NTRIP●]`
4. **FAB always visible** — remove the hasFix && hasProject conditions; show disabled state instead
5. **Add active layer indicator** — checkmark or filled circle in layer switcher dropdown

### P1 — Next release
6. **Paginate point list** — LazyColumn with 50-item pages, search by ID, sort by date/accuracy
7. **Enlarge stakeout compass** — 240dp minimum, add bearing degrees in center
8. **Split ToolsPanel** — Export as sheet action (overflow menu), Transform as standalone, Settings as separate screen
9. **Add bull's-eye threshold** to stakeout — green zone at configurable distance
10. **Adaptive theme** — auto light/dark based on ambient light sensor

### P2 — Future
11. **Radial/pie context menu** on long-press of map point
12. **Voice commands** for hands-free field operation
13. **Haptic stakeout guidance** — directional vibration patterns
14. **Accuracy convergence animation** — contracting circle as RTK converges
15. **Survey coverage heatmap** — density overlay revealing gaps
16. **Home screen widget** — project name, point count, GNSS status
17. **Glove mode** — enlarged targets, volume button mapping

---

## Innovative Features (No Competitor Has These)

| Feature | Description | Difficulty |
|---------|-------------|------------|
| **Ambient-adaptive theme** | Auto light/dark from lux sensor | Medium |
| **Haptic stakeout** | Directional vibration patterns for hands-free navigation | Medium |
| **Pie context menu** | Long-press radial menu on map points | Medium |
| **Accuracy convergence ring** | Animated shrinking circle showing RTK improving | Easy |
| **Coverage heatmap** | Point density overlay for spatial gap analysis | Medium |
| **Voice commands** | "Store point", "Code manhole", "Stakeout Alpha-7" | Hard |
| **Wear OS companion** | RTK status + Store button on wrist | Hard |
| **PiP stakeout** | Floating compass visible in other apps | Easy |
| **Cross-section swipe** | Two-finger line → elevation profile | Hard |
| **Glove mode** | One toggle: 64dp targets, volume buttons, large fonts | Easy |
| **Home widget** | Live survey status on home screen | Medium |

---

## Color Palette Refinement

### Current issues
- DGPS/Float same color → indistinguishable
- Dark mode recording color too bright
- Some dark mode contrasts fail WCAG AA

### Proposed fix status colors (accessible)
| Status | Light | Dark | Shape (for color-blind) |
|--------|-------|------|------------------------|
| RTK Fix | #1B6D2F | #A1F5A3 | ● filled circle |
| RTK Float | #E65100 | #FFAB40 | ◐ half circle |
| DGPS | #F9A825 | #FFD54F | △ triangle |
| GPS | #1565C0 | #64B5F6 | □ square |
| No Fix | #C62828 | #EF9A9A | ✕ cross |

Always pair color with shape — never rely on color alone (WCAG 1.4.1).

### Outdoor optimization
- Light mode: cream white (#FFFDF7) background, not pure white (reduces glare)
- Minimum contrast ratio: 7:1 (exceeds WCAG AAA)
- Bold weights (W600+) for all field-critical values
- Touch targets: 48dp minimum, 56dp for record/stakeout buttons

---

## Navigation Architecture Recommendation

```
┌────────────────────────────────────────────┐
│ Status strip (collapsed): Fix│Sats│Acc│NTRIP│
├────────────────────────────────────────────┤
│                                            │
│              MAP (always visible)           │
│                                            │
│   [Fix pill]                    [Layers]   │
│                                            │
│                                            │
│                              [FAB Menu]    │
├────────────────────────────────────────────┤
│ Sheet (half): [GNSS│Survey│Stake│⋮More]    │
│ ┌────────────────────────────────────────┐ │
│ │ Active panel content                   │ │
│ │ (scrollable)                           │ │
│ └────────────────────────────────────────┘ │
├────────────────────────────────────────────┤
│ Sheet (full): Detailed data + settings     │
└────────────────────────────────────────────┘
```

- **4 tabs**: GNSS, Survey, Stakeout, ⋮More (Export/Transform/Settings)
- **FAB Menu**: always visible, opens to Point/Line/Polygon/Quick Mark
- **Status strip**: single compact row in collapsed state
- **Project header**: part of Survey tab, not global

---

## Sources

### Surveying apps
- [Emlid Flow](https://emlid.com/emlid-flow/)
- [Trimble Access](https://help.fieldsystems.trimble.com/trimble-access/latest/en/)
- [Leica Captivate](https://leica-geosystems.com/products/total-stations/software/leica-captivate)
- [SW Maps](https://aviyaantech.com/swmaps/)
- [QField 4.0](https://qfield.org/blog/2025/12/17/qfield-4.0-aare/)
- [Mergin Maps](https://merginmaps.com/docs/field/mobile-app-ui/)
- [MapIt GIS 3.0](https://mapitgis.com/blog/3.0.0/)

### Design systems
- [Material 3 Expressive](https://supercharge.design/blog/material-3-expressive)
- [Google Maps 2025 Redesign](https://9to5google.com/2025/04/24/google-maps-sheet-redesign-android/)
- [Strava 2025 Redesign](https://press.strava.com/articles/strava-launches-redesigned-record-experience)
- [Map UI Patterns](https://mapuipatterns.com/)

### Accessibility
- [WCAG 1.4.1 Use of Color](https://www.w3.org/WAI/WCAG21/Understanding/use-of-color.html)
- [Salesforce Color-Blind Maps](https://www.salesforce.com/blog/how-we-designed-salesforce-maps-to-be-color-blind-friendly/)
- [Industrial UX for Sunlight](https://medium.com/@callumjcoe/industrial-ux-sunlight-susceptible-screens-2e52b1d9706b)
