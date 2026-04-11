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
- Map-centric UI: MainMapScreen with BottomSheetScaffold containing tool panels
