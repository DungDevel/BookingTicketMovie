package com.example.movieapp.Domain

import java.io.Serializable

object BookingStatus {
    const val PENDING = "pending"
    const val CONFIRMED = "confirmed"
}

data class BookingModel(
    var id: String = "",
    var filmId: String = "",
    var accountId: String = "",
    var filmTitle: String = "",
    var date: String = "",
    var time: String = "",
    var seats: List<String> = emptyList(),
    var totalPrice: Double = 0.0,
    var status: String = BookingStatus.PENDING,
    var createdAt: Long = System.currentTimeMillis(),
    var combos: List<SelectedComboModel> = emptyList()
) : Serializable