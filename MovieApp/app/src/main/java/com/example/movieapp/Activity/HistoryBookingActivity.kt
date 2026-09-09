package com.example.movieapp.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movieapp.R
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movieapp.Api.ApiService
import com.example.movieapp.BottomNavigationBar
import com.example.movieapp.Domain.BookingModel
import com.example.movieapp.Domain.BookingStatus
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.Utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HistoryBookingActivity : AppCompatActivity() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookingHistoryScreen (
                api = apiService,
                currentUserId = sessionManager.getUserId(),
                onBackClick = { finish() }
            )
        }
    }
}

@Composable
fun BookingHistoryScreen(
    api: ApiService,
    currentUserId: String?,
    onBackClick: () -> Unit
){
    val context = LocalContext.current

    var bookings by remember { mutableStateOf<List<BookingModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var posterMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val accountId = currentUserId
        if (accountId.isNullOrBlank()){
            isLoading = false
            errorMessage = "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
            return@LaunchedEffect
        }

        api.getBookingsByAccount(accountId).enqueue(object : Callback<List<BookingModel>>{
            override fun onResponse(call: Call<List<BookingModel>>, response: Response<List<BookingModel>>) {
                isLoading = false
                bookings = (response.body() ?: emptyList()).sortedByDescending { it.createdAt }
            }

            override fun onFailure(call: Call<List<BookingModel>>, t: Throwable){
                isLoading = false
                errorMessage = "Lỗi kết nối: ${t.message}"
            }
        })
    }

    LaunchedEffect(Unit) {
        api.getFilms().enqueue(object : Callback<List<FilmItemModel>>{
            override fun onResponse(call: Call<List<FilmItemModel>>, response: Response<List<FilmItemModel>>){
                posterMap = (response.body() ?: emptyList()).associate { it.id to it.Poster }
            }
            override fun onFailure(call: Call<List<FilmItemModel>>, t: Throwable){
                posterMap = emptyMap()
            }
        })
    }

    Scaffold(
        bottomBar = { BottomNavigationBar() },
        backgroundColor = colorResource(R.color.blackBackground)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colorResource(R.color.blackBackground))
                .padding(top = 20.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                 Text(
                     text = "Lịch sử Đặt Vé",
                     color = Color.White,
                     fontSize = 25.sp,
                     fontWeight = FontWeight.Bold,
                     modifier = Modifier.fillMaxWidth(),
                     textAlign = TextAlign.Center
                 )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    errorMessage != null -> {
                        Text(text = errorMessage ?: "", color = Color.Red, fontSize = 14.sp)
                    }
                    bookings.isEmpty() ->{
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ConfirmationNumber,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(56.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Bạn chưa có lịch sử đặt vé nào",
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(bookings){ booking ->
                                BookingCard(booking = booking, posterUrl = posterMap[booking.filmId])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: BookingModel,
    posterUrl: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorResource(R.color.black3))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 70.dp, height = 100.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2A2A2A))
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.ConfirmationNumber,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = booking.filmTitle.ifBlank {
                    "Phim không xác định"
                },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${booking.date} - ${booking.time}",
                color = Color(0xFFBDBDBD),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ghế: ${
                    booking.seats.joinToString(", ")
                }",
                color = Color(0xFFBDBDBD),
                fontSize = 13.sp
            )

            if (booking.combos.isNotEmpty()) {

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Bắp & nước:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                booking.combos.forEach { combo ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${combo.name} x${combo.quantity}",
                            color = Color(0xFFBDBDBD),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = formatCurrency(combo.subtotal),
                            color = Color(0xFFBDBDBD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(booking.totalPrice),
                    color = Color(0xFFE57373),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                StatusBadge(
                    status = booking.status
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String){
    val (label, color) = when(status){
        BookingStatus.CONFIRMED -> "Đã xác nhận" to Color(0xFF81C784)
        BookingStatus.PENDING -> "chỜ xác nhận" to Color(0xFFFFB74D)
        else -> status to Color.Gray
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${format.format(amount)}"
}
















