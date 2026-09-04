package com.example.movieapp.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.ProfileModel
import com.example.movieapp.Domain.RatingModel
import com.example.movieapp.R
import com.example.movieapp.Utils.SessionManager
import com.example.movieapp.ViewModel.ReviewViewModel
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MovieReviewActivity : BaseActivity() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filmId = intent.getStringExtra("filmId") ?: ""
        val filmTitle = intent.getStringExtra("filmTitle") ?: ""

        if (filmId.isBlank()) {
            Toast.makeText(this, "Không xác định được phim", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MovieReviewScreen(
                api = apiService,
                currentUserId = sessionManager.getUserId(),
                filmId = filmId,
                filmTitle = filmTitle,
                onBackClick = { finish() }
            )
        }
    }
}

@Composable
fun MovieReviewScreen(
    api: ApiService,
    currentUserId: String?,
    filmId: String,
    filmTitle: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: ReviewViewModel = hiltViewModel()

    var reviews by remember { mutableStateOf<List<RatingModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUserName by remember { mutableStateOf("") }

    val myReview = remember(reviews, currentUserId) {
        reviews.find { it.accountId == currentUserId }
    }

    var selectedStars by remember { mutableStateOf(5) }
    var commentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isEditingMyReview by remember { mutableStateOf(false) }

    fun loadReviews() {
        isLoading = true
        viewModel.loadReviews(filmId).observe(lifecycleOwner) { result ->
            isLoading = false
            if (!result.isError) {
                reviews = result.data.sortedByDescending { it.createAt }
            }
        }
    }

    // Nạp tên hiển thị của người dùng hiện tại (để lưu kèm review, tránh phải join Profile mỗi lần hiển thị)
    LaunchedEffect(Unit) {
        loadReviews()

        val accountId = currentUserId
        if (!accountId.isNullOrBlank()) {
            api.getProfile().enqueue(object : Callback<List<ProfileModel>> {
                override fun onResponse(call: Call<List<ProfileModel>>, response: Response<List<ProfileModel>>) {
                    val profile = response.body()?.find { it.accountId == accountId }
                    currentUserName = profile?.name?.ifBlank { "Người dùng ẩn danh" } ?: "Người dùng ẩn danh"
                }
                override fun onFailure(call: Call<List<ProfileModel>>, t: Throwable) {
                    currentUserName = "Người dùng ẩn danh"
                }
            })
        }
    }

    LaunchedEffect(myReview) {
        if (myReview != null && !isEditingMyReview) {
            selectedStars = myReview.rating
            commentText = myReview.comment
        }
    }

    fun handleSubmit() {
        val accountId = currentUserId
        if (accountId.isNullOrBlank()) {
            Toast.makeText(context, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
            return
        }
        if (commentText.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập bình luận", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val review = RatingModel(
            id = myReview?.id ?: "",
            filmId = filmId,
            accountId = accountId,
            userName = currentUserName.ifBlank { "Người dùng ẩn danh" },
            rating = selectedStars,
            comment = commentText,
            createAt = System.currentTimeMillis()
        )

        viewModel.submitReview(myReview?.id ?: "", review).observe(lifecycleOwner) { result ->
            isSubmitting = false
            if (result.isError) {
                Toast.makeText(context, "Gửi đánh giá thất bại, thử lại sau", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show()
                isEditingMyReview = false
                loadReviews()
            }
        }
    }

    fun handleDelete() {
        val id = myReview?.id ?: return
        viewModel.deleteReview(id).observe(lifecycleOwner) { result ->
            if (!result.isError) {
                Toast.makeText(context, "Đã xoá đánh giá", Toast.LENGTH_SHORT).show()
                selectedStars = 5
                commentText = ""
                loadReviews()
            } else {
                Toast.makeText(context, "Xoá thất bại, thử lại sau", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    contentDescription = "",
                    painter = painterResource(R.drawable.back),
                    modifier = Modifier.clickable { onBackClick() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Đánh giá & bình luận", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (filmTitle.isNotBlank()) {
                        Text(text = filmTitle, color = Color(0xFFBDBDBD), fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ---- Tóm tắt điểm trung bình ----
            RatingSummary(reviews = reviews)
            Spacer(modifier = Modifier.height(24.dp))

            // ---- Form viết / sửa đánh giá ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorResource(R.color.black3))
                    .padding(16.dp)
            ) {
                Text(
                    text = if (myReview != null) "Đánh giá của bạn" else "Viết đánh giá",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                StarPicker(
                    selected = selectedStars,
                    onSelect = { selectedStars = it; isEditingMyReview = true }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it; isEditingMyReview = true },
                    placeholder = { Text("Cảm nhận của bạn về bộ phim...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFE57373),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (myReview != null) {
                        Text(
                            text = "Xoá",
                            color = Color(0xFFE57373),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { handleDelete() }
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    GradientButton(
                        text = when {
                            isSubmitting -> "Đang gửi..."
                            myReview != null -> "Cập nhật"
                            else -> "Gửi đánh giá"
                        },
                        onClick = { if (!isSubmitting) handleSubmit() },
                        modifier = Modifier.height(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Tất cả đánh giá (${reviews.size})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                reviews.isEmpty() -> {
                    Text(
                        text = "Chưa có đánh giá nào cho phim này. Hãy là người đầu tiên!",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        reviews.forEach { review ->
                            ReviewCard(review = review, isMine = review.accountId == currentUserId)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun RatingSummary(reviews: List<RatingModel>) {
    val average = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorResource(R.color.black3))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (reviews.isEmpty()) "—" else String.format(Locale.US, "%.1f", average),
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < average.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (reviews.isEmpty()) "Chưa có đánh giá" else "${reviews.size} đánh giá",
                color = Color(0xFFBDBDBD),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StarPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row {
        for (star in 1..5) {
            Icon(
                imageVector = if (star <= selected) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Chọn $star sao",
                tint = Color(0xFFFFB74D),
                modifier = Modifier
                    .size(32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(star) }
            )
        }
    }
}

@Composable
private fun ReviewCard(review: RatingModel, isMine: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorResource(R.color.black3))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9575CD)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = review.userName.ifBlank { "Người dùng ẩn danh" } + if (isMine) " (Bạn)" else "",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatReviewDate(review.createAt),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
            Row {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < review.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        if (review.comment.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = review.comment, color = Color(0xFFE0E0E0), fontSize = 13.sp)
        }
    }
}

private fun formatReviewDate(millis: Long): String {
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(millis))
    } catch (e: Exception) {
        ""
    }
}