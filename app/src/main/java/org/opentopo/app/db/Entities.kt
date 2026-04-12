package org.opentopo.app.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "points",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class PointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val pointId: String,           // user-visible auto-incrementing ID (e.g., "P001")
    val latitude: Double,          // WGS84
    val longitude: Double,         // WGS84
    val altitude: Double?,         // ellipsoidal height (m)
    val easting: Double?,          // EGSA87 E
    val northing: Double?,         // EGSA87 N
    val horizontalAccuracy: Double?,
    val verticalAccuracy: Double?,
    val fixQuality: Int,           // 0=none, 1=GPS, 2=DGPS, 4=RTK fix, 5=RTK float
    val numSatellites: Int,
    val hdop: Double?,
    val averagingSeconds: Int,     // how many seconds averaged
    val antennaHeight: Double? = null, // metres, instrument height
    val remarks: String = "",
    @ColumnInfo(name = "photoPath") val photoPath: String? = null,
    @ColumnInfo(name = "layerType") val layerType: String = "point", // "point", "line_vertex", "polygon_vertex"
    @ColumnInfo(name = "featureId") val featureId: Long? = null, // groups vertices into lines/polygons
    val timestamp: Long = System.currentTimeMillis(),
)
