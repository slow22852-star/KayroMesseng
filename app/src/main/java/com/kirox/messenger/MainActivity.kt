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
import androidx.compose.foundation.shape.RoundedCornerShape
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
private val Accent = Color(0xFF7665F5)
private val MainText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFF929AAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MessengerApp()
        }
    }
}

data class Chat(
    val name: String,
    val message: String,
    val time: String,
    val unread: Int
)

@Composable
fun MessengerApp() {
    var selectedTab by remember { mutableStateOf("Чаты") }

    val chats = listOf(
        Chat("Kirox Support", "Добро пожаловать!", "12:30", 1),
        Chat("Избранное", "Сохранённые сообщения", "11:45", 0),
        Chat("Объявления", "Новости мессенджера", "Вчера", 0)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = "Kirox",
                color = MainText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Твой мир общения",
                color = SecondaryText,
                fontSize = 14.sp
            )
        }

        HorizontalDivider(color = CardColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedTab,
                color = MainText,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Поиск",
                color = Accent,
                fontSize = 14.sp
            )
        }

        when (selectedTab) {
            "Чаты" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(chats) { chat ->
                        ChatItem(chat)
                    }
                }
            }

            "Контакты" -> EmptySection("Контакты")

            "Каналы" -> EmptySection("Каналы")

            "Профиль" -> EmptySection("Профиль")
        }

        HorizontalDivider(color = CardColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardColor)
                .navigationBarsPadding()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Чаты", "Контакты", "Каналы", "Профиль").forEach { tab ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        selectedTab = tab
                    }
                ) {
                    Text(
                        text = tab,
                        color = if (selectedTab == tab) Accent else SecondaryText,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == tab)
                            FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.name.first().toString(),
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.name,
                color = MainText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = chat.message,
                color = SecondaryText,
                fontSize = 13.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = chat.time,
                color = SecondaryText,
                fontSize = 11.sp
            )

            if (chat.unread > 0) {
                Spacer(modifier = Modifier.height(5.dp))

                Box(
                    modifier = Modifier
                        .background(Accent, CircleShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = chat.unread.toString(),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    HorizontalDivider(
        color = CardColor,
        modifier = Modifier.padding(start = 80.dp)
    )
}

@Composable
fun EmptySection(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title скоро появятся",
            color = SecondaryText,
            fontSize = 16.sp
        )
    }
}
