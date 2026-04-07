package com.example.uptmtransportation.student

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var calendarView: CalendarView
    private lateinit var btnSelectDate: TextView
    private lateinit var btnShowAll: MaterialButton
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvEventCount: TextView
    private lateinit var rvEvents: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutLoading: LinearLayout
    private lateinit var adapter: EventAdapter

    private val eventList = mutableListOf<Event>()
    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_student)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, R.color.uptm_blue)
        }

        initViews()
        setupToolbar()
        setupListeners()
        loadEvents()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        calendarView = findViewById(R.id.calendarView)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnShowAll = findViewById(R.id.btnShowAll)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvEventCount = findViewById(R.id.tvEventCount)
        rvEvents = findViewById(R.id.rvEvents)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        layoutLoading = findViewById(R.id.layoutLoading)

        btnShowAll.backgroundTintList = ContextCompat.getColorStateList(this, R.color.uptm_blue)

        // Setup RecyclerView for scrolling
        rvEvents.layoutManager = LinearLayoutManager(this)
        rvEvents.isNestedScrollingEnabled = true
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "My Calendar"
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        calendarView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener {
            override fun onClick(calendarDay: CalendarDay) {
                val clickedDayCalendar = calendarDay.calendar
                selectedDate = clickedDayCalendar.timeInMillis
                updateSelectedDateText(selectedDate)
                showEventsOnDate(selectedDate)
            }
        })

        btnSelectDate.setOnClickListener {
            Toast.makeText(this, "Please select a date from the calendar", Toast.LENGTH_SHORT).show()
        }

        btnShowAll.setOnClickListener {
            showAllEvents()
        }
    }

    private fun loadEvents() {
        showLoading()

        FirebaseHelper.getAllEvents { events, success, error ->
            hideLoading()

            if (success) {
                eventList.clear()

                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                eventList.addAll(events.filter { it.date != null && it.date.time >= today })
                eventList.sortBy { it.date }

                setupRecyclerView()
                updateCalendarIcons()
                updateSelectedDateText(selectedDate)
                showEventsOnDate(selectedDate)
            } else {
                Toast.makeText(this, error ?: "Failed to load events", Toast.LENGTH_SHORT).show()
            }
        }
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
            onItemClick = { clickedEvent: Event ->
                val intent = Intent(this, EventDetailActivity::class.java)
                intent.putExtra("event", clickedEvent)
                startActivity(intent)
            },
            isAdmin = false
        )
        rvEvents.adapter = adapter
    }

    private fun updateSelectedDateText(dateInMillis: Long) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        tvSelectedDate.text = "Selected Date: ${dateFormat.format(Date(dateInMillis))}"
    }

    private fun showEventsOnDate(dateInMillis: Long) {
        val eventsOnDate = eventList.filter { it.date != null && isSameDay(it.date.time, dateInMillis) }
        adapter.updateList(eventsOnDate)

        tvEventCount.text = "(${eventsOnDate.size})"

        if (eventsOnDate.isEmpty()) {
            rvEvents.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvEvents.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun showAllEvents() {
        adapter.updateList(eventList)
        tvSelectedDate.text = "Showing all events"
        tvEventCount.text = "(${eventList.size})"

        if (eventList.isEmpty()) {
            rvEvents.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvEvents.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
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