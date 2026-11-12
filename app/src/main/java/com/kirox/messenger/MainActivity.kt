package com.kirox.messenger

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Background = Color(0xFF0D111B)
private val Panel = Color(0xFF171E2B)
private val PanelLight = Color(0xFF202A3A)
private val Accent = Color(0xFF8061FF)
private val MainText = Color(0xFFF5F6FA)
private val SecondaryText = Color(0xFF929DB0)
private val DividerColor = Color(0xFF263043)

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
    val unread: Int = 0,
    val isBot: Boolean = false,
    val username: String = ""
) {
    val lastMessage: String
        get() = messages.lastOrNull()?.text ?: "Нет сообщений"

    val lastTime: String
        get() = messages.lastOrNull()?.time ?: ""
}

private fun defaultChats() = listOf(
    Chat(
        1, "KayroMess Support", "Личные", "K",
        listOf(Message(1, "Добро пожаловать в KayroMess!", false, "12:30")),
        2, true, "kayromess_support"
    ),
    Chat(
        2, "Избранное", "Личные", "И",
        listOf(Message(2, "Сохранённые сообщения", false, "11:45")),
        0, false, "saved_messages"
    ),
    Chat(
        3, "Новости KayroMess", "Каналы", "N",
        listOf(Message(3, "Обновление приложения", false, "10:20")),
        1, true, "kayromess_news"
    ),
    Chat(
        4, "Друзья", "Группы", "Д",
        listOf(Message(4, "До скорой встречи!", false, "Вчера")),
        0, false, ""
    ),
    Chat(
        5, "KayroMess Team", "Группы", "T",
        listOf(Message(5, "Разработка продолжается", false, "Вчера")),
        0, false, "kayromess_team"
    )
)

private fun saveChats(context: Context, chats: List<Chat>) {
    try {
        val array = JSONArray()

        chats.forEach { chat ->
            val obj = JSONObject()
            obj.put("id", chat.id)
            obj.put("name", chat.name)
            obj.put("category", chat.category)
            obj.put("initials", chat.initials)
            obj.put("unread", chat.unread)
            obj.put("isBot", chat.isBot)
            obj.put("username", chat.username)

            val messages = JSONArray()
            chat.messages.forEach { message ->
                val msg = JSONObject()
                msg.put("id", message.id)
                msg.put("text", message.text)
                msg.put("mine", message.mine)
                msg.put("time", message.time)
                messages.put(msg)
            }

            obj.put("messages", messages)
            array.put(obj)
        }

        context.getSharedPreferences(
            "kayromess_storage",
            Context.MODE_PRIVATE
        ).edit().putString("chats", array.toString()).apply()

    } catch (_: Exception) {
    }
}

private fun loadChats(context: Context): List<Chat> {
    return try {
        val raw = context.getSharedPreferences(
            "kayromess_storage",
            Context.MODE_PRIVATE
        ).getString("chats", null) ?: return defaultChats()

        val array = JSONArray(raw)
        val result = mutableListOf<Chat>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val messagesJson = obj.optJSONArray("messages") ?: JSONArray()
            val messages = mutableListOf<Message>()

            for (j in 0 until messagesJson.length()) {
                val msg = messagesJson.getJSONObject(j)
                messages.add(
                    Message(
                        msg.optInt("id"),
                        msg.optString("text"),
                        msg.optBoolean("mine"),
                        msg.optString("time")
                    )
                )
            }

            result.add(
                Chat(
                    obj.optInt("id"),
                    obj.optString("name"),
                    obj.optString("category"),
                    obj.optString("initials"),
                    messages,
                    obj.optInt("unread"),
                    obj.optBoolean("isBot"),
                    obj.optString("username")
                )
            )
        }

        result.ifEmpty { defaultChats() }
    } catch (_: Exception) {
        defaultChats()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(13, 17, 27)
        window.navigationBarColor = android.graphics.Color.rgb(13, 17, 27)

        setContent {
            MaterialTheme {
                KayroMessApp()
            }
        }
    }
}

@Composable
fun KayroMessApp() {
    val context = androidx.compose.ui.platform.LocalContext.current

    val chats = remember {
        mutableStateListOf<Chat>().apply {
            addAll(loadChats(context))
        }
    }

    var loading by remember { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableStateOf("Чаты") }
    var selectedFilter by rememberSaveable { mutableStateOf("Все") }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedChatId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedPage by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var nextChatId by remember { mutableIntStateOf(10) }
    var nextMessageId by remember { mutableIntStateOf(100) }

    LaunchedEffect(chats.toList()) {
        saveChats(context, chats)
    }

    LaunchedEffect(Unit) {
        delay(800)
        loading = false
    }

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

    if (loading) {
        SplashScreen()
        return
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (selectedChatId == null && selectedPage == null) {
                NavigationBar(containerColor = Panel) {
                    listOf("Чаты", "Контакты", "Каналы", "Профиль").forEach { tab ->
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
                            label = { Text(tab, fontSize = 10.sp) },
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
                (selectedTab == "Чаты" || selectedTab == "Контакты" ||
                 selectedTab == "Каналы")
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            AnimatedContent(
                targetState = Triple(selectedChatId, selectedPage, selectedTab),
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "screen_transition"
            ) { state ->

                val chat = chats.firstOrNull { it.id == state.first }

                when {
                    chat != null -> {
                        ChatScreen(
                            chat = chat,
                            onBack = { selectedChatId = null },
                            onProfile = { selectedPage = "Профиль собеседника" },
                            onSend = { text ->
                                val index = chats.indexOfFirst { it.id == chat.id }
                                if (index != -1) {
                                    val updated = chat.messages + Message(
                                        nextMessageId++,
                                        text,
                                        true,
                                        SimpleDateFormat(
                                            "HH:mm",
                                            Locale.getDefault()
                                        ).format(Date())
                                    )

                                    chats[index] = chat.copy(
                                        messages = updated,
                                        unread = 0
                                    )
                                }
                            }
                        )
                    }

                    state.second != null -> {
                        val title = state.second ?: ""
                        if (title == "Профиль собеседника") {
                            val profileChat = chats.firstOrNull {
                                it.id == selectedChatId
                            }
                            if (profileChat != null) {
                                ContactProfileScreen(
                                    chat = profileChat,
                                    onBack = { selectedPage = null }
                                )
                            } else {
                                DetailScreen(
                                    title,
                                    { selectedPage = null },
                                    { dialogMessage = it }
                                )
                            }
                        } else {
                            DetailScreen(
                                title,
                                { selectedPage = null },
                                { dialogMessage = it }
                            )
                        }
                    }

                    else -> {
                        Column(Modifier.fillMaxSize()) {
                            MainHeader {
                                selectedTab = "Профиль"
                            }

                            when (state.third) {
                                "Чаты" -> ChatsScreen(
                                    chats,
                                    search,
                                    { search = it },
                                    selectedFilter,
                                    { selectedFilter = it },
                                    { selectedChatId = it.id }
                                )

                                "Контакты" -> ContactsScreen(
                                    chats,
                                    { openOrCreateChat(it) }
                                )

                                "Каналы" -> ChannelsScreen(
                                    chats,
                                    { selectedChatId = it.id }
                                )

                                "Профиль" -> ProfileScreen {
                                    selectedPage = it
                                }
                            }
                        }
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
            title = { Text("Новый чат", color = MainText) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя собеседника") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        openOrCreateChat(name.trim())
                        showCreateDialog = false
                    }
                }) {
                    Text("Создать", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена", color = SecondaryText)
                }
            }
        )
    }

    dialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            containerColor = Panel,
            title = { Text("KayroMess", color = MainText) },
            text = { Text(message, color = SecondaryText) },
            confirmButton = {
                TextButton(onClick = { dialogMessage = null }) {
                    Text("Понятно", color = Accent)
                }
            }
        )
    }
}

@Composable
fun SplashScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(88.dp)
                    .background(Accent, RoundedCornerShape(27.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "K",
                    color = Color.White,
                    fontSize = 49.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "KayroMess",
                color = MainText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Общение без границ",
                color = SecondaryText,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(27.dp))

            CircularProgressIndicator(
                color = Accent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
fun MainHeader(onProfileClick: () -> Unit) {
    Row(
        Modifier
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
            Text("Общение без границ", color = SecondaryText, fontSize = 12.sp)
        }

        Box(
            Modifier
                .size(43.dp)
                .background(Accent, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("K", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
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
        Modifier
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
            placeholder = { Text("Поиск сообщений и людей", color = SecondaryText) },
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

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
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
            val matchesSearch = it.name.contains(search, true) ||
                it.lastMessage.contains(search, true)
            val matchesFilter = selectedFilter == "Все" || it.category == selectedFilter
            matchesSearch && matchesFilter
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(filtered, key = { it.id }) { chat ->
                ChatItem(chat) { onChatClick(chat) }
            }
            if (filtered.isEmpty()) {
                item { EmptyState("Чаты не найдены") }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .animateContentSize()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.initials, 54)
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(chat.name, color = MainText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
            Text(chat.lastTime, color = SecondaryText, fontSize = 10.sp)
            if (chat.unread > 0) {
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .background(Accent, CircleShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(chat.unread.toString(), color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
    HorizontalDivider(color = DividerColor, thickness = 0.6.dp)
}

@Composable
fun Avatar(text: String, size: Int) {
    Box(
        Modifier
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
    onProfile: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember(chat.id) { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Panel)
                .clickable { onProfile() }
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
                Text(chat.name, color = MainText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    if (chat.isBot) "Бот" else "Профиль KayroMess",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = DividerColor)

        if (chat.messages.isEmpty()) {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Начните общение", color = SecondaryText)
            }
        } else {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                reverseLayout = false
            ) {
                items(chat.messages, key = { it.id }) { item ->
                    MessageBubble(item)
                }
            }
        }

        Row(
            Modifier
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
                placeholder = { Text("Сообщение...", color = SecondaryText) },
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
                Modifier
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
                Text("➤", color = Color.White, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .widthIn(max = 285.dp)
                .background(
                    if (message.mine) Accent.copy(alpha = 0.85f) else PanelLight,
                    RoundedCornerShape(17.dp)
                )
                .padding(horizontal = 13.dp, vertical = 9.dp)
        ) {
            Text(message.text, color = Color.White, fontSize = 14.sp)
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
fun ContactProfileScreen(chat: Chat, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
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
            Text("Профиль", color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = DividerColor)

        Column(
            Modifier
                .fillMaxWidth()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Avatar(chat.initials, 100)
            Spacer(Modifier.height(16.dp))
            Text(chat.name, color = MainText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))

            Text(
                if (chat.isBot) "Бот" else "Человек",
                color = Accent,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(9.dp))

            Text(
                if (chat.username.isNotBlank()) "@${chat.username}" else "Имя пользователя не установлено",
                color = SecondaryText,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ContactsScreen(chats: List<Chat>, onContactClick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("Контакты", color = MainText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))

        val contacts = chats.filter { it.category == "Личные" }

        LazyColumn {
            items(contacts, key = { it.id }) { chat ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onContactClick(chat.name) }
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(chat.initials, 48)
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text(chat.name, color = MainText, fontSize = 15.sp)
                        Text(if (chat.isBot) "Бот" else "Человек", color = SecondaryText, fontSize = 11.sp)
                    }
                }
                HorizontalDivider(color = DividerColor)
            }
        }
    }
}

@Composable
fun ChannelsScreen(chats: List<Chat>, onChannelClick: (Chat) -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("Каналы", color = MainText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))

        val channels = chats.filter { it.category == "Каналы" }

        if (channels.isEmpty()) {
            EmptyState("Пока нет каналов")
        } else {
            LazyColumn {
                items(channels, key = { it.id }) {
                    ChatItem(it) { onChannelClick(it) }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(onOptionClick: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Avatar("K", 100)
                Spacer(Modifier.height(15.dp))
                Text("Мой профиль", color = MainText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Настройки аккаунта KayroMess", color = SecondaryText, fontSize = 12.sp)
            }

            Text(
                "АККАУНТ",
                color = SecondaryText,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(
            listOf(
                "Личные данные",
                "Безопасность",
                "Уведомления",
                "Оформление",
                "Kayro Coins",
                "О приложении"
            )
        ) {
            ProfileOption(it) { onOptionClick(it) }
        }
    }
}

@Composable
fun ProfileOption(title: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onClick() },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = MainText, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("›", color = Accent, fontSize = 24.sp)
        }
    }
}

@Composable
fun DetailScreen(title: String, onBack: () -> Unit, onAction: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(Panel).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", color = Accent, fontSize = 34.sp, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(12.dp))
            Text(title, color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = DividerColor)

        Column(Modifier.fillMaxSize().padding(18.dp)) {
            when (title) {
                "Kayro Coins" -> {
                    Text("Баланс Kayro Coins", color = SecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("0 KC", color = Accent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    DetailAction("История операций") {
                        onAction("История операций появится после подключения системы Kayro Coins.")
                    }
                    DetailAction("Подарки") {
                        onAction("Каталог подарков будет добавлен в следующем обновлении.")
                    }
                }

                "Личные данные" -> {
                    DetailAction("Изменить имя") { onAction("Редактирование имени появится после подключения аккаунта.") }
                    DetailAction("Изменить фотографию") { onAction("Загрузка фотографии появится после подключения профиля.") }
                    DetailAction("Имя пользователя") { onAction("Настройка имени пользователя будет добавлена.") }
                }

                "Безопасность" -> {
                    DetailAction("Активные сеансы") { onAction("Список сеансов появится после подключения сервера.") }
                    DetailAction("Облачный пароль") { onAction("Облачный пароль будет добавлен вместе с серверной авторизацией.") }
                }

                "Уведомления" -> {
                    DetailAction("Звуки уведомлений") { onAction("Настройки звуков будут добавлены.") }
                    DetailAction("Уведомления о сообщениях") { onAction("Настройки уведомлений будут добавлены.") }
                }

                "Оформление" -> {
                    DetailAction("Тёмная тема") { onAction("Сейчас используется тёмная тема KayroMess.") }
                    DetailAction("Цвет интерфейса") { onAction("Дополнительные цвета появятся в следующем обновлении.") }
                }

                else -> {
                    Text("KayroMess", color = MainText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Версия 1.0", color = SecondaryText)
                    Spacer(Modifier.height(12.dp))
                    Text("Самостоятельный мессенджер KayroMess.", color = SecondaryText)
                }
            }
        }
    }
}

@Composable
fun DetailAction(title: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = MainText, modifier = Modifier.weight(1f))
            Text("›", color = Accent, fontSize = 23.sp)
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        Modifier.fillMaxWidth().padding(35.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = SecondaryText, fontSize = 14.sp)
    }
}
