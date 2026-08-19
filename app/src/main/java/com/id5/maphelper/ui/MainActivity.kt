package com.id5.maphelper.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.id5.maphelper.R
import com.id5.maphelper.data.MapDatabase
import com.id5.maphelper.data.MapEntity
import com.id5.maphelper.databinding.ActivityMainBinding
import com.id5.maphelper.service.FloatingWindowService
import com.id5.maphelper.util.ImageUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MapListAdapter
    private val db by lazy { MapDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        observeMaps()
    }

    private fun setupRecyclerView() {
        adapter = MapListAdapter(
            onItemClick = { map -> openMapDetail(map) },
            onMenuClick = { view, map -> showPopupMenu(view, map) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.fabAddMap.setOnClickListener {
            startActivity(Intent(this, MapEditActivity::class.java))
        }

        binding.btnMatchMap.setOnClickListener {
            startActivity(Intent(this, MapMatchActivity::class.java))
        }

        binding.btnFloatWindow.setOnClickListener {
            toggleFloatingWindow()
        }
    }

    private fun observeMaps() {
        lifecycleScope.launch {
            db.mapDao().getAllMaps().collectLatest { maps ->
                adapter.submitList(maps)
                binding.tvEmpty.visibility = if (maps.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (maps.isEmpty()) View.GONE else View.VISIBLE
                binding.tvMapCount.text = "共 ${maps.size} 张地图"
            }
        }
    }

    private fun openMapDetail(map: MapEntity) {
        val intent = Intent(this, MapDetailActivity::class.java).apply {
            putExtra("map_id", map.id)
        }
        startActivity(intent)
    }

    private fun showPopupMenu(view: View, map: MapEntity) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.map_item_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    val intent = Intent(this, MapEditActivity::class.java).apply {
                        putExtra("map_id", map.id)
                    }
                    startActivity(intent)
                    true
                }
                R.id.action_delete -> {
                    confirmDelete(map)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDelete(map: MapEntity) {
        AlertDialog.Builder(this)
            .setTitle("删除地图")
            .setMessage("确定删除「${map.name}」吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    db.mapDao().deleteMap(map)
                    if (map.imagePath.isNotEmpty()) {
                        ImageUtil.deleteImage(map.imagePath)
                    }
                    Toast.makeText(this@MainActivity, "已删除", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleFloatingWindow() {
        if (FloatingWindowService.isRunning) {
            stopService(Intent(this, FloatingWindowService::class.java))
            binding.btnFloatWindow.text = getString(R.string.start_float)
        } else {
            if (checkOverlayPermission()) {
                startFloatingService()
            } else {
                requestOverlayPermission()
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle(R.string.float_permission_needed)
            .setMessage("需要悬浮窗权限才能在游戏上方显示地图助手")
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        binding.btnFloatWindow.text = getString(R.string.stop_float)
        Toast.makeText(this, "悬浮窗已开启，切换到游戏即可使用", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (checkOverlayPermission()) {
                startFloatingService()
            } else {
                Toast.makeText(this, "权限未授予", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.btnFloatWindow.text = if (FloatingWindowService.isRunning) {
            getString(R.string.stop_float)
        } else {
            getString(R.string.start_float)
        }
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }
}

class MapListAdapter(
    private val onItemClick: (MapEntity) -> Unit,
    private val onMenuClick: (View, MapEntity) -> Unit
) : RecyclerView.Adapter<MapListAdapter.ViewHolder>() {

    private var maps: List<MapEntity> = emptyList()

    fun submitList(list: List<MapEntity>) {
        maps = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvFeatures: TextView = itemView.findViewById(R.id.tvFeatures)
        val tvRooms: TextView = itemView.findViewById(R.id.tvRooms)
        val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_map, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val map = maps[position]
        holder.tvName.text = map.name

        val features = buildString {
            if (map.doorType.isNotEmpty()) append(map.doorType)
            if (map.doorDirection.isNotEmpty()) append(" · ${map.doorDirection}")
            if (map.sideDoor.isNotEmpty() && map.sideDoor != "无") append(" · 侧门${map.sideDoor}")
            if (map.secondFloorDoor == "有") append(" · 二楼")
        }
        holder.tvFeatures.text = features.ifEmpty { "未设置特征" }
        holder.tvRooms.text = if (map.rooms.isNotEmpty()) "房间: ${map.rooms}" else ""

        if (map.imagePath.isNotEmpty()) {
            val bitmap = ImageUtil.loadBitmap(map.imagePath, 200, 200)
            if (bitmap != null) {
                holder.ivThumbnail.setImageBitmap(bitmap)
                holder.ivThumbnail.visibility = View.VISIBLE
            } else {
                holder.ivThumbnail.visibility = View.GONE
            }
        } else {
            holder.ivThumbnail.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(map) }
        holder.ivMenu.setOnClickListener { onMenuClick(it, map) }
    }

    override fun getItemCount() = maps.size
}
