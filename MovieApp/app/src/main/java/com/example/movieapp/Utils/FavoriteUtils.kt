package com.example.movieapp.Utils

import android.content.Context
import com.example.movieapp.Domain.FilmItemModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FavoriteManager {
    private const val PREF_NAME = "favorite_pref"
    private const val KEY_FAVORITES = "favorite_list"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getFavorites(context: Context): MutableList<FilmItemModel>{
        val json = getPrefs(context).getString(KEY_FAVORITES, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<FilmItemModel>>() {}.type
        return Gson().fromJson(json, type) ?: mutableListOf()
    }

    private fun saveFavorites(context: Context, list: List<FilmItemModel>){
        val json = Gson().toJson(list)
        getPrefs(context).edit().putString(KEY_FAVORITES, json).apply()
    }

    fun isFavorite(context: Context, film: FilmItemModel): Boolean{
        return getFavorites(context).any { it.id == film.id }
    }

    fun toggleFavorite(context: Context, film: FilmItemModel): Boolean{
        val list = getFavorites(context)
        val existing = list.find { it.id == film.id }
        return if (existing != null){
            list.remove(existing)
            saveFavorites(context, list)
            false
        } else {
            list.add(film)
            saveFavorites(context, list)
            true
        }
    }
}