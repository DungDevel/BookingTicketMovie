package com.example.movieapp.Api

import com.example.movieapp.Domain.AccountModel
import com.example.movieapp.Domain.BookingModel
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.Domain.ProfileModel
import com.example.movieapp.Domain.RatingModel
import com.example.movieapp.Domain.SeatConfigModel
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("Item")
    fun getFilms(): Call<List<FilmItemModel>>

    @POST("Item")
    fun addItem(@Body item: FilmItemModel): Call<FilmItemModel>

    @PUT("Item/{id}")
    fun updateItem(@Path("id") id: String, @Body item: FilmItemModel): Call<FilmItemModel>

    @DELETE("Item/{id}")
    fun deleteItem(@Path("id") id: String): Call<Void>

    @GET("Account")
    fun getAccounts(): Call<List<AccountModel>>

    @POST("Account")
    fun registerAccount(@Body account: AccountModel): Call<AccountModel>

    @PUT("Account/{id}")
    fun updateAccount(@Path("id") id: String, @Body account: AccountModel): Call<AccountModel>

    @DELETE("Account/{id}")
    fun deleteAccount(@Path("id") id: String): Call<Void>

    @GET("Profile")
    fun getProfile(): Call<List<ProfileModel>>

    @POST("Profile")
    fun createProfile(@Body profile: ProfileModel): Call<ProfileModel>

    @PUT("Profile/{id}")
    fun updateProfile(@Path("id") id: String, @Body profile: ProfileModel): Call<ProfileModel>

    @GET("SeatConfig")
    fun getSeatConfig(): Call<SeatConfigModel>

    @GET("Bookings")
    fun getBookings(
        @Query("filmId") filmId: String,
        @Query("date") date: String,
        @Query("time") time: String
    ): Call<List<BookingModel>>

    @GET("Bookings")
    fun getBookingsByAccount(@Query("accountId") accountId: String): Call<List<BookingModel>>

    @POST("Bookings")
    fun createBooking(@Body booking: BookingModel): Call<BookingModel>

    @PATCH("Bookings/{id}")
    fun updateBookingStatus(@Path("id") id: String, @Body body: Map<String, String>): Call<BookingModel>

    @DELETE("Bookings/{id}")
    fun deleteBooking(@Path("id") id: String): Call<Void>

    @GET("Bookings")
    fun getAllBookings(): Call<List<BookingModel>>

    @GET("Reviews")
    fun getReviewsByFilm(@Query("filmId") filmId: String): Call<List<RatingModel>>

    @POST("Reviews")
    fun createReview(@Body reivew: RatingModel): Call<RatingModel>

    @PUT("Reviews/{id}")
    fun updateReview(@Path("id") id: String, @Body review: RatingModel): Call<RatingModel>

    @DELETE("Reviews/{id}")
    fun deleteReview(@Path("id") id: String): Call<Void>
}