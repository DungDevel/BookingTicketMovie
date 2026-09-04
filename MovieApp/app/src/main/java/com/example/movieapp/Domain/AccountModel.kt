package com.example.movieapp.Domain

object AccountRole{
    const val ADMIN = "admin"
    const val USER = "user"
}
data class AccountModel(
    val id: String = "",
    val userName: String = "",
    val password: String = "",
    val role: String = AccountRole.USER
)
