package com.example.movieapp.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.movieapp.Domain.SeatModel
import com.example.movieapp.Domain.SeatStatus
import com.example.movieapp.R

class SeatAdapter(
    private val seats: MutableList<SeatModel>,
    private val maxSelectable: Int = 8,
    private val onSelectionChanged: (selectedSeats: List<SeatModel>) -> Unit
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

    private val selectedIds = mutableSetOf<String>()

    inner class SeatViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val seatText: TextView = itemView.findViewById(R.id.seatText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = seats[position]
        holder.seatText.text = seat.code

        val background = when {
            seat.status == SeatStatus.BOOKED -> R.drawable.ic_seat_unavailable
            selectedIds.contains(seat.id) -> R.drawable.ic_seat_selected
            else -> R.drawable.ic_seat_available
        }
        holder.seatText.setBackgroundResource(background)

        holder.itemView.setOnClickListener {
            if (seat.status == SeatStatus.BOOKED) return@setOnClickListener

            if (selectedIds.contains(seat.id)) {
                selectedIds.remove(seat.id)
                notifyItemChanged(holder.adapterPosition)
                onSelectionChanged(getSelectedSeats())
                return@setOnClickListener
            }

            if (selectedIds.size >= maxSelectable) {
                Toast.makeText(
                    holder.itemView.context,
                    "Chỉ được chọn tối đa $maxSelectable ghế",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            selectedIds.add(seat.id)
            notifyItemChanged(holder.adapterPosition)
            onSelectionChanged(getSelectedSeats())
        }
    }

    override fun getItemCount(): Int = seats.size

    fun getSelectedSeats(): List<SeatModel> = seats.filter { selectedIds.contains(it.id) }

    fun updateSeats(newSeats: List<SeatModel>) {
        selectedIds.clear()
        seats.clear()
        seats.addAll(newSeats)
        notifyDataSetChanged()
        onSelectionChanged(getSelectedSeats())
    }
}
