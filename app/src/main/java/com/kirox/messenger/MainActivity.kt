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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF0D1018)
private val CardColor = Color(0xFF191E29)
private val Accent = Color(0xFF805CFF)
private val MainText = Color(0xFFFFFFFF)
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
    val time: String,
    val initials: String,
    val category: String,
    val unread: Int = 0
)

private val chats = listOf(
    Chat("KayroMess Support", "Добро пожаловать в KayroMess!", "12:30", "K", "Личные", 2),
    Chat("Избранное", "Сохранённые сообщения", "11:45", "И", "Личные"),
    Chat("Новости KayroMess", "Обновление приложения", "10:20", "N", "Каналы", 1),
    Chat("Друзья", "До скорой встречи!", "Вчера", "Д", "Группы"),
    Chat("KayroMess Team", "Разработка продолжается", "Вчера", "T", "Группы")
)

@Composable
fun KayroMessApp() {

    var selectedTab by remember { mutableStateOf("Чаты") }
    var selectedFilter by remember { mutableStateOf("Все") }
    var search by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Background,

        bottomBar = {
            NavigationBar(
                containerColor = CardColor,
                contentColor = MainText
            ) {
                val tabs = listOf(
                    "Чаты",
                    "Контакты",
                    "Каналы",
                    "Профиль"
                )

                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                        },
                        icon = {},
                        label = {
                            Text(
                                tab,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Accent,
                            selectedTextColor = Accent,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = Accent.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },

        floatingActionButton = {
            if (selectedTab == "Чаты" || selectedTab == "Контакты") {
                FloatingActionButton(
                    onClick = {
                        selectedTab = "Контакты"
                    },
                    containerColor = Accent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Text(
                        "+",
                        fontSize = 30.sp
                    )
                }
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {

            // HEADER

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 18.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        "KayroMess",
                        color = MainText,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Общение без границ",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(43.dp)
                        .background(Accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "K",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                color = CardColor
            )

            when (selectedTab) {

                "Чаты" -> {

                    ChatsScreen(
                        search = search,
                        onSearchChange = { search = it },
                        selectedFilter = selectedFilter,
                        onFilterChange = {
                            selectedFilter = it
                        }
                    )
                }

                "Контакты" -> {

                    SimpleScreen(
                        title = "Контакты",
                        description = "Здесь будут твои контакты"
                    )
                }

                "Каналы" -> {

                    SimpleScreen(
                        title = "Каналы",
                        description = "Новости и интересные каналы"
                    )
                }

                "Профиль" -> {

                    ProfileScreen()
                }
            }
        }
    }
}

@Composable
fun ChatsScreen(
    search: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {

        Text(
            "Сообщения",
            color = MainText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                top = 20.dp,
                bottom = 15.dp
            )
        )

        // SEARCH

        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Поиск сообщений и людей",
                    color = SecondaryText
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MainText,
                unfocusedTextColor = MainText,
                focusedBorderColor = Accent,
                unfocusedBorderColor = CardColor,
                focusedContainerColor = CardColor,
                unfocusedContainerColor = CardColor,
                cursorColor = Accent
            )
        )

        Spacer(Modifier.height(16.dp))

        // FILTERS

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            listOf(
                "Все",
                "Личные",
                "Группы",
                "Каналы"
            ).forEach { filter ->

                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = {
                        onFilterChange(filter)
                    },
                    label = {
                        Text(filter)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Accent,
                        selectedLabelColor = Color.White,
                        labelColor = SecondaryText
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // CHAT LIST

        val filteredChats = chats.filter { chat ->

            val matchesSearch =
                chat.name.contains(search, ignoreCase = true) ||
                chat.message.contains(search, ignoreCase = true)

            val matchesFilter =
                selectedFilter == "Все" ||
                chat.category == selectedFilter

            matchesSearch && matchesFilter
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            items(
                filteredChats,
                key = { it.name }
            ) { chat ->

                ChatItem(chat)
            }

            if (filteredChats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(35.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Ничего не найдено",
                            color = SecondaryText
                        )
                    }
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
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // AVATAR

        Box(
            modifier = Modifier
                .size(55.dp)
                .background(
                    Accent.copy(alpha = 0.22f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                chat.initials,
                color = Accent,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(13.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                chat.name,
                color = MainText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                chat.message,
                color = SecondaryText,
                fontSize = 13.sp,
                maxLines = 1
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {

            Text(
                chat.time,
                color = SecondaryText,
                fontSize = 11.sp
            )

            if (chat.unread > 0) {

                Spacer(Modifier.height(7.dp))

                Box(
                    modifier = Modifier
                        .background(
                            Accent,
                            CircleShape
                        )
                        .padding(
                            horizontal = 7.dp,
                            vertical = 3.dp
                        )
                ) {

                    Text(
                        chat.unread.toString(),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    HorizontalDivider(
        color = CardColor,
        thickness = 0.7.dp
    )
}

@Composable
fun SimpleScreen(
    title: String,
    description: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            title,
            color = MainText,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Text(
            description,
            color = SecondaryText,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ProfileScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(35.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {

            Text(
                "K",
                color = Color.White,
                fontSize = 43.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "Мой профиль",
            color = MainText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Настройки аккаунта KayroMess",
            color = SecondaryText,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(30.dp))

        ProfileOption("Личные данные")
        ProfileOption("Безопасность")
        ProfileOption("Уведомления")
        ProfileOption("Оформление")
        ProfileOption("Kayro Coins")
        ProfileOption("О приложении")
    }
}

@Composable
fun ProfileOption(title: String) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardColor
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                title,
                color = MainText,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                "›",
                color = SecondaryText,
                fontSize = 23.sp
            )
        }
    }
}
