package com.example.uptmtransportation.admin

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Seat
import com.example.uptmtransportation.models.Vehicle
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.button.MaterialButton
import java.util.*

class AddVehicleActivity : AppCompatActivity() {

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var ivVehicleImage: ImageView
    private lateinit var etImageUrl: EditText
    private lateinit var spinnerVehicleType: Spinner
    private lateinit var etBrand: EditText
    private lateinit var etPlateNumber: EditText
    private lateinit var etSeatCapacity: EditText
    private lateinit var etPricePerKm: EditText
    private lateinit var tvSeatLayoutInfo: TextView
    private lateinit var btnAddVehicle: MaterialButton
    private lateinit var loadingDialog: LoadingDialog

    private val vehicleTypes = arrayOf("Bus", "Car")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vehicle)

        loadingDialog = LoadingDialog(this)
        initViews()
        setupToolbar()
        setupSpinner()
        setupListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        ivVehicleImage = findViewById(R.id.ivVehicleImage)
        etImageUrl = findViewById(R.id.etImageUrl)
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType)
        etBrand = findViewById(R.id.etBrand)
        etPlateNumber = findViewById(R.id.etPlateNumber)
        etSeatCapacity = findViewById(R.id.etSeatCapacity)
        etPricePerKm = findViewById(R.id.etPricePerKm)
        tvSeatLayoutInfo = findViewById(R.id.tvSeatLayoutInfo)
        btnAddVehicle = findViewById(R.id.btnAddVehicle)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicleTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVehicleType.adapter = adapter

        spinnerVehicleType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = vehicleTypes[position].lowercase()
                if (selectedType == "bus") {
                    tvSeatLayoutInfo.text = "* Seat layout: 9 rows x 4 columns = 36 seats"
                } else {
                    tvSeatLayoutInfo.text = "* Seat layout: 2 rows x 2 columns = 4 seats"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        // Preview image when URL is entered
        etImageUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val url = s.toString().trim()
                if (url.isNotEmpty()) {
                    Glide.with(this@AddVehicleActivity)
                        .load(url)
                        .placeholder(R.drawable.ic_bus)
                        .error(R.drawable.ic_bus)
                        .centerCrop()
                        .into(ivVehicleImage)
                } else {
                    val type = spinnerVehicleType.selectedItem.toString().lowercase()
                    val defaultIcon = if (type == "bus") R.drawable.ic_bus else R.drawable.ic_car
                    ivVehicleImage.setImageResource(defaultIcon)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnAddVehicle.setOnClickListener {
            if (validateInputs()) {
                addVehicle()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (etBrand.text.toString().trim().isEmpty()) {
            etBrand.error = "Please enter model"
            isValid = false
        }

        if (etPlateNumber.text.toString().trim().isEmpty()) {
            etPlateNumber.error = "Please enter plate number"
            isValid = false
        }

        val capacityStr = etSeatCapacity.text.toString().trim()
        if (capacityStr.isEmpty()) {
            etSeatCapacity.error = "Please enter seat capacity"
            isValid = false
        } else if (capacityStr.toIntOrNull() == null || capacityStr.toInt() <= 0) {
            etSeatCapacity.error = "Please enter valid seat capacity"
            isValid = false
        }

        val priceStr = etPricePerKm.text.toString().trim()
        if (priceStr.isEmpty()) {
            etPricePerKm.error = "Please enter price per km"
            isValid = false
        } else if (priceStr.toDoubleOrNull() == null || priceStr.toDouble() <= 0) {
            etPricePerKm.error = "Please enter valid price"
            isValid = false
        }

        return isValid
    }

    private fun addVehicle() {
        loadingDialog.show()

        val vehicleId = System.currentTimeMillis().toString()
        val type = spinnerVehicleType.selectedItem.toString().lowercase()
        val brand = etBrand.text.toString().trim()
        val plateNumber = etPlateNumber.text.toString().trim()
        val seatCapacity = etSeatCapacity.text.toString().trim().toInt()
        val pricePerKm = etPricePerKm.text.toString().trim().toDouble()
        val imageUrl = etImageUrl.text.toString().trim()

        // Generate seat layout based on type
        val seatLayout = generateSeatLayout(seatCapacity, type)

        val images = if (imageUrl.isNotEmpty()) listOf(imageUrl) else listOf()

        val newVehicle = Vehicle(
            id = vehicleId,
            type = type,
            brand = brand,
            plateNumber = plateNumber,
            seatCapacity = seatCapacity,
            seatLayout = seatLayout,
            pricePerKm = pricePerKm,
            images = images,
            isAvailable = true,
            isActive = true,
            createdAt = Date()
        )

        FirebaseHelper.addVehicle(newVehicle) { success, error ->
            loadingDialog.dismiss()
            if (success) {
                showSuccessDialog()
            } else {
                Toast.makeText(this@AddVehicleActivity, error ?: "Failed to add vehicle", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateSeatLayout(capacity: Int, type: String): List<Seat> {
        val seats = mutableListOf<Seat>()

        if (type == "car") {
            // Car layout: 2 rows x 2 columns (4 seats)
            val letters = listOf("A", "B")
            for (row in 1..2) {
                for (col in letters.indices) {
                    if (seats.size >= capacity) break
                    val seatNumber = "$row${letters[col]}"
                    seats.add(
                        Seat(
                            seatNumber = seatNumber,
                            status = "available",
                            bookedBy = "",
                            gender = ""
                        )
                    )
                }
            }
        } else {
            // Bus layout: 9 rows x 4 columns (36 seats maximum)
            // Letters: A, B, C, D (4 columns)
            val letters = listOf("A", "B", "C", "D")

            for (row in 1..9) {
                for (col in letters.indices) {
                    if (seats.size >= capacity) break
                    val seatNumber = "$row${letters[col]}"
                    seats.add(
                        Seat(
                            seatNumber = seatNumber,
                            status = "available",
                            bookedBy = "",
                            gender = ""
                        )
                    )
                }
                if (seats.size >= capacity) break
            }
        }

        return seats
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("Vehicle added successfully!")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}