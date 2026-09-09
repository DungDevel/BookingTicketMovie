package com.example.movieapp.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieapp.Adapter.DateAdapter
import com.example.movieapp.Adapter.SeatAdapter
import com.example.movieapp.Adapter.TimeAdapter
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.Domain.SeatModel
import com.example.movieapp.Domain.ShowDateGenerator
import com.example.movieapp.Domain.ShowDateModel
import com.example.movieapp.Domain.ShowTimeGenerator
import com.example.movieapp.R
import com.example.movieapp.Utils.SessionManager
import com.example.movieapp.ViewModel.SeatViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

private const val SEAT_GRID_SPAN_COUNT = 8

@AndroidEntryPoint
class SeatListActivity : AppCompatActivity() {

    private lateinit var film: FilmItemModel
    private val viewModel: SeatViewModel by viewModels()

    @Inject lateinit var sessionManager: SessionManager

    private lateinit var dateRecyclerview: RecyclerView
    private lateinit var timeRecyclerview: RecyclerView
    private lateinit var seatRecyclerview: RecyclerView
    private lateinit var priceText: TextView
    private lateinit var numberSelectText: TextView
    private lateinit var emptySeatText: TextView
    private lateinit var payButton: Button

    private lateinit var dateAdapter: DateAdapter
    private lateinit var timeAdapter: TimeAdapter
    private lateinit var seatAdapter: SeatAdapter

    private var selectedSeats: List<SeatModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_seat_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val filmExtra = intent.getSerializableExtra("object") as? FilmItemModel
        if (filmExtra == null) {
            Toast.makeText(this, "Không tìm thấy thông tin phim", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        film = filmExtra

        bindViews()
        findViewById<TextView>(R.id.textView).text = "Chọn chỗ ngồi - ${film.Title}"
        findViewById<ImageView>(R.id.backbtn).setOnClickListener { finish() }

        setupDateAndTimeSelectors()
        setupSeatGrid()
        setupPayButton()

        loadSeats(dateAdapter.getSelected(), timeAdapter.getSelected())
    }

    private fun bindViews() {
        dateRecyclerview = findViewById(R.id.DataRecyclerview)
        timeRecyclerview = findViewById(R.id.TimeRecyclerview)
        seatRecyclerview = findViewById(R.id.seatRecyclerview)
        priceText = findViewById(R.id.priceText)
        numberSelectText = findViewById(R.id.numberSelectText)
        emptySeatText = findViewById(R.id.emptySeatText)
        payButton = findViewById(R.id.button)
    }

    private fun setupDateAndTimeSelectors() {
        val dates = ShowDateGenerator.nextDays(7)
        val times = ShowTimeGenerator.defaultTimes()

        dateAdapter = DateAdapter(dates) { selectedDate ->
            val newTimes = ShowTimeGenerator.availableTimes(selectedDate)
            timeAdapter.updateTimes(newTimes)
            loadSeats(selectedDate, timeAdapter.getSelected())
        }
        dateRecyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        dateRecyclerview.adapter = dateAdapter

        val initialTimes = ShowTimeGenerator.availableTimes(dateAdapter.getSelected())
        timeAdapter = TimeAdapter(initialTimes) { selectedTime ->
            loadSeats(dateAdapter.getSelected(), selectedTime)
        }
        timeRecyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        timeRecyclerview.adapter = timeAdapter
    }

    private fun setupSeatGrid() {
        seatRecyclerview.layoutManager = GridLayoutManager(this, SEAT_GRID_SPAN_COUNT)
        seatRecyclerview.isNestedScrollingEnabled = false

        seatAdapter = SeatAdapter(mutableListOf()) { selected ->
            selectedSeats = selected
            updateSummary()
        }
        seatRecyclerview.adapter = seatAdapter
    }

    private fun setupPayButton() {
        payButton.setOnClickListener {
            if (selectedSeats.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val accountId = sessionManager.getUserId()
            if (accountId.isNullOrBlank()) {
                Toast.makeText(this, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val date = dateAdapter.getSelected()
            val time = timeAdapter.getSelected()
            if (time == null){
                Toast.makeText(this, "Suất chiếu này đã bắt đầu, vui lòng chọn suất chiếu khác",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Chuyển sang màn chọn bắp/nước trước khi thực sự giữ ghế + tạo booking.
            // Việc giữ ghế (holdSeats) sẽ được thực hiện ở bước tiếp theo (ComboSelectionActivity),
            // dù người dùng chọn mua bắp nước hay bấm "Bỏ qua".
            val intent = Intent(this, ComboSelectionActivity::class.java).apply {
                putExtra("film", film)
                putExtra("accountId", accountId)
                putExtra("date", date.fullDate)
                putExtra("time", time)
                putExtra("seats", ArrayList(selectedSeats))
                putExtra("seatsTotalPrice", calculateTotal())
            }
            startActivity(intent)
        }
    }

    private fun loadSeats(date: ShowDateModel, time: String?) {
        if (time == null){
            seatRecyclerview.visibility = View.GONE
            emptySeatText.text = "Suất chiếu trong ngày hôm nay đã không còn, vui lòng chọn ngày khác"
            emptySeatText.visibility = View.VISIBLE
            seatAdapter.updateSeats(mutableListOf())
            selectedSeats = emptyList()
            updateSummary()
            return
        }

        seatRecyclerview.visibility = View.VISIBLE
        emptySeatText.text = "Suất chiếu này hiện chưa có sơ đồ ghế"
        emptySeatText.visibility = View.GONE

        viewModel.loadSeats(film.id, date.fullDate, time).observe(this) { result ->
            if (result.isError) {
                Toast.makeText(this, "Không tải được sơ đồ ghế, thử lại sau", Toast.LENGTH_SHORT).show()
            }
            seatAdapter.updateSeats(result.data)

            val isEmpty = result.data.isEmpty()
            seatRecyclerview.visibility = if (isEmpty) View.GONE else View.VISIBLE
            emptySeatText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }

    private fun calculateTotal(): Double {
        return selectedSeats.sumOf { seat -> if (seat.price > 0) seat.price else film.price }
    }

    private fun updateSummary() {
        val total = calculateTotal()
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        priceText.text = formatter.format(total)
        numberSelectText.text = "${selectedSeats.size} ghế đã chọn"
    }
}