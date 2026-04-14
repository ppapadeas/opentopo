# Multi-Country Support — Deep Dive Analysis

## The Problem

OpenTopo currently has **47+ Greece-specific touchpoints** across 25+ files. Every layer of the stack — from `lib-transform` Helmert parameters to UI labels to export PRJ strings — assumes EGSA87/GGRS87. Supporting additional countries requires abstracting all of these into a pluggable architecture.

---

## Inventory of Country-Specific Elements

### What each country needs (the "Country Pack")

| Component | Greece (current) | What varies per country |
|-----------|-----------------|------------------------|
| **Datum transformation** | 7-param Helmert (HTRS07→GGRS87) | Helmert params, or grid-based (NTv2), or identity (ETRS89 countries) |
| **Correction grids** | dE/dN 2km Ktimatologio grids (.grd) | Grid format (NTv2, .grd, .gsb), resolution, coverage |
| **Map projection** | Transverse Mercator (CM 24°, k=0.9996) | TM with different params, Lambert (France), Stereographic (Romania) |
| **Geoid model** | None (receiver EGM96 only) | National quasi-geoid grid (RAF20, OSGM15, AUSGeoid2020, etc.) |
| **Trig points** | vathra.xyz API (GYS, GGRS87 coords) | Different APIs, different datums, different data models |
| **NTRIP presets** | HEPOS, CivilPOS, SmartNet GR | Country-specific casters, ports, mountpoint patterns |
| **Basemap** | Vathra.xyz tiles + Ktimatologio WMS | OSM global works everywhere; ortho layers are country-specific |
| **Export CRS** | EPSG:2100, GGRS87 WKT/PRJ | EPSG code, WKT string, CSV headers, GeoJSON CRS |
| **UI labels** | "EGSA87", "GGRS87", "HEPOS" | CRS name, datum name, system-specific terminology |
| **Vertical datum** | Athens datum (Piraeus tide gauge) | Normaal Amsterdams Peil, Newlyn, NAVD88, etc. |

### Current hardcoded locations (Greece)

| Category | Files affected | Key references |
|----------|---------------|----------------|
| Helmert params | `Helmert.kt`, `TransformPanel.kt` | TX/TY/TZ/EX/EY/EZ/DS constants |
| Projection params | `HeposTransform.kt`, `TransverseMercator.kt`, `MainMapScreen.kt` | CM=24°, k=0.9996, FE=500k, FN=0/-2M |
| Correction grids | `CorrectionGrid.kt`, `HeposTransform.kt`, `assets/dE*.grd`, `assets/dN*.grd` | Grid format, interpolation, file loading |
| EPSG/CRS | `GeoJsonExporter.kt`, `ShapefileExporter.kt`, `CsvExporter.kt`, `Coordinates.kt` | EPSG:2100, WKT string |
| Trig points | `TrigPointService.kt`, `MainMapScreen.kt`, `VerificationDialog.kt`, `TrigPointCache.kt` | vathra.xyz API, GYS data model |
| NTRIP | `NtripConfig.kt` | hepos.ktimatologio.gr, civilpos, smartnet |
| Basemap | `style_vathra.json`, `MainMapScreen.kt` | Vathra tiles, Ktimatologio WMS |
| UI strings | `TransformPanel.kt`, `MainMapScreen.kt`, `VerificationDialog.kt`, exporters | "EGSA87", "GGRS87", etc. |

---

## Country Comparison Matrix

### Transformation complexity

| Country | Source datum | Target datum | Transform type | Grids needed | Geoid available |
|---------|------------|--------------|----------------|--------------|-----------------|
| **Greece** | HTRS07/WGS84 | GGRS87 | 7-param Helmert + dE/dN grid | Yes (.grd) | Needed |
| **Cyprus** | WGS84 | CGRS93 | 7-param Helmert | No | No (use EGM2008) |
| **Spain** | ETRS89 | ETRS89 | Identity (ED50 legacy: NTv2) | Optional | Yes (EGM08-REDNAP) |
| **Poland** | ETRS89 | PL-ETRF2000 | Identity | No | Yes (PL-geoid2021) |
| **France** | ETRS89 | RGF93 | Identity (NTF legacy: NTv2) | Optional | Yes (RAF20) |
| **UK** | ETRS89 | OSGB36 | Grid-based (OSTN15) | Yes (NTv2) | Yes (OSGM15) |
| **Germany** | ETRS89 | ETRS89 | Identity (DHDN legacy: NTv2) | Optional | Yes (GCG2016) |
| **Italy** | ETRS89 | RDN2008 | Identity (Roma40 legacy: NTv2) | Optional | Yes (ITALGEO2005) |
| **Turkey** | WGS84 | TUREF | 7-param Helmert | No | Yes (TG-20) |
| **Romania** | ETRS89 | Stereo70 | Grid-based (TransDatRo) | Yes | Partial |
| **Australia** | ITRF2014 | GDA2020 | NTv2 grids | Yes (GitHub) | Yes (AUSGeoid2020) |
| **USA** | WGS84 | NAD83 | NADCON5 grids | Yes | Yes (GEOID18) |

### Key insight

Most EU countries use ETRS89 as their modern datum, making the Helmert transformation to WGS84 effectively **identity** (sub-cm differences). The real work is:
1. **Projection** — parameterized TM or Lambert (already mostly solved)
2. **Geoid** — always country-specific, always needed
3. **Legacy datums** — NTv2 grid transforms for older coordinate systems still in use

---

## Proposed Architecture

### CountryProfile data model

```kotlin
data class CountryProfile(
    val id: String,                    // "gr", "cy", "es", "pl", ...
    val displayName: String,           // "Greece (EGSA87)"
    val datumName: String,             // "GGRS87"
    val projectedCrsName: String,      // "EGSA87"
    val epsgCode: Int,                 // 2100
    val wktPrj: String,               // Full WKT for Shapefile .prj

    // Datum transformation
    val helmert: HelmertParams?,       // null = identity (ETRS89 countries)
    val datumGridFile: String?,        // NTv2 grid filename, null if Helmert-only

    // Map projection
    val projection: ProjectionConfig,  // TM or Lambert with all params

    // Correction grids (post-Helmert refinement)
    val correctionGrids: CorrectionGridConfig?,

    // Geoid model
    val geoidGridFile: String?,        // Geoid undulation grid filename

    // Trig points
    val trigPointApi: TrigPointApiConfig?,

    // NTRIP presets
    val ntripPresets: List<NtripPreset>,

    // Basemap
    val orthophotoWms: String?,        // Country-specific ortho layer URL
)

data class ProjectionConfig(
    val type: ProjectionType,          // TM, LAMBERT_CC
    val centralMeridianDeg: Double,
    val scaleFactor: Double,
    val falseEastingM: Double,
    val falseNorthingM: Double,
    val latitudeOfOriginDeg: Double,   // For Lambert
    val standardParallel1Deg: Double?, // For Lambert
    val standardParallel2Deg: Double?, // For Lambert
)

data class CorrectionGridConfig(
    val format: GridFormat,            // KTIMATOLOGIO_GRD, NTV2
    val deFile: String?,               // dE grid (Greece-style)
    val dnFile: String?,               // dN grid (Greece-style)
    val combinedFile: String?,         // NTv2 combined grid
    val lookupProjection: ProjectionConfig?, // TM07-style grid index CRS
)
```

### Grid format support

Two formats cover essentially all countries:

1. **Ktimatologio .grd** (current) — Greece-specific ASCII grid. Already implemented in `CorrectionGrid.kt`. Used only for Greece dE/dN corrections.

2. **NTv2 (.gsb)** — Industry standard binary grid format. Used by UK (OSTN15), Australia, Germany (BeTA2007), Spain, France, and virtually every other country. One reader covers all of them.

NTv2 reader implementation: ~200 lines of Kotlin. The format is well-documented (Canadian NTv2 spec). Binary, with sub-grids for varying resolution. Returns lat/lon shifts in arcseconds.

### Geoid grid format

Most national geoids are distributed as:
- **ISG format** (International Service for the Geoid) — ASCII grid, similar to our .grd
- **GeoTIFF** — raster grid
- **NTv2** — same format as datum grids but for vertical shifts
- **Custom ASCII** — varies by country

A single ASCII grid reader (like our CorrectionGrid) plus NTv2 covers 90%+ of cases.

### Transform pipeline (generalized)

```
WGS84/ETRS89 geographic (from GNSS)
    │
    ├── [if helmert] 7-param Helmert → national datum geographic
    ├── [if datumGrid] NTv2 grid shift → national datum geographic
    ├── [if identity] pass through
    │
    ├── Map projection (TM or Lambert) → national projected (E, N)
    │
    ├── [if correctionGrids] bilinear interpolation → refined (E, N)
    │
    └── [if geoidGrid] H = h - N_geoid → orthometric height
```

The current `HeposTransform` is a special case of this pipeline with Helmert + TM + correction grids.

---

## Implementation Effort Estimate

### Phase 1: Greek geoid grid (v1.9.2)
- Load geoid .grd from assets (same CorrectionGrid reader) — **2 hours**
- Compute H = h - N_greek everywhere H is displayed — **3 hours**
- Re-enable ΔH in verification — **1 hour**
- Source the actual geoid grid file from HEPOS/Ktimatologio — **research needed**

### Phase 2: CountryProfile abstraction (v2.0.0)

| Task | Effort | Notes |
|------|--------|-------|
| Define CountryProfile data model | 1 day | Kotlin data classes, sealed types |
| Refactor HeposTransform → generic TransformPipeline | 2 days | Parameterize Helmert, projection, grids |
| Extract Greece profile from hardcoded values | 1 day | Move constants into GreeceProfile |
| NTv2 grid reader | 2 days | Binary format parser + interpolation |
| Lambert projection | 1 day | For France, Belgium |
| Profile switcher UI | 1 day | Settings dropdown, persisted preference |
| Dynamic UI labels | 1 day | Replace hardcoded "EGSA87" etc. |
| Dynamic export CRS | 0.5 days | PRJ/WKT/GeoJSON from profile |
| Grid download manager | 2 days | Fetch grids on demand, cache locally |
| **Total core architecture** | **~11 days** | |

### Phase 3: Country packs (incremental)

| Country | Extra effort beyond core | Key challenge |
|---------|------------------------|---------------|
| **Cyprus** | 1 day | Nearly identical to Greece; LTM params + CYPOS NTRIP |
| **Spain** | 2 days | UTM zones, ERGNSS presets, EGM08-REDNAP geoid |
| **Poland** | 2 days | PL-2000 zones, ASG-EUPOS, PL-geoid2021 |
| **France** | 3 days | Lambert-93 projection (new), Centipede, RAF20 |
| **UK** | 3 days | OSTN15 NTv2 grid (grid-based datum, no Helmert), OSGM15 |
| **Australia** | 2 days | NTv2 grids (on GitHub), AUSGeoid2020 |

---

## What Makes Each Country Unique

### The easy ones (ETRS89, TM projection, just need geoid)
**Spain, Poland, Germany, Portugal** — Modern datum is ETRS89 (identity to WGS84). Standard UTM projection. Main work is geoid grid and NTRIP presets. Legacy datum transforms (ED50, Pulkovo) are optional.

### The medium ones (different projection type)
**France** — Lambert Conformal Conic instead of Transverse Mercator. Requires implementing a second projection engine (~200 lines). Everything else is standard ETRS89.

**Romania** — Double Stereographic projection. Unusual but well-documented.

### The hard ones (grid-based datum transformation)
**UK** — No Helmert at all. OSGB36 is defined purely by the OSTN15 grid transform. The grid IS the datum definition. Well-documented, freely available, but architecturally different from Helmert-based systems.

**Australia** — GDA2020 uses NTv2 grids with both conformal and distortion components. Well-documented, grids on GitHub.

### The complex ones (multi-zone, fragmented)
**USA** — 50+ State Plane zones, some TM, some Lambert. NADCON5 grids. Massive scope. Worth doing but not in first wave.

**Italy** — Standard geodetically, but NTRIP is fragmented across regions with no single national service.

### The tricky one (tectonic)
**Turkey** — Anatolian plate moves ~2 cm/year relative to ITRF. A static 7-param Helmert degrades over time. Full solution needs a velocity model. For low-accuracy work, static Helmert is fine.

---

## Trig Point APIs by Country

| Country | Source | Status |
|---------|--------|--------|
| Greece | vathra.xyz (25,259 GYS points) | Working, integrated |
| Spain | IGN API-Features (REGENTE vertices) | Public, needs adapter |
| USA | NGS Datasheets (geodesy.noaa.gov) | Public API, comprehensive |
| UK | trigpointing.uk (community) | Public, crowdsourced |
| Australia | State survey portals | Varies by state |
| Others | No public API | Could crowdsource or partner |

---

## Grid File Sizes and Distribution

| Country | Grid | Format | Size | Distribution |
|---------|------|--------|------|-------------|
| Greece dE/dN | Ktimatologio correction | .grd ASCII | 2×1.2 MB | Bundled in APK |
| Greece geoid | TBD | .grd ASCII | ~1-2 MB | Bundle or download |
| UK OSTN15 | Datum + geoid | NTv2 binary | ~10 MB | Download |
| Australia | GDA2020 + AUSGeoid2020 | NTv2 | ~15 MB | Download |
| France RAF20 | Geoid | Multiple | ~5 MB | Download |

**Decision:** Greece grids stay bundled (2.4 MB is fine). All other countries should download on first use and cache locally. This keeps the APK small while supporting many countries.

---

## Recommended Implementation Order

1. **v1.9.2** — Greek geoid grid (immediate fix for ΔH verification)
2. **v2.0.0** — Core abstraction (CountryProfile, NTv2 reader, Lambert, grid manager)
3. **v2.0.0** — Cyprus pack (validates architecture with minimal effort)
4. **v2.0.x** — Spain, Poland, France (high-value markets with free NTRIP)
5. **v2.0.x** — UK, Australia (grid-based datums, validates NTv2 pipeline)
6. **v2.1.0+** — Germany, Italy, Turkey, Romania, USA

The key architectural bet: **NTv2 support unlocks 80% of countries**. Once the NTv2 reader works, adding a new country is mostly configuration: Helmert params (if any), projection params, NTRIP URLs, and a grid download URL.
