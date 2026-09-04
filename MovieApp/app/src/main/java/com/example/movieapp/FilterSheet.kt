package com.example.movieapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Các kiểu sắp xếp cho danh sách phim khi đang tìm kiếm/lọc. */
enum class MovieSortOption(val label: String) {
    RATING_DESC("Đánh giá cao nhất"),
    YEAR_DESC("Mới nhất"),
    YEAR_ASC("Cũ nhất"),
    NAME_ASC("Tên A-Z")
}

/**
 * Trạng thái bộ lọc hiện tại. `yearRange = null` nghĩa là chưa giới hạn năm
 * (đang lấy toàn bộ khoảng năm có trong dữ liệu).
 */
data class MovieFilterState(
    val selectedGenres: Set<String> = emptySet(),
    val minRating: Float = 0f,
    val yearRange: ClosedFloatingPointRange<Float>? = null,
    val sortOption: MovieSortOption = MovieSortOption.RATING_DESC
) {
    val isDefault: Boolean
        get() = selectedGenres.isEmpty() &&
                minRating <= 0f &&
                yearRange == null &&
                sortOption == MovieSortOption.RATING_DESC

    /** Số điều kiện lọc đang bật, hiển thị dạng badge trên icon bộ lọc. */
    val activeCount: Int
        get() {
            var count = selectedGenres.size
            if (minRating > 0f) count++
            if (yearRange != null) count++
            if (sortOption != MovieSortOption.RATING_DESC) count++
            return count
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    availableGenres: List<String>,
    yearBounds: IntRange,
    currentFilter: MovieFilterState,
    onFilterChange: (MovieFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val fullYearRange = yearBounds.first.toFloat()..yearBounds.last.toFloat()
    val currentYearRange = currentFilter.yearRange ?: fullYearRange

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1c1c24)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Bộ lọc phim",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            // ---- Thể loại (đa chọn) — bấm là lọc ngay ----
            Text("Thể loại", color = Color(0xffffc107), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (availableGenres.isEmpty()) {
                Text("Chưa có dữ liệu thể loại", color = Color(0xffbdbdbd), fontSize = 13.sp)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableGenres.forEach { genre ->
                        val isSelected = genre in currentFilter.selectedGenres
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newGenres = if (isSelected) {
                                    currentFilter.selectedGenres - genre
                                } else {
                                    currentFilter.selectedGenres + genre
                                }
                                onFilterChange(currentFilter.copy(selectedGenres = newGenres))
                            },
                            label = { Text(genre) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0x20ffffff),
                                labelColor = Color.White,
                                selectedContainerColor = Color(0xffffc107),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // ---- Đánh giá tối thiểu — kéo là lọc ngay ----
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Đánh giá tối thiểu: " + if (currentFilter.minRating <= 0f) "Tất cả" else
                    String.format(Locale.US, "%.1f ★", currentFilter.minRating),
                color = Color(0xffffc107), fontWeight = FontWeight.Bold, fontSize = 14.sp
            )
            Slider(
                value = currentFilter.minRating,
                onValueChange = { onFilterChange(currentFilter.copy(minRating = it)) },
                valueRange = 0f..10f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xffffc107),
                    activeTrackColor = Color(0xffffc107)
                )
            )

            // ---- Khoảng năm phát hành (chỉ hiện nếu dữ liệu có ít nhất 2 năm khác nhau) ----
            if (yearBounds.first < yearBounds.last) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Năm phát hành: ${currentYearRange.start.toInt()} - ${currentYearRange.endInclusive.toInt()}",
                    color = Color(0xffffc107), fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
                RangeSlider(
                    value = currentYearRange,
                    onValueChange = { newRange ->
                        onFilterChange(
                            currentFilter.copy(
                                yearRange = if (newRange == fullYearRange) null else newRange
                            )
                        )
                    },
                    valueRange = fullYearRange,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xffffc107),
                        activeTrackColor = Color(0xffffc107)
                    )
                )
            }

            // ---- Sắp xếp — chọn là áp dụng ngay ----
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sắp xếp theo", color = Color(0xffffc107), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            MovieSortOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFilterChange(currentFilter.copy(sortOption = option)) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentFilter.sortOption == option,
                        onClick = { onFilterChange(currentFilter.copy(sortOption = option)) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xffffc107),
                            unselectedColor = Color(0xffbdbdbd)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(option.label, color = Color.White, fontSize = 14.sp)
                }
            }

            // ---- Nút hành động: mọi lựa chọn ở trên đã áp dụng ngay lúc bấm/kéo,
            // 2 nút này chỉ còn tác dụng "đặt lại toàn bộ" và "đóng sheet" ----
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onFilterChange(MovieFilterState()) },
                    modifier = Modifier.weight(1f)
                ) { Text("Đặt lại") }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("Xong") }
            }
        }
    }
}

/**
 * Dải chip tóm tắt các điều kiện lọc đang bật, cho phép bấm X để gỡ nhanh
 * từng điều kiện mà không cần mở lại bottom sheet.
 */
@Composable
fun ActiveFilterChipsRow(
    filterState: MovieFilterState,
    onRemoveGenre: (String) -> Unit,
    onClearRating: () -> Unit,
    onClearYearRange: () -> Unit,
    onClearSort: () -> Unit,
    onClearAll: () -> Unit
) {
    if (filterState.isDefault) return

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filterState.selectedGenres.forEach { genre ->
            RemovableChip(text = genre) { onRemoveGenre(genre) }
        }
        if (filterState.minRating > 0f) {
            RemovableChip(
                text = "≥ ${String.format(Locale.US, "%.1f", filterState.minRating)}★"
            ) { onClearRating() }
        }
        filterState.yearRange?.let { range ->
            RemovableChip(text = "${range.start.toInt()} - ${range.endInclusive.toInt()}") { onClearYearRange() }
        }
        if (filterState.sortOption != MovieSortOption.RATING_DESC) {
            RemovableChip(text = filterState.sortOption.label) { onClearSort() }
        }
        RemovableChip(text = "Xoá tất cả", tint = Color(0xffff5252), onRemove = onClearAll)
    }
}

@Composable
private fun RemovableChip(text: String, tint: Color = Color(0xffffc107), onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .background(color = Color(0x20ffffff), shape = RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = tint, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Xoá điều kiện lọc",
            tint = tint,
            modifier = Modifier
                .size(14.dp)
                .clickable { onRemove() }
        )
    }
}
