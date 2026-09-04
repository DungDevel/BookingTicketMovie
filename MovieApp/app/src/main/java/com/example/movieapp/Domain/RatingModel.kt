package com.example.movieapp.Domain

import java.io.Serializable

class RatingModel (
    var id: String = "",
    var filmId: String = "",
    var accountId: String = "",
    var userName: String = "",
    var rating: Int = 5,
    var comment: String = "",
    var createAt: Long = System.currentTimeMillis()
) : Serializable