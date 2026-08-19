package com.id5.maphelper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 地图实体类
 * 存储加页手记的每张地图信息
 */
@Entity(tableName = "maps")
data class MapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 地图名称，如"左T房"、"Y形房" */
    val name: String,

    /** 地图图片本地路径 */
    val imagePath: String = "",

    /** 门型特征：T型/Y型/十字/L型/方型/三缺一/其他 */
    val doorType: String = "",

    /** 大门朝向：上/下/左/右 */
    val doorDirection: String = "",

    /** 侧门位置：无/左上/右上/左下/右下/左/右/上/下 */
    val sideDoor: String = "",

    /** 二楼侧门：无/有 */
    val secondFloorDoor: String = "无",

    /** 楼层：一楼/二楼/双层 */
    val floor: String = "一楼",

    /** 房间列表，用逗号分隔 */
    val rooms: String = "",

    /** 宝箱数量 */
    val treasureCount: Int = 0,

    /** 跑图路线文字说明 */
    val routeNote: String = "",

    /** 备注 */
    val remark: String = "",

    /** 创建时间 */
    val createTime: Long = System.currentTimeMillis(),

    /** 更新时间 */
    val updateTime: Long = System.currentTimeMillis()
)
