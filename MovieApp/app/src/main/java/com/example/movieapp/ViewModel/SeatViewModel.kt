package com.example.movieapp.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.movieapp.Domain.BookingModel
import com.example.movieapp.Domain.SeatModel
import com.example.movieapp.Domain.SelectedComboModel
import com.example.movieapp.Repository.ApiResult
import com.example.movieapp.Repository.SeatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SeatViewModel @Inject constructor(
    private val repository: SeatRepository
): ViewModel() {

    fun loadSeats(filmId: String, date: String, time: String): LiveData<ApiResult<MutableList<SeatModel>>> {
        return repository.loadSeats(filmId, date, time)
    }

    fun holdSeats(
        accountId: String,
        filmId: String,
        filmTitle: String,
        date: String,
        time: String,
        selectedSeatCodes: List<String>,
        totalPrice: Double,
        combos: List<SelectedComboModel> = emptyList()
    ): LiveData<ApiResult<BookingModel?>> {
        return repository.holdSeats(accountId, filmId, filmTitle, date, time, selectedSeatCodes, totalPrice, combos)
    }

    fun confirmBooking(bookingId: String): LiveData<ApiResult<BookingModel?>> {
        return repository.confirmBooking(bookingId)
    }

    fun releaseBooking(bookingId: String) {
        repository.releaseBooking(bookingId)
    }

    fun loadComboItems() = repository.loadComboItems()
}