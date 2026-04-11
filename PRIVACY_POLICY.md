# Privacy Policy

**Last updated:** April 2026

## Overview

OpenTopo is an open-source Android application for GNSS surveying. Your privacy is important to us. This policy explains what data OpenTopo handles and how.

## Data Collection

**OpenTopo does not collect, transmit, or store any personal data on external servers.**

### Data processed locally on your device

- **GNSS position data** - Coordinates received from your connected GNSS receiver are processed in real-time for display and coordinate transformation. Positions are only saved when you explicitly record a survey point.
- **Survey projects and points** - Stored locally in an on-device database. This data never leaves your device unless you explicitly export and share it.
- **NTRIP credentials** - Caster hostname, port, username, and password that you enter are used to connect to NTRIP services. These are stored locally on your device.

### Data transmitted to external services

- **NTRIP casters** - When you connect to an NTRIP service, your approximate GNSS position (GGA sentence) is sent to the caster to receive correction data. This is necessary for Virtual Reference Station (VRS) operation. The NTRIP caster is chosen and configured by you.
- **Map tiles** - When viewing the map, tile data is fetched from OpenStreetMap tile servers. Your approximate map viewport location is visible to the tile server. No personal information is transmitted.

### Data NOT collected

- No analytics or telemetry
- No crash reporting to external services
- No advertising identifiers
- No user accounts or registration
- No contact information
- No device identifiers transmitted externally

## Data Storage

All survey data is stored locally on your Android device in a SQLite database. Data is only shared when you explicitly use the Export and Share function.

## Third-Party Services

- **NTRIP casters** - Operated by third parties (e.g., Ktimatologio, CivilPOS, Hexagon). Their privacy policies apply to the data exchanged with them.
- **Map tile providers** - OpenStreetMap. See the [OpenStreetMap Privacy Policy](https://wiki.osmfoundation.org/wiki/Privacy_Policy).

## Permissions

OpenTopo requests the following Android permissions:

| Permission | Purpose |
|------------|---------|
| Bluetooth | Connect to GNSS receivers |
| Fine Location | Required by Android for Bluetooth scanning |
| Internet | NTRIP corrections and map tiles |

## Your Rights

Since all data is stored locally on your device, you have full control. You can:
- Delete individual points or entire projects within the app
- Clear app data from Android Settings
- Uninstall the app to remove all data

## Changes

This privacy policy may be updated. Changes will be documented in the [CHANGELOG](CHANGELOG.md).

## Contact

For privacy-related questions: **pierros@papadeas.gr**
