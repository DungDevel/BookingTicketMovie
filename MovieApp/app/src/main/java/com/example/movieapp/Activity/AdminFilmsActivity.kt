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

object FilmSource {
    const val ITEM  = "item"
    const val UPCOMING = "upcoming"
}

data class AdminFilmRow(val film: FilmItemModel, val source: String)

data class AdminFilmGroup(
    val title: String,
    val poster: String,
    val price: Double,
    val entries: List<AdminFilmRow>
)

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
                onEditFilm = { group ->
                    val itemEntry = group.entries.find { it.source == FilmSource.ITEM }
                    val upcomingEntry = group.entries.find { it.source == FilmSource.UPCOMING }
                    val filmForForm = itemEntry?.film ?: upcomingEntry?.film

                    val intent = Intent(this, AdminFilmFormActivity::class.java)
                    intent.putExtra("film", filmForForm)
                    intent.putExtra("itemId", itemEntry?.film?.id)
                    intent.putExtra("upcomingId", upcomingEntry?.film?.id)
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
    onEditFilm: (AdminFilmGroup) -> Unit
) {
    val context = LocalContext.current
    var rows by remember { mutableStateOf<List<AdminFilmRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf<String?>(null)}

    fun loadFilms() {
        isLoading = true
        var itemResult: List<FilmItemModel>? = null
        var upcomingResult: List<FilmItemModel>? = null
        var pending = 2

        fun tryFinish(){
            pending -= 1
            if (pending == 0){
                isLoading = false
                val merged = mutableListOf<AdminFilmRow>()
                itemResult?.forEach { merged.add(AdminFilmRow(it, FilmSource.ITEM)) }
                upcomingResult?.forEach { merged.add(AdminFilmRow(it, FilmSource.UPCOMING)) }
                rows = merged
            }
        }

        api.getItems().enqueue(object : Callback<List<FilmItemModel>> {
            override fun onResponse(call: Call<List<FilmItemModel>>, response: Response<List<FilmItemModel>>) {
                itemResult = response.body() ?: emptyList()
                tryFinish()
            }
            override fun onFailure(call: Call<List<FilmItemModel>>, t: Throwable) {
                itemResult = emptyList()
                tryFinish()
            }
        })
        api.getUpcoming().enqueue(object : Callback<List<FilmItemModel>>{
            override fun onResponse(call: Call<List<FilmItemModel>>, response: Response<List<FilmItemModel>>){
                upcomingResult = response.body() ?: emptyList()
                tryFinish()
            }
            override fun onFailure(call: Call<List<FilmItemModel>>, t: Throwable){
                upcomingResult = emptyList()
                tryFinish()
            }
        })
    }

    LaunchedEffect(Unit) { loadFilms() }

    fun deleteFilm(row: AdminFilmRow) {
        val call = if (row.source == FilmSource.ITEM){
            api.deleteItem(row.film.id)
        } else{
            api.deleteUpcomming(row.film.id)
        }
        call.enqueue(object : Callback<Void>{
            override fun onResponse(call: Call<Void>, response: Response<Void>){
                if (response.isSuccessful){
                    Toast.makeText(context, "Đã xoá phim", Toast.LENGTH_SHORT).show()
                    loadFilms()
                } else{
                    Toast.makeText(context, "Xoá thất bại", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable){
                Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    val groups: List<AdminFilmGroup> = remember(rows, selectedFilter){
        val base = if (selectedFilter == null) rows else rows.filter { it.source == selectedFilter }
        base.groupBy { it.film.Title }
            .map { (title, entries) ->
                AdminFilmGroup(
                    title = title,
                    poster = entries.first().film.Poster,
                    price = entries.first().film.price,
                    entries = entries.sortedBy { it.source }
                )
            }
    }

    val filteredRows = remember(rows, selectedFilter) {
        if (selectedFilter == null) rows else rows.filter { it.source == selectedFilter }
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
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                FilterChip(
                    text = "Đang chiếu",
                    selected = selectedFilter == FilmSource.ITEM,
                    onClick = { selectedFilter = FilmSource.ITEM }
                )
                FilterChip(
                    text = "Sắp chiếu",
                    selected = selectedFilter == FilmSource.UPCOMING,
                    onClick = { selectedFilter = FilmSource.UPCOMING }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (filteredRows.isEmpty()) {
                Text(text = "Chưa có phim nào", color = Color.Gray, fontSize = 14.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(groups, key = { it.title }) { group ->
                        FilmGroupRow(
                            group = group,
                            onEdit = { onEditFilm(group) },
                            onDelete = { row -> deleteFilm(row) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(source: String){
    val (label, color) = if (source == FilmSource.ITEM){
        "Đang chiếu" to Color(0xFF81C784)
    } else {
        "Sắp chiếu" to Color(0xFF64B5F6)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ){
        Text(
            text = label,
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
private fun FilmGroupRow(
    group: AdminFilmGroup,
    onEdit: () -> Unit,
    onDelete: (AdminFilmRow) -> Unit
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
            model = group.poster,
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
                    text = group.title,
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
                text = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(group.price),
                color = Color(0xFFE57373),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            group.entries.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(2.dp)
                ) {
                    SourceBadge(source = entry.source)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Xoá",
                        color = Color(0xFFE57373),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable{ onDelete(entry) }.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}




















