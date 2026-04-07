package com.example.uptmtransportation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Booking
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private var bookings: List<Booking>,
    private val onItemClick: (Booking) -> Unit,
    private val onApprove: (Booking) -> Unit,
    private val onReject: (Booking) -> Unit,
    private val onCancel: (Booking) -> Unit,
    private var isAdmin: Boolean = false
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun updateList(newList: List<Booking>) {
        bookings = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val layout = if (isAdmin) R.layout.item_booking_admin else R.layout.item_booking_student
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        holder.tvUserName.text = booking.userName.ifEmpty { "Unknown User" }
        holder.tvVehicleName.text = booking.vehicleName
        holder.tvVehicleInfo.text = "${booking.vehicleType} • ${booking.selectedSeats.size} seats"
        holder.tvFromLocation.text = booking.fromLocation
        holder.tvToLocation.text = booking.toLocation
        holder.tvDate.text = if (booking.tripDate != null) dateFormat.format(booking.tripDate) else "-"
        holder.tvTime.text = if (booking.tripDate != null) timeFormat.format(booking.tripDate) else "-"
        holder.tvPrice.text = String.format("RM %.2f", booking.price)

        if (booking.selectedSeats.isNotEmpty()) {
            holder.tvSeats.text = "Seats: ${booking.selectedSeats.joinToString(", ")}"
        } else {
            holder.tvSeats.text = "Seats: -"
        }

        setStatus(holder, booking.status)

        if (isAdmin) {
            when (booking.status.lowercase()) {
                "pending" -> {
                    holder.layoutActions?.visibility = View.VISIBLE
                    holder.btnCancel?.visibility = View.GONE
                    holder.tvStatusMessage?.visibility = View.GONE
                }
                "approved" -> {
                    holder.layoutActions?.visibility = View.GONE
                    holder.btnCancel?.visibility = View.VISIBLE
                    holder.tvStatusMessage?.visibility = View.VISIBLE
                    holder.tvStatusMessage?.text = "✓ Approved"
                    holder.tvStatusMessage?.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green_available))
                }
                "rejected" -> {
                    holder.layoutActions?.visibility = View.GONE
                    holder.btnCancel?.visibility = View.GONE
                    holder.tvStatusMessage?.visibility = View.VISIBLE
                    holder.tvStatusMessage?.text = "✕ Rejected"
                    holder.tvStatusMessage?.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red_booked))
                }
                "cancelled" -> {
                    holder.layoutActions?.visibility = View.GONE
                    holder.btnCancel?.visibility = View.GONE
                    holder.tvStatusMessage?.visibility = View.VISIBLE
                    holder.tvStatusMessage?.text = "✕ Cancelled"
                    holder.tvStatusMessage?.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.gray))
                }
            }
            holder.btnApprove?.setOnClickListener { onApprove(booking) }
            holder.btnReject?.setOnClickListener { onReject(booking) }
            holder.btnCancel?.setOnClickListener { onCancel(booking) }
        } else {
            holder.btnCancel?.visibility = if (booking.status == "pending") View.VISIBLE else View.GONE
            holder.btnCancel?.setOnClickListener { onCancel(booking) }
        }

        holder.itemView.setOnClickListener { onItemClick(booking) }
    }

    private fun setStatus(holder: BookingViewHolder, status: String) {
        holder.tvStatus.text = status.uppercase()
        val color = when (status.lowercase()) {
            "pending" -> R.color.yellow_pending
            "approved" -> R.color.green_available
            "rejected", "cancelled" -> R.color.red_booked
            else -> R.color.gray
        }
        holder.tvStatus.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, color))
    }

    override fun getItemCount() = bookings.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvVehicleName: TextView = itemView.findViewById(R.id.tvVehicleName)
        val tvVehicleInfo: TextView = itemView.findViewById(R.id.tvVehicleInfo)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvFromLocation: TextView = itemView.findViewById(R.id.tvFromLocation)
        val tvToLocation: TextView = itemView.findViewById(R.id.tvToLocation)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvSeats: TextView = itemView.findViewById(R.id.tvSeats)
        val layoutActions: LinearLayout? = itemView.findViewById(R.id.layoutActions)
        val btnApprove: MaterialButton? = itemView.findViewById(R.id.btnApprove)
        val btnReject: MaterialButton? = itemView.findViewById(R.id.btnReject)
        val btnCancel: MaterialButton? = itemView.findViewById(R.id.btnCancel)
        val tvStatusMessage: TextView? = itemView.findViewById(R.id.tvStatusMessage)
    }
}