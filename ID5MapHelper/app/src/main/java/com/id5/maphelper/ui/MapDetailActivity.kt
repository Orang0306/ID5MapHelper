package com.id5.maphelper.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.id5.maphelper.data.MapDatabase
import com.id5.maphelper.data.MapEntity
import com.id5.maphelper.databinding.ActivityMapDetailBinding
import com.id5.maphelper.util.ImageUtil
import kotlinx.coroutines.launch

class MapDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapDetailBinding
    private val db by lazy { MapDatabase.getDatabase(this) }
    private var mapId: Long = 0
    private var currentMap: MapEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapId = intent.getLongExtra("map_id", 0)
        if (mapId == 0L) {
            Toast.makeText(this, "地图不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupButtons()
        loadMap()
    }

    private fun setupButtons() {
        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, MapEditActivity::class.java).apply {
                putExtra("map_id", mapId)
            }
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadMap() {
        lifecycleScope.launch {
            val map = db.mapDao().getMapById(mapId) ?: return@launch
            currentMap = map
            title = map.name
            binding.tvName.text = map.name

            // 特征信息
            val features = buildString {
                append("门型: ${map.doorType.ifEmpty { "未设置" }}\n")
                append("朝向: ${map.doorDirection.ifEmpty { "未设置" }}\n")
                append("侧门: ${map.sideDoor.ifEmpty { "未设置" }}\n")
                append("二楼侧门: ${map.secondFloorDoor}\n")
                append("楼层: ${map.floor}\n")
                append("宝箱数: ${map.treasureCount}")
            }
            binding.tvFeatures.text = features

            // 房间列表
            if (map.rooms.isNotEmpty()) {
                binding.tvRooms.text = "房间: ${map.rooms}"
                binding.tvRooms.visibility = View.VISIBLE
            } else {
                binding.tvRooms.visibility = View.GONE
            }

            // 跑图路线
            if (map.routeNote.isNotEmpty()) {
                binding.tvRouteNote.text = map.routeNote
                binding.layoutRouteNote.visibility = View.VISIBLE
            } else {
                binding.layoutRouteNote.visibility = View.GONE
            }

            // 备注
            if (map.remark.isNotEmpty()) {
                binding.tvRemark.text = map.remark
                binding.layoutRemark.visibility = View.VISIBLE
            } else {
                binding.layoutRemark.visibility = View.GONE
            }

            // 地图图片
            if (map.imagePath.isNotEmpty()) {
                val bitmap = ImageUtil.loadBitmap(map.imagePath)
                if (bitmap != null) {
                    binding.photoView.setImageBitmap(bitmap)
                    binding.photoView.visibility = View.VISIBLE
                    binding.tvNoImage.visibility = View.GONE
                } else {
                    binding.photoView.visibility = View.GONE
                    binding.tvNoImage.visibility = View.VISIBLE
                }
            } else {
                binding.photoView.visibility = View.GONE
                binding.tvNoImage.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mapId > 0) loadMap()
    }
}
