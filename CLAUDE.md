# OpenTopo Development Guide

## Quick Reference

- **HEPOS transformation spec:** `docs/HEPOS_SPECIFICATION.md`
- **Architecture:** `docs/ARCHITECTURE.md`
- **Roadmap:** `docs/ROADMAP.md`
- **Contributing:** `CONTRIBUTING.md`

## Build & Test

```bash
./gradlew :lib-transform:test              # Transformation engine (JVM)
./gradlew :app:testDebugUnitTest           # App unit tests (JVM)
./gradlew assembleDebug                    # Debug APK
```

## Coding Conventions

- Pure Kotlin (no Java), Coroutines (no RxJava), StateFlow, Compose (no XML layouts)
- No proprietary dependencies
- All coordinates internally WGS84 lat/lon/h; GGRS87 computed on demand
- Double precision for all coordinate math

## Key Architecture Decisions

- `lib-transform` is pure Kotlin/JVM — no Android deps, testable on JVM
- `CorrectionGrid` takes `InputStream` — app opens assets, passes stream to library
- Grid files loaded once per HeposTransform instance, cached in memory
- NTRIP RTCM data routed to both BT and USB services (whichever is connected accepts)
- NTRIP configs are Room-backed `NtripProfile` rows; `NtripProfileRepository` auto-connects the transport on active-profile change and derives `NtripConnectionState` from raw `NtripClient.state` (10 s Live→Stale)
- Map-centric UI: `MainMapScreen` with `BottomSheetScaffold`; full-screen overlays (stakeout HUD, NTRIP profile manager + editor) rendered at the composable root with `WindowInsets.systemBars` safe zones
- Five screens are pixel-locked to `opentopo-v2.html` — if you touch `MainMapScreen`, `ConnectionPanel`, `SurveyPanel`, `StakeoutPanel`, or `TrigPanel`, diff against the mockup before shipping

## Basemap

- Vector tiles: `https://vathra-tiles.vathra.workers.dev/{greece,contours}.json` (Cloudflare Worker fronting R2-hosted PMTiles)
- Style: `app/src/main/assets/style_vathra.json`, derived from `@protomaps/basemaps@5.7.1` LIGHT with `lang: 'el'`, plus hand-written `contours-lines` / `contours-labels` layers
- **Do not re-include `water_label_ocean` or `water_label_lakes` from the protomaps library** — their nested `is-supported-script` multi-name expressions silently break MapLibre Android 11.8.4, leaving the map blank
- Initial camera uses `LocationManager.getLastKnownLocation` at zoom 15, falling back to (38.5, 23.8, zoom 7) when unavailable
