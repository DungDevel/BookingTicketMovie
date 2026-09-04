package com.example.movieapp.Activity

import android.app.DatePickerDialog
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.ProfileModel
import com.example.movieapp.Utils.AvatarUtils
import com.example.movieapp.Utils.SessionManager
import com.example.movieapp.Utils.ValidationUtils
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class UpdateProfileActivity : BaseActivity() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UpdateProfileScreen(
                api = apiService,
                currentUserId = sessionManager.getUserId(),
                onUpdateSuccess = {
                    startActivity(Intent(this, ProfileActivity::class.java))
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
fun UpdateProfileScreen(
    api: ApiService,
    currentUserId: String?,
    onUpdateSuccess: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var day_of_birth by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var gmail by remember { mutableStateOf("") }
    var avatarBase64 by remember { mutableStateOf("") }
    var isProcessingAvatar by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isProcessingAvatar = true
            coroutineScope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    AvatarUtils.uriToBase64(context, uri)
                }
                isProcessingAvatar = false
                if (encoded != null) {
                    avatarBase64 = encoded
                } else {
                    Toast.makeText(context, "Không thể tải ảnh này, vui lòng thử ảnh khác", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var profileId by remember { mutableStateOf("") }
    var isNewProfile by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (currentUserId.isNullOrBlank()) {
            isLoadingProfile = false
            errorMessage = "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
            return@LaunchedEffect
        }

        api.getProfile().enqueue(object : retrofit2.Callback<List<ProfileModel>> {
            override fun onResponse(call: Call<List<ProfileModel>>, response: retrofit2.Response<List<ProfileModel>>) {
                isLoadingProfile = false
                val current = response.body()?.find { it.accountId == currentUserId }
                if (current != null) {
                    profileId = current.id
                    name = current.name
                    day_of_birth = current.day_of_birth
                    telephone = current.telephone
                    gmail = current.gmail
                    avatarBase64 = current.avatar
                } else {
                    profileId = ""
                    isNewProfile = true
                }
            }
            override fun onFailure(call: Call<List<ProfileModel>>, t: Throwable) {
                isLoadingProfile = false
                errorMessage = "Lỗi kết nối: ${t.message}"
            }
        })
    }

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val existing = day_of_birth.let {
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        }
        if (existing != null) {
            calendar.set(existing.year, existing.monthValue - 1, existing.dayOfMonth)
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                day_of_birth = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun handleProfileUpdate() {
        val accountId = currentUserId
        if (accountId.isNullOrBlank()) {
            errorMessage = "Không xác định được tài khoản, vui lòng đăng nhập lại"
            return
        }
        if (!isNewProfile && profileId.isBlank()) {
            errorMessage = "Không xác định được hồ sơ để cập nhật"
            return
        }

        if (name.isBlank()) {
            errorMessage = "Vui lòng nhập tên người dùng"
            return
        }

        val telephoneError = ValidationUtils.validateTelephone(telephone)
        if (telephoneError != null) {
            errorMessage = telephoneError
            return
        }

        if (gmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(gmail).matches()) {
            errorMessage = "Gmail không hợp lệ"
            return
        }

        try {
            LocalDate.parse(day_of_birth)
        } catch (e: DateTimeParseException) {
            errorMessage = "Ngày sinh không hợp lệ, vui lòng chọn lại"
            return
        }

        errorMessage = null
        isSubmitting = true

        api.getProfile().enqueue(object : retrofit2.Callback<List<ProfileModel>> {
            override fun onResponse(call: Call<List<ProfileModel>>, response: retrofit2.Response<List<ProfileModel>>) {
                val profiles = response.body() ?: emptyList()

                val otherProfiles = profiles.filterNot { it.accountId == accountId }

                val existsTelephone = otherProfiles.any { it.telephone.equals(telephone, ignoreCase = true) }
                val existsGmail = otherProfiles.any { it.gmail.equals(gmail, ignoreCase = true) }

                if (existsTelephone) {
                    isSubmitting = false
                    errorMessage = "Số điện thoại đã tồn tại ở tài khoản khác"
                    return
                }
                if (existsGmail) {
                    isSubmitting = false
                    errorMessage = "Gmail này đã tồn tại"
                    return
                }

                val updateProfile = ProfileModel(
                    id = profileId,
                    accountId = accountId,
                    name = name,
                    day_of_birth = day_of_birth,
                    telephone = telephone,
                    gmail = gmail,
                    avatar = avatarBase64
                )

                val call = if (isNewProfile) {
                    api.createProfile(updateProfile)
                } else {
                    api.updateProfile(profileId, updateProfile)
                }

                call.enqueue(object : retrofit2.Callback<ProfileModel> {
                    override fun onResponse(call: Call<ProfileModel>, response: retrofit2.Response<ProfileModel>) {
                        isSubmitting = false
                        if (response.isSuccessful) {
                            Toast.makeText(
                                context,
                                if (isNewProfile) "Tạo hồ sơ thành công" else "Cập nhật thành công",
                                Toast.LENGTH_SHORT
                            ).show()
                            onUpdateSuccess()
                        } else {
                            errorMessage = "Cập nhật không thành công"
                        }
                    }

                    override fun onFailure(call: Call<ProfileModel>, t: Throwable) {
                        isSubmitting = false
                        errorMessage = "Lỗi kết nối: ${t.message}"
                    }
                })
            }

            override fun onFailure(call: Call<List<ProfileModel>>, t: Throwable) {
                isSubmitting = false
                errorMessage = "Lỗi kết nối: ${t.message}"
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(com.example.movieapp.R.color.blackBackground))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(headerGradient)
            ) {
                Text(
                    text = if (isNewProfile) "Hoàn thiện hồ sơ" else "Cập nhật hồ sơ",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 28.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 40.dp)
            ) {
                if (isLoadingProfile) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarPicker(
                            avatarBase64 = avatarBase64,
                            isProcessing = isProcessingAvatar,
                            onClick = { imagePickerLauncher.launch("image/*") }
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colorResource(com.example.movieapp.R.color.black3))
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Thông tin cá nhân",
                            style = TextStyle(
                                color = Color(0xFFBDBDBD),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        GradientTextField(
                            value = name,
                            onValueChange = { name = it },
                            hint = "Tên người dùng",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Trường ngày sinh chỉ nhập qua DatePicker để tránh sai định dạng
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                GradientTextField(
                                    value = if (day_of_birth.isBlank()) "" else runCatching {
                                        val d = LocalDate.parse(day_of_birth)
                                        "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)
                                    }.getOrDefault(day_of_birth),
                                    onValueChange = {},
                                    hint = "Ngày sinh (dd/mm/yyyy)",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Lớp phủ trong suốt để chặn gõ tay, chỉ cho mở DatePicker
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showDatePicker() }
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = "Chọn ngày sinh",
                                    tint = Color(0xFFE57373)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        GradientTextField(
                            value = telephone,
                            onValueChange = { telephone = it },
                            hint = "Số điện thoại",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        GradientTextField(
                            value = gmail,
                            onValueChange = { gmail = it },
                            hint = "Gmail",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFB71C1C).copy(alpha = 0.18f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE57373),
                                modifier = Modifier.height(20.dp).width(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFFCDD2),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    GradientButton(
                        text = when {
                            isSubmitting -> "Đang xử lý..."
                            isNewProfile -> "Tạo hồ sơ"
                            else -> "Cập nhật"
                        },
                        onClick = { if (!isSubmitting) handleProfileUpdate() },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }
            }
        }

        // Chặn thao tác trên form trong lúc đang gửi yêu cầu cập nhật
        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun AvatarPicker(
    avatarBase64: String,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val bitmap = remember(avatarBase64) { AvatarUtils.base64ToBitmap(avatarBase64) }

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(colorResource(com.example.movieapp.R.color.black3))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isProcessing -> {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Ảnh đại diện",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        // Icon máy ảnh nhỏ ở góc dưới phải, báo hiệu có thể bấm để đổi ảnh
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFE57373)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Đổi ảnh đại diện",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}