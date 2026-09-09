package com.example.movieapp.Domain

import java.io.Serializable

data class SelectedComboModel(
    var id: String = "",
    var name: String = "",
    var price: Double = 0.0,
    var quantity: Int = 0

) : Serializable {
    val subtotal: Double get() = price * quantity
}