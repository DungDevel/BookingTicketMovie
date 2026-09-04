package com.example.movieapp.Activity

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.AccountModel
import com.example.movieapp.Domain.AccountRole
import com.example.movieapp.R
import com.example.movieapp.Utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class AdminAccountActivity : BaseActivity() {

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
            AdminAccountScreen(
                api = apiService,
                currentUserId = sessionManager.getUserId(),
                onBackClick = { finish() }
            )
        }
    }
}

@Composable
fun AdminAccountScreen(
    api: ApiService,
    currentUserId: String?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    var accounts by remember { mutableStateOf<List<AccountModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadAccounts() {
        isLoading = true
        api.getAccounts().enqueue(object : Callback<List<AccountModel>> {
            override fun onResponse(call: Call<List<AccountModel>>, response: Response<List<AccountModel>>) {
                isLoading = false
                accounts = response.body() ?: emptyList()
            }
            override fun onFailure(call: Call<List<AccountModel>>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    LaunchedEffect(Unit) { loadAccounts() }

    fun toggleRole(account: AccountModel) {
        if (account.id == currentUserId) {
            Toast.makeText(context, "Không thể tự đổi quyền của chính mình", Toast.LENGTH_SHORT).show()
            return
        }
        val newRole = if (account.role == AccountRole.ADMIN) AccountRole.USER else AccountRole.ADMIN
        val updated = account.copy(role = newRole)
        api.updateAccount(account.id, updated).enqueue(object : Callback<AccountModel> {
            override fun onResponse(call: Call<AccountModel>, response: Response<AccountModel>) {
                if (response.isSuccessful) {
                    loadAccounts()
                } else {
                    Toast.makeText(context, "Đổi quyền thất bại", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<AccountModel>, t: Throwable) {
                Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun deleteAccount(account: AccountModel) {
        if (account.id == currentUserId) {
            Toast.makeText(context, "Không thể tự xoá chính mình", Toast.LENGTH_SHORT).show()
            return
        }
        api.deleteAccount(account.id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Đã xoá tài khoản", Toast.LENGTH_SHORT).show()
                    loadAccounts()
                } else {
                    Toast.makeText(context, "Xoá thất bại", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
                Text(text = "Quản Lý Tài Khoản", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (accounts.isEmpty()) {
                Text(text = "Chưa có tài khoản nào", color = Color.Gray, fontSize = 14.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(accounts, key = { it.id }) { account ->
                        AccountRow(
                            account = account,
                            isSelf = account.id == currentUserId,
                            onToggleRole = { toggleRole(account) },
                            onDelete = { deleteAccount(account) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountModel,
    isSelf: Boolean,
    onToggleRole: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorResource(R.color.black3))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.userName + if (isSelf) " (Bạn)" else "",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            RoleBadge(role = account.role)
        }

        if (!isSelf) {
            Text(
                text = if (account.role == AccountRole.ADMIN) "Hạ quyền" else "Cấp admin",
                color = Color(0xFF64B5F6),
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { onToggleRole() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                text = "Xoá",
                color = Color(0xFFE57373),
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { onDelete() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    val (label, color) = if (role == AccountRole.ADMIN) {
        "Admin" to Color(0xFFFFB74D)
    } else {
        "Người dùng" to Color(0xFF81C784)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}