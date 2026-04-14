package org.opentopo.app.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import org.opentopo.app.survey.TrigPoint

@Entity(tableName = "trig_points_cache")
data class TrigPointCacheEntity(
    @PrimaryKey val gysId: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val egsa87Easting: Double?,
    val egsa87Northing: Double?,
    val egsa87Z: Double?,
    val status: String?,
    val pointOrder: Int,
    val distanceM: Double?,
    val cachedAt: Long = System.currentTimeMillis(),
) {
    fun toTrigPoint() = TrigPoint(
        gysId = gysId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        elevation = elevation,
        egsa87Easting = egsa87Easting,
        egsa87Northing = egsa87Northing,
        egsa87Z = egsa87Z,
        status = status,
        pointOrder = pointOrder,
        distanceM = distanceM,
    )

    companion object {
        fun from(tp: TrigPoint) = TrigPointCacheEntity(
            gysId = tp.gysId,
            name = tp.name,
            latitude = tp.latitude,
            longitude = tp.longitude,
            elevation = tp.elevation,
            egsa87Easting = tp.egsa87Easting,
            egsa87Northing = tp.egsa87Northing,
            egsa87Z = tp.egsa87Z,
            status = tp.status,
            pointOrder = tp.pointOrder,
            distanceM = tp.distanceM,
        )
    }
}

@Dao
interface TrigPointCacheDao {
    @Query("SELECT * FROM trig_points_cache")
    suspend fun getAll(): List<TrigPointCacheEntity>

    @Query("""
        SELECT *, (
            (latitude - :lat) * (latitude - :lat) +
            (longitude - :lon) * (longitude - :lon)
        ) AS dist2
        FROM trig_points_cache
        WHERE latitude BETWEEN :lat - :degRange AND :lat + :degRange
        AND longitude BETWEEN :lon - :degRange AND :lon + :degRange
        ORDER BY dist2 ASC
        LIMIT 50
    """)
    suspend fun getNearby(lat: Double, lon: Double, degRange: Double): List<TrigPointCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<TrigPointCacheEntity>)

    @Query("SELECT COUNT(*) FROM trig_points_cache")
    suspend fun count(): Int
}
