package com.example.movieapp

import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.BookingModel
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


class SeatBookingConcurrencyTest {

    private lateinit var api: ApiService

    private val testBookingId = "UAjkx44viK8-2026-08-19-18:00-A1"

    private val testFilmId = "UAjkx44viK8"
    private val testDate = "2026-08-19"
    private val testTime = "18:00"

    @Before
    fun setup() {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl("http://localhost:3000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        api.updateBookingStatus(testBookingId, mapOf("status" to "available")).execute()
    }

    @Test
    fun `hai request dat ve cung luc cho cung 1 ghe - hien trang cua PATCH truc tiep`() {
        val threadPool = Executors.newFixedThreadPool(2)

        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(2)

        val successCount = AtomicInteger(0)
        val results = mutableListOf<Pair<String, Boolean>>()

        for (userName in listOf("User A", "User B")) {
            threadPool.submit {
                startLatch.await()
                val response = api.updateBookingStatus(testBookingId, mapOf("status" to "booked")).execute()
                val isSuccess = response.isSuccessful
                synchronized(results) { results.add(userName to isSuccess) }
                if (isSuccess) successCount.incrementAndGet()
                doneLatch.countDown()
            }
        }

        startLatch.countDown()
        doneLatch.await(10, TimeUnit.SECONDS)
        threadPool.shutdown()

        println("Kết quả từng request: $results")
        println("Số request PATCH thành công: ${successCount.get()} / 2")

        assertEquals(
            "Với backend hiện tại (json-server, không transaction), cả 2 request PATCH đều 'thắng' " +
                    "-> xác nhận có race condition / double-booking.",
            2, successCount.get()
        )
    }

    @Test
    fun `kiem tra optimistic check - GET truoc khi PATCH van khong dong bit hoan toan`() {

        val threadPool = Executors.newFixedThreadPool(2)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(2)
        val successCount = AtomicInteger(0)

        for (i in 1..2) {
            threadPool.submit {
                startLatch.await()

                val bookingsResponse = api.getBookings(
                    filmId = testFilmId,
                    date = testDate,
                    time = testTime
                ).execute()
                val booking: BookingModel? = bookingsResponse.body()?.find { it.id == testBookingId }

                if (booking?.status == "available") {
                    val patchResponse = api.updateBookingStatus(testBookingId, mapOf("status" to "booked")).execute()
                    if (patchResponse.isSuccessful) successCount.incrementAndGet()
                }
                doneLatch.countDown()
            }
        }

        api.updateBookingStatus(testBookingId, mapOf("status" to "available")).execute()

        startLatch.countDown()
        doneLatch.await(10, TimeUnit.SECONDS)
        threadPool.shutdown()

        println("Số request thành công dù đã thêm bước kiểm tra trước (optimistic check): ${successCount.get()} / 2")
    }
}