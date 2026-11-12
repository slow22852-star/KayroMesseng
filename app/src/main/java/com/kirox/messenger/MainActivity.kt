package com.kirox.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable

private val Background = Color(0xFF0D111B)
private val Panel = Color(0xFF171E2B)
private val PanelLight = Color(0xFF202A3A)
private val Accent = Color(0xFF8061FF)
private val MainText = Color(0xFFF5F6FA)
private val SecondaryText = Color(0xFF929DB0)
private val DividerColor = Color(0xFF263043)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                KayroMessApp()
            }
        }
    }
}

data class Message(
    val id: Int,
    val text: String,
    val mine: Boolean,
    val time: String
)

data class Chat(
    val id: Int,
    val name: String,
    val category: String,
    val initials: String,
    val messages: List<Message>,
    val unread: Int = 0
) {
    val lastMessage: String
        get() = messages.lastOrNull()?.text ?: "Нет сообщений"

    val lastTime: String
        get() = messages.lastOrNull()?.time ?: ""
}

@Composable
fun KayroMessApp() {

    val chats = remember {
        mutableStateListOf(
            Chat(
                1, "KayroMess Support", "Личные", "K",
                listOf(
                    Message(1, "Добро пожаловать в KayroMess!", false, "12:30")
                ), 2
            ),
            Chat(
                2, "Избранное", "Личные", "И",
                listOf(
                    Message(2, "Сохранённые сообщения", false, "11:45")
                )
            ),
            Chat(
                3, "Новости KayroMess", "Каналы", "N",
                listOf(
                    Message(3, "Обновление приложения", false, "10:20")
                ), 1
            ),
            Chat(
                4, "Друзья", "Группы", "Д",
                listOf(
                    Message(4, "До скорой встречи!", false, "Вчера")
                )
            ),
            Chat(
                5, "KayroMess Team", "Группы", "T",
                listOf(
                    Message(5, "Разработка продолжается", false, "Вчера")
                )
            )
        )
    }

    var selectedTab by rememberSaveable { mutableStateOf("Чаты") }
    var selectedFilter by rememberSaveable { mutableStateOf("Все") }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedChatId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedPage by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var nextChatId by remember { mutableIntStateOf(10) }
    var nextMessageId by remember { mutableIntStateOf(100) }

    val selectedChat = chats.firstOrNull { it.id == selectedChatId }

    fun openOrCreateChat(name: String) {
        val existing = chats.firstOrNull {
            it.name.equals(name, ignoreCase = true)
        }

        if (existing != null) {
            selectedChatId = existing.id
        } else {
            val newChat = Chat(
                nextChatId++,
                name,
                "Личные",
                name.take(1).uppercase(),
                emptyList()
            )
            chats.add(0, newChat)
            selectedChatId = newChat.id
        }
    }

    Scaffold(
        containerColor = Background,

        bottomBar = {
            if (selectedChatId == null && selectedPage == null) {
                NavigationBar(
                    containerColor = Panel,
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
                            onClick = { selectedTab = tab },
                            icon = {
                                Text(
                                    when (tab) {
                                        "Чаты" -> "▤"
                                        "Контакты" -> "◉"
                                        "Каналы" -> "▥"
                                        else -> "●"
                                    },
                                    fontSize = 19.sp
                                )
                            },
                            label = {
                                Text(tab, fontSize = 10.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                unselectedIconColor = SecondaryText,
                                unselectedTextColor = SecondaryText,
                                indicatorColor = Accent.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        },

        floatingActionButton = {
            if (selectedChatId == null && selectedPage == null &&
                (selectedTab == "Чаты" || selectedTab == "Контакты")
            ) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Accent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Text("+", fontSize = 28.sp)
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

            when {
                selectedChat != null -> {
                    ChatScreen(
                        chat = selectedChat,
                        onBack = { selectedChatId = null },
                        onSend = { text ->
                            val index = chats.indexOfFirst {
                                it.id == selectedChat.id
                            }

                            if (index != -1) {
                                val updated = selectedChat.messages +
                                    Message(
                                        nextMessageId++,
                                        text,
                                        true,
                                        "Сейчас"
                                    )

                                chats[index] = selectedChat.copy(
                                    messages = updated,
                                    unread = 0
                                )
                            }
                        }
                    )
                }

                selectedPage != null -> {
                    DetailScreen(
                        title = selectedPage!!,
                        onBack = { selectedPage = null },
                        onAction = { dialogMessage = it }
                    )
                }

                else -> {
                    MainHeader(
                        onProfileClick = {
                            selectedTab = "Профиль"
                        }
                    )

                    when (selectedTab) {
                        "Чаты" -> ChatsScreen(
                            chats = chats,
                            search = search,
                            onSearchChange = { search = it },
                            selectedFilter = selectedFilter,
                            onFilterChange = { selectedFilter = it },
                            onChatClick = {
                                selectedChatId = it.id
                            }
                        )

                        "Контакты" -> ContactsScreen(
                            chats = chats,
                            onContactClick = { openOrCreateChat(it) }
                        )

                        "Каналы" -> ChannelsScreen(
                            chats = chats,
                            onChannelClick = {
                                selectedChatId = it.id
                            }
                        )

                        "Профиль" -> ProfileScreen(
                            onOptionClick = {
                                selectedPage = it
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Panel,
            title = {
                Text("Новый чат", color = MainText)
            },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя собеседника") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MainText,
                        unfocusedTextColor = MainText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = DividerColor
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            openOrCreateChat(name.trim())
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Создать", color = Accent)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false }
                ) {
                    Text("Отмена", color = SecondaryText)
                }
            }
        )
    }

    if (dialogMessage != null) {
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            containerColor = Panel,
            title = {
                Text("KayroMess", color = MainText)
            },
            text = {
                Text(dialogMessage!!, color = SecondaryText)
            },
            confirmButton = {
                TextButton(
                    onClick = { dialogMessage = null }
                ) {
                    Text("Понятно", color = Accent)
                }
            }
        )
    }
}

@Composable
fun MainHeader(onProfileClick: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "KayroMess",
                color = MainText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Общение без границ",
                color = SecondaryText,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .size(43.dp)
                .background(Accent, CircleShape)
                .clickable { onProfileClick() },
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

    HorizontalDivider(color = DividerColor)
}

@Composable
fun ChatsScreen(
    chats: List<Chat>,
    search: String,
    onSearchChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onChatClick: (Chat) -> Unit
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
            modifier = Modifier.padding(top = 18.dp, bottom = 13.dp)
        )

        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Поиск сообщений и людей", color = SecondaryText)
            },
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MainText,
                unfocusedTextColor = MainText,
                focusedBorderColor = Accent,
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = Panel,
                unfocusedContainerColor = Panel,
                cursorColor = Accent
            )
        )

        Spacer(Modifier.height(13.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf("Все", "Личные", "Группы", "Каналы").forEach {
                FilterChip(
                    selected = selectedFilter == it,
                    onClick = { onFilterChange(it) },
                    label = { Text(it, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Accent,
                        selectedLabelColor = Color.White,
                        labelColor = SecondaryText
                    )
                )
            }
        }

        val filtered = chats.filter {
            val matchesSearch =
                it.name.contains(search, true) ||
                it.lastMessage.contains(search, true)

            val matchesFilter =
                selectedFilter == "Все" ||
                it.category == selectedFilter

            matchesSearch && matchesFilter
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(filtered, key = { it.id }) { chat ->
                ChatItem(chat) {
                    onChatClick(chat)
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState("Чаты не найдены")
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.initials, 54)

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                chat.name,
                color = MainText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(5.dp))

            Text(
                chat.lastMessage,
                color = SecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                chat.lastTime,
                color = SecondaryText,
                fontSize = 10.sp
            )

            if (chat.unread > 0) {
                Spacer(Modifier.height(7.dp))

                Box(
                    modifier = Modifier
                        .background(Accent, CircleShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
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

    HorizontalDivider(color = DividerColor, thickness = 0.6.dp)
}

@Composable
fun Avatar(text: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Accent.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = Accent,
            fontSize = (size / 2.3f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ChatScreen(
    chat: Chat,
    onBack: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember(chat.id) { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                color = Accent,
                fontSize = 35.sp,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(Modifier.width(12.dp))

            Avatar(chat.initials, 42)

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    chat.name,
                    color = MainText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    "KayroMess",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = DividerColor)

        if (chat.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Начните общение",
                    color = SecondaryText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(chat.messages, key = { it.id }) { item ->
                    MessageBubble(item)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel)
                .imePadding()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Сообщение...", color = SecondaryText)
                },
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MainText,
                    unfocusedTextColor = MainText,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = DividerColor,
                    focusedContainerColor = Background,
                    unfocusedContainerColor = Background,
                    cursorColor = Accent
                )
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Accent, CircleShape)
                    .clickable {
                        if (message.isNotBlank()) {
                            onSend(message.trim())
                            message = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "➤",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.mine) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 285.dp)
                .background(
                    if (message.mine) Accent.copy(alpha = 0.85f)
                    else PanelLight,
                    RoundedCornerShape(17.dp)
                )
                .padding(horizontal = 13.dp, vertical = 9.dp)
        ) {
            Text(
                message.text,
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                message.time,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun ContactsScreen(
    chats: List<Chat>,
    onContactClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            "Контакты",
            color = MainText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(15.dp))

        val contacts = listOf(
            "KayroMess Support",
            "Друзья",
            "KayroMess Team"
        )

        LazyColumn {
            items(contacts) { name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContactClick(name) }
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(name.take(1), 48)

                    Spacer(Modifier.width(13.dp))

                    Text(
                        name,
                        color = MainText,
                        fontSize = 15.sp
                    )
                }

                HorizontalDivider(color = DividerColor)
            }
        }
    }
}

@Composable
fun ChannelsScreen(
    chats: List<Chat>,
    onChannelClick: (Chat) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            "Каналы",
            color = MainText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(15.dp))

        val channels = chats.filter { it.category == "Каналы" }

        if (channels.isEmpty()) {
            EmptyState("Пока нет каналов")
        } else {
            LazyColumn {
                items(channels, key = { it.id }) {
                    ChatItem(it) {
                        onChannelClick(it)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(onOptionClick: (String) -> Unit) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Avatar("K", 100)

                Spacer(Modifier.height(15.dp))

                Text(
                    "Мой профиль",
                    color = MainText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Настройки аккаунта KayroMess",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }

            Text(
                "АККАУНТ",
                color = SecondaryText,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        val options = listOf(
            "Личные данные",
            "Безопасность",
            "Уведомления",
            "Оформление",
            "Kayro Coins",
            "О приложении"
        )

        items(options) {
            ProfileOption(it) {
                onOptionClick(it)
            }
        }
    }
}

@Composable
fun ProfileOption(title: String, onClick: () -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = Panel
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
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
                color = Accent,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun DetailScreen(
    title: String,
    onBack: () -> Unit,
    onAction: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Panel)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                color = Accent,
                fontSize = 34.sp,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(Modifier.width(12.dp))

            Text(
                title,
                color = MainText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(color = DividerColor)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            when (title) {
                "Kayro Coins" -> {
                    Text(
                        "Баланс Kayro Coins",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "0 KC",
                        color = Accent,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(20.dp))

                    DetailAction("История операций") {
                        onAction("История операций появится после подключения системы Kayro Coins.")
                    }

                    DetailAction("Подарки") {
                        onAction("Каталог подарков будет добавлен в следующем обновлении.")
                    }
                }

                "Личные данные" -> {
                    DetailAction("Изменить имя") {
                        onAction("Редактирование имени будет подключено к аккаунту.")
                    }
                    DetailAction("Изменить фотографию") {
                        onAction("Загрузка фотографии появится после подключения профиля.")
                    }
                    DetailAction("Имя пользователя") {
                        onAction("Настройка имени пользователя будет добавлена.")
                    }
                }

                "Безопасность" -> {
                    DetailAction("Активные сеансы") {
                        onAction("Список сеансов появится после подключения сервера.")
                    }
                    DetailAction("Защита аккаунта") {
                        onAction("Настройки защиты будут добавлены.")
                    }
                }

                "Уведомления" -> {
                    DetailAction("Звуки уведомлений") {
                        onAction("Настройки звуков будут добавлены.")
                    }
                    DetailAction("Уведомления о сообщениях") {
                        onAction("Настройки уведомлений будут добавлены.")
                    }
                }

                "Оформление" -> {
                    DetailAction("Тёмная тема") {
                        onAction("Сейчас используется тёмная тема KayroMess.")
                    }
                    DetailAction("Цвет интерфейса") {
                        onAction("Дополнительные цвета появятся в следующем обновлении.")
                    }
                }

                else -> {
                    Text(
                        "KayroMess",
                        color = MainText,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Версия 1.0",
                        color = SecondaryText
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Самостоятельный мессенджер KayroMess.",
                        color = SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun DetailAction(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Panel
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = MainText,
                modifier = Modifier.weight(1f)
            )

            Text("›", color = Accent, fontSize = 23.sp)
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(35.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = SecondaryText,
            fontSize = 14.sp
        )
    }
}
