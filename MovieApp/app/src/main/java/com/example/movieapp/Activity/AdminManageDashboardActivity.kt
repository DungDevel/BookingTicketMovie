package com.example.movieapp.Activity

import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.AccountModel
import com.example.movieapp.Domain.BookingModel
import com.example.movieapp.Domain.BookingStatus
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.R
import com.example.movieapp.Utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class TimeRange(val label: String, val days: Int?){
    TODAY("Hôm nay", 0),
    WEEK("7 ngày", 7),
    MONTH("30 ngày", 30),
    ALL("Tất cả", null)
}

@AndroidEntryPoint
class AdminManageDashboardActivity : AppCompatActivity() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionManager.isAdmin()){
            Toast.makeText(this, "Bạn không có quyền truy cập trang ny", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContent {
            AdminManageDashboardScreen(apiService = apiService, onBackClick = { finish() })
        }
    }
}

@Composable
fun AdminManageDashboardScreen(apiService: ApiService, onBackClick: () -> Unit){
    var bookings by remember { mutableStateOf<List<BookingModel>>(emptyList()) }
    var totalFilmCount by remember { mutableStateOf(0) }
    var totalAccountCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedRange by remember { mutableStateOf(TimeRange.WEEK) }

    fun load(){
        isLoading = true
        var pending = 3

        fun tryFinsh(){
            pending -= 1
            if (pending == 0) isLoading = false
        }
        apiService.getAllBookings().enqueue(object : Callback<List<BookingModel>>{
            override fun onResponse(call: Call<List<BookingModel>>, response: Response<List<BookingModel>>){
                bookings = response.body() ?: emptyList()
                tryFinsh()
            }
            override fun onFailure(call: Call<List<BookingModel>>, t : Throwable){
                bookings = emptyList()
                tryFinsh()
            }
        })
        apiService.getFilms().enqueue(object : Callback<List<FilmItemModel>>{
            override fun onResponse(call: Call<List<FilmItemModel>>, response: Response<List<FilmItemModel>>){
                totalFilmCount = response.body()?.size ?: 0
                tryFinsh()
            }
            override fun onFailure(call: Call<List<FilmItemModel>>, t : Throwable){
                tryFinsh()
            }
        })

        apiService.getAccounts().enqueue(object : Callback<List<AccountModel>>{
            override fun onResponse(call: Call<List<AccountModel>>, response: Response<List<AccountModel>>){
                totalAccountCount = response.body()?.size ?: 0
                tryFinsh()
            }
            override fun onFailure(call: Call<List<AccountModel>>, t: Throwable){
                tryFinsh()
            }
        })
    }

    LaunchedEffect(Unit) { load() }

    val filteredConfirmed = remember(bookings, selectedRange){
        val confirmed = bookings.filter { it.status == BookingStatus.CONFIRMED }
        val days = selectedRange.days
        if (days == null){
            confirmed
        } else {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_YEAR, -days)
            val fromMillis = cal.timeInMillis
            confirmed.filter { it.createdAt >= fromMillis }
        }
    }

    val totalRevenue = remember(filteredConfirmed) { filteredConfirmed.sumOf { it.totalPrice } }
    val totalTickets = remember(filteredConfirmed) { filteredConfirmed.sumOf { it.seats.size } }

    data class FilmRevenue(val title: String, val revenue: Double, val tickets: Int)
    val topFilms = remember(filteredConfirmed){
        filteredConfirmed.groupBy { it.filmTitle.ifBlank { it.filmId } }
            .map { (title, list) ->
                FilmRevenue(title, list.sumOf { it.totalPrice }, list.sumOf { it.seats.size })
            }
            .sortedByDescending { it.revenue }
            .take(5)
    }

    val dayFormat = remember { SimpleDateFormat("dd/MM", Locale("vi", "VN")) }
    val last7DaysRevenue = remember(bookings) {
        val confirmed = bookings.filter { it.status == BookingStatus.CONFIRMED }
        val cal = Calendar.getInstance()
        val result = mutableListOf<Pair<String, Double>>()
        for (i in 6 downTo 0){
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0); dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0); dayCal.set(Calendar.MILLISECOND, 0)

            val startMillis = dayCal.timeInMillis
            val endMillis = startMillis + 24 * 60 * 60 * 1000L
            val revenue = confirmed.filter { it.createdAt in startMillis until endMillis }
                .sumOf { it.totalPrice }
            result.add(dayFormat.format(Date(startMillis)) to revenue)
        }
        result
    }

    val currency = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.blackBackground))
            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "<-",
                    color = Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier.clickable{ onBackClick() }.padding(end = 12.dp)
                )
                Text(
                    text = "Thống Kê Doanh Thu",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeRange.entries.forEach { range ->
                    RangeChip(
                        text = range.label,
                        selected = selectedRange == range,
                        onClick = { selectedRange = range }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading){
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ){
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Doanh thu",
                        value = "${currency.format(totalRevenue)} đ",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Vé đã bán",
                        value = "$totalTickets",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        title = "Tổng số phim",
                        value = "$totalFilmCount",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Tài khoản",
                        value = "$totalAccountCount",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Doanh thu 7 ngày gần nhất",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                RevenueLineChart(data = last7DaysRevenue, currency = currency)

                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Top 5 phim doanh thu cao nhất",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (topFilms.isEmpty()){
                    Text(
                        text = "Chưa có dữ liệu trong khoảng thời gian này",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                } else {
                    val maxRevenue = topFilms.maxOf { it.revenue }.coerceAtLeast(1.0)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        topFilms.forEach { film ->
                            TopFilmBar(
                                title = film.title,
                                revenue = film.revenue,
                                tickets = film.tickets,
                                ratio = (film.revenue / maxRevenue).toFloat(),
                                currency = currency
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
){
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFFE57373) else Color(0xFF2A2A2A))
            .clickable{ onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E1E1E))
            .padding(14.dp)
    ) {
        Text(text = title, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
private fun TopFilmBar(title: String, revenue: Double, tickets: Int, ratio: Float, currency: NumberFormat){
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$tickets vé",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF333333))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio.coerceIn(0.03f, 1f))
                    .background(Color(0xFFE57373))
                    .padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${currency.format(revenue)} đ",
            color = Color(0xFFE57373),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RevenueLineChart(data: List<Pair<String, Double>>, currency: NumberFormat){
    val maxVal = (data.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)

    val labelPaintCenter = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val labelPaintLeft = remember {
        android.graphics.Paint(labelPaintCenter).apply { textAlign = android.graphics.Paint.Align.LEFT }
    }
    val labelPaintRight = remember {
        android.graphics.Paint(labelPaintCenter).apply { textAlign = android.graphics.Paint.Align.RIGHT }
    }

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 12.dp)
        ){
            if (data.size < 2) return@Canvas

            val labelSpace = labelPaintCenter.textSize + 20f
            val chartTop = labelSpace
            val chartBottom = size.height - labelSpace
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

            val stepX = size.width / (data.size - 1)
            val points = data.mapIndexed { index, pair ->
                val x = index * stepX
                val y = chartBottom - (pair.second / maxVal * chartHeight).toFloat()
                Offset(x, y)
            }

            for (i in 0 until points.size - 1){
                drawLine(
                    color = Color(0xFFE57373),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 4f
                )
            }

            points.forEachIndexed { index, p ->
                drawCircle(color = Color(0xFFE57373), radius = 6f, center = p)

                val cur = data[index].second
                val prev = if (index > 0) data[index - 1].second else null
                val next = if (index < data.size - 1) data[index + 1].second else null
                val isPeakLike = (prev == null || cur >= prev) && (next == null || cur >= next)

                val revenueText = currency.format(cur)
                val paint = when (index) {
                    0 -> labelPaintLeft
                    data.size - 1 -> labelPaintRight
                    else -> labelPaintCenter
                }

                val textY = if (isPeakLike) {
                    p.y - 16f
                } else {
                    p.y + 16f + paint.textSize
                }

                drawContext.canvas.nativeCanvas.drawText(revenueText, p.x, textY, paint)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (label, _) ->
                Text(text = label, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}