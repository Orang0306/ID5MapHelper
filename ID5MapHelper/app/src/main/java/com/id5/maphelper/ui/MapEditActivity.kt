package com.id5.maphelper.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.id5.maphelper.R
import com.id5.maphelper.data.MapDatabase
import com.id5.maphelper.data.MapEntity
import com.id5.maphelper.databinding.ActivityMapEditBinding
import com.id5.maphelper.util.FeatureMatcher
import com.id5.maphelper.util.ImageUtil
import kotlinx.coroutines.launch

class MapEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapEditBinding
    private val db by lazy { MapDatabase.getDatabase(this) }
    private var mapId: Long = 0
    private var currentImagePath: String = ""
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapId = intent.getLongExtra("map_id", 0)
        isEditMode = mapId > 0

        setupSpinners()
        setupButtons()

        if (isEditMode) {
            title = "编辑地图"
            loadMapData()
        } else {
            title = "添加地图"
        }
    }

    private fun setupSpinners() {
        binding.spinnerDoorType.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, FeatureMatcher.Options.DOOR_TYPES
        )
        binding.spinnerDoorDirection.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, FeatureMatcher.Options.DOOR_DIRECTIONS
        )
        binding.spinnerSideDoor.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, FeatureMatcher.Options.SIDE_DOORS
        )
        binding.spinnerSecondFloor.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, FeatureMatcher.Options.SECOND_FLOOR_DOORS
        )
        binding.spinnerFloor.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, FeatureMatcher.Options.FLOORS
        )
    }

    private fun setupButtons() {
        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_PICK_IMAGE)
        }

        binding.btnSave.setOnClickListener {
            saveMap()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadMapData() {
        lifecycleScope.launch {
            val map = db.mapDao().getMapById(mapId) ?: return@launch
            binding.etName.setText(map.name)
            binding.etRooms.setText(map.rooms)
            binding.etTreasureCount.setText(map.treasureCount.toString())
            binding.etRouteNote.setText(map.routeNote)
            binding.etRemark.setText(map.remark)
            currentImagePath = map.imagePath

            if (currentImagePath.isNotEmpty()) {
                val bitmap = ImageUtil.loadBitmap(currentImagePath, 400, 400)
                if (bitmap != null) {
                    binding.ivPreview.setImageBitmap(bitmap)
                    binding.ivPreview.visibility = View.VISIBLE
                }
            }

            binding.spinnerDoorType.setSelection(FeatureMatcher.Options.DOOR_TYPES.indexOf(map.doorType).coerceAtLeast(0))
            binding.spinnerDoorDirection.setSelection(FeatureMatcher.Options.DOOR_DIRECTIONS.indexOf(map.doorDirection).coerceAtLeast(0))
            binding.spinnerSideDoor.setSelection(FeatureMatcher.Options.SIDE_DOORS.indexOf(map.sideDoor).coerceAtLeast(0))
            binding.spinnerSecondFloor.setSelection(FeatureMatcher.Options.SECOND_FLOOR_DOORS.indexOf(map.secondFloorDoor).coerceAtLeast(0))
            binding.spinnerFloor.setSelection(FeatureMatcher.Options.FLOORS.indexOf(map.floor).coerceAtLeast(0))
        }
    }

    private fun saveMap() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入地图名称", Toast.LENGTH_SHORT).show()
            return
        }

        val map = MapEntity(
            id = if (isEditMode) mapId else 0,
            name = name,
            imagePath = currentImagePath,
            doorType = binding.spinnerDoorType.selectedItem as String,
            doorDirection = binding.spinnerDoorDirection.selectedItem as String,
            sideDoor = binding.spinnerSideDoor.selectedItem as String,
            secondFloorDoor = binding.spinnerSecondFloor.selectedItem as String,
            floor = binding.spinnerFloor.selectedItem as String,
            rooms = binding.etRooms.text.toString().trim(),
            treasureCount = binding.etTreasureCount.text.toString().toIntOrNull() ?: 0,
            routeNote = binding.etRouteNote.text.toString().trim(),
            remark = binding.etRemark.text.toString().trim(),
            updateTime = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            if (isEditMode) {
                db.mapDao().updateMap(map)
                Toast.makeText(this@MapEditActivity, "已更新", Toast.LENGTH_SHORT).show()
            } else {
                db.mapDao().insertMap(map)
                Toast.makeText(this@MapEditActivity, "已添加", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val savedPath = ImageUtil.saveImageToInternalStorage(this, uri)
                if (savedPath != null) {
                    // 删除旧图片
                    if (isEditMode && currentImagePath.isNotEmpty()) {
                        ImageUtil.deleteImage(currentImagePath)
                    }
                    currentImagePath = savedPath
                    val bitmap = ImageUtil.loadBitmap(savedPath, 400, 400)
                    binding.ivPreview.setImageBitmap(bitmap)
                    binding.ivPreview.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_PICK_IMAGE = 2001
    }
}
