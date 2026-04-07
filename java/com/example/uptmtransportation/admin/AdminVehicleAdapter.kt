package com.example.uptmtransportation.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Vehicle
import com.google.android.material.button.MaterialButton
import java.util.Locale

class AdminVehicleAdapter(
    private var vehicles: List<Vehicle>,
    private val onEditClick: (Vehicle) -> Unit,
    private val onDeleteClick: (Vehicle) -> Unit,
    private val onToggleActive: (Vehicle, Boolean) -> Unit
) : RecyclerView.Adapter<AdminVehicleAdapter.VehicleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle_admin, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(vehicles[position])
    }

    override fun getItemCount() = vehicles.size

    fun updateList(newList: List<Vehicle>) {
        vehicles = newList
        notifyDataSetChanged()
    }

    inner class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivVehicleIcon: ImageView = itemView.findViewById(R.id.ivVehicleIcon)
        private val tvVehicleName: TextView = itemView.findViewById(R.id.tvVehicleName)
        private val tvPlateNumber: TextView = itemView.findViewById(R.id.tvPlateNumber)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val switchActive: SwitchCompat = itemView.findViewById(R.id.switchActive)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)

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
                    tvType.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                "car" -> {
                    tvType.text = "CAR"
                    tvType.setBackgroundResource(R.drawable.badge_rounded_orange)
                    tvType.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
                else -> {
                    tvType.text = vehicle.type.uppercase()
                    tvType.setBackgroundResource(R.drawable.badge_rounded_gray)
                    tvType.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                }
            }

            // Capacity
            tvCapacity.text = "${vehicle.seatCapacity} seats"

            // Price
            tvPrice.text = String.format(Locale.getDefault(), "RM %.2f/km", vehicle.pricePerKm)

            // Load image with Glide
            val imageUrl = vehicle.getMainImage()
            if (imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(if (vehicleType == "bus") R.drawable.ic_bus else R.drawable.ic_car)
                    .error(if (vehicleType == "bus") R.drawable.ic_bus else R.drawable.ic_car)
                    .centerCrop()
                    .into(ivVehicleIcon)
            } else {
                val iconRes = if (vehicleType == "bus") R.drawable.ic_bus else R.drawable.ic_car
                ivVehicleIcon.setImageResource(iconRes)
            }

            // Set toggle switch state
            switchActive.isChecked = isActive

            // Set image alpha based on active status
            ivVehicleIcon.alpha = if (isActive) 1.0f else 0.5f

            // Toggle switch listener
            switchActive.setOnCheckedChangeListener { _, isChecked ->
                // Prevent infinite loop
                if (isChecked != isActive) {
                    onToggleActive(vehicle, isChecked)
                }
            }

            // Edit button
            btnEdit.setOnClickListener { onEditClick(vehicle) }

            // Delete button
            btnDelete.setOnClickListener { onDeleteClick(vehicle) }
        }
    }
}