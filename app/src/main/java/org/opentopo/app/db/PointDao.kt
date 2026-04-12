package org.opentopo.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PointDao {
    @Query("SELECT * FROM points WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getByProject(projectId: Long): Flow<List<PointEntity>>

    @Query("SELECT * FROM points WHERE id = :id")
    suspend fun getById(id: Long): PointEntity?

    @Query("SELECT COUNT(*) FROM points WHERE projectId = :projectId")
    suspend fun countByProject(projectId: Long): Int

    @Insert
    suspend fun insert(point: PointEntity): Long

    @Update
    suspend fun update(point: PointEntity)

    @Delete
    suspend fun delete(point: PointEntity)

    @Query("DELETE FROM points WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)

    @Query("SELECT * FROM points WHERE projectId = :projectId ORDER BY timestamp ASC")
    suspend fun getByProjectOnce(projectId: Long): List<PointEntity>

    @Query("SELECT * FROM points WHERE projectId = :projectId AND layerType = :layerType ORDER BY timestamp ASC")
    fun getByProjectAndType(projectId: Long, layerType: String): Flow<List<PointEntity>>

    @Query("SELECT DISTINCT featureId FROM points WHERE projectId = :projectId AND layerType = :layerType AND featureId IS NOT NULL")
    suspend fun getFeatureIds(projectId: Long, layerType: String): List<Long>

    @Query("SELECT * FROM points WHERE featureId = :featureId ORDER BY timestamp ASC")
    fun getByFeature(featureId: Long): Flow<List<PointEntity>>

    @Query("SELECT * FROM points WHERE featureId = :featureId ORDER BY timestamp ASC")
    suspend fun getByFeatureOnce(featureId: Long): List<PointEntity>

    @Query("SELECT MAX(featureId) FROM points WHERE projectId = :projectId")
    suspend fun getMaxFeatureId(projectId: Long): Long?
}
