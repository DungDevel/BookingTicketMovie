package com.example.movieapp.Activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movieapp.Api.ApiService
import com.example.movieapp.Domain.AccountModel
import com.example.movieapp.Domain.AccountRole
import com.example.movieapp.Domain.GoogleLoginRequest
import com.example.movieapp.R
import com.example.movieapp.Utils.GoogleAuthConfig
import com.example.movieapp.Utils.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : BaseActivity() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var sessionManager: SessionManager

    private lateinit var googleSignInClient: GoogleSignInClient
    private val isGoogleLoading = mutableStateOf(false)

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            isGoogleLoading.value = false
            return@registerForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                isGoogleLoading.value = false
                Toast.makeText(this, "Không lấy được thông tin đăng nhập từ Google", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            sendGoogleTokenToServer(idToken)
        } catch (e: ApiException) {
            isGoogleLoading.value = false
            Toast.makeText(this, "Đăng nhập Google thất bại (mã lỗi ${e.statusCode})", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendGoogleTokenToServer(idToken: String) {
        apiService.loginWithGoogle(GoogleLoginRequest(idToken)).enqueue(object : Callback<AccountModel> {
            override fun onResponse(call: Call<AccountModel>, response: Response<AccountModel>) {
                isGoogleLoading.value = false
                val account = response.body()
                if (!response.isSuccessful || account == null) {
                    Toast.makeText(this@LoginActivity, "Đăng nhập Google thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                    return
                }
                sessionManager.saveSession(account.id, account.role)
                Toast.makeText(this@LoginActivity, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                if (account.role == AccountRole.ADMIN) {
                    startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
                } else {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                }
                finish()
            }

            override fun onFailure(call: Call<AccountModel>, t: Throwable) {
                isGoogleLoading.value = false
                Toast.makeText(this@LoginActivity, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GoogleAuthConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        setContent {
            LoginScreen (
                api = apiService,
                sessionManager = sessionManager,
                isGoogleLoading = isGoogleLoading.value,
                onGoogleLoginClick = {
                    isGoogleLoading.value = true
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                onLoginSuccess = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() },
                onAdminLoginSuccess = {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                },
                onRegisterClick = { startActivity(Intent(this, RegisterActivity::class.java))
                }  )
        }
    }
}

@Composable
fun LoginScreen(
    api: ApiService,
    sessionManager: SessionManager,
    isGoogleLoading: Boolean = false,
    onGoogleLoginClick: () -> Unit = {},
    onLoginSuccess:() -> Unit,
    onAdminLoginSuccess:() -> Unit,
    onRegisterClick:() -> Unit,
){
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun handleLogin() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin"
            return
        }
        errorMessage = null
        isLoading = true

        api.getAccounts().enqueue(object : Callback<List<AccountModel>> {
            override fun onResponse(
                call: Call<List<AccountModel>>,
                response: Response<List<AccountModel>>
            ) {
                isLoading = false
                val accounts = response.body() ?: emptyList()
                val matched = accounts.find {
                    it.userName.equals(username, ignoreCase = true) && it.password == password
                }
                if (matched != null) {
                    sessionManager.saveSession(matched.id, matched.role)
                    Toast.makeText(context, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    if (matched.role == AccountRole.ADMIN) {
                        onAdminLoginSuccess()
                    } else {
                        onLoginSuccess()
                    }
                } else {
                    errorMessage = "Tên đăng nhập hoặc mật khẩu không đúng"
                }
            }

            override fun onFailure(call: Call<List<AccountModel>>, t: Throwable) {
                isLoading = false
                errorMessage = "Lỗi kết nối: ${t.message}"
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.blackBackground))
    ){
        Image(
            painter = painterResource(id = R.drawable.bg1),
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
            Text(text = "Đăng Nhập",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(100.dp))
            GradientTextField(
                value = username,
                onValueChange = { username = it },
                hint = "Tên đăng nhập",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            GradientTextField(
                value = password,
                onValueChange = { password = it },
                hint = "Mật khẩu",
                modifier = Modifier.fillMaxWidth(),
                isPassword = true
            )
            if (errorMessage != null){
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = errorMessage ?: "", color = Color.Red, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Quên Mật Khẩu?",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(64.dp))
            GradientButton(
                text = if (isLoading) "Đang xử lý..." else "Đăng Nhập",
                onClick = { if ( !isLoading ) handleLogin()},
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.3f)))
                Text(
                    text = "  hoặc  ",
                    style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                )
                Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.3f)))
            }
            Spacer(modifier = Modifier.height(16.dp))
            GoogleSignInButton(
                text = if (isGoogleLoading) "Đang xử lý..." else "Đăng nhập bằng Google",
                enabled = !isGoogleLoading,
                onClick = onGoogleLoginClick,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ){
                Text(
                    text = "Chưa có tài khoản. ",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                    )
                )
                Text(
                    text = "Đăng ký",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier.clickable{onRegisterClick()}
                )
            }
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(60.dp),
        border = BorderStroke(
            width = 4.dp,
            brush = Brush.linearGradient(
                colors = listOf(colorResource(R.color.pink), colorResource(R.color.green))
            )
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        )
    ) {
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun GoogleSignInButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
){
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(60.dp),
        border = BorderStroke(width = 1.dp, color = Color(0xFFDADCE0)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF3C4043),
            disabledContainerColor = Color.White.copy(alpha = 0.6f)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.height(20.dp).width(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF3C4043))
        }
    }
}

@Composable
fun GradientTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
){
    Box(
        modifier = modifier
            .height(60.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(colorResource(R.color.pink), colorResource(R.color.green))
                ),
                shape = RoundedCornerShape(50.dp)
            )
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = hint,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            singleLine = true,
            visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = TextStyle(
                color = Color.White,
                textAlign = TextAlign.Center
            ),
            colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White
            ),
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(R.color.black1),
                    shape = RoundedCornerShape(50.dp)
                )
                .align(Alignment.Center)
        )
    }
}