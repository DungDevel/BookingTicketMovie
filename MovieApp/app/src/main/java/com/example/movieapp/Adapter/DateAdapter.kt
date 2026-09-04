package com.example.movieapp.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.movieapp.Domain.ShowDateModel
import com.example.movieapp.R

class DateAdapter(
    private val dates: List<ShowDateModel>,
    private val onDateSelected: (ShowDateModel) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private var selectedPosition = 0

    inner class DateViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.dateContainer)
        val dayOfWeekText: TextView = itemView.findViewById(R.id.dayOfWeekText)
        val dayNumberText: TextView = itemView.findViewById(R.id.dayNumberText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val item = dates[position]
        holder.dayOfWeekText.text = item.dayOfWeek
        holder.dayNumberText.text = item.dayNumber

        holder.container.setBackgroundResource(
            if (position == selectedPosition) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )

        holder.itemView.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onDateSelected(item)
        }
    }

    override fun getItemCount(): Int = dates.size

    fun getSelected(): ShowDateModel = dates[selectedPosition]
}
