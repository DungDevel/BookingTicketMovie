package com.example.movieapp.Utils

object ValidationUtils{
    fun validateUsername(username: String): String? {
        if (username.length <= 8){
            return "Tên đăng nhập phải trên 8 ký tự"
        }
        if (!username.matches(Regex("^[a-zA-Z]+$"))){
            return "Tên đăng nhập không được có dấu, số hoặc ký tự đặc biệt"
        }
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.length < 8){
            return "Mật khẩu phải có ít nhất 8 ký tự"
        }
        if (!password.any { it.isUpperCase() }){
            return "Mật khẩu phải có ít nhất 1 chữ in hoa"
        }
        if (!password.any{ it.isLowerCase() }){
            return "Mật khẩu phải có ít nhất 1 chữ thường"
        }
        return null
    }

    fun validateTelephone(telephone: String): String? {
        if (!telephone.all { it.isDigit() }){
            return "Số điện thoại chỉ được nhập số"
        }
        return null
    }
}