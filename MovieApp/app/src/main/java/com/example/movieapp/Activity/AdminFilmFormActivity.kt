package com.example.movieapp.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.CastModel
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.R
import com.example.movieapp.Utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class AdminFilmFormActivity : BaseActivity() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Bạn không có quyền truy cập trang này", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val existingFilm = intent.getSerializableExtra("film") as? FilmItemModel
        val existingItemId = intent.getStringExtra("itemId")
        val existingUpcomingId = intent.getStringExtra("upcomingId")

        setContent {
            AdminFilmFormScreen(
                api = apiService,
                existingFilm = existingFilm,
                existingItemId = existingItemId,
                existingUpcomingId = existingUpcomingId,
                onSaved = { finish() },
                onBackClick = { finish() }
            )
        }
    }
}

@Composable
fun AdminFilmFormScreen(
    api: ApiService,
    existingFilm: FilmItemModel?,
    existingItemId: String?,
    existingUpcomingId: String?,
    onSaved: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = existingFilm != null

    var title by remember { mutableStateOf(existingFilm?.Title ?: "") }
    var description by remember { mutableStateOf(existingFilm?.Description ?: "") }
    var poster by remember { mutableStateOf(existingFilm?.Poster ?: "") }
    var time by remember { mutableStateOf(existingFilm?.Time ?: "") }
    var trailer by remember { mutableStateOf(existingFilm?.Trailer ?: "") }
    var imdb by remember { mutableStateOf(existingFilm?.Imdb?.toString() ?: "") }
    var year by remember { mutableStateOf(existingFilm?.Year?.toString() ?: "") }
    var price by remember { mutableStateOf(existingFilm?.price?.toString() ?: "") }
    var genre by remember { mutableStateOf(existingFilm?.Genre?.joinToString(", ") ?: "") }
    var castList by remember { mutableStateOf(existingFilm?.Casts?.toList() ?: emptyList()) }

    var includeInItem by remember { mutableStateOf(existingItemId != null) }
    var includeInUpcoming by remember { mutableStateOf(existingUpcomingId != null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun buildFilm(id: String): FilmItemModel{
        return FilmItemModel(
            id = id,
            Title = title,
            Description = description,
            Poster = poster,
            Time = time,
            Trailer = trailer,
            Imdb = imdb.toDoubleOrNull() ?: 0.0,
            Year = year.toIntOrNull() ?: 0,
            price = price.toDoubleOrNull() ?: 0.0,
            Genre = ArrayList(genre.split(",").map { it.trim() }.filter { it.isNotBlank() }),
            Casts = ArrayList(castList)
        )
    }

    fun handleSave() {
        if (title.isBlank()) {
            errorMessage = "Vui lòng nhập tên phim"
            return
        }
        if (imdb.toDoubleOrNull() == null || year.toIntOrNull() == null || price.toDoubleOrNull() == null) {
            errorMessage = "IMDB, năm và giá phải là số hợp lệ"
            return
        }
        if (!includeInItem && !includeInUpcoming) {
            errorMessage = "Chọn 1 trong 2"
            return
        }

        errorMessage = null
        isSubmitting = true

        var pending = 0
        var hasError = false

        fun onOneDone(success: Boolean) {
            if (!success) hasError = true
            pending -= 1
            if (pending == 0) {
                isSubmitting = false
                if (hasError) {
                    Toast.makeText(
                        context, "Một phần thao tác thất bại, vui lòng kiểm tra lại",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context, if (isEditing) "Đã lưu thay đổi" else "Đã thêm phim",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                onSaved()
            }
        }

        fun submitFilm(call: Call<FilmItemModel>) {
            pending++
            call.enqueue(object : Callback<FilmItemModel> {
                override fun onResponse(call: Call<FilmItemModel>, response: Response<FilmItemModel>) {
                    onOneDone(response.isSuccessful)
                }
                override fun onFailure(call: Call<FilmItemModel>, t: Throwable) {
                    onOneDone(false)
                }
            })
        }

        fun submitDelete(call: Call<Void>){
            pending++
            call.enqueue(object : Callback<Void>{
                override fun onResponse(call: Call<Void>, response: Response<Void>){
                    onOneDone(response.isSuccessful)
                }
                override fun onFailure(call: Call<Void>, t: Throwable){
                    onOneDone(false)
                }
            })
        }

        if (includeInItem) {
            if (existingItemId != null) {
                submitFilm(api.updateItem(existingItemId, buildFilm(existingItemId)))
            } else {
                submitFilm(api.addItem(buildFilm("")))
            }
        } else if (existingItemId != null){
            submitDelete(api.deleteItem(existingItemId))
        }

        if (includeInUpcoming) {
            if (existingUpcomingId != null) {
                submitFilm(api.updateUpcomming(existingUpcomingId, buildFilm(existingUpcomingId)))
            } else {
                submitFilm(api.addUpcomming(buildFilm("")))
            }
        } else if (existingUpcomingId != null) {
            submitDelete(api.deleteUpcomming(existingUpcomingId))
        }
    }

    val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = Color(0xFFE57373),
        unfocusedBorderColor = Color.Gray,
        cursorColor = Color.White,
        textColor = Color.White
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.blackBackground))
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 40.dp)
        ) {
            Text(
                text = if (isEditing) "Sửa phim" else "Thêm phim mới",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.black3), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Đăng phim vào",
                    color = Color(0xFFBDBDBD),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                CheckboxRow(
                    label = "Phim đang chiếu",
                    checked = includeInItem,
                    onCheckedChange = { includeInItem = it }
                )
                CheckboxRow(
                    label = "Phim sắp chiếu",
                    checked = includeInUpcoming,
                    onCheckedChange = { includeInUpcoming = it }
                )
                if (isEditing){
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Chọn 1 trong 2 hoặc cả 2",
                        color = Color(0xFFFFB74D),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LabeledField("Tên phim", title, { title = it }, fieldColors)
            Spacer(modifier = Modifier.height(14.dp))
            LabeledField("Mô tả", description, { description = it }, fieldColors, minLines = 3)
            Spacer(modifier = Modifier.height(14.dp))
            LabeledField("Poster (URL ảnh)", poster, { poster = it }, fieldColors)
            Spacer(modifier = Modifier.height(14.dp))
            LabeledField("Trailer (URL)", trailer, { trailer = it }, fieldColors)
            Spacer(modifier = Modifier.height(14.dp))
            LabeledField("Thời lượng phim", time, { time = it }, fieldColors)
            Spacer(modifier = Modifier.height(14.dp))
            LabeledField("Thể loại (cách nhau bởi dấu phẩy)", genre, { genre = it }, fieldColors)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Diễn viên",
                color = Color(0xFFBDBDBD),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            castList.forEachIndexed { index, cast ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            colorResource(R.color.black3),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Diễn viên ${index + 1}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                castList = castList.toMutableList().also {
                                    it.removeAt(index)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Xoá diễn viên",
                                tint = Color(0xFFE57373)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LabeledField(
                        label = "Tên diễn viên",
                        value = cast.Actor,
                        onValueChange = { value ->
                            castList = castList.toMutableList().also {
                                it[index] = it[index].copy(Actor = value)
                            }
                        },
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LabeledField(
                        label = "URL ảnh diễn viên",
                        value = cast.PicUrl,
                        onValueChange = { value ->
                            castList = castList.toMutableList().also {
                                it[index] = it[index].copy(PicUrl = value)
                            }
                        },
                        colors = fieldColors
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        castList = castList + CastModel()
                    },
                    modifier = Modifier
                        .width(150.dp)
                        .height(40.dp)
                ) {
                    Text(
                        text = "Thêm diễn viên",
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    LabeledField("IMDB", imdb, { imdb = it }, fieldColors, keyboardType = KeyboardType.Decimal)
                }
                Box(modifier = Modifier.weight(1f)) {
                    LabeledField("Năm", year, { year = it }, fieldColors, keyboardType = KeyboardType.Number)
                }
                Box(modifier = Modifier.weight(1f)) {
                    LabeledField("Giá vé", price, { price = it }, fieldColors, keyboardType = KeyboardType.Decimal)
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = errorMessage ?: "", color = Color.Red, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientButton(
                    text = "Huỷ",
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f).height(50.dp)
                )
                GradientButton(
                    text = if (isSubmitting) "Đang lưu..." else "Lưu",
                    onClick = { if (!isSubmitting) handleSave() },
                    modifier = Modifier.weight(1f).height(50.dp)
                )
            }

            if (isSubmitting) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{ onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE57373),
            uncheckedColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    colors: androidx.compose.material.TextFieldColors,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(text = label, color = Color(0xFFBDBDBD), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(color = Color.White),
            colors = colors
        )
    }
}