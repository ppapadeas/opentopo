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

/** All intermediate results from the HEPOS transformation pipeline. */
data class TransformResult(
    val input: GeographicCoordinate,
    val cartesianHtrs07: CartesianCoordinate,
    val cartesianEgsa87: CartesianCoordinate,
    val geographicEgsa87: GeographicCoordinate,
    val tm87: ProjectedCoordinate,
    val tm07: ProjectedCoordinate,
    val gridCorrectionDeCm: Double,
    val gridCorrectionDnCm: Double,
    val output: ProjectedCoordinate,
    val geoidUndulation: Double? = null,
    val orthometricHeight: Double? = null,
)
