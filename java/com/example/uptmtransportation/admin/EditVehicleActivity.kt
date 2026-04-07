package com.example.uptmtransportation.admin

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Vehicle
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.util.*

class EditVehicleActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rgVehicleType: RadioGroup
    private lateinit var rbBus: RadioButton
    private lateinit var rbCar: RadioButton
    private lateinit var etBrand: EditText
    private lateinit var etPlateNumber: EditText
    private lateinit var etSeatCapacity: EditText
    private lateinit var etPricePerKm: EditText
    private lateinit var etImageUrl: EditText
    private lateinit var tvSeatLayoutInfo: TextView
    private lateinit var btnUpdateVehicle: MaterialButton
    private lateinit var btnDeleteVehicle: MaterialButton
    private lateinit var loadingDialog: LoadingDialog

    private var vehicle: Vehicle? = null
    private var vehicleId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_vehicle)

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.uptm_blue)
        }

        loadingDialog = LoadingDialog(this)
        initViews()
        setupToolbar()
        getVehicleData()
        setupListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rgVehicleType = findViewById(R.id.rgVehicleType)
        rbBus = findViewById(R.id.rbBus)
        rbCar = findViewById(R.id.rbCar)
        etBrand = findViewById(R.id.etBrand)
        etPlateNumber = findViewById(R.id.etPlateNumber)
        etSeatCapacity = findViewById(R.id.etSeatCapacity)
        etPricePerKm = findViewById(R.id.etPricePerKm)
        etImageUrl = findViewById(R.id.etImageUrl)
        tvSeatLayoutInfo = findViewById(R.id.tvSeatLayoutInfo)
        btnUpdateVehicle = findViewById(R.id.btnUpdateVehicle)
        btnDeleteVehicle = findViewById(R.id.btnDeleteVehicle)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun getVehicleData() {
        vehicle = intent.getSerializableExtra("vehicle") as? Vehicle
        vehicleId = intent.getStringExtra("vehicleId") ?: ""

        if (vehicle == null && vehicleId.isNotEmpty()) {
            loadingDialog.show()
            FirebaseHelper.getVehicleById(vehicleId) { v, success, error ->
                loadingDialog.dismiss()
                if (success && v != null) {
                    vehicle = v
                    populateFields()
                } else {
                    Toast.makeText(this, error ?: "Failed to load vehicle", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else if (vehicle != null) {
            populateFields()
        } else {
            Toast.makeText(this, "Vehicle not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateFields() {
        vehicle?.let { v ->
            // Set vehicle type
            when (v.type.lowercase()) {
                "bus" -> rbBus.isChecked = true
                "car" -> rbCar.isChecked = true
            }

            // Set other fields
            etBrand.setText(v.brand)
            etPlateNumber.setText(v.plateNumber)
            etSeatCapacity.setText(v.seatCapacity.toString())
            etPricePerKm.setText(String.format("%.2f", v.pricePerKm))

            // Set image URL if exists
            val imageUrl = v.getMainImage()
            if (imageUrl.isNotEmpty()) {
                etImageUrl.setText(imageUrl)
            }

            // Update seat layout info based on type
            updateSeatLayoutInfo()
        }
    }

    private fun updateSeatLayoutInfo() {
        val capacity = etSeatCapacity.text.toString().toIntOrNull() ?: 0
        val vehicleType = if (rbBus.isChecked) "Bus" else "Car"

        if (vehicleType == "Bus") {
            val rows = if (capacity <= 20) 5 else if (capacity <= 40) 8 else 10
            val cols = if (capacity <= 20) 4 else if (capacity <= 40) 5 else 6
            tvSeatLayoutInfo.text = "* Seat layout: $rows rows x $cols columns (auto-generated)"
        } else {
            tvSeatLayoutInfo.text = "* Seat layout: Car has standard seat layout (2 rows x 2-3 seats)"
        }
    }

    private fun setupListeners() {
        // Update seat layout info when capacity or type changes
        etSeatCapacity.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSeatLayoutInfo()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        rgVehicleType.setOnCheckedChangeListener { _, _ ->
            updateSeatLayoutInfo()
        }

        btnUpdateVehicle.setOnClickListener {
            updateVehicle()
        }

        btnDeleteVehicle.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun updateVehicle() {
        val type = if (rbBus.isChecked) "bus" else "car"
        val brand = etBrand.text.toString().trim()
        val plateNumber = etPlateNumber.text.toString().trim()
        val seatCapacity = etSeatCapacity.text.toString().toIntOrNull() ?: 0
        val pricePerKm = etPricePerKm.text.toString().toDoubleOrNull() ?: 0.0
        val imageUrl = etImageUrl.text.toString().trim()

        // Validation
        if (brand.isEmpty()) {
            etBrand.error = "Please enter brand"
            return
        }
        if (plateNumber.isEmpty()) {
            etPlateNumber.error = "Please enter plate number"
            return
        }
        if (seatCapacity <= 0) {
            etSeatCapacity.error = "Please enter valid seat capacity"
            return
        }
        if (pricePerKm <= 0) {
            etPricePerKm.error = "Please enter valid price"
            return
        }

        loadingDialog.show()

        // Prepare images list
        val images = if (imageUrl.isNotEmpty()) listOf(imageUrl) else listOf()

        val updatedVehicle = vehicle?.copy(
            type = type,
            brand = brand,
            plateNumber = plateNumber,
            seatCapacity = seatCapacity,
            pricePerKm = pricePerKm,
            images = images
        ) ?: return

        FirebaseHelper.updateVehicle(updatedVehicle) { success, error ->
            loadingDialog.dismiss()
            if (success) {
                Toast.makeText(this, "Vehicle updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, error ?: "Failed to update vehicle", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Vehicle")
            .setMessage("Are you sure you want to delete this vehicle?\n\nVehicle: ${vehicle?.brand} ${vehicle?.plateNumber}\nType: ${vehicle?.type?.uppercase()}")
            .setPositiveButton("DELETE") { _, _ ->
                deleteVehicle()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun deleteVehicle() {
        val vehicleId = vehicle?.id ?: return

        loadingDialog.show()
        FirebaseHelper.deleteVehicle(vehicleId) { success, error ->
            loadingDialog.dismiss()
            if (success) {
                Toast.makeText(this, "Vehicle deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, error ?: "Failed to delete vehicle", Toast.LENGTH_SHORT).show()
            }
        }
    }
}