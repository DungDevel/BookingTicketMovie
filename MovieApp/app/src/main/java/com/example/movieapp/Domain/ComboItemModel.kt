package com.example.movieapp.Domain

import java.io.Serializable

object ComboCategory {
    const val POPCORN = "popcorn"
    const val DRINK = "drink"
    const val COMBO = "combo"
}

data class ComboItemModel(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var category: String = ComboCategory.POPCORN,
    var imageUrl: String = "",
    var isActive: Boolean = true
) : Serializable