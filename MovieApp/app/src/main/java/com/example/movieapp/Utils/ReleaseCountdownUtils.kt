package com.example.movieapp.Utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


object ReleaseCountdownUtils {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))

    fun getCountdownText(releaseAt: Long?): String? {
        if (releaseAt == null) return null

        val now = System.currentTimeMillis()
        val diffMillis = releaseAt - now

        if (diffMillis <= 0) {
            return "Sắp khởi chiếu"
        }

        val totalDays = TimeUnit.MILLISECONDS.toDays(diffMillis)
        val totalHours = TimeUnit.MILLISECONDS.toHours(diffMillis)

        return when {
            totalDays >= 1 -> "🎬 Còn $totalDays ngày"
            totalHours >= 1 -> "🎬 Còn $totalHours giờ"
            else -> "Sắp chiếu, còn vài phút nữa"
        }
    }

    fun formatReleaseDate(releaseAt: Long?): String? {
        if (releaseAt == null) return null
        return dateFormat.format(Date(releaseAt))
    }
}
