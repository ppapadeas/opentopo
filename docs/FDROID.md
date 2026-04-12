# F-Droid Publication Guide

## Self-Hosted Repo (immediate)

A self-hosted F-Droid repository is automatically updated on every GitHub Release via the `fdroid-repo.yml` workflow. Users can add it to their F-Droid client:

**Repo URL:** `https://ppapadeas.github.io/opentopo/fdroid/repo`

To add: F-Droid → Settings → Repositories → Add → paste the URL above.

## Official F-Droid Submission

### Prerequisites

1. GitLab account at https://gitlab.com
2. Fork https://gitlab.com/fdroid/fdroiddata

### Steps

```bash
# Clone your fork
git clone --depth=1 https://gitlab.com/YOUR_ACCOUNT/fdroiddata ~/fdroiddata
cd ~/fdroiddata

# Create branch
git checkout -b org.opentopo.app

# Copy our recipe
cp /path/to/opentopo/fdroid/org.opentopo.app.yml metadata/

# Validate
fdroid readmeta
fdroid lint org.opentopo.app

# Test build (optional but recommended)
fdroid build org.opentopo.app

# Commit and push
git add metadata/org.opentopo.app.yml
git commit -m "Add OpenTopo - GNSS survey app for Greece"
git push origin org.opentopo.app
```

Then create a Merge Request on GitLab targeting `fdroiddata/master`.

### Auto-Updates

The recipe uses `AutoUpdateMode: Version v%v` and `UpdateCheckMode: Tags`, so F-Droid will automatically detect new version tags and build them. Just push a tag like `v1.8.0` to GitHub.

### Metadata

F-Droid reads localized descriptions from our Fastlane structure:
```
fastlane/metadata/android/en-US/
├── title.txt
├── short_description.txt
├── full_description.txt
├── changelogs/
│   └── 8.txt  (versionCode)
└── images/
    ├── icon.png
    └── phoneScreenshots/
```

### Potential Issues

- **AGP 9.1.0** may not be tested on F-Droid build servers yet. If the build fails, consider the self-hosted repo as a fallback.
- **material3:1.5.0-alpha17** is an alpha dependency. It's FOSS (Apache-2.0) from Google Maven, so it should be accepted, but may cause build flakiness.
- NTRIP connections to government casters may get the `NonFreeNet` anti-feature flag.

### FOSS Compliance

- ✅ License: AGPL-3.0-only
- ✅ No Google Play Services
- ✅ No Firebase / analytics / tracking
- ✅ No proprietary dependencies
- ✅ MapLibre: BSD-2-Clause
- ✅ All tile sources: OpenStreetMap-based (vathra.xyz)
