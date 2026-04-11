# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| latest  | Yes       |

## Reporting a Vulnerability

If you discover a security vulnerability in OpenTopo, please report it responsibly.

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, please email **pierros@papadeas.gr** with:

- A description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

You will receive an acknowledgment within 48 hours. We will work with you to understand and address the issue before any public disclosure.

## Scope

OpenTopo is an Android app that:
- Connects to GNSS receivers via Bluetooth and USB
- Connects to NTRIP casters over the network
- Stores survey data locally on the device
- Does not transmit data to any server (except NTRIP casters configured by the user)

Security concerns may include:
- Bluetooth/USB connection handling
- NTRIP credential storage
- Local database access
- File export and sharing
