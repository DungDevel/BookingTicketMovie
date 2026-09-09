package com.example.movieapp.Domain

import java.io.Serializable

object SeatStatus {
    const val AVAILABLE = "available"
    const val BOOKED = "booked"
}

object SeatType {
    const val NORMAL = "normal"
    const val VIP = "vip"
}

data class SeatModel(
    var id: String = "",
    var row: String = "",
    var number: Int = 0,
    var type: String = SeatType.NORMAL,
    var price: Double = 0.0,
    var status: String = SeatStatus.AVAILABLE
) : Serializable {
    val code: String get() = "$row$number"
}