package com.example.movieapp.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.Repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
): ViewModel(){

    fun loadUpcoming(): LiveData<MutableList<FilmItemModel>>{
        return repository.loadUpcoming()
    }

    fun loadItems(): LiveData<MutableList<FilmItemModel>>{
        return repository.loadItems()
    }
}
