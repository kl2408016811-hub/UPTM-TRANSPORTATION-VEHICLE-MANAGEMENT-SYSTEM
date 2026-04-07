package com.example.uptmtransportation.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Event
import com.example.uptmtransportation.models.EventTimeSlot
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class AddEventActivity : AppCompatActivity() {

    // Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etTitle: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var btnSelectEndTime: Button
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnCreateEvent: MaterialButton
    private lateinit var loadingDialog: LoadingDialog

    // Time Slot Settings Views
    private lateinit var etDepartureHoursBefore: EditText
    private lateinit var etReturnHoursAfter: EditText
    private lateinit var etSlotInterval: EditText
    private lateinit var etDepartureQuota: EditText
    private lateinit var etReturnQuota: EditText

    // Data
    private var selectedDate: Date? = null
    private var selectedTime: String = ""
    private var selectedEndTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.uptm_blue)
        }

        loadingDialog = LoadingDialog(this)
        initViews()
        setupToolbar()
        setupListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etTitle = findViewById(R.id.etTitle)
        etLocation = findViewById(R.id.etLocation)
        etDescription = findViewById(R.id.etDescription)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        btnSelectEndTime = findViewById(R.id.btnSelectEndTime)
        btnCancel = findViewById(R.id.btnCancel)
        btnCreateEvent = findViewById(R.id.btnCreateEvent)

        // Time Slot Settings
        etDepartureHoursBefore = findViewById(R.id.etDepartureHoursBefore)
        etReturnHoursAfter = findViewById(R.id.etReturnHoursAfter)
        etSlotInterval = findViewById(R.id.etSlotInterval)
        etDepartureQuota = findViewById(R.id.etDepartureQuota)
        etReturnQuota = findViewById(R.id.etReturnQuota)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        btnSelectDate.setOnClickListener { showDatePicker() }
        btnSelectTime.setOnClickListener { showTimePicker(true) }
        btnSelectEndTime.setOnClickListener { showTimePicker(false) }
        btnCancel.setOnClickListener { finish() }
        btnCreateEvent.setOnClickListener { addEvent() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this,
            { _, year, month, day ->
                val cal = Calendar.getInstance().apply { set(year, month, day) }
                selectedDate = cal.time
                btnSelectDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this,
            { _, selectedHour, selectedMinute ->
                val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                if (isStartTime) {
                    selectedTime = timeString
                    btnSelectTime.text = timeString
                } else {
                    selectedEndTime = timeString
                    btnSelectEndTime.text = timeString
                }
            },
            hour, minute, true
        ).show()
    }

    private fun addEvent() {
        val title = etTitle.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // Get time slot settings
        val departureHoursBefore = etDepartureHoursBefore.text.toString().toIntOrNull() ?: 3
        val returnHoursAfter = etReturnHoursAfter.text.toString().toIntOrNull() ?: 3
        val slotInterval = etSlotInterval.text.toString().toIntOrNull() ?: 60
        val departureQuota = etDepartureQuota.text.toString().toIntOrNull() ?: 34
        val returnQuota = etReturnQuota.text.toString().toIntOrNull() ?: 34

        // Validation
        if (title.isEmpty()) {
            etTitle.error = "Please enter event title"
            return
        }
        if (location.isEmpty()) {
            etLocation.error = "Please enter event location"
            return
        }
        if (selectedDate == null) {
            Toast.makeText(this, "Please select event date", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select event start time", Toast.LENGTH_SHORT).show()
            return
        }
        // ✅ WAJIBKAN END TIME
        if (selectedEndTime.isEmpty()) {
            Toast.makeText(this, "Please select event end time", Toast.LENGTH_SHORT).show()
            return
        }

        loadingDialog.show()

        val eventId = System.currentTimeMillis().toString()
        val event = Event(
            id = eventId,
            title = title,
            date = selectedDate,
            time = selectedTime,
            endTime = selectedEndTime,
            location = location,
            description = description,
            createdBy = FirebaseHelper.getCurrentUserId() ?: "",
            createdAt = Date(),
            departureHoursBefore = departureHoursBefore,
            returnHoursAfter = returnHoursAfter,
            slotIntervalMinutes = slotInterval,
            departureQuota = departureQuota,
            returnQuota = returnQuota
        )

        FirebaseHelper.addEvent(event) { success, error ->
            if (success) {
                generateTimeSlots(event)
            } else {
                loadingDialog.dismiss()
                Toast.makeText(this, error ?: "Failed to create event", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateTimeSlots(event: Event) {
        val slots = mutableListOf<EventTimeSlot>()

        // Generate departure time slots
        val departureSlots = generateDepartureTimeSlots(event)
        slots.addAll(departureSlots)

        // Generate return time slots
        val returnSlots = generateReturnTimeSlots(event)
        slots.addAll(returnSlots)

        if (slots.isEmpty()) {
            loadingDialog.dismiss()
            Toast.makeText(this, "Event created but no time slots generated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Save all time slots to Firebase
        var savedCount = 0
        slots.forEach { slot ->
            FirebaseHelper.addEventTimeSlot(slot) { success, _ ->
                if (success) {
                    savedCount++
                    if (savedCount == slots.size) {
                        loadingDialog.dismiss()
                        AlertDialog.Builder(this)
                            .setTitle("Success")
                            .setMessage("Event created with ${slots.size} time slots!\n\nDeparture: ${departureSlots.size} slots\nReturn: ${returnSlots.size} slots")
                            .setPositiveButton("OK") { _, _ -> finish() }
                            .show()
                    }
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Failed to create some time slots", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun generateDepartureTimeSlots(event: Event): List<EventTimeSlot> {
        val slots = mutableListOf<EventTimeSlot>()

        val startHour = event.time.split(":")[0].toInt()
        val startMinute = event.time.split(":")[1].toInt()

        var departureHour = startHour - event.departureHoursBefore
        var departureMinute = startMinute

        if (departureHour < 0) {
            departureHour = 0
        }

        var currentHour = departureHour
        var currentMinute = departureMinute

        while (currentHour < startHour || (currentHour == startHour && currentMinute < startMinute)) {
            val timeString = String.format("%02d:%02d", currentHour, currentMinute)

            val slot = EventTimeSlot(
                id = "${event.id}_dep_${timeString.replace(":", "")}",
                eventId = event.id,
                type = "departure",
                time = timeString,
                quota = event.departureQuota,
                bookedSeats = listOf(),
                bookedCount = 0,
                isAvailable = true,
                createdAt = Date()
            )
            slots.add(slot)

            currentMinute += event.slotIntervalMinutes
            if (currentMinute >= 60) {
                currentHour += currentMinute / 60
                currentMinute %= 60
            }
        }

        return slots
    }

    private fun generateReturnTimeSlots(event: Event): List<EventTimeSlot> {
        val slots = mutableListOf<EventTimeSlot>()

        val endHour = event.endTime.split(":")[0].toInt()
        val endMinute = event.endTime.split(":")[1].toInt()

        // START FROM EVENT END TIME
        var returnHour = endHour
        var returnMinute = endMinute

        val latestHour = endHour + event.returnHoursAfter
        val latestMinute = endMinute

        var currentHour = returnHour
        var currentMinute = returnMinute

        while (currentHour < latestHour || (currentHour == latestHour && currentMinute <= latestMinute)) {
            val timeString = String.format("%02d:%02d", currentHour, currentMinute)

            val slot = EventTimeSlot(
                id = "${event.id}_ret_${timeString.replace(":", "")}",
                eventId = event.id,
                type = "return",
                time = timeString,
                quota = event.returnQuota,
                bookedSeats = listOf(),
                bookedCount = 0,
                isAvailable = true,
                createdAt = Date()
            )
            slots.add(slot)

            currentMinute += event.slotIntervalMinutes
            if (currentMinute >= 60) {
                currentHour += currentMinute / 60
                currentMinute %= 60
            }
        }

        return slots
    }
}