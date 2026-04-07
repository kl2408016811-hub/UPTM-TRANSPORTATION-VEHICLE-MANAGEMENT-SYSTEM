package com.example.uptmtransportation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uptmtransportation.R
import com.example.uptmtransportation.models.Event
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private var events: List<Event>,
    private val onItemClick: (Event) -> Unit,
    private val isAdmin: Boolean = false,
    private val onEditClick: ((Event) -> Unit)? = null,
    private val onDeleteClick: ((Event) -> Unit)? = null
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event)
    }

    override fun getItemCount() = events.size

    fun updateList(newList: List<Event>) {
        events = newList
        notifyDataSetChanged()
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        private val tvEventTime: TextView = itemView.findViewById(R.id.tvEventTime)
        private val tvEventLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        private val tvEventDescription: TextView = itemView.findViewById(R.id.tvEventDescription)
        private val layoutAdminActions: LinearLayout = itemView.findViewById(R.id.layoutAdminActions)
        private val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
        private val btnEditEvent: Button = itemView.findViewById(R.id.btnEditEvent)
        private val btnDeleteEvent: Button = itemView.findViewById(R.id.btnDeleteEvent)

        fun bind(event: Event) {
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            tvEventDate.text = event.date?.let { dateFormat.format(it) } ?: "No Date"
            tvEventTime.text = event.time
            tvEventLocation.text = event.location
            tvEventDescription.text = event.title

            if (isAdmin) {
                layoutAdminActions.visibility = View.VISIBLE
                btnViewDetails.visibility = View.GONE
                btnEditEvent.text = "EDIT"
                btnDeleteEvent.text = "DELETE"

                // Set click listeners
                btnEditEvent.setOnClickListener {
                    onEditClick?.invoke(event)
                }
                btnDeleteEvent.setOnClickListener {
                    onDeleteClick?.invoke(event)
                }
            } else {
                layoutAdminActions.visibility = View.GONE
                btnViewDetails.visibility = View.VISIBLE
                btnViewDetails.text = "VIEW DETAILS"
                btnViewDetails.setOnClickListener {
                    onItemClick(event)
                }
            }
        }
    }
}