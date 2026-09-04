package com.example.movieapp.Domain

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ShowDateModel(
    val fullDate: String,   // "2026-08-19"
    val dayOfWeek: String,  // "T3", "T4", ...
    val dayNumber: String   // "19"
) : Serializable

object ShowDateGenerator {
    fun nextDays(count: Int = 7): List<ShowDateModel> {
        val result = mutableListOf<ShowDateModel>()
        val calendar = Calendar.getInstance()
        val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale("vi"))
        val numberFormat = SimpleDateFormat("dd", Locale.getDefault())

        repeat(count) {
            result.add(
                ShowDateModel(
                    fullDate = apiFormat.format(calendar.time),
                    dayOfWeek = dayFormat.format(calendar.time).replaceFirstChar { c -> c.uppercase() },
                    dayNumber = numberFormat.format(calendar.time)
                )
            )
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }
}

object ShowTimeGenerator {

    private val ALL_TIMES = listOf("09:00", "12:00", "15:00", "18:00", "20:00", "22:00")

    fun defaultTimes(): List<String> = ALL_TIMES

    fun availableTimes(date: ShowDateModel, now: Calendar = Calendar.getInstance()): List<String>{
        val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = apiFormat.format(now.time)
        if (date.fullDate != today) return ALL_TIMES

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return ALL_TIMES.filter { time ->
            val (hour, minute) = time.split(":").map { it.toInt() }
            (hour * 60 + minute) > nowMinutes
        }
    }
}
