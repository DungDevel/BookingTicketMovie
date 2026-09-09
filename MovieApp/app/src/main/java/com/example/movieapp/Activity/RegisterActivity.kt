package com.example.movieapp.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.AccountModel
import com.example.movieapp.Utils.ValidationUtils
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : BaseActivity() {

    @Inject lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegisterScreen (
                api = apiService,
                onRegisterSuccess = {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish() },
                onLoginClick = { startActivity(Intent(this, LoginActivity::class.java))}
            )
        }
    }
}

@Composable
fun RegisterScreen(
    api: ApiService,
    onRegisterSuccess:() -> Unit,
    onLoginClick:() -> Unit
){

    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun handleRegister(){
        val usernameError = ValidationUtils.validateUsername(username)
        val passwordError = ValidationUtils.validatePassword(password)

        if (usernameError != null) {
            errorMessage = usernameError
            return
        }
        if (passwordError != null) {
            errorMessage = passwordError
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Mật khẩu xác nhận không khớp"
            return
        }

        errorMessage = null
        isLoading = true

        api.getAccounts().enqueue(object : Callback<List<AccountModel>>{
            override fun onResponse(call: Call<List<AccountModel>>, response: Response<List<AccountModel>>){
                val accounts = response.body() ?: emptyList()
                val exists = accounts.any() { it.userName.equals(username, ignoreCase = true) }

                if (exists){
                    isLoading = false
                    errorMessage = "Tên đăng nhập đã tồn tại"
                    return
                }

                val newAccount = AccountModel(userName = username, password = password)
                api.registerAccount(newAccount).enqueue(object : Callback<AccountModel> {
                    override fun onResponse(call: Call<AccountModel>, response: Response<AccountModel>) {
                        isLoading = false
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                            onRegisterSuccess()
                        } else {
                            errorMessage = "Đăng ký thất bại, thử lại sau"
                        }
                    }
                    override fun onFailure(call: Call<AccountModel>, t: Throwable) {
                        isLoading = false
                        errorMessage = "Lỗi kết nối: ${t.message}"
                    }
                })
            }
            override fun onFailure(call: Call<List<AccountModel>>, t: Throwable){
                isLoading = false
                errorMessage = "Lỗi kết nối: ${t.message}"
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(com.example.movieapp.R.color.blackBackground))
    ){
        Image(
            painter = painterResource(id = com.example.movieapp.R.drawable.bg1),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Text(
                text = "Đăng Ký",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(128.dp))
            GradientTextField(
                value = username,
                onValueChange = {username = it},
                hint = "Tên đăng nhập",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(16.dp))
            GradientTextField(
                value = password,
                onValueChange = {password = it},
                hint = "Mật khẩu",
                modifier = Modifier.fillMaxWidth(),
                isPassword = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            GradientTextField(
                value = confirmPassword,
                onValueChange = {confirmPassword = it},
                hint = "Xác nhận mật khẩu",
                modifier = Modifier.fillMaxWidth(),
                isPassword = true
            )
            if (errorMessage != null){
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFB71C1C).copy(alpha = 0.25f))
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
            Spacer(modifier = Modifier.height(64.dp))
            GradientButton(
                text = if (isLoading) "Đang xử lý..." else "Đăng Ký",
                onClick = { if (!isLoading) handleRegister() },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Đã có tài khoản. ",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp
                    )
                )
                Text(
                    text = "Đăng nhập",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable{onLoginClick()}
                )
            }
        }

        if (isLoading) {
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