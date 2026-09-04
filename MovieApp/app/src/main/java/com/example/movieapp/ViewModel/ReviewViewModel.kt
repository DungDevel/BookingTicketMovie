package com.example.movieapp.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.movieapp.Domain.RatingModel
import com.example.movieapp.Repository.ApiResult
import com.example.movieapp.Repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: ReviewRepository
): ViewModel(){
    fun loadReviews(filmId: String): LiveData<ApiResult<List<RatingModel>>>{
        return repository.loadReviews(filmId)
    }

    fun submitReview(existingReviewId: String, review: RatingModel): LiveData<ApiResult<RatingModel?>>{
        return repository.submitReview(existingReviewId, review)
    }

    fun deleteReview(reviewId: String): LiveData<ApiResult<Boolean>>{
        return repository.deleteReview(reviewId)
    }
}