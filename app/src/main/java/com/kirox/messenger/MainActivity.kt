package com.kirox.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF10131B)
private val CardColor = Color(0xFF1B202B)
private val Accent = Color(0xFF7658E8)
private val MainText = Color.White
private val SecondaryText = Color(0xFF929AAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KayroMessApp()
        }
    }
}

data class Chat(
    val name: String,
    val message: String,
    val time: String
)

@Composable
fun KayroMessApp() {
    var selectedTab by remember { mutableStateOf("Чаты") }

    val chats = listOf(
        Chat("KayroMess Support", "Добро пожаловать!", "12:30"),
        Chat("Избранное", "Сохранённые сообщения", "11:45"),
        Chat("Объявления", "Новости KayroMess", "Вчера")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "KayroMess",
                color = MainText,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Общение без границ",
                color = SecondaryText,
                fontSize = 14.sp
            )
        }

        HorizontalDivider(color = CardColor)

        Text(
            selectedTab,
            color = MainText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(18.dp)
        )

        when (selectedTab) {
            "Чаты" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(chats) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    chat.name.first().toString(),
                                    color = Color.White,
                                    fontSize = 22.sp
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    chat.name,
                                    color = MainText,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    chat.message,
                                    color = SecondaryText,
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                chat.time,
                                color = SecondaryText,
                                fontSize = 11.sp
                            )
                        }

                        HorizontalDivider(color = CardColor)
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$selectedTab появятся здесь",
                        color = SecondaryText
                    )
                }
            }
        }

        HorizontalDivider(color = CardColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor)
                .navigationBarsPadding()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Чаты", "Контакты", "Каналы", "Профиль")
                .forEach { tab ->
                    Text(
                        tab,
                        color = if (selectedTab == tab)
                            Accent else SecondaryText,
                        modifier = Modifier.clickable {
                            selectedTab = tab
                        },
                        fontSize = 12.sp
                    )
                }
        }
    }
}
