package org.opentopo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.opentopo.app.ui.theme.CoordinateFont
import org.opentopo.transform.GeographicCoordinate
import org.opentopo.transform.HeposTransform
import org.opentopo.transform.TransformResult

@Composable
fun TransformPanel(
    transform: HeposTransform?,
    modifier: Modifier = Modifier,
) {
    var latInput by remember { mutableStateOf("") }
    var lonInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("0.0") }
    var result by remember { mutableStateOf<TransformResult?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showPipeline by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Converter ──
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "WGS84 \u2192 EGSA87 (EPSG:2100)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("Latitude") },
                        placeholder = { Text("38.0000") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = CoordinateFont),
                    )
                    OutlinedTextField(
                        value = lonInput,
                        onValueChange = { lonInput = it },
                        label = { Text("Longitude") },
                        placeholder = { Text("23.0000") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = CoordinateFont),
                    )
                }
                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = { Text("Height (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = CoordinateFont),
                )
                var geoidSepInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = geoidSepInput,
                    onValueChange = { geoidSepInput = it },
                    label = { Text("Geoid N (m)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = CoordinateFont),
                )
                FilledTonalButton(
                    onClick = {
                        val lat = latInput.toDoubleOrNull()
                        val lon = lonInput.toDoubleOrNull()
                        val h = heightInput.toDoubleOrNull() ?: 0.0
                        if (lat != null && lon != null && transform != null) {
                            try {
                                result = transform.forwardDetailed(
                                    GeographicCoordinate(lat, lon, h),
                                    geoidSeparation = geoidSepInput.toDoubleOrNull(),
                                )
                                errorMsg = null
                                showPipeline = true
                            } catch (e: Exception) {
                                errorMsg = e.message ?: "Transform failed"
                                result = null
                            }
                        } else {
                            errorMsg = "Enter valid lat/lon"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(percent = 50),
                ) {
                    Icon(Icons.Outlined.SwapVert, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Transform")
                }

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Result ──
        result?.let { r ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("EGSA87 Result", style = MaterialTheme.typography.titleSmall)
                    CoordRow("Easting", "%.3f m".format(r.output.eastingM))
                    CoordRow("Northing", "%.3f m".format(r.output.northingM))
                }
            }
        }

        // ── Pipeline Details ──
        if (showPipeline && result != null) {
            val r = result!!

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Timeline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("Pipeline Steps", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    PipelineStep("1. Input (WGS84/HTRS07)") {
                        CoordRow("Lat", "%.8f\u00B0".format(r.input.latitudeDeg))
                        CoordRow("Lon", "%.8f\u00B0".format(r.input.longitudeDeg))
                        CoordRow("Height", "%.3f m".format(r.input.heightM))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PipelineStep("2. Cartesian (HTRS07)") {
                        CoordRow("X", "%.3f m".format(r.cartesianHtrs07.x))
                        CoordRow("Y", "%.3f m".format(r.cartesianHtrs07.y))
                        CoordRow("Z", "%.3f m".format(r.cartesianHtrs07.z))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PipelineStep("3. Helmert \u2192 Cartesian (EGSA87)") {
                        CoordRow("X", "%.3f m".format(r.cartesianEgsa87.x))
                        CoordRow("Y", "%.3f m".format(r.cartesianEgsa87.y))
                        CoordRow("Z", "%.3f m".format(r.cartesianEgsa87.z))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PipelineStep("4. Geographic (EGSA87)") {
                        CoordRow("Lat", "%.8f\u00B0".format(r.geographicEgsa87.latitudeDeg))
                        CoordRow("Lon", "%.8f\u00B0".format(r.geographicEgsa87.longitudeDeg))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PipelineStep("5. TM87 (approx EGSA87)") {
                        CoordRow("E", "%.3f m".format(r.tm87.eastingM))
                        CoordRow("N", "%.3f m".format(r.tm87.northingM))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PipelineStep("6. TM07 (grid lookup)") {
                        CoordRow("E", "%.3f m".format(r.tm07.eastingM))
                        CoordRow("N", "%.3f m".format(r.tm07.northingM))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PipelineStep("7. Grid Corrections") {
                        CoordRow("\u0394E", "%.2f cm".format(r.gridCorrectionDeCm))
                        CoordRow("\u0394N", "%.2f cm".format(r.gridCorrectionDnCm))
                    }

                    r.geoidUndulation?.let { n ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PipelineStep("8. Geoid Undulation") {
                            CoordRow("N (geoid)", "%.3f m".format(n))
                            CoordRow("h (ellipsoidal)", "%.3f m".format(r.input.heightM))
                            r.orthometricHeight?.let { h ->
                                CoordRow("H (orthometric)", "%.3f m".format(h))
                            }
                        }
                    }
                }
            }
        }

        // ── Helmert Parameters ──
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "HELMERT PARAMETERS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("HTRS07 \u2192 EGSA87 (Position Vector)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                CoordRow("TX", "203.437 m")
                CoordRow("TY", "\u221273.461 m")
                CoordRow("TZ", "\u2212243.594 m")
                CoordRow("RX", "\u22120.170\"")
                CoordRow("RY", "\u22120.060\"")
                CoordRow("RZ", "\u22120.151\"")
                CoordRow("Scale", "\u22120.294 ppm")
            }
        }

        // ── TM Projection Parameters ──
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "PROJECTION PARAMETERS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text("TM87 (EGSA87 output)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                CoordRow("Central meridian", "24\u00B0")
                CoordRow("Scale factor", "0.9996")
                CoordRow("False Easting", "500,000 m")
                CoordRow("False Northing", "0 m")
                CoordRow("Ellipsoid", "GRS80")
                Spacer(Modifier.height(8.dp))
                Text("TM07 (grid lookup)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                CoordRow("Central meridian", "24\u00B0")
                CoordRow("Scale factor", "0.9996")
                CoordRow("False Easting", "500,000 m")
                CoordRow("False Northing", "\u22122,000,000 m")
            }
        }

        // ── Grid Info ──
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.GridOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("CORRECTION GRIDS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                CoordRow("Version", "V1.0 (2km)")
                CoordRow("Grid size", "408 \u00D7 422 nodes")
                CoordRow("Cell size", "2,000 m")
                CoordRow("Easting range", "41,600 \u2013 883,600 m (TM07)")
                CoordRow("Northing range", "1,845,619 \u2013 2,659,619 m (TM07)")
                CoordRow("Files", "dE_2km_V1-0.grd, dN_2km_V1-0.grd")
                CoordRow("Source", "Ktimatologio S.A.")
                if (transform == null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Grid files not loaded \u2014 transformation unavailable",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PipelineStep(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun CoordRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = CoordinateFont,
            fontWeight = FontWeight.Bold,
        )
    }
}
