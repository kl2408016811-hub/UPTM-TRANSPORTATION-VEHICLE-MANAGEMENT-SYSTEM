package com.example.uptmtransportation.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener
import com.example.uptmtransportation.R
import com.example.uptmtransportation.adapters.EventAdapter
import com.example.uptmtransportation.models.Event
import com.example.uptmtransportation.utils.FirebaseHelper
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class ManageCalendarActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var calendarView: CalendarView
    private lateinit var btnAddEvent: MaterialButton
    private lateinit var btnShowAll: MaterialButton
    private lateinit var rvEvents: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutLoading: LinearLayout
    private lateinit var tvEventCount: TextView
    private lateinit var loadingDialog: LoadingDialog

    private val eventList = mutableListOf<Event>()
    private var selectedDate: Long = System.currentTimeMillis()
    private lateinit var adapter: EventAdapter
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_calendar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.uptm_blue)
        }

        loadingDialog = LoadingDialog(this)
        initViews()
        setupToolbar()
        setupCalendar()
        setupRecyclerView()
        loadEvents()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        calendarView = findViewById(R.id.calendarView)
        btnAddEvent = findViewById(R.id.btnAddEvent)
        btnShowAll = findViewById(R.id.btnShowAll)
        rvEvents = findViewById(R.id.rvEvents)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        layoutLoading = findViewById(R.id.layoutLoading)
        tvEventCount = findViewById(R.id.tvEventCount)

        btnAddEvent.backgroundTintList = ContextCompat.getColorStateList(this, R.color.uptm_blue)
        btnShowAll.backgroundTintList = ContextCompat.getColorStateList(this, R.color.uptm_blue)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.title = "Manage Calendar"
    }

    private fun setupCalendar() {
        calendarView.setDate(Calendar.getInstance())
    }

    private fun updateCalendarIcons() {
        val calendarDays = mutableListOf<CalendarDay>()

        eventList.forEach { event ->
            event.date?.let { date ->
                val calendar = Calendar.getInstance()
                calendar.time = date
                val calendarDay = CalendarDay(calendar)
                calendarDay.imageResource = R.drawable.ic_event_indicator
                calendarDays.add(calendarDay)
            }
        }

        calendarView.setCalendarDays(calendarDays)
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(
            events = eventList,
            onItemClick = { event ->
                showEditEventDialog(event)
            },
            isAdmin = true,
            onEditClick = { event ->
                showEditEventDialog(event)
            },
            onDeleteClick = { event ->
                showDeleteConfirmation(event)
            }
        )
        rvEvents.layoutManager = LinearLayoutManager(this)
        rvEvents.adapter = adapter

        // Enable scrolling
        rvEvents.isNestedScrollingEnabled = true
    }

    private fun loadEvents() {
        showLoading()
        FirebaseHelper.getAllEvents { events, success, error ->
            hideLoading()
            if (success) {
                eventList.clear()
                eventList.addAll(events)
                eventList.sortBy { it.date }
                adapter.updateList(eventList)
                updateCalendarIcons()
                showEventsOnDate(selectedDate)

                if (eventList.isEmpty()) {
                    rvEvents.visibility = View.GONE
                    layoutEmpty.visibility = View.VISIBLE
                } else {
                    rvEvents.visibility = View.VISIBLE
                    layoutEmpty.visibility = View.GONE
                }
                tvEventCount.text = "(${eventList.size})"
            } else {
                Toast.makeText(this, error ?: "Failed to load events", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEventsOnDate(dateInMillis: Long) {
        val eventsOnDate = eventList.filter { it.date != null && isSameDay(it.date.time, dateInMillis) }
        adapter.updateList(eventsOnDate)

        if (eventsOnDate.isEmpty()) {
            if (rvEvents.visibility == View.VISIBLE) {
                rvEvents.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            }
        } else {
            rvEvents.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun showAllEvents() {
        adapter.updateList(eventList)
        if (eventList.isEmpty()) {
            rvEvents.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvEvents.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        calendarView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
            override fun onClick(calendarDay: CalendarDay) {
                val clickedDayCalendar = calendarDay.calendar
                selectedDate = clickedDayCalendar.timeInMillis
                showEventsOnDate(selectedDate)
            }
        })

        btnAddEvent.setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        btnShowAll.setOnClickListener {
            showAllEvents()
        }
    }

    private fun showEditEventDialog(event: Event) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_event, null)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etTitle)
        val etLocation = dialogView.findViewById<TextInputEditText>(R.id.etLocation)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.etDate)
        val etStartTime = dialogView.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = dialogView.findViewById<TextInputEditText>(R.id.etEndTime)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)

        etTitle.setText(event.title)
        etLocation.setText(event.location)
        event.date?.let { etDate.setText(dateFormat.format(it)) }
        etStartTime.setText(event.time)
        etEndTime.setText(event.endTime)
        etDescription.setText(event.description)

        var selectedDateLong = event.date?.time ?: System.currentTimeMillis()
        var selectedStartTime = event.time
        var selectedEndTime = event.endTime

        etDate.setOnClickListener {
            showDatePicker { dateInMillis ->
                selectedDateLong = dateInMillis
                etDate.setText(dateFormat.format(Date(dateInMillis)))
            }
        }

        etStartTime.setOnClickListener {
            showTimePicker { time ->
                selectedStartTime = time
                etStartTime.setText(time)
            }
        }

        etEndTime.setOnClickListener {
            showTimePicker { time ->
                selectedEndTime = time
                etEndTime.setText(time)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Event")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val title = etTitle.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(this, "Please enter event title", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (location.isEmpty()) {
                    Toast.makeText(this, "Please enter event location", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedEvent = event.copy(
                    title = title,
                    date = Date(selectedDateLong),
                    time = selectedStartTime,
                    endTime = selectedEndTime,
                    location = location,
                    description = description
                )

                loadingDialog.show()
                FirebaseHelper.updateEvent(updatedEvent) { success, error ->
                    loadingDialog.dismiss()
                    if (success) {
                        Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show()
                        loadEvents()
                    } else {
                        Toast.makeText(this, error ?: "Failed to update event", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(event: Event) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete \"${event.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent(event)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent(event: Event) {
        loadingDialog.show()
        FirebaseHelper.deleteEvent(event.id) { success, error ->
            loadingDialog.dismiss()
            if (success) {
                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show()
                loadEvents()
            } else {
                Toast.makeText(this, error ?: "Failed to delete event", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                onDateSelected(cal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val timeString = String.format("%02d:%02d", hour, minute)
                onTimeSelected(timeString)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun showLoading() {
        layoutLoading.visibility = View.VISIBLE
        rvEvents.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
    }

    private fun hideLoading() {
        layoutLoading.visibility = View.GONE
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.timeInMillis = date1
        cal2.timeInMillis = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}