package com.example.movieapp.Repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.RatingModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val api: ApiService
) {

    fun loadReviews(filmId: String): LiveData<ApiResult<List<RatingModel>>>{
        val liveData = MutableLiveData<ApiResult<List<RatingModel>>>()
        api.getReviewsByFilm(filmId).enqueue(object : Callback<List<RatingModel>>{
            override fun onResponse(call: Call<List<RatingModel>>, response: Response<List<RatingModel>>){
                if (response.isSuccessful){
                    liveData.value = ApiResult(response.body() ?: emptyList())
                } else {
                    liveData.value = ApiResult(emptyList(), isError = true)
                }
            }
            override fun onFailure(call: Call<List<RatingModel>>, t: Throwable){
                liveData.value = ApiResult(emptyList(), isError = true, errorMessage = t.message)
            }
        })
        return liveData
    }

    fun submitReview(existingReviewId: String, review: RatingModel): LiveData<ApiResult<RatingModel?>>{
        val liveData = MutableLiveData<ApiResult<RatingModel?>>()
        val call = if (existingReviewId.isBlank()){
            api.createReview(review)
        } else {
            api.updateReview(existingReviewId, review)
        }

        call.enqueue(object : Callback<RatingModel>{
            override fun onResponse(call: Call<RatingModel>, response: Response<RatingModel>){
                if(response.isSuccessful){
                    liveData.value = ApiResult(response.body())
                } else {
                    liveData.value = ApiResult(null, isError = true)
                }
            }

            override fun onFailure(call: Call<RatingModel>, t: Throwable){
                liveData.value = ApiResult(null, isError = true, errorMessage = t.message)
            }
        })
        return  liveData
    }

    fun deleteReview(reviewId: String): LiveData<ApiResult<Boolean>>{
        val liveData = MutableLiveData<ApiResult<Boolean>>()
        if (reviewId.isBlank()){
            liveData.value = ApiResult(false, isError = true)
            return liveData
        }
        api.deleteReview(reviewId).enqueue(object : Callback<Void>{
            override fun onResponse(call: Call<Void>, response: Response<Void>){
                liveData.value = ApiResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable){
                liveData.value = ApiResult(false, isError = true, errorMessage = t.message)
            }
        })
        return liveData
    }
}