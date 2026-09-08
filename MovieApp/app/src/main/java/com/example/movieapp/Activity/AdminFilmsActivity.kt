package com.example.movieapp.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.R
import com.example.movieapp.Utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

enum class AdminFilmFilter { ALL, NOW_SHOWING, UPCOMING }

@AndroidEntryPoint
class AdminFilmsActivity : BaseActivity() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Bạn không có quyền truy cập trang này", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContent {
            AdminFilmsScreen(
                api = apiService,
                onBackClick = { finish() },
                onAddFilm = { startActivity(Intent(this, AdminFilmFormActivity::class.java)) },
                onEditFilm = { film ->
                    val intent = Intent(this, AdminFilmFormActivity::class.java)
                    intent.putExtra("film", film)
                    startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun AdminFilmsScreen(
    api: ApiService,
    onBackClick: () -> Unit,
    onAddFilm: () -> Unit,
    onEditFilm: (FilmItemModel) -> Unit
) {
    val context = LocalContext.current
    var films by remember { mutableStateOf<List<FilmItemModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(AdminFilmFilter.ALL)}

    fun loadFilms() {
        isLoading = true

        api.getFilms().enqueue(object : Callback<List<FilmItemModel>>{
            override fun onResponse(call: Call<List<FilmItemModel>>, response: Response<List<FilmItemModel>>){
                isLoading = false
                films = response.body() ?: emptyList()
            }
            override fun onFailure(call: Call<List<FilmItemModel>>, t: Throwable){
                isLoading = false
                films = emptyList()
            }
        })
    }

    LaunchedEffect(Unit) { loadFilms() }

    fun deleteFilm(film: FilmItemModel) {
        api.deleteItem(film.id).enqueue(object : Callback<Void>{
            override fun onResponse(call: Call<Void>, response: Response<Void>){
                if (response.isSuccessful){
                    Toast.makeText(context, "Đã xoá phim", Toast.LENGTH_SHORT).show()
                    loadFilms()
                } else {
                    Toast.makeText(context, "Xoá thất bại", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable){
                Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT)
            }
        })
    }

    val filteredFilms = remember(films, selectedFilter) {
        when (selectedFilter){
            AdminFilmFilter.ALL -> films
            AdminFilmFilter.NOW_SHOWING -> films.filter { it.IsNowShowing }
            AdminFilmFilter.UPCOMING -> films.filter { it.IsUpcoming }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.blackBackground))
            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    contentDescription = "",
                    painter = painterResource(R.drawable.back),
                    modifier = Modifier.clickable { onBackClick() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Quản Lý Phim",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "+ Thêm phim",
                    color = Color(0xFF64B5F6),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onAddFilm() }
                        .padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Danh sách quản lý phim",
                color = Color.Gray,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)){
                FilterChip(
                    text = "Tất cả",
                    selected = selectedFilter == AdminFilmFilter.ALL,
                    onClick = { selectedFilter = AdminFilmFilter.ALL }
                )
                FilterChip(
                    text = "Đang chiếu",
                    selected = selectedFilter == AdminFilmFilter.NOW_SHOWING,
                    onClick = { selectedFilter = AdminFilmFilter.NOW_SHOWING }
                )
                FilterChip(
                    text = "Sắp chiếu",
                    selected = selectedFilter == AdminFilmFilter.UPCOMING,
                    onClick = { selectedFilter = AdminFilmFilter.UPCOMING }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (filteredFilms.isEmpty()) {
                Text(text = "Chưa có phim nào", color = Color.Gray, fontSize = 14.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredFilms, key = { it.id }) { film ->
                        FilmRow(
                            film = film,
                            onEdit = { onEditFilm(film) },
                            onDelete = { deleteFilm(film) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color){
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ){
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit){
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF64B5F6) else Color(0xFF2A2A2A))
            .clickable{ onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun FilmRow(
    film: FilmItemModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorResource(R.color.black3))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = film.Poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = film.Title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Sửa",
                    color = Color(0xFF64B5F6),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable{ onEdit() }.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(film.price),
                color = Color(0xFFE57373),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (film.IsNowShowing) {
                        StatusBadge(
                            text = "Đang chiếu",
                            color = Color(0xFF81C784)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (film.IsUpcoming) {
                        StatusBadge(
                            text = "Sắp chiếu",
                            color = Color(0xFF64B5F6)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Xoá",
                        color = Color(0xFFE57373),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable{ onDelete() }.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
            }
        }
    }
}




















