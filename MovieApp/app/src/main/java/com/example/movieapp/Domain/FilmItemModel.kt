package com.example.movieapp.Domain

import java.io.Serializable

data class FilmItemModel(
    var id: String = "",
    var Title: String = "",
    var Description: String = "",
    var Poster: String = "",
    var Time: String = "",
    var Trailer: String = "",
    var Imdb: Double = 0.0,
    var Year: Int = 0,
    var price: Double = 0.0,
    var Genre: ArrayList<String> = ArrayList(),
    var Casts: ArrayList<CastModel> = ArrayList(),
    var IsNowShowing: Boolean = true,
    var IsUpcoming: Boolean = false,
    var ReleaseAt: Long? = null
): Serializable
