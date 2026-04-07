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
import com.bumptech.glide.Glide
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Vehicle
import com.google.android.material.button.MaterialButton
import java.util.Locale

class VehicleAdapter(
    private var vehicles: List<Vehicle>,
    private val onViewSeatsClick: (Vehicle) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.bind(vehicle)

        val isActive = vehicle.isActive && vehicle.isAvailable
        val isBus = vehicle.type.lowercase() == "bus"

        // Handle inactive vehicle - grey out and disable clicks
        if (!isActive) {
            // Grey out the card background
            holder.cardVehicle.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.gray)
            )

            // Reduce opacity of image
            holder.ivVehicleImage.alpha = 0.5f

            // Disable click on entire card
            holder.cardVehicle.isClickable = false
            holder.cardVehicle.isFocusable = false

            // Hide VIEW SEATS button for inactive vehicles
            holder.btnViewSeats.visibility = View.GONE
        } else {
            // Active vehicle - normal appearance
            holder.cardVehicle.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.white)
            )
            holder.ivVehicleImage.alpha = 1.0f
            holder.cardVehicle.isClickable = true
            holder.cardVehicle.isFocusable = true

            // Show VIEW SEATS button for active buses only
            if (isBus) {
                holder.btnViewSeats.visibility = View.VISIBLE
                holder.btnViewSeats.setOnClickListener {
                    onViewSeatsClick(vehicle)
                }
            } else {
                holder.btnViewSeats.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = vehicles.size

    fun updateList(newList: List<Vehicle>) {
        vehicles = newList
        notifyDataSetChanged()
    }

    class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardVehicle: CardView = itemView.findViewById(R.id.cardVehicle)
        val layoutVehicleContent: LinearLayout = itemView.findViewById(R.id.layoutVehicleContent)
        val ivVehicleImage: ImageView = itemView.findViewById(R.id.ivVehicleImage)
        private val tvVehicleName: TextView = itemView.findViewById(R.id.tvVehicleName)
        private val tvPlateNumber: TextView = itemView.findViewById(R.id.tvPlateNumber)
        private val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvSeatAvailability: TextView = itemView.findViewById(R.id.tvSeatAvailability)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        val btnViewSeats: MaterialButton = itemView.findViewById(R.id.btnViewSeats)

        fun bind(vehicle: Vehicle) {
            val vehicleType = vehicle.type.lowercase()
            val isActive = vehicle.isActive && vehicle.isAvailable

            // Vehicle name
            tvVehicleName.text = vehicle.brand
            tvPlateNumber.text = vehicle.plateNumber

            // Type badge
            when (vehicleType) {
                "bus" -> {
                    tvType.text = "BUS"
                    tvType.setBackgroundResource(R.drawable.badge_rounded_blue)
                    tvType.visibility = View.VISIBLE
                }
                "car" -> {
                    tvType.text = "CAR"
                    tvType.setBackgroundResource(R.drawable.badge_rounded_orange)
                    tvType.visibility = View.VISIBLE
                }
                else -> {
                    tvType.visibility = View.GONE
                }
            }

            // Capacity
            tvCapacity.text = "${vehicle.seatCapacity} seats"

            // Price
            tvPrice.text = String.format(Locale.getDefault(), "RM %.2f/km", vehicle.pricePerKm)

            // Load image
            val imageUrl = vehicle.getMainImage()
            if (imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(if (vehicleType == "bus") R.drawable.ic_bus else R.drawable.ic_car)
                    .error(if (vehicleType == "bus") R.drawable.ic_bus else R.drawable.ic_car)
                    .centerCrop()
                    .into(ivVehicleImage)
            } else {
                val iconRes = if (vehicleType == "bus") R.drawable.ic_bus else R.drawable.ic_car
                ivVehicleImage.setImageResource(iconRes)
            }

            // Status and appearance based on active/inactive
            if (isActive) {
                tvStatus.text = "AVAILABLE"
                tvStatus.setBackgroundResource(R.drawable.badge_available)
                tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.green_available))
                tvVehicleName.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                tvPlateNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                tvCapacity.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                tvPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.red_booked))
            } else {
                tvStatus.text = "UNAVAILABLE"
                tvStatus.setBackgroundResource(R.drawable.badge_inactive)
                tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                tvVehicleName.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                tvPlateNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                tvCapacity.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                tvPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
            }

            // Seat Availability (only for Bus)
            if (vehicleType == "bus") {
                val availableSeats = vehicle.seatLayout.count { it.status == "available" }
                val pendingSeats = vehicle.seatLayout.count { it.status == "pending" }
                val bookedSeats = vehicle.seatLayout.count { it.status == "booked" }

                tvSeatAvailability.visibility = View.VISIBLE
                tvSeatAvailability.text = "$availableSeats available • $pendingSeats pending • $bookedSeats booked"

                if (availableSeats > 0 && isActive) {
                    tvSeatAvailability.setTextColor(ContextCompat.getColor(itemView.context, R.color.green_available))
                } else if (!isActive) {
                    tvSeatAvailability.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                } else {
                    tvSeatAvailability.setTextColor(ContextCompat.getColor(itemView.context, R.color.red_booked))
                }
            } else {
                tvSeatAvailability.visibility = View.GONE
            }
        }
    }
}