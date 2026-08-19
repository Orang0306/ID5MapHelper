package com.id5.maphelper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [MapEntity::class], version = 1, exportSchema = false)
abstract class MapDatabase : RoomDatabase() {

    abstract fun mapDao(): MapDao

    companion object {
        @Volatile
        private var INSTANCE: MapDatabase? = null

        fun getDatabase(context: Context): MapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MapDatabase::class.java,
                    "id5_map_database"
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 数据库创建时插入示例数据
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        insertSampleData(database.mapDao())
                    }
                }
            }
        }

        private suspend fun insertSampleData(dao: MapDao) {
            val samples = listOf(
                MapEntity(
                    name = "左T房(2)",
                    doorType = "T型",
                    doorDirection = "左",
                    sideDoor = "右",
                    secondFloorDoor = "无",
                    floor = "一楼",
                    rooms = "入口,走廊,餐厅,宝箱房",
                    treasureCount = 3,
                    routeNote = "进门直走到前面右拐，出房间后贴左墙走，遇到宝箱先拿，全程约3分钟。",
                    remark = "侧门进来开口向右，形状像T"
                ),
                MapEntity(
                    name = "Y形房",
                    doorType = "Y型",
                    doorDirection = "上",
                    sideDoor = "左",
                    secondFloorDoor = "有",
                    floor = "双层",
                    rooms = "入口,Y型岔路,红房间,餐厅,二楼走廊",
                    treasureCount = 5,
                    routeNote = "进门大Y形，左手边第一个红房间，出来直走到底左转，上二楼第二个红房间。",
                    remark = "现存28张地图中仅此一张Y形地形"
                ),
                MapEntity(
                    name = "方字型",
                    doorType = "方型",
                    doorDirection = "左",
                    sideDoor = "右",
                    secondFloorDoor = "有",
                    floor = "双层",
                    rooms = "入口,方型大厅,一楼侧门,二楼走廊,宝箱房",
                    treasureCount = 4,
                    routeNote = "进门方字型，左拐贴左墙一直走，看到一楼侧门右拐直走，进门继续贴左墙跑。",
                    remark = "贴墙跑图速刷流"
                ),
                MapEntity(
                    name = "十字门",
                    doorType = "十字",
                    doorDirection = "下",
                    sideDoor = "左右",
                    secondFloorDoor = "无",
                    floor = "一楼",
                    rooms = "十字入口,南走廊,餐厅,缪斯房,罐子房",
                    treasureCount = 4,
                    routeNote = "南门十字型，先走左边拿餐厅宝箱，回头走右边到缪斯房，最后罐子房收尾。",
                    remark = "南门篇典型地图"
                ),
                MapEntity(
                    name = "三缺一门",
                    doorType = "三缺一",
                    doorDirection = "下",
                    sideDoor = "左",
                    secondFloorDoor = "无",
                    floor = "一楼",
                    rooms = "三缺一入口,主卧,餐厅,图书长廊",
                    treasureCount = 3,
                    routeNote = "南门三缺一，先进主卧拿宝箱，出来去餐厅，最后图书长廊探索。",
                    remark = "深色地块有概率刷紫箱"
                )
            )
            samples.forEach { dao.insertMap(it) }
        }
    }
}
