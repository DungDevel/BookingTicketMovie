package com.example.movieapp.Activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.Domain.RatingModel
import com.example.movieapp.R
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.movieapp.Utils.FavoriteManager
import com.example.movieapp.Utils.ReleaseCountdownUtils
import com.example.movieapp.ViewModel.ReviewViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class DetailMovieActivity : BaseActivity() {
    private lateinit var filmItem: FilmItemModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filmItem = (intent.getSerializableExtra("object") as FilmItemModel)

        setContent {
            DetailScreen(filmItem, onBackClick = {finish()})
        }
    }
}

@Composable
fun DetailScreen(film: FilmItemModel, onBackClick:() -> Unit){
    val scrollState = rememberScrollState()
    val isLoading = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFavorite by remember { mutableStateOf(FavoriteManager.isFavorite(context, film)) }

    val reviewViewModel: ReviewViewModel = hiltViewModel()
    var reviews by remember { mutableStateOf<List<RatingModel>>(emptyList()) }

    LaunchedEffect(film.id) {
        reviewViewModel.loadReviews(film.id).observe(lifecycleOwner) { result ->
            if (!result.isError) {
                reviews = result.data
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.blackBackground))
    ){
        if (isLoading.value){
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier.height(400.dp)
                ) {
                    Image(
                        contentDescription = "",
                        painter = painterResource(R.drawable.back),
                        modifier = Modifier
                            .padding(start = 16.dp, top = 48.dp)
                            .clickable { onBackClick() }
                    )
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        tint = if (isFavorite) Color.Red else Color.White,
                        modifier = Modifier
                            .padding(start = 16.dp, top = 48.dp, end = 30.dp)
                            .align(Alignment.TopEnd)
                            .size(35.dp)
                            .clickable{
                                isFavorite = FavoriteManager.toggleFavorite(context, film)
                                Toast.makeText(
                                    context,
                                    if (isFavorite) "Đã thêm vào mục yêu thích" else "Đã xoá khỏi mục yêu thích",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    )
                    AsyncImage(
                        model = film.Poster,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.1f
                    )
                    AsyncImage(
                        model = film.Poster,
                        contentDescription = null,
                        modifier = Modifier
                            .size(210.dp, 300.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .align(Alignment.BottomCenter),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        colorResource(R.color.black2),
                                        colorResource(R.color.black1)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, Float.POSITIVE_INFINITY)
                                )
                            )
                    )
                    Text(
                        text = film.Title,
                        style = TextStyle(color = Color.White, fontSize = 27.sp),
                        modifier = Modifier
                            .padding(end = 16.dp, top = 48.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (film.IsUpcoming) {
                        val countdownText = ReleaseCountdownUtils.getCountdownText(film.ReleaseAt)
                        val releaseDateText = ReleaseCountdownUtils.formatReleaseDate(film.ReleaseAt)
                        if (countdownText != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3364B5F6))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "$countdownText",
                                        color = Color(0xFF64B5F6),
                                        fontSize = 15.sp
                                    )
                                    if (releaseDateText != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Khởi chiếu: $releaseDateText",
                                            color = Color(0xFFBDBDBD),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.star),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(text = "IMDB: ${film.Imdb}", color = Color.White)

                        Icon(
                            painter = painterResource(R.drawable.time),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(text = "Thời gian: ${film.Time}", color = Color.White)

                        Icon(
                            painter = painterResource(R.drawable.cal),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(text = "Năm: ${film.Year}", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorResource(R.color.black1))
                            .clickable {
                                val intent = Intent(context, MovieReviewActivity::class.java).apply {
                                    putExtra("filmId", film.id)
                                    putExtra("filmTitle", film.Title)
                                }
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (reviews.isEmpty()) {
                                    "Chưa có đánh giá"
                                } else {
                                    val avg = reviews.map { it.rating }.average()
                                    String.format(Locale.US, "%.1f", avg) + " · ${reviews.size} đánh giá"
                                },
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Xem & viết đánh giá", color = Color(0xFFBDBDBD), fontSize = 12.sp)
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Giới thiệu", style = TextStyle(color = Color.Yellow, fontSize = 16.sp))

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(film.Description, style = TextStyle(color = Color.White, fontSize = 14.sp))

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Diễn viên", style = TextStyle(color = Color.Yellow, fontSize = 16.sp))

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(contentPadding = PaddingValues(8.dp)) {
                        items(film.Casts.size){
                            film.Casts[it].Actor?.let{
                                Text(
                                    text = "$it, ",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                LazyRow(contentPadding = PaddingValues(8.dp)) {
                    items(film.Casts.size){
                        AsyncImage(
                            model = film.Casts[it].PicUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(75.dp)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorResource(R.color.pink))
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(film.Trailer)
                                )
                                context.startActivity(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶  Xem Trailer",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorResource(R.color.blue))
                            .clickable {
                                val intent = Intent(context, SeatListActivity::class.java)
                                intent.putExtra("object", film)
                                context.startActivity(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đặt Vé",                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}