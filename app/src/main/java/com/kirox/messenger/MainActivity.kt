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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class AppPalette(
    val background: Color,
    val panel: Color,
    val panelLight: Color,
    val accent: Color,
    val mainText: Color,
    val secondaryText: Color,
    val divider: Color
)

private val DarkPalette = AppPalette(
    Color(0xFF0D111B), Color(0xFF171E2B), Color(0xFF202A3A),
    Color(0xFF8061FF), Color(0xFFF5F6FA), Color(0xFF929DB0),
    Color(0xFF263043)
)

private val LightPalette = AppPalette(
    Color(0xFFF5F7FC), Color(0xFFFFFFFF), Color(0xFFECEFF6),
    Color(0xFF7052E8), Color(0xFF172033), Color(0xFF778196),
    Color(0xFFE0E5EF)
)

private var activePalette by mutableStateOf(DarkPalette)
private val Background get() = activePalette.background
private val Panel get() = activePalette.panel
private val PanelLight get() = activePalette.panelLight
private val Accent get() = activePalette.accent
private val MainText get() = activePalette.mainText
private val SecondaryText get() = activePalette.secondaryText
private val DividerColor get() = activePalette.divider

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

/**
 * Displays a PNG from res/drawable-nodpi by its filename without extension.
 * If the resource is absent, a text fallback is shown instead of crashing.
 */
@Composable
fun AppIcon(
    name: String,
    fallback: String,
    size: Dp = 24.dp
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resourceId = remember(name, context) {
        context.resources.getIdentifier(
            name,
            "drawable",
            context.packageName
        )
    }

    if (resourceId != 0) {
        KayroSpriteIcon(
            resource = resourceId,
            size = size,
            removeBackground = true
        )
    } else {
        Text(
            text = fallback,
            color = Accent,
            fontSize = size.value.sp
        )
    }
}

@Composable
fun KayroMessApp() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedTheme by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences("kayromess_storage", Context.MODE_PRIVATE)
                .getString("theme", "Тёмная") ?: "Тёмная"
        )
    }
    SideEffect {
        activePalette = if (selectedTheme == "Светлая") LightPalette else DarkPalette
        val activity = context as? ComponentActivity
        activity?.window?.statusBarColor = activePalette.background.toArgb()
        activity?.window?.navigationBarColor = activePalette.background.toArgb()
        activity?.window?.decorView?.systemUiVisibility =
            if (selectedTheme == "Светлая") {
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                0
            }
    }

    var profileName by rememberSaveable {
        mutableStateOf(context.getSharedPreferences("kayromess_storage", Context.MODE_PRIVATE)
            .getString("profile_name", "Мой профиль") ?: "Мой профиль")
    }
    var profileUsername by rememberSaveable {
        mutableStateOf(context.getSharedPreferences("kayromess_storage", Context.MODE_PRIVATE)
            .getString("profile_username", "kirox") ?: "kirox")
    }
    var profileBio by rememberSaveable {
        mutableStateOf(context.getSharedPreferences("kayromess_storage", Context.MODE_PRIVATE)
            .getString("profile_bio", "Настройки аккаунта KayroMess") ?: "Настройки аккаунта KayroMess")
    }

    var showProfileEditor by remember { mutableStateOf(false) }
    var showMessageActions by remember { mutableStateOf(false) }
    var showMessageEditor by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    var activeMessage by remember { mutableStateOf<Message?>(null) }
    var activeMessageChatId by remember { mutableIntStateOf(-1) }
    var editedMessageText by remember { mutableStateOf("") }
    var draftProfileName by remember { mutableStateOf("") }
    var draftProfileUsername by remember { mutableStateOf("") }
    var draftProfileBio by remember { mutableStateOf("") }

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
    var profileChatId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var nextChatId by remember { mutableIntStateOf(10) }
    var nextMessageId by remember { mutableIntStateOf(100) }

    LaunchedEffect(chats.toList()) {
        saveChats(context, chats)
    }

    LaunchedEffect(selectedTheme, profileName, profileUsername, profileBio) {
        context.getSharedPreferences("kayromess_storage", Context.MODE_PRIVATE)
            .edit()
            .putString("theme", selectedTheme)
            .putString("profile_name", profileName)
            .putString("profile_username", profileUsername)
            .putString("profile_bio", profileBio)
            .apply()
    }

    LaunchedEffect(Unit) {
        delay(800)
        loading = false
    }

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
                NavigationBar(
                    containerColor = Panel,
                    contentColor = SecondaryText
                ) {
                    listOf("Чаты", "Контакты", "Каналы", "Профиль").forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                when (tab) {
                                    "Чаты" -> AppIcon("chat_bubble", "▤", 22.dp)
                                    "Контакты" -> AppIcon("add_contact", "◉", 22.dp)
                                    "Каналы" -> AppIcon("channel_broadcast", "▥", 22.dp)
                                    else -> AppIcon("profile", "●", 22.dp)
                                }
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
                    AppIcon("compose_new_chat", "+", 30.dp)
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
                            onProfile = {
                                profileChatId = chat.id
                                selectedChatId = null
                                selectedPage = "Профиль собеседника"
                            },
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
                            },
                            onMessageLongPress = { message ->
                                activeMessage = message
                                activeMessageChatId = chat.id
                                showMessageActions = true
                            }
                        )
                    }

                    state.second != null -> {
                        val title = state.second ?: ""
                        if (title == "Профиль собеседника") {
                            val profileChat = chats.firstOrNull {
                                it.id == profileChatId
                            }
                            if (profileChat != null) {
                                ContactProfileScreen(
                                    chat = profileChat,
                                    onBack = {
                                        selectedPage = null
                                        profileChatId = null
                                    }
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
                                { action ->
                                    when (action) {
                                        "THEME_LIGHT" -> selectedTheme = "Светлая"
                                        "THEME_DARK" -> selectedTheme = "Тёмная"
                                        else -> dialogMessage = action
                                    }
                                },
                                selectedTheme
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

                                "Профиль" -> ProfileScreen(
                                    profileName = profileName,
                                    profileUsername = profileUsername,
                                    profileBio = profileBio,
                                    onEditProfile = {
                                        draftProfileName = profileName
                                        draftProfileUsername = profileUsername
                                        draftProfileBio = profileBio
                                        showProfileEditor = true
                                    },
                                    onOptionClick = { selectedPage = it }
                                )
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

    if (showMessageActions && activeMessage != null) {
        val message = activeMessage!!
        AlertDialog(
            onDismissRequest = { showMessageActions = false },
            containerColor = Panel,
            title = { Text("Действия с сообщением", color = MainText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (message.mine) {
                        TextButton(onClick = {
                            editedMessageText = message.text
                            showMessageActions = false
                            showMessageEditor = true
                        }) { Text("Изменить сообщение", color = Accent) }
                        TextButton(onClick = {
                            val index = chats.indexOfFirst { it.id == activeMessageChatId }
                            if (index >= 0) {
                                chats[index] = chats[index].copy(
                                    messages = chats[index].messages.filterNot { it.id == message.id }
                                )
                            }
                            showMessageActions = false
                            activeMessage = null
                        }) { Text("Удалить сообщение", color = Color(0xFFFF6B78)) }
                    }
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("message", message.text))
                        showMessageActions = false
                        dialogMessage = "Текст сообщения скопирован."
                    }) { Text("Копировать", color = MainText) }
                    TextButton(onClick = {
                        showMessageActions = false
                        dialogMessage = "Сообщение: ${message.time}\\n${message.text}"
                    }) { Text("Информация", color = MainText) }
                    TextButton(onClick = {
                        showMessageActions = false
                        showForwardDialog = true
                    }) { Text("Переслать", color = MainText) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMessageActions = false }) {
                    Text("Закрыть", color = SecondaryText)
                }
            }
        )
    }

    if (showMessageEditor && activeMessage != null) {
        AlertDialog(
            onDismissRequest = { showMessageEditor = false },
            containerColor = Panel,
            title = { Text("Изменить сообщение", color = MainText) },
            text = {
                OutlinedTextField(
                    value = editedMessageText,
                    onValueChange = { editedMessageText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MainText,
                        unfocusedTextColor = MainText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = DividerColor
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = activeMessage!!
                    val index = chats.indexOfFirst { it.id == activeMessageChatId }
                    if (index >= 0 && editedMessageText.isNotBlank()) {
                        chats[index] = chats[index].copy(
                            messages = chats[index].messages.map {
                                if (it.id == target.id) it.copy(text = editedMessageText.trim()) else it
                            }
                        )
                    }
                    showMessageEditor = false
                    activeMessage = null
                }) { Text("Сохранить", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showMessageEditor = false }) {
                    Text("Отмена", color = SecondaryText)
                }
            }
        )
    }

    if (showForwardDialog && activeMessage != null) {
        AlertDialog(
            onDismissRequest = { showForwardDialog = false },
            containerColor = Panel,
            title = { Text("Переслать сообщение", color = MainText) },
            text = {
                Column {
                    chats.filter { it.id != activeMessageChatId }.forEach { target ->
                        TextButton(onClick = {
                            val forwarded = activeMessage!!
                            val index = chats.indexOfFirst { it.id == target.id }
                            if (index >= 0) {
                                chats[index] = target.copy(
                                    messages = target.messages + Message(
                                        nextMessageId++,
                                        forwarded.text,
                                        true,
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                    )
                                )
                            }
                            showForwardDialog = false
                            activeMessage = null
                            dialogMessage = "Сообщение переслано в чат «${target.name}»."
                        }) {
                            Text(target.name, color = MainText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showForwardDialog = false }) {
                    Text("Отмена", color = SecondaryText)
                }
            }
        )
    }

    if (showProfileEditor) {
        AlertDialog(
            onDismissRequest = { showProfileEditor = false },
            containerColor = Panel,
            title = { Text("Редактировать профиль", color = MainText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = draftProfileName,
                        onValueChange = { draftProfileName = it },
                        label = { Text("Имя") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draftProfileUsername,
                        onValueChange = { draftProfileUsername = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                        label = { Text("Имя пользователя") },
                        prefix = { Text("@") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draftProfileBio,
                        onValueChange = { draftProfileBio = it },
                        label = { Text("О себе") },
                        minLines = 2,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draftProfileName.isNotBlank() && draftProfileUsername.isNotBlank()) {
                        profileName = draftProfileName.trim()
                        profileUsername = draftProfileUsername.trim()
                        profileBio = draftProfileBio.trim()
                    }
                    showProfileEditor = false
                }) { Text("Сохранить", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showProfileEditor = false }) {
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
            leadingIcon = {
                AppIcon("search", "⌕", 22.dp)
            },
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
    onSend: (String) -> Unit,
    onMessageLongPress: (Message) -> Unit
) {
    var message by remember(chat.id) { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
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
            Row(
                Modifier.weight(1f).clickable { onProfile() },
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        if (chat.isBot) "Бот" else "Профиль KayroMess",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
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
                    MessageBubble(item) { onMessageLongPress(item) }
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
                AppIcon("send_message", "➤", 22.dp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(message: Message, onLongPress: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .widthIn(max = 285.dp)
                .combinedClickable(
                    onClick = { },
                    onLongClick = onLongPress
                )
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
                        Text(
                            if (chat.isBot) "Бот" else "Человек",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
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
fun ProfileScreen(
    profileName: String,
    profileUsername: String,
    profileBio: String,
    onEditProfile: () -> Unit,
    onOptionClick: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Avatar(profileName.take(1).ifBlank { "K" }, 100)
                    Box(
                        Modifier
                            .size(32.dp)
                            .background(Accent, CircleShape)
                            .clickable { onEditProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon("edit_pencil", "✎", 16.dp)
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text(profileName, color = MainText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("@$profileUsername", color = Accent, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(profileBio, color = SecondaryText, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onEditProfile, shape = RoundedCornerShape(14.dp)) {
                    AppIcon("edit_profile", "✎", 17.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Редактировать профиль")
                }
            }
            Text(
                "НАСТРОЙКИ",
                color = SecondaryText,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(listOf("Безопасность", "Уведомления", "Оформление", "Kayro Coins", "О приложении")) {
            ProfileOption(it) { onOptionClick(it) }
        }
    }
}

@Composable
fun ProfileOption(title: String, onClick: () -> Unit) {
    val icon = when (title) {
        "Личные данные" -> "edit_profile"
        "Безопасность" -> "account_security"
        "Уведомления" -> "notification_bell_filled"
        "Оформление" -> "appearance_theme"
        "Kayro Coins" -> "gift"
        "О приложении" -> "about_info"
        else -> "settings"
    }

    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onClick() },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(icon, "◆", 23.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                title,
                color = MainText,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text("›", color = Accent, fontSize = 24.sp)
        }
    }
}

@Composable
fun DetailScreen(
    title: String,
    onBack: () -> Unit,
    onAction: (String) -> Unit,
    selectedTheme: String = "Тёмная"
) {
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
                    DetailAction("Изменить имя") {
                        onAction("Редактирование имени появится после подключения аккаунта.")
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
                    DetailAction("Облачный пароль") {
                        onAction("Облачный пароль будет добавлен вместе с серверной авторизацией.")
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
                    Text("Тема приложения", color = SecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    DetailAction(if (selectedTheme == "Светлая") "✓ Светлая тема" else "Светлая тема") {
                        onAction("THEME_LIGHT")
                    }
                    DetailAction(if (selectedTheme == "Тёмная") "✓ Тёмная тема" else "Тёмная тема") {
                        onAction("THEME_DARK")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Выбор сохраняется на устройстве и применяется ко всем основным экранам.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
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
