package com.example.movieapp.Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Person as PersonOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import com.example.movieapp.BottomNavigationBar
import com.example.movieapp.Domain.ProfileModel
import com.example.movieapp.Repository.MainRepository
import com.example.movieapp.Utils.AvatarUtils
import com.example.movieapp.Utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : BaseActivity() {

    @Inject lateinit var repository: MainRepository
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileScreen(
                repository = repository,
                currentUserId = sessionManager.getUserId(),
                lifecycleOwner = this,
                onEditProfile = {
                    startActivity(Intent(this, UpdateProfileActivity::class.java))
                },
                onLogout = {
                    sessionManager.clearSeesion()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            )
        }
    }
}

private val headerGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFB71C1C), Color(0xFF3E0000))
)

@Composable
fun ProfileScreen(
    currentUserId: String?,
    repository: MainRepository,
    lifecycleOwner: LifecycleOwner,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    var profile by remember { mutableStateOf<ProfileModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repository.loadProfile().observe(lifecycleOwner) { profiles ->
            isLoading = false
            profile = currentUserId?.let { id -> profiles.find { it.accountId == id } }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar() },
        backgroundColor = colorResource(com.example.movieapp.R.color.blackBackground)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(color = colorResource(com.example.movieapp.R.color.blackBackground))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileHeader(profile = profile)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 40.dp)
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                        profile == null -> {
                            EmptyProfileState(onEditProfile = onEditProfile)
                        }
                        else -> {
                            ProfileInfoCard(profile = profile!!)
                            Spacer(modifier = Modifier.height(32.dp))
                            GradientButton(
                                text = "Chỉnh sửa",
                                onClick = onEditProfile,
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            GradientButton(
                                text = "Đăng xuất",
                                onClick = onLogout,
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: ProfileModel?) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(headerGradient)
        ) {
            Text(
                text = "Hồ sơ",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 152.dp, top = 5.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 130.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(colorResource(com.example.movieapp.R.color.blackBackground)),
                contentAlignment = Alignment.Center
            ) {
                val avatarBitmap = remember(profile?.avatar) {
                    profile?.avatar?.let { AvatarUtils.base64ToBitmap(it) }
                }
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Ảnh đại diện",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                    )
                } else {
                    InitialAvatar(name = profile?.name.orEmpty().ifBlank { "?" }, size = 104.dp)
                }
            }
            if (profile != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = profile.name,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile.gmail,
                    style = TextStyle(
                        color = Color(0xFFBDBDBD),
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
fun InitialAvatar(name: String, size: androidx.compose.ui.unit.Dp = 100.dp) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    val palette = listOf(
        Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784),
        Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC),
        Color(0xFFF06292), Color(0xFF9575CD)
    )
    val bgColor = palette[(name.hashCode().let { if (it < 0) -it else it }) % palette.size]

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = TextStyle(
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun ProfileInfoCard(profile: ProfileModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colorResource(com.example.movieapp.R.color.black3))
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ProfileFieldRow(
            icon = Icons.Filled.Person,
            label = "Tên người dùng",
            value = profile.name
        )
        Divider(color = Color.White.copy(alpha = 0.08f))
        ProfileFieldRow(
            icon = Icons.Filled.DateRange,
            label = "Ngày sinh",
            value = formatBirthDate(profile.day_of_birth)
        )
        Divider(color = Color.White.copy(alpha = 0.08f))
        ProfileFieldRow(
            icon = Icons.Filled.Phone,
            label = "Số điện thoại",
            value = profile.telephone
        )
        Divider(color = Color.White.copy(alpha = 0.08f))
        ProfileFieldRow(
            icon = Icons.Filled.Email,
            label = "Gmail",
            value = profile.gmail
        )
    }
}

@Composable
private fun ProfileFieldRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFE57373),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = TextStyle(
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun EmptyProfileState(onEditProfile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colorResource(com.example.movieapp.R.color.black3))
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonOutline,
                contentDescription = null,
                tint = Color(0xFFE57373),
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Bạn chưa cập nhật thông tin cá nhân",
            style = TextStyle(
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Thêm tên, ngày sinh, số điện thoại và gmail để hoàn thiện hồ sơ của bạn.",
            style = TextStyle(
                color = Color(0xFFBDBDBD),
                fontSize = 13.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        GradientButton(
            text = "Cập nhật hồ sơ ngay",
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )
    }
}

/**
 * Format ngày sinh (chuỗi ISO "yyyy-MM-dd") sang "dd/MM/yyyy" để hiển thị.
 * Trả về "Chưa cập nhật" nếu chuỗi rỗng hoặc không đúng định dạng (ví dụ dữ liệu cũ
 * bị lưu sai trước khi sửa lỗi serialize LocalDate -> {}).
 */
private fun formatBirthDate(raw: String): String {
    if (raw.isBlank()) return "Chưa cập nhật"
    return try {
        LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (e: Exception) {
        "Chưa cập nhật"
    }
}