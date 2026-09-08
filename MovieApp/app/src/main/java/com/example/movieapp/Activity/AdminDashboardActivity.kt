package com.example.movieapp.Activity

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movieapp.R
import com.example.movieapp.Utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdminDashboardActivity : BaseActivity() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!sessionManager.isAdmin()) {
            Toast.makeText(this, "Bạn không có quyền truy cập trang này", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            AdminDashboardScreen(
                onManageAccounts = { startActivity(Intent(this, AdminAccountActivity::class.java)) },
                onManageFilms = { startActivity(Intent(this, AdminFilmsActivity::class.java)) },
                onDashBoard = { startActivity(Intent(this, AdminManageDashboardActivity::class.java)) },
                onLogout = {
                    sessionManager.clearSeesion()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun AdminDashboardScreen(
    onManageAccounts: () -> Unit,
    onManageFilms: () -> Unit,
    onDashBoard: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.blackBackground))
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(56.dp))
            Text(
                text = "Trang Quản Trị",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Quản lý tài khoản và phim trong hệ thống",
                color = Color(0xFFBDBDBD),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            AdminMenuCard(
                icon = Icons.Filled.People,
                title = "Quản lý tài khoản",
                subtitle = "Xem, đổi quyền, xoá tài khoản người dùng",
                onClick = onManageAccounts
            )
            Spacer(modifier = Modifier.height(14.dp))
            AdminMenuCard(
                icon = Icons.Filled.Movie,
                title = "Quản lý phim",
                subtitle = "Thêm, sửa, xoá phim đang chiếu / sắp chiếu",
                onClick = onManageFilms
            )
            Spacer(modifier = Modifier.height(14.dp))
            AdminMenuCard(
                icon = Icons.Filled.BarChart,
                title = "Thống Kê Doanh Thu",
                subtitle = "Xem thống kê doanh thu",
                onClick = onDashBoard
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onLogout() }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(28.dp)
                    )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Đăng xuất", color = Color(0xFFE57373), fontSize = 22.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdminMenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colorResource(R.color.black3))
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFFE57373).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFE57373))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = Color(0xFFBDBDBD), fontSize = 12.sp)
        }
        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}