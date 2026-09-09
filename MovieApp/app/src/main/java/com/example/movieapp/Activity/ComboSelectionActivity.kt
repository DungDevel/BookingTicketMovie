package com.example.movieapp.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import com.example.movieapp.Domain.ComboCategory
import com.example.movieapp.Domain.ComboItemModel
import com.example.movieapp.Domain.FilmItemModel
import com.example.movieapp.Domain.SeatModel
import com.example.movieapp.Domain.SelectedComboModel
import com.example.movieapp.R
import com.example.movieapp.Repository.ApiResult
import com.example.movieapp.ViewModel.SeatViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@AndroidEntryPoint
class ComboSelectionActivity : BaseActivity() {

    private val viewModel: SeatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val film = intent.getSerializableExtra("film") as? FilmItemModel
        val accountId = intent.getStringExtra("accountId")
        val date = intent.getStringExtra("date")
        val time = intent.getStringExtra("time")
        @Suppress("UNCHECKED_CAST")
        val seats = intent.getSerializableExtra("seats") as? ArrayList<SeatModel>
        val seatsTotalPrice = intent.getDoubleExtra("seatsTotalPrice", 0.0)

        if (film == null || accountId.isNullOrBlank() || date == null || time == null || seats.isNullOrEmpty()) {
            Toast.makeText(this, "Thiếu thông tin đặt vé", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ComboSelectionScreen(
                viewModel = viewModel,
                seatsTotalPrice = seatsTotalPrice,
                onBackClick = { finish() },
                onContinue = { selectedCombos ->
                    val comboTotal = selectedCombos.sumOf { it.subtotal }
                    val finalTotal = seatsTotalPrice + comboTotal
                    val seatCodes = seats.map { it.code }

                    viewModel.holdSeats(
                        accountId = accountId,
                        filmId = film.id,
                        filmTitle = film.Title,
                        date = date,
                        time = time,
                        selectedSeatCodes = seatCodes,
                        totalPrice = finalTotal,
                        combos = selectedCombos
                    ).observe(this) { result ->
                        if (result.isError || result.data == null) {
                            Toast.makeText(
                                this,
                                result.errorMessage ?: "Không giữ được ghế, vui lòng thử lại",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                            return@observe
                        }

                        val booking = result.data
                        val paymentIntent = Intent(this, PaymentActivity::class.java).apply {
                            putExtra("film", film)
                            putExtra("date", date)
                            putExtra("time", time)
                            putExtra("seats", seats)
                            putExtra("totalPrice", finalTotal)
                            putExtra("bookingId", booking.id)
                            putExtra("combos", ArrayList(selectedCombos))
                        }
                        startActivity(paymentIntent)
                        finish()
                    }
                }
            )
        }
    }
}

@Composable
private fun ComboSelectionScreen(
    viewModel: SeatViewModel,
    seatsTotalPrice: Double,
    onBackClick: () -> Unit,
    onContinue: (List<SelectedComboModel>) -> Unit
) {
    var comboItems by remember { mutableStateOf<List<ComboItemModel>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val quantities = remember { mutableStateOf(mutableMapOf<String, Int>()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val liveData = viewModel.loadComboItems()
        val observer = Observer<ApiResult<List<ComboItemModel>>> { result ->
            isLoadingList = false
            if (result.isError) {
                loadError = true
            } else {
                comboItems = result.data
            }
        }
        liveData.observe(lifecycleOwner, observer)
        onDispose { liveData.removeObserver(observer) }
    }

    fun setQuantity(item: ComboItemModel, qty: Int) {
        val map = quantities.value.toMutableMap()
        if (qty <= 0) map.remove(item.id) else map[item.id] = qty
        quantities.value = map
    }

    val selectedCombos: List<SelectedComboModel> = comboItems
        .mapNotNull { item ->
            val qty = quantities.value[item.id] ?: 0
            if (qty > 0) SelectedComboModel(id = item.id, name = item.name, price = item.price, quantity = qty) else null
        }
    val comboTotal = selectedCombos.sumOf { it.subtotal }
    val grandTotal = seatsTotalPrice + comboTotal
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val hasSelection = selectedCombos.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.blackBackground))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
            }
            Text(
                text = "Bắp & Nước",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Bỏ qua",
                color = Color(0xFFE57373),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(enabled = !isSubmitting) {
                        isSubmitting = true
                        onContinue(emptyList())
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        when {
            isLoadingList -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE57373))
                }
            }
            loadError -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Không tải được danh sách bắp/nước", color = Color.Gray, fontSize = 14.sp)
                }
            }
            comboItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Hiện chưa có bắp/nước để chọn", color = Color.Gray, fontSize = 14.sp)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val combos = comboItems.filter { it.category == ComboCategory.COMBO }
                    val popcorns = comboItems.filter { it.category == ComboCategory.POPCORN }
                    val drinks = comboItems.filter { it.category == ComboCategory.DRINK }

                    if (combos.isNotEmpty()) {
                        item { SectionHeader("Combo") }
                        items(combos) { item ->
                            ComboRow(item, quantities.value[item.id] ?: 0, ::setQuantity)
                        }
                    }
                    if (popcorns.isNotEmpty()) {
                        item { SectionHeader("Bắp") }
                        items(popcorns) { item ->
                            ComboRow(item, quantities.value[item.id] ?: 0, ::setQuantity)
                        }
                    }
                    if (drinks.isNotEmpty()) {
                        item { SectionHeader("Nước") }
                        items(drinks) { item ->
                            ComboRow(item, quantities.value[item.id] ?: 0, ::setQuantity)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.black1))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Vé: ${formatter.format(seatsTotalPrice)}", color = Color.Gray, fontSize = 12.sp)
                    if (hasSelection) {
                        Text(text = "Bắp/nước: ${formatter.format(comboTotal)}", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text(
                        text = "Tổng: ${formatter.format(grandTotal)}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    isSubmitting = true
                    onContinue(selectedCombos)
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE57373))
            ) {
                Text(
                    text = if (isSubmitting) "Đang xử lý..."
                    else if (hasSelection) "Tiếp tục đến thanh toán"
                    else "Bỏ qua, đến thanh toán",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFFE57373),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun ComboRow(
    item: ComboItemModel,
    quantity: Int,
    onQuantityChange: (ComboItemModel, Int) -> Unit
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(colorResource(R.color.black1), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.name,
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (item.description.isNotBlank()) {
                Text(text = item.description, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = formatter.format(item.price), color = Color(0xFFE57373), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuantityButton(
                icon = Icons.Filled.Remove,
                enabled = quantity > 0,
                onClick = { onQuantityChange(item, (quantity - 1).coerceAtLeast(0)) }
            )
            Text(
                text = quantity.toString(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            QuantityButton(
                icon = Icons.Filled.Add,
                enabled = true,
                onClick = { onQuantityChange(item, quantity + 1) }
            )
        }
    }
}

@Composable
private fun QuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(
                color = if (enabled) Color(0xFFE57373) else Color(0xFF555555),
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}