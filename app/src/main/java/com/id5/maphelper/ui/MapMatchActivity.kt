package com.id5.maphelper.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.id5.maphelper.R
import com.id5.maphelper.data.MapDatabase
import com.id5.maphelper.data.MapEntity
import com.id5.maphelper.databinding.ActivityMapMatchBinding
import com.id5.maphelper.util.FeatureMatcher
import kotlinx.coroutines.launch

class MapMatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapMatchBinding
    private val db by lazy { MapDatabase.getDatabase(this) }
    private lateinit var resultAdapter: MatchResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "识别地图"

        setupSpinners()
        setupRecyclerView()
        setupButtons()
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

    private fun setupRecyclerView() {
        resultAdapter = MatchResultAdapter { map ->
            val intent = Intent(this, MapDetailActivity::class.java).apply {
                putExtra("map_id", map.id)
            }
            startActivity(intent)
        }
        binding.recyclerViewResults.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewResults.adapter = resultAdapter
    }

    private fun setupButtons() {
        binding.btnMatch.setOnClickListener {
            performMatch()
        }
        binding.btnReset.setOnClickListener {
            resetFilters()
        }
    }

    private fun performMatch() {
        val doorType = binding.spinnerDoorType.selectedItem as String
        val doorDirection = binding.spinnerDoorDirection.selectedItem as String
        val sideDoor = binding.spinnerSideDoor.selectedItem as String
        val secondFloorDoor = binding.spinnerSecondFloor.selectedItem as String
        val floor = binding.spinnerFloor.selectedItem as String

        if (doorType.isEmpty() && doorDirection.isEmpty() && sideDoor.isEmpty()
            && secondFloorDoor.isEmpty() && floor.isEmpty()) {
            Toast.makeText(this, "请至少选择一个特征", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val allMaps = db.mapDao().getAllMapsOnce()
            val results = FeatureMatcher.matchMaps(
                allMaps, doorType, doorDirection, sideDoor, secondFloorDoor, floor
            )

            binding.tvResultCount.text = getString(R.string.match_count, results.size)

            if (results.isEmpty()) {
                binding.tvNoMatch.visibility = View.VISIBLE
                binding.recyclerViewResults.visibility = View.GONE
            } else {
                binding.tvNoMatch.visibility = View.GONE
                binding.recyclerViewResults.visibility = View.VISIBLE
                resultAdapter.submitList(results)

                if (FeatureMatcher.isHighConfidence(results)) {
                    val best = results.first()
                    Toast.makeText(
                        this@MapMatchActivity,
                        "最可能是: ${best.map.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun resetFilters() {
        binding.spinnerDoorType.setSelection(0)
        binding.spinnerDoorDirection.setSelection(0)
        binding.spinnerSideDoor.setSelection(0)
        binding.spinnerSecondFloor.setSelection(0)
        binding.spinnerFloor.setSelection(0)
        binding.tvResultCount.text = ""
        binding.tvNoMatch.visibility = View.GONE
        binding.recyclerViewResults.visibility = View.GONE
    }
}

class MatchResultAdapter(
    private val onItemClick: (MapEntity) -> Unit
) : RecyclerView.Adapter<MatchResultAdapter.ViewHolder>() {

    private var results: List<FeatureMatcher.MatchResult> = emptyList()

    fun submitList(list: List<FeatureMatcher.MatchResult>) {
        results = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        val tvMatched: TextView = itemView.findViewById(R.id.tvMatched)
        val tvFeatures: TextView = itemView.findViewById(R.id.tvFeatures)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = results[position]
        holder.tvName.text = result.map.name
        holder.tvScore.text = "匹配度: ${result.score}%"
        holder.tvMatched.text = FeatureMatcher.getMatchDescription(result)

        val features = buildString {
            if (result.map.doorType.isNotEmpty()) append(result.map.doorType)
            if (result.map.doorDirection.isNotEmpty()) append(" · ${result.map.doorDirection}")
            if (result.map.sideDoor.isNotEmpty()) append(" · 侧门${result.map.sideDoor}")
        }
        holder.tvFeatures.text = features

        holder.itemView.setOnClickListener { onItemClick(result.map) }
    }

    override fun getItemCount() = results.size
}
