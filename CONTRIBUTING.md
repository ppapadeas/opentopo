# Contributing to OpenTopo

Thank you for your interest in contributing to OpenTopo! This guide will help you get started.

## Development Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/ppapadeas/opentopo.git
   cd opentopo
   ```

2. **Install Android Studio** (recommended) or ensure you have JDK 17+ and Android SDK API 35.

3. **Run the tests:**
   ```bash
   ./gradlew :lib-transform:test          # Transformation engine (JVM, no device)
   ./gradlew :app:testDebugUnitTest       # App unit tests (JVM)
   ./gradlew assembleDebug                # Build APK
   ```

4. **Open in Android Studio:** File > Open > select the `opentopo` directory. Let Gradle sync complete.

## Project Structure

- **`lib-transform/`** - Pure Kotlin/JVM transformation library. No Android dependencies. This is where the coordinate math lives.
- **`app/`** - Android application. Jetpack Compose UI, Bluetooth/USB GNSS, NTRIP, Room database.
- **`reference/`** - Development reference materials (not shipped in the app).
- **`docs/`** - Project documentation.

## Coding Conventions

- **Language:** Pure Kotlin throughout. No Java.
- **Async:** Coroutines. No RxJava.
- **State:** StateFlow for reactive state.
- **UI:** Jetpack Compose. No XML layouts.
- **Dependencies:** No proprietary libraries. All dependencies must be open-source compatible.
- **Coordinates:** All positions stored internally as WGS84 lat/lon/h. GGRS87 computed on demand.
- **Precision:** Double precision for all coordinate math.
- **Formatting:** Standard Kotlin style (`kotlin.code.style=official`).

## Making Changes

1. **Fork** the repository and create a feature branch:
   ```bash
   git checkout -b feature/my-feature
   ```

2. **Write tests first** when adding transformation logic or NMEA parsing.

3. **Run all tests** before submitting:
   ```bash
   ./gradlew :lib-transform:test :app:testDebugUnitTest
   ```

4. **Submit a pull request** against `main` with a clear description of the change.

## Pull Request Guidelines

- Keep PRs focused on a single change.
- Include tests for new functionality.
- Update `CHANGELOG.md` under `[Unreleased]`.
- Ensure CI passes (tests, lint, build).
- Reference any related issues.

## Reporting Bugs

Use [GitHub Issues](https://github.com/ppapadeas/opentopo/issues) with the bug report template. Include:
- Device model and Android version
- GNSS receiver model
- Steps to reproduce
- Expected vs. actual behavior
- Logs if available

## Feature Requests

Use [GitHub Issues](https://github.com/ppapadeas/opentopo/issues) with the feature request template.

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). Please be respectful and constructive.

## License

By contributing, you agree that your contributions will be licensed under the [AGPL-3.0](LICENSE).
