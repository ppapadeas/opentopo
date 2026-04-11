#!/usr/bin/env python3
"""
OpenTopo — HEPOS Coordinate Transformation Reference Implementation

Transforms HTRS07/WGS84 geographic coordinates to EGSA87/GGRS87 projected
coordinates (EPSG:2100) using the official HEPOS model published by
Ktimatologio S.A. (October 2008).

Source: HEPOS_coord_transf_model_summary_081107_gr.pdf

Usage:
    python hepos_transform_reference.py --lat 37.039941477 --lon 22.082250298 --h 50
    python hepos_transform_reference.py --csv input.csv --output output.csv
"""

import math
import argparse
import csv
import os
import sys

# ═══════════════════════════════════════════════
# GRS80 Ellipsoid
# ═══════════════════════════════════════════════

GRS80_A = 6378137.0  # semi-major axis (m)
GRS80_F = 1.0 / 298.257222101  # flattening
GRS80_E2 = 2 * GRS80_F - GRS80_F ** 2  # first eccentricity squared


def geographic_to_cartesian(lat_deg, lon_deg, h):
    """Convert geographic (lat, lon, h) to Cartesian (X, Y, Z) on GRS80."""
    phi = math.radians(lat_deg)
    lam = math.radians(lon_deg)
    N = GRS80_A / math.sqrt(1 - GRS80_E2 * math.sin(phi) ** 2)
    X = (N + h) * math.cos(phi) * math.cos(lam)
    Y = (N + h) * math.cos(phi) * math.sin(lam)
    Z = (N * (1 - GRS80_E2) + h) * math.sin(phi)
    return X, Y, Z


def cartesian_to_geographic(X, Y, Z):
    """Convert Cartesian (X, Y, Z) to geographic (lat_deg, lon_deg, h) on GRS80.
    Uses iterative method (Bowring)."""
    p = math.sqrt(X ** 2 + Y ** 2)
    lam = math.atan2(Y, X)
    phi = math.atan2(Z, p * (1 - GRS80_E2))  # initial approximation
    for _ in range(10):
        N = GRS80_A / math.sqrt(1 - GRS80_E2 * math.sin(phi) ** 2)
        phi = math.atan2(Z + GRS80_E2 * N * math.sin(phi), p)
    N = GRS80_A / math.sqrt(1 - GRS80_E2 * math.sin(phi) ** 2)
    h = p / math.cos(phi) - N if abs(math.cos(phi)) > 1e-10 else abs(Z) / math.sin(phi) - N * (1 - GRS80_E2)
    return math.degrees(phi), math.degrees(lam), h


# ═══════════════════════════════════════════════
# 7-Parameter Helmert Transformation
# ═══════════════════════════════════════════════

# Official HEPOS parameters (HTRS07 → EGSA87)
# Source: PDF equation (1), page 5
TX = 203.437    # m
TY = -73.461    # m
TZ = -243.594   # m
EX = -0.170 * math.pi / (180.0 * 3600.0)  # arcsec → rad
EY = -0.060 * math.pi / (180.0 * 3600.0)  # arcsec → rad
EZ = -0.151 * math.pi / (180.0 * 3600.0)  # arcsec → rad
DS = -0.294e-6  # ppm → dimensionless


def helmert_forward(X, Y, Z):
    """Apply HEPOS 7-parameter Helmert: HTRS07 → EGSA87 (Cartesian)."""
    X2 = X + TX + DS * X + EZ * Y - EY * Z
    Y2 = Y + TY - EZ * X + DS * Y + EX * Z
    Z2 = Z + TZ + EY * X - EX * Y + DS * Z
    return X2, Y2, Z2


# ═══════════════════════════════════════════════
# Transverse Mercator Projection
# ═══════════════════════════════════════════════

def tm_forward(lat_deg, lon_deg, lon0=24.0, k0=0.9996, fe=500000.0, fn=0.0):
    """Project geographic to Transverse Mercator. Returns (E, N)."""
    phi = math.radians(lat_deg)
    lam = math.radians(lon_deg)
    lam0 = math.radians(lon0)

    a = GRS80_A
    e2 = GRS80_E2
    e_prime2 = e2 / (1 - e2)

    N = a / math.sqrt(1 - e2 * math.sin(phi) ** 2)
    T = math.tan(phi) ** 2
    C = e_prime2 * math.cos(phi) ** 2
    A = (lam - lam0) * math.cos(phi)

    # Meridional arc length
    e4 = e2 ** 2
    e6 = e2 ** 3
    M = a * (
        (1 - e2 / 4 - 3 * e4 / 64 - 5 * e6 / 256) * phi
        - (3 * e2 / 8 + 3 * e4 / 32 + 45 * e6 / 1024) * math.sin(2 * phi)
        + (15 * e4 / 256 + 45 * e6 / 1024) * math.sin(4 * phi)
        - (35 * e6 / 3072) * math.sin(6 * phi)
    )

    E = fe + k0 * N * (
        A
        + (1 - T + C) * A ** 3 / 6
        + (5 - 18 * T + T ** 2 + 72 * C - 58 * e_prime2) * A ** 5 / 120
    )

    N_coord = fn + k0 * (
        M
        + N * math.tan(phi) * (
            A ** 2 / 2
            + (5 - T + 9 * C + 4 * C ** 2) * A ** 4 / 24
            + (61 - 58 * T + T ** 2 + 600 * C - 330 * e_prime2) * A ** 6 / 720
        )
    )

    return E, N_coord


# ═══════════════════════════════════════════════
# Correction Grid
# ═══════════════════════════════════════════════

class CorrectionGrid:
    """Loads and interpolates a HEPOS correction grid (.grd file)."""

    def __init__(self, filepath):
        with open(filepath, 'r') as f:
            lines = f.readlines()

        self.nrows = int(lines[0].strip())   # 408 (Northing direction, S→N)
        self.ncols = int(lines[1].strip())   # 422 (Easting direction, W→E)
        self.cellsize = float(lines[2].strip())  # 2000 m
        self.n_sw = float(lines[3].strip())  # TM07 Northing of SW corner
        self.e_sw = float(lines[4].strip())  # TM07 Easting of SW corner

        values = []
        for line in lines[5:]:
            for v in line.strip().split():
                try:
                    values.append(float(v))
                except ValueError:
                    pass

        expected = self.nrows * self.ncols
        if len(values) < expected:
            raise ValueError(f"Grid has {len(values)} values, expected {expected}")

        # data[row][col]: row 0 = south, row nrows-1 = north
        #                 col 0 = west, col ncols-1 = east
        self.data = []
        for r in range(self.nrows):
            self.data.append(values[r * self.ncols : (r + 1) * self.ncols])

    def interpolate(self, tm07_e, tm07_n):
        """Bilinear interpolation. Returns correction in cm."""
        col = (tm07_e - self.e_sw) / self.cellsize
        row = (tm07_n - self.n_sw) / self.cellsize

        if col < 0 or col >= self.ncols - 1 or row < 0 or row >= self.nrows - 1:
            raise ValueError(
                f"Point TM07 E={tm07_e:.1f} N={tm07_n:.1f} outside grid bounds "
                f"(E: {self.e_sw}–{self.e_sw + (self.ncols-1)*self.cellsize}, "
                f"N: {self.n_sw}–{self.n_sw + (self.nrows-1)*self.cellsize})"
            )

        c0 = int(col)
        r0 = int(row)
        dc = col - c0
        dr = row - r0

        v00 = self.data[r0][c0]
        v01 = self.data[r0][c0 + 1]
        v10 = self.data[r0 + 1][c0]
        v11 = self.data[r0 + 1][c0 + 1]

        return (
            v00 * (1 - dc) * (1 - dr)
            + v01 * dc * (1 - dr)
            + v10 * (1 - dc) * dr
            + v11 * dc * dr
        )


# ═══════════════════════════════════════════════
# Full HEPOS Pipeline
# ═══════════════════════════════════════════════

class HeposTransform:
    """Complete HTRS07 → EGSA87 transformation."""

    def __init__(self, grid_dir=None):
        if grid_dir is None:
            grid_dir = os.path.join(os.path.dirname(__file__), '..', 'assets')
        self.grid_de = CorrectionGrid(os.path.join(grid_dir, 'dE_2km_V1-0.grd'))
        self.grid_dn = CorrectionGrid(os.path.join(grid_dir, 'dN_2km_V1-0.grd'))

    def forward(self, lat_deg, lon_deg, h=0.0):
        """Transform HTRS07/WGS84 geographic to EGSA87 projected (E, N).
        
        Args:
            lat_deg: Latitude in decimal degrees (HTRS07/WGS84)
            lon_deg: Longitude in decimal degrees (HTRS07/WGS84)
            h: Ellipsoidal height in metres (default 0, has negligible effect)
            
        Returns:
            (E, N): EGSA87/GGRS87 projected coordinates in metres
        """
        # Step 1: Geographic → Cartesian (HTRS07)
        X, Y, Z = geographic_to_cartesian(lat_deg, lon_deg, h)

        # Step 2: 7-parameter Helmert (HTRS07 → EGSA87 Cartesian)
        X2, Y2, Z2 = helmert_forward(X, Y, Z)

        # Step 3: Cartesian → Geographic (EGSA87)
        lat2, lon2, h2 = cartesian_to_geographic(X2, Y2, Z2)

        # Step 4: Geographic → TM87 projection (approximate EGSA87)
        E_approx, N_approx = tm_forward(lat2, lon2, lon0=24, k0=0.9996, fe=500000, fn=0)

        # Step 5: Compute TM07 coordinates for grid lookup
        # Project the ORIGINAL HTRS07 geographic coords to TM07
        tm07_e, tm07_n = tm_forward(lat_deg, lon_deg, lon0=24, k0=0.9996, fe=500000, fn=-2000000)

        # Step 6: Grid interpolation
        de_cm = self.grid_de.interpolate(tm07_e, tm07_n)
        dn_cm = self.grid_dn.interpolate(tm07_e, tm07_n)

        # Step 7: Apply corrections
        E_final = E_approx + de_cm / 100.0
        N_final = N_approx + dn_cm / 100.0

        return E_final, N_final


# ═══════════════════════════════════════════════
# CLI Interface
# ═══════════════════════════════════════════════

def main():
    parser = argparse.ArgumentParser(description='HEPOS HTRS07→EGSA87 Coordinate Transformation')
    parser.add_argument('--lat', type=float, help='Latitude (decimal degrees, WGS84/HTRS07)')
    parser.add_argument('--lon', type=float, help='Longitude (decimal degrees, WGS84/HTRS07)')
    parser.add_argument('--h', type=float, default=0.0, help='Ellipsoidal height (m, default 0)')
    parser.add_argument('--csv', type=str, help='Input CSV file (columns: lat,lon[,h])')
    parser.add_argument('--output', type=str, help='Output CSV file')
    parser.add_argument('--grid-dir', type=str, default=None, help='Directory containing .grd files')
    args = parser.parse_args()

    transform = HeposTransform(grid_dir=args.grid_dir)

    if args.lat is not None and args.lon is not None:
        E, N = transform.forward(args.lat, args.lon, args.h)
        print(f"Input:  lat={args.lat:.10f}  lon={args.lon:.10f}  h={args.h:.3f}")
        print(f"Output: E={E:.3f}  N={N:.3f}  (EGSA87/GGRS87)")

    elif args.csv:
        results = []
        with open(args.csv, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                lat = float(row.get('lat') or row.get('Latitude'))
                lon = float(row.get('lon') or row.get('Longitude'))
                h = float(row.get('h', row.get('Elevation', 0)))
                E, N = transform.forward(lat, lon, h)
                results.append({
                    'lat': lat, 'lon': lon, 'h': h,
                    'E_EGSA87': f'{E:.3f}', 'N_EGSA87': f'{N:.3f}'
                })
                print(f"  {lat:.8f}, {lon:.8f} → E={E:.3f} N={N:.3f}")

        if args.output:
            with open(args.output, 'w', newline='') as f:
                writer = csv.DictWriter(f, fieldnames=['lat', 'lon', 'h', 'E_EGSA87', 'N_EGSA87'])
                writer.writeheader()
                writer.writerows(results)
            print(f"\nResults written to {args.output}")
    else:
        parser.print_help()


if __name__ == '__main__':
    main()
