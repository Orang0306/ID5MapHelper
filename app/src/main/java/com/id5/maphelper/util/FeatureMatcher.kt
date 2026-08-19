package com.id5.maphelper.util

import com.id5.maphelper.data.MapEntity

/**
 * 地图特征匹配工具
 * 根据用户选择的门型、朝向、侧门等特征，匹配出最可能的地图
 */
object FeatureMatcher {

    /**
     * 匹配结果数据类
     */
    data class MatchResult(
        val map: MapEntity,
        val score: Int,
        val matchedFeatures: List<String>
    )

    /**
     * 特征选项定义
     */
    object Options {
        val DOOR_TYPES = listOf("", "T型", "Y型", "十字", "L型", "方型", "三缺一", "红门", "凹门", "其他")
        val DOOR_DIRECTIONS = listOf("", "上", "下", "左", "右")
        val SIDE_DOORS = listOf("", "无", "左", "右", "上", "下", "左上", "右上", "左下", "右下", "左右")
        val SECOND_FLOOR_DOORS = listOf("", "无", "有")
        val FLOORS = listOf("", "一楼", "二楼", "双层")
    }

    /**
     * 对地图列表进行评分匹配
     * @param maps 所有地图
     * @param doorType 门型（空字符串表示不限制）
     * @param doorDirection 大门朝向
     * @param sideDoor 侧门位置
     * @param secondFloorDoor 二楼侧门
     * @param floor 楼层
     * @return 按匹配度排序的结果列表
     */
    fun matchMaps(
        maps: List<MapEntity>,
        doorType: String = "",
        doorDirection: String = "",
        sideDoor: String = "",
        secondFloorDoor: String = "",
        floor: String = ""
    ): List<MatchResult> {
        return maps.map { map ->
            calculateScore(map, doorType, doorDirection, sideDoor, secondFloorDoor, floor)
        }.filter { it.score > 0 }
            .sortedByDescending { it.score }
    }

    /**
     * 计算单张地图的匹配分数
     */
    private fun calculateScore(
        map: MapEntity,
        doorType: String,
        doorDirection: String,
        sideDoor: String,
        secondFloorDoor: String,
        floor: String
    ): MatchResult {
        var score = 0
        val matched = mutableListOf<String>()

        // 门型权重最高（40分）
        if (doorType.isNotEmpty() && map.doorType == doorType) {
            score += 40
            matched.add("门型:$doorType")
        }

        // 大门朝向（25分）
        if (doorDirection.isNotEmpty() && map.doorDirection == doorDirection) {
            score += 25
            matched.add("朝向:$doorDirection")
        }

        // 侧门位置（20分）
        if (sideDoor.isNotEmpty() && map.sideDoor == sideDoor) {
            score += 20
            matched.add("侧门:$sideDoor")
        }

        // 二楼侧门（10分）
        if (secondFloorDoor.isNotEmpty() && map.secondFloorDoor == secondFloorDoor) {
            score += 10
            matched.add("二楼侧门:$secondFloorDoor")
        }

        // 楼层（5分）
        if (floor.isNotEmpty() && map.floor == floor) {
            score += 5
            matched.add("楼层:$floor")
        }

        return MatchResult(map, score, matched)
    }

    /**
     * 获取最佳匹配
     */
    fun getBestMatch(results: List<MatchResult>): MatchResult? {
        return results.maxByOrNull { it.score }
    }

    /**
     * 判断是否为高置信度匹配（分数>=60且只有一个最高分）
     */
    fun isHighConfidence(results: List<MatchResult>): Boolean {
        if (results.isEmpty()) return false
        val best = results.first()
        if (best.score < 60) return false
        // 检查是否有并列第一
        val tiedCount = results.count { it.score == best.score }
        return tiedCount == 1
    }

    /**
     * 生成匹配描述文本
     */
    fun getMatchDescription(result: MatchResult): String {
        return if (result.matchedFeatures.isEmpty()) {
            "无匹配特征"
        } else {
            "匹配: ${result.matchedFeatures.joinToString(", ")}"
        }
    }
}
