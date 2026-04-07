package com.example.uptmtransportation.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Vehicle
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar

class AdminVehicleCatalogActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etSearch: EditText
    private lateinit var rvVehicles: RecyclerView
    private lateinit var tvAll: TextView
    private lateinit var tvBus: TextView
    private lateinit var tvCar: TextView
    private lateinit var tvVehicleCount: TextView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var loadingDialog: LoadingDialog

    private val vehicleList = mutableListOf<Vehicle>()
    private var filteredList = mutableListOf<Vehicle>()
    private var adapter: AdminVehicleAdapter? = null

    private var selectedType = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_vehicle_catalog)

        loadingDialog = LoadingDialog(this)

        initViews()
        setupToolbar()
        loadVehiclesFromFirebase()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etSearch = findViewById(R.id.etSearch)
        rvVehicles = findViewById(R.id.rvVehicles)
        tvAll = findViewById(R.id.tvAll)
        tvBus = findViewById(R.id.tvBus)
        tvCar = findViewById(R.id.tvCar)
        tvVehicleCount = findViewById(R.id.tvVehicleCount)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        rvVehicles.layoutManager = LinearLayoutManager(this)

        updateTabUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = "Manage Vehicles"
    }

    private fun loadVehiclesFromFirebase() {
        loadingDialog.show()
        FirebaseHelper.getAllVehicles { vehicles, success, error ->
            loadingDialog.dismiss()
            if (success) {
                vehicleList.clear()
                vehicleList.addAll(vehicles)
                filterVehicles()
                setupRecyclerView()
                setupListeners()
            } else {
                Toast.makeText(this, error ?: "Failed to load vehicles", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminVehicleAdapter(
            vehicles = filteredList,
            onEditClick = { vehicle ->
                val intent = Intent(this, EditVehicleActivity::class.java)
                intent.putExtra("vehicle", vehicle)
                startActivity(intent)
            },
            onDeleteClick = { vehicle ->
                showDeleteConfirmation(vehicle)
            },
            onToggleActive = { vehicle, isActive ->
                toggleVehicleActive(vehicle, isActive)
            }
        )
        rvVehicles.adapter = adapter
    }

    private fun setupListeners() {
        tvAll.setOnClickListener {
            selectedType = "all"
            updateTabUI()
            filterVehicles()
        }

        tvBus.setOnClickListener {
            selectedType = "bus"
            updateTabUI()
            filterVehicles()
        }

        tvCar.setOnClickListener {
            selectedType = "car"
            updateTabUI()
            filterVehicles()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterVehicles()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateTabUI() {
        tvAll.isSelected = selectedType == "all"
        tvBus.isSelected = selectedType == "bus"
        tvCar.isSelected = selectedType == "car"

        val selectedColor = getColor(R.color.white)
        val unselectedColor = getColor(R.color.black)

        tvAll.setTextColor(if (tvAll.isSelected) selectedColor else unselectedColor)
        tvBus.setTextColor(if (tvBus.isSelected) selectedColor else unselectedColor)
        tvCar.setTextColor(if (tvCar.isSelected) selectedColor else unselectedColor)
    }

    private fun filterVehicles() {
        val query = etSearch.text.toString().lowercase()
        filteredList.clear()
        filteredList.addAll(vehicleList.filter { vehicle ->
            val typeMatch = selectedType == "all" || vehicle.type.lowercase() == selectedType.lowercase()
            val searchMatch = query.isEmpty() ||
                    vehicle.brand.lowercase().contains(query) ||
                    vehicle.plateNumber.lowercase().contains(query)
            typeMatch && searchMatch
        })
        adapter?.updateList(filteredList)
        updateVehicleCount()
        if (filteredList.isEmpty()) showEmptyState() else hideEmptyState()
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

    private fun showDeleteConfirmation(vehicle: Vehicle) {
        AlertDialog.Builder(this)
            .setTitle("Delete Vehicle")
            .setMessage("Are you sure you want to delete ${vehicle.brand} ${vehicle.plateNumber}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteVehicle(vehicle)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteVehicle(vehicle: Vehicle) {
        loadingDialog.show()
        FirebaseHelper.deleteVehicle(vehicle.id) { success, error ->
            loadingDialog.dismiss()
            if (success) {
                vehicleList.remove(vehicle)
                filterVehicles()
                Toast.makeText(this, "Vehicle deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, error ?: "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleVehicleActive(vehicle: Vehicle, isActive: Boolean) {
        val updatedVehicle = vehicle.copy(
            isActive = isActive,
            isAvailable = isActive
        )

        loadingDialog.show()
        FirebaseHelper.updateVehicle(updatedVehicle) { success, error ->
            loadingDialog.dismiss()
            if (success) {
                val index = vehicleList.indexOfFirst { it.id == vehicle.id }
                if (index != -1) {
                    vehicleList[index] = updatedVehicle
                    filterVehicles()
                }
                val status = if (isActive) "activated" else "deactivated"
                Toast.makeText(this, "Vehicle $status", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, error ?: "Failed to update", Toast.LENGTH_SHORT).show()
                // Reload to revert toggle if failed
                loadVehiclesFromFirebase()
            }
        }
    }
}