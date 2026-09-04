package com.example.movieapp.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.Domain.SeatModel
import com.example.movieapp.R
import com.example.movieapp.ViewModel.SeatViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class PaymentActivity : AppCompatActivity() {

    private val viewModel: SeatViewModel by viewModels()

    private lateinit var film: FilmItemModel
    private lateinit var date: String
    private lateinit var time: String
    private lateinit var seats: List<SeatModel>
    private var totalPrice: Double = 0.0
    private lateinit var bookingId: String

    private var isConfirmed = false

    private lateinit var confirmButton: Button
    private lateinit var loadingProgress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val filmExtra = intent.getSerializableExtra("film") as? FilmItemModel
        @Suppress("UNCHECKED_CAST")
        val seatsExtra = intent.getSerializableExtra("seats") as? ArrayList<SeatModel>
        val dateExtra = intent.getStringExtra("date")
        val timeExtra = intent.getStringExtra("time")

        val bookingIdExtra = intent.getStringExtra("bookingId")

        if (filmExtra == null || seatsExtra.isNullOrEmpty() || dateExtra == null || timeExtra == null || bookingIdExtra.isNullOrBlank()) {
            Toast.makeText(this, "Thiếu thông tin đặt vé", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        film = filmExtra
        seats = seatsExtra
        date = dateExtra
        time = timeExtra
        bookingId = bookingIdExtra
        totalPrice = intent.getDoubleExtra("totalPrice", seats.sumOf { if (it.price > 0) it.price else film.price })

        findViewById<ImageView>(R.id.backbtn).setOnClickListener { releaseAndFinish() }
        confirmButton = findViewById(R.id.confirmButton)
        loadingProgress = findViewById(R.id.loadingProgress)

        bindSummary()

        confirmButton.setOnClickListener { confirmBooking() }
        setupSystemBackRelease()
    }

    private fun bindSummary() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        findViewById<TextView>(R.id.filmTitleText).text = film.Title
        findViewById<TextView>(R.id.showtimeText).text = "Suất chiếu: $date • $time"
        findViewById<TextView>(R.id.seatsText).text = "Ghế: ${seats.joinToString(", ") { it.code }}"
        findViewById<TextView>(R.id.totalPriceText).text = formatter.format(totalPrice)
    }

    private fun confirmBooking() {
        setLoading(true)
        viewModel.confirmBooking(bookingId).observe(this) { result ->
            setLoading(false)

            if (result.isError || result.data == null) {
                Toast.makeText(
                    this,
                    "Đặt vé thất bại, vui lòng thử lại.",
                    Toast.LENGTH_LONG
                ).show()
                return@observe
            }

            isConfirmed = true
            Toast.makeText(this, "Đặt vé thành công!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun releaseAndFinish() {
        if (!isConfirmed) {
            viewModel.releaseBooking(bookingId)
        }
        finish()
    }

    private fun setupSystemBackRelease() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                releaseAndFinish()
            }
        })
    }

    private fun setLoading(isLoading: Boolean) {
        loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        confirmButton.isEnabled = !isLoading
        confirmButton.text = if (isLoading) "Đang xử lý..." else "Xác nhận thanh toán"
    }
}