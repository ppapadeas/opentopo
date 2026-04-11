package org.opentopo.transform

/** WGS84/HTRS07 geographic coordinates. */
data class GeographicCoordinate(
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val heightM: Double = 0.0,
)

/** Cartesian (X, Y, Z) coordinates in metres. */
data class CartesianCoordinate(
    val x: Double,
    val y: Double,
    val z: Double,
)

/** GGRS87/EGSA87 projected coordinates (EPSG:2100). */
data class ProjectedCoordinate(
    val eastingM: Double,
    val northingM: Double,
)
