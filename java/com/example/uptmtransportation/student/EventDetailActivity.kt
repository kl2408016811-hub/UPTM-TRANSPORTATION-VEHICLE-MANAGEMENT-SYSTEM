package com.example.uptmtransportation.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Event
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class EventDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvEventTitle: TextView
    private lateinit var tvEventDate: TextView
    private lateinit var tvEventTime: TextView
    private lateinit var tvEventLocation: TextView
    private lateinit var tvEventDescription: TextView
    private lateinit var cardDescription: CardView
    private lateinit var btnBookNow: MaterialButton   // TAMBAH INI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        initViews()
        setupToolbar()

        val event = intent.getSerializableExtra("event") as? Event
        event?.let { displayEvent(it) }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvEventTitle = findViewById(R.id.tvEventTitle)
        tvEventDate = findViewById(R.id.tvEventDate)
        tvEventTime = findViewById(R.id.tvEventTime)
        tvEventLocation = findViewById(R.id.tvEventLocation)
        tvEventDescription = findViewById(R.id.tvEventDescription)
        cardDescription = findViewById(R.id.cardDescription)
        btnBookNow = findViewById(R.id.btnBookNow)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun displayEvent(event: Event) {
        tvEventTitle.text = event.title

        event.date?.let { date ->
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            tvEventDate.text = dateFormat.format(date)
        }

        event.time?.let { time ->
            tvEventTime.text = time
        }

        tvEventLocation.text = event.location

        val description = event.description
        if (description.isNullOrEmpty()) {
            cardDescription.visibility = View.GONE
        } else {
            tvEventDescription.text = description
            cardDescription.visibility = View.VISIBLE
        }

        // Setup Book Now button
        btnBookNow.setOnClickListener {
            val intent = Intent(this, MakeBookingActivity::class.java)
            intent.putExtra("event", event)
            startActivity(intent)
        }
    }
}