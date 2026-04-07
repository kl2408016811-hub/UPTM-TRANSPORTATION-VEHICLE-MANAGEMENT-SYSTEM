package com.example.uptmtransportation.student

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Seat
import com.example.uptmtransportation.models.Vehicle
import com.example.uptmtransportation.utils.GridSpacingItemDecoration
import com.example.uptmtransportation.utils.LoadingDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class SeatMappingActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvSeats: RecyclerView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var tvVehicleInfo: TextView
    private lateinit var loadingDialog: LoadingDialog

    private var vehicle: Vehicle? = null
    private var selectedSeats = mutableListOf<Seat>()
    private var seatAdapter: SeatAdapter? = null
    private var mode: String = "BOOKING"  // Default BOOKING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_mapping)

        loadingDialog = LoadingDialog(this)
        initViews()
        setupToolbar()

        vehicle = intent.getSerializableExtra("vehicle") as? Vehicle
        mode = intent.getStringExtra("mode") ?: "BOOKING"

        if (vehicle == null) {
            finish()
            return
        }

        displayVehicleInfo()
        setupSeatRecyclerView()
        setupConfirmButton()

        // Set UI based on mode
        if (mode == "VIEW") {
            toolbar.title = "View Seats"
            btnConfirm.visibility = View.GONE  // HIDE confirm button for VIEW mode
        } else {
            toolbar.title = "Select Seats"
            btnConfirm.visibility = View.VISIBLE  // SHOW confirm button for BOOKING mode
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnSelectedSeats()
                finish()
            }
        })
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvSeats = findViewById(R.id.rvSeats)
        btnConfirm = findViewById(R.id.btnConfirm)
        tvVehicleInfo = findViewById(R.id.tvVehicleInfo)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            returnSelectedSeats()
            finish()
        }
    }

    private fun displayVehicleInfo() {
        vehicle?.let {
            tvVehicleInfo.text = "${it.brand} ${it.plateNumber} • ${it.seatCapacity} seats"
        }
    }

    private fun setupSeatRecyclerView() {
        val seats = vehicle?.seatLayout ?: emptyList()

        val columns = if (seats.size >= 40) 6 else 4
        val layoutManager = GridLayoutManager(this, columns)
        rvSeats.layoutManager = layoutManager

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.seat_spacing)
        rvSeats.addItemDecoration(GridSpacingItemDecoration(columns, spacingInPixels, true))

        seatAdapter = SeatAdapter(seats, mode) { seat ->
            handleSeatClick(seat)
        }
        rvSeats.adapter = seatAdapter
    }

    private fun handleSeatClick(seat: Seat) {
        if (mode == "VIEW") {
            // VIEW mode: just show details
            showSeatDetailsDialog(seat)
        } else {
            // BOOKING mode: allow selection if available
            if (seat.status == "available") {
                toggleSeatSelection(seat)
            } else {
                showSeatDetailsDialog(seat)
            }
        }
    }

    private fun showSeatDetailsDialog(seat: Seat) {
        val statusText = seat.status.replaceFirstChar { it.uppercase() }
        val genderText = if (seat.gender.isNotEmpty()) {
            seat.gender.replaceFirstChar { it.uppercase() }
        } else {
            "Not specified"
        }

        AlertDialog.Builder(this)
            .setTitle("Seat ${seat.seatNumber} Details")
            .setMessage("Status: $statusText\nGender: $genderText")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleSeatSelection(seat: Seat) {
        if (selectedSeats.any { it.seatNumber == seat.seatNumber }) {
            selectedSeats.removeAll { it.seatNumber == seat.seatNumber }
            Toast.makeText(this, "Seat ${seat.seatNumber} deselected", Toast.LENGTH_SHORT).show()
        } else {
            selectedSeats.add(seat)
            Toast.makeText(this, "Seat ${seat.seatNumber} selected", Toast.LENGTH_SHORT).show()
        }

        seatAdapter?.updateSelection(selectedSeats)
        updateConfirmButton()
    }

    private fun updateConfirmButton() {
        btnConfirm.text = if (selectedSeats.isEmpty()) {
            "Select Seats"
        } else {
            "Confirm ${selectedSeats.size} Seat${if (selectedSeats.size > 1) "s" else ""}"
        }
    }

    private fun returnSelectedSeats() {
        if (mode == "BOOKING" && selectedSeats.isNotEmpty()) {
            val selectedSeatNumbers = ArrayList(selectedSeats.map { it.seatNumber })
            val resultIntent = Intent().apply {
                putStringArrayListExtra("selectedSeats", selectedSeatNumbers)
            }
            setResult(RESULT_OK, resultIntent)
        }
    }

    private fun setupConfirmButton() {
        btnConfirm.setOnClickListener {
            if (mode == "BOOKING") {
                if (selectedSeats.isEmpty()) {
                    Toast.makeText(this, "Please select at least one seat", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                returnSelectedSeats()
                finish()
            }
        }
    }

    inner class SeatAdapter(
        private val seats: List<Seat>,
        private val mode: String,
        private val onSeatClick: (Seat) -> Unit
    ) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

        private val selectedSeatNumbers = mutableSetOf<String>()

        fun updateSelection(selected: List<Seat>) {
            selectedSeatNumbers.clear()
            selectedSeatNumbers.addAll(selected.map { it.seatNumber })
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = seats.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_seat, parent, false)
            return SeatViewHolder(view)
        }

        override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
            holder.bind(seats[position])
        }

        inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvSeatNumber: TextView = itemView.findViewById(R.id.tvSeatNumber)
            private val viewSeat: View = itemView

            fun bind(seat: Seat) {
                tvSeatNumber.text = seat.seatNumber

                val isSelected = selectedSeatNumbers.contains(seat.seatNumber)

                when {
                    isSelected -> {
                        viewSeat.setBackgroundResource(R.drawable.seat_selected)
                        tvSeatNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                    }
                    seat.status == "available" -> {
                        viewSeat.setBackgroundResource(R.drawable.seat_available)
                        tvSeatNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.green_available))
                    }
                    seat.status == "pending" -> {
                        viewSeat.setBackgroundResource(R.drawable.seat_pending)
                        tvSeatNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.yellow_pending))
                    }
                    else -> {
                        viewSeat.setBackgroundResource(R.drawable.seat_booked)
                        tvSeatNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.red_booked))
                    }
                }

                viewSeat.setOnClickListener {
                    onSeatClick(seat)
                }
            }
        }
    }
}