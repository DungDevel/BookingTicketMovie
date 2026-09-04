package com.example.movieapp.Repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.BookingModel
import com.example.movieapp.Domain.BookingStatus
import com.example.movieapp.Domain.SeatConfigModel
import com.example.movieapp.Domain.SeatModel
import com.example.movieapp.Domain.SeatRowConfig
import com.example.movieapp.Domain.SeatStatus
import com.example.movieapp.Domain.SeatType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

data class ApiResult<T>(
    val data: T,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

@Singleton
class SeatRepository @Inject constructor(
    private val api: ApiService
) {

    companion object {
        private const val PENDING_HOLD_MINUTES = 10L
        private const val PENDING_HOLD_MILLIS = PENDING_HOLD_MINUTES * 60_000L
    }

    private var cachedConfig: SeatConfigModel? = null

    fun loadSeats(filmId: String, date: String, time: String): LiveData<ApiResult<MutableList<SeatModel>>> {
        val liveData = MutableLiveData<ApiResult<MutableList<SeatModel>>>()

        fun proceed(config: SeatConfigModel) {
            fetchBlockedSeatCodes(filmId, date, time) { blocked, error ->
                if (error) {
                    liveData.value = ApiResult(mutableListOf(), isError = true)
                } else {
                    liveData.value = ApiResult(generateSeats(config, blocked))
                }
            }
        }

        val cached = cachedConfig
        if (cached != null) {
            proceed(cached)
            return liveData
        }

        api.getSeatConfig().enqueue(object : Callback<SeatConfigModel> {
            override fun onResponse(call: Call<SeatConfigModel>, response: Response<SeatConfigModel>) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    cachedConfig = body
                    proceed(body)
                } else {
                    liveData.value = ApiResult(mutableListOf(), isError = true)
                }
            }

            override fun onFailure(call: Call<SeatConfigModel>, t: Throwable) {
                t.printStackTrace()
                liveData.value = ApiResult(mutableListOf(), isError = true)
            }
        })

        return liveData
    }

    private fun fetchBlockedSeatCodes(
        filmId: String,
        date: String,
        time: String,
        onResult: (blocked: Set<String>, error: Boolean) -> Unit
    ) {
        api.getBookings(filmId, date, time)
            .enqueue(object : Callback<List<BookingModel>> {
                override fun onResponse(call: Call<List<BookingModel>>, response: Response<List<BookingModel>>) {
                    if (!response.isSuccessful) {
                        onResult(emptySet(), true)
                        return
                    }
                    val bookings = response.body() ?: emptyList()
                    val now = System.currentTimeMillis()
                    val blocked = mutableSetOf<String>()

                    bookings.forEach { booking ->
                        when (booking.status) {
                            BookingStatus.CONFIRMED -> blocked.addAll(booking.seats)
                            BookingStatus.PENDING -> {
                                val isExpired = now - booking.createdAt > PENDING_HOLD_MILLIS
                                if (isExpired) {
                                    // Dọn rác: xóa booking pending hết hạn (fire-and-forget).
                                    if (booking.id.isNotBlank()) releaseBooking(booking.id)
                                } else {
                                    blocked.addAll(booking.seats)
                                }
                            }
                        }
                    }
                    onResult(blocked, false)
                }

                override fun onFailure(call: Call<List<BookingModel>>, t: Throwable) {
                    t.printStackTrace()
                    onResult(emptySet(), true)
                }
            })
    }

    private fun generateSeats(config: SeatConfigModel, blockedSeatCodes: Set<String>): MutableList<SeatModel> {
        val seats = mutableListOf<SeatModel>()

        fun addRows(rowConfig: SeatRowConfig, type: String) {
            for (row in rowConfig.rows) {
                for (number in 1..rowConfig.seatsPerRow) {
                    val code = "$row$number"
                    seats.add(
                        SeatModel(
                            id = code,
                            row = row,
                            number = number,
                            type = type,
                            price = rowConfig.price,
                            status = if (blockedSeatCodes.contains(code)) SeatStatus.BOOKED else SeatStatus.AVAILABLE
                        )
                    )
                }
            }
        }

        addRows(config.normal, SeatType.NORMAL)
        addRows(config.vip, SeatType.VIP)
        return seats
    }

    // ---------- Giữ chỗ / xác nhận / hủy ----------

    // Gọi khi user bấm "Thanh toán" ở màn chọn ghế: kiểm tra lại các ghế đã chọn
    // có còn trống không (giảm rủi ro race condition), rồi tạo booking pending.
    fun holdSeats(
        accountId: String,
        filmId: String,
        filmTitle: String,
        date: String,
        time: String,
        selectedSeatCodes: List<String>,
        totalPrice: Double
    ): LiveData<ApiResult<BookingModel?>> {
        val liveData = MutableLiveData<ApiResult<BookingModel?>>()

        fetchBlockedSeatCodes(filmId, date, time) { blocked, error ->
            if (error) {
                liveData.value = ApiResult(null, isError = true, errorMessage = "Không kiểm tra được tình trạng ghế")
                return@fetchBlockedSeatCodes
            }
            val conflict = selectedSeatCodes.any { it in blocked }
            if (conflict) {
                liveData.value = ApiResult(
                    null,
                    isError = true,
                    errorMessage = "Một số ghế bạn chọn vừa được người khác giữ, vui lòng chọn lại"
                )
                return@fetchBlockedSeatCodes
            }

            val booking = BookingModel(
                accountId = accountId,
                filmId = filmId,
                filmTitle = filmTitle,
                date = date,
                time = time,
                seats = selectedSeatCodes,
                totalPrice = totalPrice,
                status = BookingStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )

            api.createBooking(booking).enqueue(object : Callback<BookingModel> {
                override fun onResponse(call: Call<BookingModel>, response: Response<BookingModel>) {
                    if (response.isSuccessful) {
                        liveData.value = ApiResult(response.body())
                    } else {
                        liveData.value = ApiResult(null, isError = true)
                    }
                }

                override fun onFailure(call: Call<BookingModel>, t: Throwable) {
                    t.printStackTrace()
                    liveData.value = ApiResult(null, isError = true)
                }
            })
        }

        return liveData
    }

    // Gọi khi thanh toán thành công: pending -> confirmed.
    fun confirmBooking(bookingId: String): LiveData<ApiResult<BookingModel?>> {
        val liveData = MutableLiveData<ApiResult<BookingModel?>>()
        api.updateBookingStatus(bookingId, mapOf("status" to BookingStatus.CONFIRMED))
            .enqueue(object : Callback<BookingModel> {
                override fun onResponse(call: Call<BookingModel>, response: Response<BookingModel>) {
                    if (response.isSuccessful) {
                        liveData.value = ApiResult(response.body())
                    } else {
                        liveData.value = ApiResult(null, isError = true)
                    }
                }

                override fun onFailure(call: Call<BookingModel>, t: Throwable) {
                    t.printStackTrace()
                    liveData.value = ApiResult(null, isError = true)
                }
            })
        return liveData
    }

    fun releaseBooking(bookingId: String) {
        if (bookingId.isBlank()) return
        api.deleteBooking(bookingId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }
}