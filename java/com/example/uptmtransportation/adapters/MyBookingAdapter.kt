package com.example.uptmtransportation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Booking
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class MyBookingAdapter(
    private var bookings: List<Booking>,
    private val onCancelClick: (Booking) -> Unit
) : RecyclerView.Adapter<MyBookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_student, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking)
    }

    override fun getItemCount() = bookings.size

    fun updateList(newList: List<Booking>) {
        bookings = newList
        notifyDataSetChanged()
    }

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivVehicleIcon: ImageView = itemView.findViewById(R.id.ivVehicleIcon)
        private val tvVehicleName: TextView = itemView.findViewById(R.id.tvVehicleName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvFromLocation: TextView = itemView.findViewById(R.id.tvFromLocation)
        private val tvToLocation: TextView = itemView.findViewById(R.id.tvToLocation)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvSeats: TextView = itemView.findViewById(R.id.tvSeats)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btnCancel)

        fun bind(booking: Booking) {
            tvVehicleName.text = booking.vehicleName
            tvFromLocation.text = booking.fromLocation
            tvToLocation.text = booking.toLocation

            val priceText = String.format("RM %.2f", booking.price)
            tvPrice.text = priceText

            val iconRes = when (booking.vehicleType.lowercase()) {
                "bus" -> R.drawable.ic_bus
                "car" -> R.drawable.ic_car
                else -> R.drawable.ic_bus
            }
            ivVehicleIcon.setImageResource(iconRes)

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            tvDate.text = dateFormat.format(booking.tripDate)
            tvTime.text = timeFormat.format(booking.tripDate)

            tvSeats.text = if (booking.selectedSeats.isNotEmpty()) {
                "Seats: ${booking.selectedSeats.joinToString()}"
            } else {
                "No seats selected"
            }

            // Status UI
            when (booking.status) {
                "pending" -> {
                    tvStatus.text = "PENDING"
                    tvStatus.setBackgroundColor(itemView.context.getColor(R.color.yellow_pending))
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.black))
                    btnCancel.visibility = View.VISIBLE
                }
                "approved" -> {
                    tvStatus.text = "APPROVED"
                    tvStatus.setBackgroundColor(itemView.context.getColor(R.color.green_available))
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                    btnCancel.visibility = View.GONE
                }
                "rejected" -> {
                    tvStatus.text = "REJECTED"
                    tvStatus.setBackgroundColor(itemView.context.getColor(R.color.red_booked))
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                    btnCancel.visibility = View.GONE
                }
                "cancelled" -> {
                    tvStatus.text = "CANCELLED"
                    tvStatus.setBackgroundColor(itemView.context.getColor(R.color.gray))
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.white))
                    btnCancel.visibility = View.GONE
                }
                else -> {
                    btnCancel.visibility = View.GONE
                }
            }

            btnCancel.setOnClickListener { onCancelClick(booking) }
        }
    }
}