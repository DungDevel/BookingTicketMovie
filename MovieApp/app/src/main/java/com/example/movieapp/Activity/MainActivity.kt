package com.example.movieapp.Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movieapp.BottomNavigationBar
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.FilmItem
import com.example.movieapp.ActiveFilterChipsRow
import com.example.movieapp.FilterSheet
import com.example.movieapp.MovieFilterState
import com.example.movieapp.MovieSortOption
import com.example.movieapp.R
import com.example.movieapp.R.color.blackBackground
import com.example.movieapp.R.drawable.bg1
import com.example.movieapp.SearchBar
import com.example.movieapp.ViewModel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(onItemClick = { item ->
                val intent = Intent(this, DetailMovieActivity::class.java)
                intent.putExtra("object", item)
                startActivity(intent)
            })
        }
    }
}

@Composable
fun MainScreen(onItemClick: (FilmItemModel) -> Unit = {}){
    Scaffold(
        bottomBar = { BottomNavigationBar() },
        backgroundColor = colorResource(R.color.blackBackground)
    ) {
            paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .background(color = colorResource(blackBackground))
        )
        {
            Image(
                painter = painterResource(id = bg1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        MainContent(onItemClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(onItemClick: (FilmItemModel) -> Unit){

    val viewModel: MainViewModel = hiltViewModel()
    val upcoming = remember{ mutableStateListOf<FilmItemModel>() }
    val newMoview = remember { mutableStateListOf<FilmItemModel>() }

    var showUpcomingLoad by remember { mutableStateOf(true) }
    var showNewMoviesLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterState by remember { mutableStateOf(MovieFilterState()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUpcoming().observeForever {
            upcoming.clear()
            upcoming.addAll(it)
            showNewMoviesLoading=false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadItems().observeForever {
            newMoview.clear()
            newMoview.addAll(it)
            showUpcomingLoad=false
        }
    }

    val isSearching = searchQuery.isNotBlank()
    val hasActiveFilters = !filterState.isDefault
    val isBrowsing = isSearching || hasActiveFilters
    val allFilms = remember(upcoming.toList(), newMoview.toList()) {
        (newMoview + upcoming).distinctBy { it.id }
    }


    val availableGenres = remember(allFilms) {
        allFilms.flatMap { it.Genre }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val yearBounds = remember(allFilms) {
        val years = allFilms.map { it.Year }.filter { it > 0 }
        if (years.isEmpty()) {
            val current = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            (current - 1)..current
        } else {
            val min = years.min()
            val max = years.max()
            if (min == max) min..(max + 1) else min..max
        }
    }

    val displayedFilms = remember(allFilms, searchQuery, filterState) {
        if (!isBrowsing) {
            emptyList()
        } else {
            var list = allFilms

            if (isSearching) {
                val normalizedQuery = normalizeForSearch(searchQuery)
                list = list.filter { film ->
                    normalizeForSearch(film.Title).contains(normalizedQuery) ||
                            film.Genre.any { genre -> normalizeForSearch(genre).contains(normalizedQuery) }
                }
            }

            if (filterState.selectedGenres.isNotEmpty()) {
                list = list.filter { film -> film.Genre.any { it in filterState.selectedGenres } }
            }

            if (filterState.minRating > 0f) {
                list = list.filter { it.Imdb >= filterState.minRating }
            }

            filterState.yearRange?.let { range ->
                list = list.filter { it.Year.toFloat() in range }
            }

            when (filterState.sortOption) {
                MovieSortOption.RATING_DESC -> list.sortedByDescending { it.Imdb }
                MovieSortOption.YEAR_DESC -> list.sortedByDescending { it.Year }
                MovieSortOption.YEAR_ASC -> list.sortedBy { it.Year }
                MovieSortOption.NAME_ASC -> list.sortedBy { normalizeForSearch(it.Title) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 60.dp, bottom = 100.dp)
    ) {
        Text(
            text = "Bạn muốn xem gì",
            style = TextStyle(color = Color.White, fontSize = 25.sp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(start = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                hint = "Tìm phim theo tên, thể loại...",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BadgedBox(
                badge = {
                    if (filterState.activeCount > 0) {
                        Badge(containerColor = Color(0xffffc107)) {
                            Text(text = "${filterState.activeCount}", color = Color.Black)
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier
                        .size(50.dp)
                        .background(color = Color(0x20ffffff), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "Bộ lọc",
                        tint = Color.White
                    )
                }
            }
        }

        ActiveFilterChipsRow(
            filterState = filterState,
            onRemoveGenre = { genre ->
                filterState = filterState.copy(selectedGenres = filterState.selectedGenres - genre)
            },
            onClearRating = { filterState = filterState.copy(minRating = 0f) },
            onClearYearRange = { filterState = filterState.copy(yearRange = null) },
            onClearSort = { filterState = filterState.copy(sortOption = MovieSortOption.RATING_DESC) },
            onClearAll = { filterState = MovieFilterState() }
        )

        if (showFilterSheet) {
            FilterSheet(
                availableGenres = availableGenres,
                yearBounds = yearBounds,
                currentFilter = filterState,
                onFilterChange = { filterState = it },
                onDismiss = { showFilterSheet = false }
            )
        }

        if (isBrowsing) {
            SectionTitle(
                if (isSearching) "Kết quả tìm kiếm cho \"$searchQuery\"" else "Kết quả lọc phim"
            )

            if (showUpcomingLoad || showNewMoviesLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (displayedFilms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy phim nào phù hợp",
                        style = TextStyle(color = Color(0xffbdbdbd), fontSize = 14.sp)
                    )
                }
            } else {
                // Lưới thủ công (không dùng LazyVerticalGrid) vì đang nằm trong 1 Column
                // đã verticalScroll ở ngoài -> lồng 2 lớp cuộn dọc sẽ crash.
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayedFilms.chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { item ->
                                FilmItem(item, onItemClick)
                            }
                        }
                    }
                }
            }

            return@Column
        }

        SectionTitle("Phim mới")
        if (showNewMoviesLoading){
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(newMoview) { item ->
                    FilmItem(item, onItemClick)
                }
            }
        }

        SectionTitle("Phim sắp tới")

        if (showUpcomingLoad){
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(upcoming) { item ->
                    FilmItem(item, onItemClick)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String){
    Text(
        text = title,
        style = TextStyle(color = Color(0xffffc107), fontSize = 18.sp),
        modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 8.dp),
        fontWeight = FontWeight.Bold
    )
}

/**
 * Chuẩn hoá chuỗi để so khớp tìm kiếm: bỏ dấu tiếng Việt (bao gồm cả "đ"/"Đ" vốn
 * không tự tách dấu qua NFD) và chuyển về chữ thường, giúp gõ không dấu vẫn tìm ra.
 */
private fun normalizeForSearch(text: String): String {
    val withoutDBar = text.replace('đ', 'd').replace('Đ', 'D')
    val decomposed = java.text.Normalizer.normalize(withoutDBar, java.text.Normalizer.Form.NFD)
    return decomposed.replace(Regex("\\p{Mn}+"), "").lowercase()
}