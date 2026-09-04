package com.example.movieapp

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.movieapp.Activity.FavoriteActivity
import com.example.movieapp.Activity.HistoryBookingActivity
import com.example.movieapp.Activity.MainActivity
import com.example.movieapp.Activity.ProfileActivity

@Composable
fun BottomNavigationBar(){
    val bottomMenuItemsList = prepareBottomMenu()
    val context = LocalContext.current
    val contextForToast = context.applicationContext
    var selectedItem by remember {
        mutableStateOf("Home")
    }

    BottomAppBar(
        cutoutShape = CircleShape,
        contentColor = colorResource(id = R.color.white),
        backgroundColor = colorResource(id = R.color.black3),
        elevation = 3.dp
    ){
        bottomMenuItemsList.forEach { bottomMenuItem ->
            BottomNavigationItem(
                selected = (selectedItem == bottomMenuItem.label),
                onClick = {
                    selectedItem = bottomMenuItem.label
                    when (bottomMenuItem.label) {
                        "Favorite" -> {
                            context.startActivity(
                                Intent(context, FavoriteActivity::class.java)
                            )
                        }
                        "Profile" -> {
                            try {
                                context.startActivity(
                                    Intent(context, ProfileActivity::class.java)
                                )
                            } catch (e: Exception) {
                                Log.e("BottomNavigationBar", "Không mở được ProfileActivity", e)
                                Toast.makeText(
                                    contextForToast,
                                    "Không thể mở trang cá nhân",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        "Home" -> {
                            try {
                                context.startActivity(
                                    Intent(context, MainActivity::class.java)
                                )
                            } catch (e: Exception){
                                Log.e("BottomNavigationBar", "Không mở được HomeActivity", e)
                                Toast.makeText(
                                    contextForToast,
                                    "Không thể mở trang chủ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        "History" -> {
                            try {
                                context.startActivity(
                                    Intent(context, HistoryBookingActivity::class.java)
                                )
                            } catch (e: Exception){
                                Log.e("BottomNavigationBar", "Không mở được lịch sử", e)
                                Toast.makeText(
                                    contextForToast,
                                    "Không thể mở lịch sử",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        else -> {
                            Toast.makeText(contextForToast, bottomMenuItem.label, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                icon = {
                    when (val icon = bottomMenuItem.icon) {
                        is ImageVector -> {
                            Icon(
                                imageVector = icon,
                                contentDescription = bottomMenuItem.label,
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(30.dp)
                            )
                        }

                        is Painter -> {
                            Icon(
                                painter = icon,
                                contentDescription = bottomMenuItem.label,
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(20.dp)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = bottomMenuItem.label,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                },
                alwaysShowLabel = true,
                enabled = true
            )
        }
    }
}

data class BottomMenuItem(
    val label:String, val icon: Any
)

@Composable
fun prepareBottomMenu(): List<BottomMenuItem>{
    return listOf(
        BottomMenuItem(
            label = "Home",
            icon = Icons.Default.Home
        ),
        BottomMenuItem(
            label = "Favorite",
            icon = Icons.Default.Favorite
        ),
        BottomMenuItem(
            label = "History",
            icon = Icons.Default.History
        ),
        BottomMenuItem(
            label = "Profile",
            icon = Icons.Default.Person
        ),
    )
}