package com.example.uptmtransportation.student

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmtransportation.R
import com.example.uptmtransportation.adapters.VehicleAdapter
import com.example.uptmtransportation.models.Vehicle
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip

class VehicleCatalogActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etSearch: EditText
    private lateinit var rvVehicles: RecyclerView
    private lateinit var tvAll: TextView
    private lateinit var tvBus: TextView
    private lateinit var tvCar: TextView
    private lateinit var chipAvailable: Chip
    private lateinit var tvVehicleCount: TextView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var loadingDialog: LoadingDialog

    private val vehicleList = mutableListOf<Vehicle>()
    private var filteredList = mutableListOf<Vehicle>()
    private var adapter: VehicleAdapter? = null
    private var selectedCategory = 0
    private var showAvailableOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_catalog)

        loadingDialog = LoadingDialog(this)
        initViews()
        setupToolbar()
        setupCategoryButtons()
        setupSearch()
        setupRecyclerView()
        loadVehiclesFromFirebase()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etSearch = findViewById(R.id.etSearch)
        rvVehicles = findViewById(R.id.rvVehicles)
        tvAll = findViewById(R.id.tvAll)
        tvBus = findViewById(R.id.tvBus)
        tvCar = findViewById(R.id.tvCar)
        chipAvailable = findViewById(R.id.chipAvailable)
        tvVehicleCount = findViewById(R.id.tvVehicleCount)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        rvVehicles.layoutManager = LinearLayoutManager(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = "Vehicle Catalog"
    }

    private fun setupCategoryButtons() {
        val buttons = listOf(tvAll, tvBus, tvCar)

        buttons.forEachIndexed { index, button ->
            button.isSelected = index == selectedCategory
            button.setOnClickListener {
                if (selectedCategory != index) {
                    selectedCategory = index
                    updateCategoryButtons()
                    filterVehicles()
                }
            }
        }

        chipAvailable.setOnClickListener {
            showAvailableOnly = chipAvailable.isChecked
            filterVehicles()
        }
    }

    private fun updateCategoryButtons() {
        tvAll.isSelected = selectedCategory == 0
        tvBus.isSelected = selectedCategory == 1
        tvCar.isSelected = selectedCategory == 2
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterVehicles()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadVehiclesFromFirebase() {
        loadingDialog.show()

        FirebaseHelper.getAllVehicles { vehicles, success, error ->
            loadingDialog.dismiss()
            if (success) {
                vehicleList.clear()
                // Filter ONLY Bus and Car
                vehicleList.addAll(vehicles.filter {
                    val type = it.type.lowercase()
                    type == "bus" || type == "car"
                })
                filterVehicles()
                Log.d("VehicleCatalog", "Loaded ${vehicleList.size} vehicles")
            } else {
                Toast.makeText(this, error ?: "Failed to load vehicles", Toast.LENGTH_SHORT).show()
                if (vehicleList.isEmpty()) showEmptyState()
            }
        }
    }

    private fun filterVehicles() {
        val type = when (selectedCategory) {
            1 -> "bus"
            2 -> "car"
            else -> "all"
        }
        val searchQuery = etSearch.text.toString().trim().lowercase()

        filteredList.clear()
        filteredList.addAll(vehicleList.filter { vehicle ->
            val vehicleType = vehicle.type.lowercase()
            val typeMatch = type == "all" || vehicleType == type
            val searchMatch = searchQuery.isEmpty() ||
                    vehicle.brand.lowercase().contains(searchQuery) ||
                    vehicle.plateNumber.lowercase().contains(searchQuery)
            val availableMatch = if (showAvailableOnly) {
                vehicle.isActive && vehicle.isAvailable
            } else {
                true  // Show ALL vehicles including inactive ones
            }
            typeMatch && searchMatch && availableMatch
        })

        adapter?.updateList(filteredList)
        updateVehicleCount()

        if (filteredList.isEmpty()) showEmptyState() else hideEmptyState()
    }

    private fun setupRecyclerView() {
        adapter = VehicleAdapter(
            vehicles = filteredList,
            onViewSeatsClick = { vehicle ->
                // Only allow viewing seats if vehicle is active
                if (vehicle.isActive && vehicle.isAvailable) {
                    val intent = Intent(this, SeatMappingActivity::class.java)
                    intent.putExtra("vehicle", vehicle)
                    intent.putExtra("mode", "VIEW")
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "This vehicle is currently unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rvVehicles.adapter = adapter
    }

    private fun updateVehicleCount() {
        tvVehicleCount.text = " (${filteredList.size})"
    }

    private fun showEmptyState() {
        layoutEmpty.visibility = View.VISIBLE
        rvVehicles.visibility = View.GONE
    }

    private fun hideEmptyState() {
        layoutEmpty.visibility = View.GONE
        rvVehicles.visibility = View.VISIBLE
    }
}