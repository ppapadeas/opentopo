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
}
