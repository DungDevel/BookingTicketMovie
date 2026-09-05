package com.example.movieapp.Repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.Domain.ProfileModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val api: ApiService
) {

    fun loadFilms(): LiveData<MutableList<FilmItemModel>>{
        val listData = MutableLiveData<MutableList<FilmItemModel>>()
        api.getFilms().enqueue(object : Callback<List<FilmItemModel>>{
            override fun onResponse(call: Call<List<FilmItemModel>>, response: Response<List<FilmItemModel>>){
                listData.value = response.body()?.toMutableList() ?: mutableListOf()
            }
            override fun onFailure(call: Call<List<FilmItemModel>>, t: Throwable){
                t.printStackTrace()
                listData.value = mutableListOf()
            }
        })
        return listData
    }

    fun loadProfile(): LiveData<MutableList<ProfileModel>> {
        val listData = MutableLiveData<MutableList<ProfileModel>>()
        api.getProfile().enqueue(object : Callback<List<ProfileModel>> {
            override fun onResponse(call: Call<List<ProfileModel>>, response: Response<List<ProfileModel>>){
                listData.value = response.body()?.toMutableList() ?: mutableListOf()
            }
            override fun onFailure(call: Call<List<ProfileModel>>, t: Throwable) {
                t.printStackTrace()
                listData.value = mutableListOf()
            }
        })
        return listData
    }
}