package com.example.movieapp.Domain

import java.time.LocalDate

data class ProfileModel(
    val id: String = "",
    val accountId: String = "",
    val name: String = "",
    val day_of_birth: String,
    val telephone: String = "",
    val gmail: String = "",
    val avatar: String = ""
)