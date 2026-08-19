package com.id5.maphelper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MapDao {

    @Query("SELECT * FROM maps ORDER BY updateTime DESC")
    fun getAllMaps(): Flow<List<MapEntity>>

    @Query("SELECT * FROM maps ORDER BY updateTime DESC")
    suspend fun getAllMapsOnce(): List<MapEntity>

    @Query("SELECT * FROM maps WHERE id = :id")
    suspend fun getMapById(id: Long): MapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMap(map: MapEntity): Long

    @Update
    suspend fun updateMap(map: MapEntity)

    @Delete
    suspend fun deleteMap(map: MapEntity)

    @Query("DELETE FROM maps WHERE id = :id")
    suspend fun deleteMapById(id: Long)

    @Query("SELECT COUNT(*) FROM maps")
    suspend fun getMapCount(): Int

    /**
     * 根据特征搜索地图
     * 每个特征为空时不参与过滤
     */
    @Query("""
        SELECT * FROM maps 
        WHERE (:doorType = '' OR doorType = :doorType)
        AND (:doorDirection = '' OR doorDirection = :doorDirection)
        AND (:sideDoor = '' OR sideDoor = :sideDoor)
        AND (:secondFloorDoor = '' OR secondFloorDoor = :secondFloorDoor)
        AND (:floor = '' OR floor = :floor)
        ORDER BY updateTime DESC
    """)
    suspend fun searchMaps(
        doorType: String,
        doorDirection: String,
        sideDoor: String,
        secondFloorDoor: String,
        floor: String
    ): List<MapEntity>
}
