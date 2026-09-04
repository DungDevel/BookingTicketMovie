package com.example.movieapp.Utils

import android.content.Context
import com.example.movieapp.Domain.AccountRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserId(userId: String){
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun saveSession(userId: String, role: String){
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getUserId():String?{
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getRole():String?{
        return prefs.getString(KEY_ROLE, null)
    }

    fun isAdmin(): Boolean{
        return getRole() == AccountRole.ADMIN
    }

    fun clearSeesion(){
        prefs.edit().clear().apply()
    }
}