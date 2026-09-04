package com.example.movieapp

import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.SeatModel
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

    private val testSeatId = "UAjkx44viK8-2026-08-19-18:00-A1"

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

        api.updateSeatStatus(testSeatId, mapOf("status" to "available")).execute()
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
                val response = api.updateSeatStatus(testSeatId, mapOf("status" to "booked")).execute()
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

                val seatsResponse = api.getSeats(
                    filmId = "UAjkx44viK8",
                    date = "2026-08-19",
                    time = "18:00"
                ).execute()
                val seat = seatsResponse.body()?.find { it.id == testSeatId }

                if (seat?.status == "available") {
                    val patchResponse = api.updateSeatStatus(testSeatId, mapOf("status" to "booked")).execute()
                    if (patchResponse.isSuccessful) successCount.incrementAndGet()
                }
                doneLatch.countDown()
            }
        }

        api.updateSeatStatus(testSeatId, mapOf("status" to "available")).execute()

        startLatch.countDown()
        doneLatch.await(10, TimeUnit.SECONDS)
        threadPool.shutdown()

        println("Số request thành công dù đã thêm bước kiểm tra trước (optimistic check): ${successCount.get()} / 2")
    }
}