package com.example.uptmtransportation.student

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Booking
import com.example.uptmtransportation.models.Event
import com.example.uptmtransportation.models.EventTimeSlot
import com.example.uptmtransportation.models.Vehicle
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class MakeBookingActivity : AppCompatActivity() {

    // Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rgTripType: RadioGroup
    private lateinit var rbFixed: RadioButton
    private lateinit var rbEvent: RadioButton
    private lateinit var layoutFixed: LinearLayout
    private lateinit var layoutEvent: LinearLayout
    private lateinit var rgFixedDestination: RadioGroup
    private lateinit var layoutReturnDateTime: LinearLayout
    private lateinit var btnSelectReturnDate: Button
    private lateinit var btnSelectReturnTime: Button
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var spinnerVehicle: Spinner
    private lateinit var layoutPrice: View
    private lateinit var tvDepartureInfo: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvOneWayPrice: TextView
    private lateinit var tvReturnPrice: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var layoutReturnSection: LinearLayout
    private lateinit var tvReturnInfo: TextView
    private lateinit var btnBookNow: MaterialButton
    private lateinit var btnSelectSeats: MaterialButton
    private lateinit var loadingDialog: LoadingDialog

    // Event Trip Views
    private lateinit var tvEventTitle: TextView
    private lateinit var tvEventLocation: TextView
    private lateinit var tvEventDateTime: TextView
    private lateinit var layoutTimeSlots: LinearLayout
    private lateinit var tvTimeSlotsError: TextView
    private lateinit var layoutReturnTimeSlots: LinearLayout
    private lateinit var layoutReturnTimeSlotsContainer: LinearLayout
    private lateinit var tvReturnTimeSlotsError: TextView

    private lateinit var layoutSeatsInfo: LinearLayout
    private lateinit var tvSeatsInfo: TextView

    // Data
    private var selectedDate: Long = System.currentTimeMillis()
    private var selectedTime: String = "08:00"
    private var selectedReturnDate: Long = System.currentTimeMillis()
    private var selectedReturnTime: String = "18:00"
    private var selectedVehicle: Vehicle? = null
    private var selectedSeats = listOf<String>()
    private var currentUserId = ""
    private var currentUserName = ""
    private var currentUserGender = ""

    // Event Trip - Time Slots from Firebase
    private var currentEvent: Event? = null
    private var departureSlots = mutableListOf<EventTimeSlot>()
    private var returnSlots = mutableListOf<EventTimeSlot>()
    private var selectedDepartureSlot: EventTimeSlot? = null
    private var selectedReturnSlot: EventTimeSlot? = null
    private var selectedTimeSlot: String? = null
    private var selectedReturnTimeSlot: String? = null

    private val vehicleList = mutableListOf<Vehicle>()

    private val seatMappingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val returnedSeats = result.data?.getStringArrayListExtra("selectedSeats") ?: listOf()
            Log.d("MakeBooking", "Seats returned: $returnedSeats")

            if (returnedSeats.isNotEmpty()) {
                selectedSeats = returnedSeats
                btnSelectSeats.text = "Seats: ${selectedSeats.joinToString()}"
                btnSelectSeats.setBackgroundColor(ContextCompat.getColor(this, R.color.green_available))
            } else {
                selectedSeats = emptyList()
                btnSelectSeats.text = "SELECT SEATS"
                btnSelectSeats.setBackgroundColor(ContextCompat.getColor(this, R.color.uptm_blue))
            }
            calculatePrice()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_booking)

        Log.d("MAKEBOOKING", "========== MAKE BOOKING ACTIVITY STARTED ==========")

        loadingDialog = LoadingDialog(this)
        initViews()
        setupToolbar()
        loadUserData()
        loadVehicles()
        setupListeners()

        val event = intent.getSerializableExtra("event") as? Event
        Log.d("MAKEBOOKING", "Event received: ${event?.title} (ID: ${event?.id})")

        if (event != null) {
            currentEvent = event
            setupEventTrip(event)
        } else {
            Log.d("MAKEBOOKING", "No event received - Fixed Trip mode")
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rgTripType = findViewById(R.id.rgTripType)
        rbFixed = findViewById(R.id.rbFixed)
        rbEvent = findViewById(R.id.rbEvent)
        layoutFixed = findViewById(R.id.layoutFixed)
        layoutEvent = findViewById(R.id.layoutEvent)
        rgFixedDestination = findViewById(R.id.rgFixedDestination)
        layoutReturnDateTime = findViewById(R.id.layoutReturnDateTime)
        btnSelectReturnDate = findViewById(R.id.btnSelectReturnDate)
        btnSelectReturnTime = findViewById(R.id.btnSelectReturnTime)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        spinnerVehicle = findViewById(R.id.spinnerVehicle)
        layoutPrice = findViewById(R.id.layoutPrice)
        tvDepartureInfo = findViewById(R.id.tvDepartureInfo)
        tvDistance = findViewById(R.id.tvDistance)
        tvOneWayPrice = findViewById(R.id.tvOneWayPrice)
        tvReturnPrice = findViewById(R.id.tvReturnPrice)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        layoutReturnSection = findViewById(R.id.layoutReturnSection)
        tvReturnInfo = findViewById(R.id.tvReturnInfo)
        btnBookNow = findViewById(R.id.btnBookNow)
        btnSelectSeats = findViewById(R.id.btnSelectSeats)
        layoutSeatsInfo = findViewById(R.id.layoutSeatsInfo)
        tvSeatsInfo = findViewById(R.id.tvSeatsInfo)

        tvEventTitle = findViewById(R.id.tvEventTitle)
        tvEventLocation = findViewById(R.id.tvEventLocation)
        tvEventDateTime = findViewById(R.id.tvEventDateTime)
        layoutTimeSlots = findViewById(R.id.layoutTimeSlots)
        tvTimeSlotsError = findViewById(R.id.tvTimeSlotsError)
        layoutReturnTimeSlots = findViewById(R.id.layoutReturnTimeSlots)
        layoutReturnTimeSlotsContainer = findViewById(R.id.layoutReturnTimeSlotsContainer)
        tvReturnTimeSlotsError = findViewById(R.id.tvReturnTimeSlotsError)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadUserData() {
        FirebaseHelper.getUser { user, success, _ ->
            if (success && user != null) {
                currentUserId = user.id
                currentUserName = user.fullName
                currentUserGender = user.gender
                Log.d("MakeBooking", "User loaded: $currentUserName")
            }
        }
    }

    private fun loadVehicles() {
        loadingDialog.show()
        FirebaseHelper.getAllVehicles { vehicles, success, error ->
            loadingDialog.dismiss()
            if (success) {
                vehicleList.clear()
                vehicleList.addAll(vehicles.filter { it.isAvailable && it.isActive })
                setupVehicleSpinner()
            } else {
                Toast.makeText(this, error ?: "Failed to load vehicles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupVehicleSpinner() {
        val availableVehicles = vehicleList.filter { it.isAvailable && it.isActive }
        val vehicleNames = availableVehicles
            .map { "${it.brand} (${it.plateNumber}) - ${it.type}" }
            .toTypedArray()

        if (vehicleNames.isEmpty()) {
            val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayOf("No vehicles available")) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as TextView).setTextColor(ContextCompat.getColor(this@MakeBookingActivity, R.color.black))
                    return view
                }
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerVehicle.adapter = adapter
            return
        }

        val vehicleAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, vehicleNames) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(ContextCompat.getColor(this@MakeBookingActivity, R.color.black))
                textView.textSize = 14f
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                textView.setTextColor(ContextCompat.getColor(this@MakeBookingActivity, R.color.black))
                textView.textSize = 14f
                textView.setPadding(16, 12, 16, 12)
                return view
            }
        }

        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVehicle.adapter = vehicleAdapter

        var isProgrammaticSelection = false
        var lastSelectedVehicleId: String? = null

        spinnerVehicle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < availableVehicles.size) {
                    val newVehicle = availableVehicles[position]
                    val newVehicleId = newVehicle.id

                    if (!isProgrammaticSelection && lastSelectedVehicleId != null && lastSelectedVehicleId != newVehicleId && selectedSeats.isNotEmpty()) {
                        selectedSeats = emptyList()
                        btnSelectSeats.text = "SELECT SEATS"
                        btnSelectSeats.setBackgroundColor(ContextCompat.getColor(this@MakeBookingActivity, R.color.uptm_blue))
                    }

                    lastSelectedVehicleId = newVehicleId
                    selectedVehicle = newVehicle
                    calculatePrice()

                    val isBus = selectedVehicle?.type?.lowercase() == "bus"
                    btnSelectSeats.visibility = if (isBus) View.VISIBLE else View.GONE

                    isProgrammaticSelection = false
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        if (selectedVehicle != null) {
            isProgrammaticSelection = true
            val index = availableVehicles.indexOfFirst { it.id == selectedVehicle!!.id }
            if (index != -1) {
                spinnerVehicle.setSelection(index, false)
            }
            isProgrammaticSelection = false
        }
    }

    // ==================== EVENT TRIP WITH FIREBASE TIME SLOTS ====================

    private fun setupEventTrip(event: Event) {
        rbEvent.isChecked = true
        layoutFixed.visibility = View.GONE
        layoutEvent.visibility = View.VISIBLE
        layoutReturnDateTime.visibility = View.GONE
        layoutReturnTimeSlots.visibility = View.VISIBLE
        layoutTimeSlots.visibility = View.VISIBLE

        tvEventTitle.text = event.title
        tvEventLocation.text = event.location
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val eventDateStr = event.date?.let { dateFormat.format(it) } ?: ""
        tvEventDateTime.text = "$eventDateStr • Start: ${event.time} • End: ${event.endTime.ifEmpty { "TBA" }}"

        event.date?.let {
            selectedDate = it.time
            btnSelectDate.text = dateFormat.format(it)
            btnSelectDate.isEnabled = false
        }

        btnSelectTime.visibility = View.GONE
        btnSelectDate.visibility = View.GONE

        loadEventTimeSlots(event)
    }

    private fun loadEventTimeSlots(event: Event) {
        loadingDialog.show()

        Log.d("MAKEBOOKING", "=========================================")
        Log.d("MAKEBOOKING", "LOADING TIME SLOTS")
        Log.d("MAKEBOOKING", "Event ID: ${event.id}")
        Log.d("MAKEBOOKING", "Event Title: ${event.title}")
        Log.d("MAKEBOOKING", "=========================================")

        FirebaseHelper.getEventTimeSlots(event.id) { slots, success, error ->
            loadingDialog.dismiss()

            Log.d("MAKEBOOKING", "Success: $success")
            Log.d("MAKEBOOKING", "Slots count: ${slots.size}")
            Log.d("MAKEBOOKING", "Error: $error")

            if (success) {
                departureSlots.clear()
                returnSlots.clear()

                slots.forEach { slot ->
                    if (slot.type == "departure") {
                        departureSlots.add(slot)
                        Log.d("MAKEBOOKING", "Added departure: ${slot.time} | quota: ${slot.quota} | booked: ${slot.bookedCount} | available: ${slot.availableSeats()}")
                    } else if (slot.type == "return") {
                        returnSlots.add(slot)
                        Log.d("MAKEBOOKING", "Added return: ${slot.time} | quota: ${slot.quota} | booked: ${slot.bookedCount} | available: ${slot.availableSeats()}")
                    }
                }

                departureSlots.sortBy { it.time }
                returnSlots.sortBy { it.time }

                Log.d("MAKEBOOKING", "=========================================")
                Log.d("MAKEBOOKING", "Total departure slots: ${departureSlots.size}")
                Log.d("MAKEBOOKING", "Total return slots: ${returnSlots.size}")
                Log.d("MAKEBOOKING", "=========================================")

                displayDepartureSlots()
                displayReturnSlots()
            } else {
                tvTimeSlotsError.visibility = View.VISIBLE
                tvTimeSlotsError.text = error ?: "Failed to load time slots"
                Log.e("MAKEBOOKING", "Error loading slots: $error")
            }
        }
    }

    private fun displayDepartureSlots() {
        layoutTimeSlots.removeAllViews()

        Log.d("MAKEBOOKING", "=== DISPLAY DEPARTURE SLOTS ===")
        Log.d("MAKEBOOKING", "Departure slots in list: ${departureSlots.size}")

        departureSlots.forEach { slot ->
            val inFuture = isSlotInFuture(slot.time)
            Log.d("MAKEBOOKING", "  Slot ${slot.time}: available=${slot.isAvailable}, full=${slot.isFull()}, inFuture=$inFuture")
        }

        val availableSlots = departureSlots.filter { slot ->
            slot.isAvailable && !slot.isFull()
        }

        Log.d("MAKEBOOKING", "Available departure slots after filter: ${availableSlots.size}")

        if (availableSlots.isEmpty()) {
            tvTimeSlotsError.visibility = View.VISIBLE
            tvTimeSlotsError.text = "No available departure time slots"
            Log.d("MAKEBOOKING", "No available departure slots")
            return
        }

        tvTimeSlotsError.visibility = View.GONE

        for (slot in availableSlots) {
            addDepartureTimeSlotButton(slot)
        }
    }

    private fun displayReturnSlots() {
        layoutReturnTimeSlotsContainer.removeAllViews()

        Log.d("MAKEBOOKING", "=== DISPLAY RETURN SLOTS ===")
        Log.d("MAKEBOOKING", "Return slots in list: ${returnSlots.size}")

        returnSlots.forEach { slot ->
            val inFuture = isSlotInFuture(slot.time)
            Log.d("MAKEBOOKING", "  Slot ${slot.time}: available=${slot.isAvailable}, full=${slot.isFull()}, inFuture=$inFuture")
        }

        val availableSlots = returnSlots.filter { slot ->
            slot.isAvailable && !slot.isFull() && isSlotInFuture(slot.time)
        }

        Log.d("MAKEBOOKING", "Available return slots after filter: ${availableSlots.size}")

        if (availableSlots.isEmpty()) {
            tvReturnTimeSlotsError.visibility = View.VISIBLE
            tvReturnTimeSlotsError.text = "No available return time slots"
            Log.d("MAKEBOOKING", "No available return slots")
            return
        }

        tvReturnTimeSlotsError.visibility = View.GONE

        for (slot in availableSlots) {
            addReturnTimeSlotButton(slot)
        }
    }

    private fun isSlotInFuture(slotTime: String): Boolean {
        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val currentCal = Calendar.getInstance()

        if (selectedCal.get(Calendar.YEAR) > currentCal.get(Calendar.YEAR) ||
            selectedCal.get(Calendar.DAY_OF_YEAR) > currentCal.get(Calendar.DAY_OF_YEAR)) {
            return true
        }

        val slotHour = slotTime.split(":")[0].toInt()
        val slotMinute = slotTime.split(":")[1].toInt()
        val currentHour = currentCal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentCal.get(Calendar.MINUTE)

        return slotHour > currentHour || (slotHour == currentHour && slotMinute >= currentMinute)
    }

    private fun addDepartureTimeSlotButton(slot: EventTimeSlot) {
        val timeString = formatTimeForDisplay(slot.time)
        val isFull = slot.isFull()

        val button = MaterialButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
            // HANYA MASA - tanpa "seats available"
            text = if (isFull) "$timeString (FULL)" else timeString
            isEnabled = !isFull
            setBackgroundColor(ContextCompat.getColor(context, if (isFull) R.color.gray_400 else R.color.uptm_blue))
            setTextColor(ContextCompat.getColor(context, R.color.white))
            cornerRadius = 24
            setOnClickListener {
                for (i in 0 until layoutTimeSlots.childCount) {
                    (layoutTimeSlots.getChildAt(i) as? MaterialButton)?.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.uptm_blue)
                    )
                }
                setBackgroundColor(ContextCompat.getColor(context, R.color.green_available))
                selectedDepartureSlot = slot
                selectedTimeSlot = slot.time
                selectedTime = slot.time
                calculatePrice()
                Log.d("MAKEBOOKING", "Selected departure: ${slot.time}")
            }
        }
        layoutTimeSlots.addView(button)
    }

    private fun addReturnTimeSlotButton(slot: EventTimeSlot) {
        val timeString = formatTimeForDisplay(slot.time)
        val isFull = slot.isFull()

        val button = MaterialButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
            // HANYA MASA - tanpa "seats available"
            text = if (isFull) "$timeString (FULL)" else timeString
            isEnabled = !isFull
            setBackgroundColor(ContextCompat.getColor(context, if (isFull) R.color.gray_400 else R.color.uptm_blue))
            setTextColor(ContextCompat.getColor(context, R.color.white))
            cornerRadius = 24
            setOnClickListener {
                for (i in 0 until layoutReturnTimeSlotsContainer.childCount) {
                    (layoutReturnTimeSlotsContainer.getChildAt(i) as? MaterialButton)?.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.uptm_blue)
                    )
                }
                setBackgroundColor(ContextCompat.getColor(context, R.color.green_available))
                selectedReturnSlot = slot
                selectedReturnTimeSlot = slot.time
                calculatePrice()
                Log.d("MAKEBOOKING", "Selected return: ${slot.time}")
            }
        }
        layoutReturnTimeSlotsContainer.addView(button)
    }

    private fun formatTimeForDisplay(time: String): String {
        if (time.isEmpty()) return ""
        val hour = time.split(":")[0].toInt()
        val minute = time.split(":")[1].toInt()
        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        return String.format("%d:%02d %s", displayHour, minute, ampm)
    }

    // ==================== LISTENERS ====================

    private fun setupListeners() {
        rgTripType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbFixed -> {
                    layoutFixed.visibility = View.VISIBLE
                    layoutEvent.visibility = View.GONE
                    layoutReturnDateTime.visibility = View.VISIBLE
                    layoutReturnTimeSlots.visibility = View.GONE
                    btnSelectTime.visibility = View.VISIBLE
                    btnSelectDate.visibility = View.VISIBLE
                    btnSelectReturnDate.visibility = View.VISIBLE
                    btnSelectReturnTime.visibility = View.VISIBLE
                }
                R.id.rbEvent -> {
                    layoutFixed.visibility = View.GONE
                    layoutEvent.visibility = View.VISIBLE
                    layoutReturnDateTime.visibility = View.GONE
                    layoutReturnTimeSlots.visibility = if (returnSlots.isNotEmpty()) View.VISIBLE else View.GONE
                    btnSelectTime.visibility = View.GONE
                    btnSelectDate.visibility = View.GONE
                    btnSelectReturnDate.visibility = View.GONE
                    btnSelectReturnTime.visibility = View.GONE
                }
            }
            calculatePrice()
        }

        rgFixedDestination.setOnCheckedChangeListener { _, _ -> calculatePrice() }

        btnSelectDate.setOnClickListener { showDatePicker() }
        btnSelectTime.setOnClickListener { showTimePicker() }
        btnSelectReturnDate.setOnClickListener { showReturnDatePicker() }
        btnSelectReturnTime.setOnClickListener { showReturnTimePicker() }

        btnSelectSeats.setOnClickListener {
            selectedVehicle?.let {
                val intent = Intent(this, SeatMappingActivity::class.java).apply {
                    putExtra("vehicle", it)
                    putExtra("mode", "BOOKING")
                }
                seatMappingLauncher.launch(intent)
            } ?: Toast.makeText(this, "Select vehicle first", Toast.LENGTH_SHORT).show()
        }

        btnBookNow.setOnClickListener { submitBooking() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate
        DatePickerDialog(this,
            { _, year, month, day ->
                val cal = Calendar.getInstance().apply { set(year, month, day) }
                selectedDate = cal.timeInMillis
                btnSelectDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
                calculatePrice()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this,
            { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                btnSelectTime.text = selectedTime
                calculatePrice()
            },
            12, 0, true
        ).show()
    }

    private fun showReturnDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedReturnDate
        DatePickerDialog(this,
            { _, year, month, day ->
                val cal = Calendar.getInstance().apply { set(year, month, day) }
                selectedReturnDate = cal.timeInMillis
                btnSelectReturnDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
                calculatePrice()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showReturnTimePicker() {
        TimePickerDialog(this,
            { _, hour, minute ->
                selectedReturnTime = String.format("%02d:%02d", hour, minute)
                btnSelectReturnTime.text = selectedReturnTime
                calculatePrice()
            },
            12, 0, true
        ).show()
    }

    private fun calculatePrice() {
        if (selectedVehicle == null) return

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        tvDepartureInfo.text = "${dateFormat.format(Date(selectedDate))} $selectedTime"

        if (selectedSeats.isNotEmpty()) {
            tvSeatsInfo.text = selectedSeats.joinToString(", ")
            layoutSeatsInfo.visibility = View.VISIBLE
        } else {
            layoutSeatsInfo.visibility = View.GONE
        }

        val distance = if (rbEvent.isChecked) {
            5.0
        } else {
            when (rgFixedDestination.checkedRadioButtonId) {
                R.id.rbUPTMtoSiswa, R.id.rbSiswatoUPTM -> 6.4
                R.id.rbUPTMtoSiswi, R.id.rbSiswitoUPTM -> 1.5
                else -> 1.5
            }
        }

        tvDistance.text = String.format("%.1f km", distance)
        val basePrice = distance * selectedVehicle!!.pricePerKm
        tvOneWayPrice.text = String.format("RM %.2f", basePrice)
        var total = basePrice

        // Return trip logic
        if (rbEvent.isChecked) {
            // Event Trip - return only if user selected a return slot
            if (selectedReturnSlot != null && selectedReturnTimeSlot != null) {
                layoutReturnSection.visibility = View.VISIBLE
                tvReturnInfo.text = "${dateFormat.format(Date(selectedDate))} $selectedReturnTimeSlot"
                val returnPrice = basePrice * 0.8
                tvReturnPrice.text = String.format("RM %.2f", returnPrice)
                total += returnPrice
            } else {
                layoutReturnSection.visibility = View.GONE
            }
        } else if (rbFixed.isChecked) {
            // Fixed Trip - return based on return date/time pickers
            if (selectedReturnDate != 0L && selectedReturnTime.isNotEmpty()) {
                layoutReturnSection.visibility = View.VISIBLE
                tvReturnInfo.text = "${dateFormat.format(Date(selectedReturnDate))} $selectedReturnTime"
                val returnPrice = basePrice * 0.8
                tvReturnPrice.text = String.format("RM %.2f", returnPrice)
                total += returnPrice
            } else {
                layoutReturnSection.visibility = View.GONE
            }
        } else {
            layoutReturnSection.visibility = View.GONE
        }

        tvTotalPrice.text = String.format("RM %.2f", total)
        layoutPrice.visibility = View.VISIBLE
    }

    private fun submitBooking() {
        if (selectedVehicle == null) {
            Toast.makeText(this, "Please select a vehicle", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedVehicle?.type?.lowercase() == "bus" && selectedSeats.isEmpty()) {
            Toast.makeText(this, "Please select at least one seat", Toast.LENGTH_SHORT).show()
            return
        }

        if (rbEvent.isChecked) {
            if (selectedDepartureSlot == null) {
                Toast.makeText(this, "Please select a departure time", Toast.LENGTH_SHORT).show()
                return
            }
            if (!selectedDepartureSlot!!.hasAvailableSeats(selectedSeats.size)) {
                Toast.makeText(this, "Only ${selectedDepartureSlot!!.availableSeats()} seats available for this time slot", Toast.LENGTH_SHORT).show()
                return
            }
            // Return trip validation - only if user selected a return slot
            if (selectedReturnSlot != null) {
                if (!selectedReturnSlot!!.hasAvailableSeats(selectedSeats.size)) {
                    Toast.makeText(this, "Only ${selectedReturnSlot!!.availableSeats()} seats available for return time slot", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        } else if (rbFixed.isChecked) {
            if (selectedReturnDate == 0L || selectedReturnTime.isEmpty()) {
                Toast.makeText(this, "Please select return date and time", Toast.LENGTH_SHORT).show()
                return
            }
        }

        showConfirmationDialog()
    }

    private fun showConfirmationDialog() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val message = buildString {
            append("Vehicle: ${selectedVehicle?.brand}\n")
            append("Departure: ${dateFormat.format(Date(selectedDate))} $selectedTime\n")
            if (rbFixed.isChecked) {
                append("Return: ${dateFormat.format(Date(selectedReturnDate))} $selectedReturnTime\n")
            } else if (rbEvent.isChecked && selectedReturnTimeSlot != null) {
                append("Return: ${dateFormat.format(Date(selectedDate))} $selectedReturnTimeSlot\n")
            }
            append("Seats: ${if (selectedSeats.isEmpty()) "N/A" else selectedSeats.joinToString()}\n")
            if (rbEvent.isChecked && currentEvent != null) {
                append("Event: ${currentEvent?.title}\n")
            }
            append("Total: ${tvTotalPrice.text}\n\nProceed with booking?")
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Booking")
            .setMessage(message)
            .setPositiveButton("YES") { _, _ -> processBooking() }
            .setNegativeButton("NO", null)
            .show()
    }

    private fun processBooking() {
        loadingDialog.show()

        val departureCalendar = Calendar.getInstance()
        departureCalendar.timeInMillis = selectedDate
        val timeParts = selectedTime.split(":")
        if (timeParts.size == 2) {
            departureCalendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            departureCalendar.set(Calendar.MINUTE, timeParts[1].toInt())
        }

        var returnDate: Date? = null

        // For Fixed Trip
        if (rbFixed.isChecked) {
            if (selectedReturnDate != 0L && selectedReturnTime.isNotEmpty()) {
                val returnCalendar = Calendar.getInstance()
                returnCalendar.timeInMillis = selectedReturnDate
                val returnTimeParts = selectedReturnTime.split(":")
                if (returnTimeParts.size == 2) {
                    returnCalendar.set(Calendar.HOUR_OF_DAY, returnTimeParts[0].toInt())
                    returnCalendar.set(Calendar.MINUTE, returnTimeParts[1].toInt())
                    returnDate = returnCalendar.time
                }
            }
        }
        // For Event Trip - only if user selected a return slot
        else if (rbEvent.isChecked && selectedReturnSlot != null && selectedReturnTimeSlot != null) {
            val returnCalendar = Calendar.getInstance()
            returnCalendar.timeInMillis = selectedDate
            val returnParts = selectedReturnTimeSlot!!.split(":")
            if (returnParts.size == 2) {
                returnCalendar.set(Calendar.HOUR_OF_DAY, returnParts[0].toInt())
                returnCalendar.set(Calendar.MINUTE, returnParts[1].toInt())
                returnDate = returnCalendar.time
            }
        }

        val fromLocation = if (rbEvent.isChecked) {
            currentEvent?.location ?: "Event Location"
        } else {
            when (rgFixedDestination.checkedRadioButtonId) {
                R.id.rbUPTMtoSiswa, R.id.rbUPTMtoSiswi -> "UPTM"
                R.id.rbSiswatoUPTM -> "Siswa"
                R.id.rbSiswitoUPTM -> "Siswi"
                else -> "UPTM"
            }
        }

        val toLocation = if (rbEvent.isChecked) {
            "Event: ${currentEvent?.title ?: "Event"}"
        } else {
            when (rgFixedDestination.checkedRadioButtonId) {
                R.id.rbUPTMtoSiswa -> "Siswa"
                R.id.rbUPTMtoSiswi -> "Siswi"
                R.id.rbSiswatoUPTM, R.id.rbSiswitoUPTM -> "UPTM"
                else -> "Siswi"
            }
        }

        val booking = Booking(
            id = System.currentTimeMillis().toString(),
            userId = currentUserId,
            userName = currentUserName,
            userGender = currentUserGender,
            vehicleId = selectedVehicle!!.id,
            vehicleName = "${selectedVehicle!!.brand} ${selectedVehicle!!.plateNumber}",
            vehicleType = selectedVehicle!!.type,
            fromLocation = fromLocation,
            toLocation = toLocation,
            distance = tvDistance.text.toString().replace(" km", "").toDoubleOrNull() ?: 0.0,
            price = tvTotalPrice.text.toString().replace("RM ", "").toDoubleOrNull() ?: 0.0,
            bookingDate = Date(),
            tripDate = departureCalendar.time,
            returnDate = returnDate,
            status = "pending",
            selectedSeats = selectedSeats
        )

        FirebaseHelper.createBooking(booking) { success, error ->
            loadingDialog.dismiss()
            if (success) {
                if (rbEvent.isChecked && selectedDepartureSlot != null && selectedSeats.isNotEmpty()) {
                    updateTimeSlotWithSeats(selectedDepartureSlot!!, selectedSeats)
                }
                if (rbEvent.isChecked && selectedReturnSlot != null && selectedSeats.isNotEmpty()) {
                    updateTimeSlotWithSeats(selectedReturnSlot!!, selectedSeats)
                }
                updateSeatStatusToPending(selectedVehicle!!.id, selectedSeats, currentUserGender)
                AlertDialog.Builder(this)
                    .setTitle("Success")
                    .setMessage("Booking submitted successfully!")
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .show()
            } else {
                Toast.makeText(this, error ?: "Booking failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateTimeSlotWithSeats(slot: EventTimeSlot, seatNumbers: List<String>) {
        val updatedSlot = slot.addSeats(seatNumbers)
        FirebaseHelper.updateEventTimeSlot(updatedSlot) { success, error ->
            if (!success) {
                Log.e("MakeBooking", "Failed to update time slot: $error")
            }
        }
    }

    private fun updateSeatStatusToPending(vehicleId: String, seatNumbers: List<String>, gender: String) {
        if (seatNumbers.isEmpty()) return
        FirebaseHelper.getVehicleById(vehicleId) { vehicle, success, _ ->
            if (success && vehicle != null) {
                val updatedSeats = vehicle.seatLayout.map { seat ->
                    if (seatNumbers.contains(seat.seatNumber)) {
                        seat.copy(status = "pending", gender = gender)
                    } else {
                        seat
                    }
                }
                FirebaseHelper.updateVehicle(vehicle.copy(seatLayout = updatedSeats)) { _, _ -> }
            }
        }
    }
}