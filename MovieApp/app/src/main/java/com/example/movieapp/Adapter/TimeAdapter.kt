package com.example.movieapp.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.movieapp.R

class TimeAdapter(
    private var times: List<String>,
    private val onTimeSelected: (String) -> Unit
) : RecyclerView.Adapter<TimeAdapter.TimeViewHolder>() {

    private var selectedPosition = 0

    inner class TimeViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.timeContainer)
        val timeText: TextView = itemView.findViewById(R.id.timeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time, parent, false)
        return TimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        val item = times[position]
        holder.timeText.text = item

        holder.container.setBackgroundResource(
            if (position == selectedPosition) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )

        holder.itemView.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onTimeSelected(item)
        }
    }

    override fun getItemCount(): Int = times.size

    fun getSelected(): String? = times.getOrNull(selectedPosition)

    fun updateTimes(newTimes: List<String>){
        val currentlySelected = times.getOrNull(selectedPosition)
        times = newTimes
        selectedPosition = currentlySelected?.let { newTimes.indexOf(it) } ?.takeIf { it >= 0 } ?: 0
        notifyDataSetChanged()
    }
}
