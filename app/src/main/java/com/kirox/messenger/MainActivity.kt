package com.kirox.messenger

import android.content.Context
import android.net.Uri
import android.os.Build
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.widget.MediaController
import android.os.Bundle
import android.graphics.Color as AndroidColor
import android.widget.ImageView
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private var darkPalette by mutableStateOf(false)
private val Ink get() = if (darkPalette) Color(0xFFF3F4F8) else Color(0xFF20242C)
private val Muted get() = if (darkPalette) Color(0xFFA3A9B5) else Color(0xFF858B96)
private val Line get() = if (darkPalette) Color(0xFF303744) else Color(0xFFE9EBEF)
private val Accent = Color(0xFF5D70D6)
private val Page get() = if (darkPalette) Color(0xFF111722) else Color(0xFFF7F8FA)
private val SurfaceWhite get() = if (darkPalette) Color(0xFF1B2431) else Color.White
private val PaleAccent get() = if (darkPalette) Color(0xFF29304D) else Color(0xFFEEF0FF)

private val MAIN_TABS = listOf("Чаты", "Контакты", "Каналы", "Профиль")

data class KMMessage(
    val id: Long,
    val text: String,
    val mine: Boolean,
    val time: String,
    val reaction: String = "",
    val reply: String = "",
    val edited: Boolean = false,
    val replyToId: Long? = null,
    val forwardedFrom: String = "",
    val mediaUri: String = "",
    val mediaType: String = "",
    val replyPreview: String = ""
)

data class KMChat(
    val id: Long,
    val title: String,
    val kind: String,
    val subtitle: String = "",
    val messages: List<KMMessage> = emptyList()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(247, 248, 250)
        window.navigationBarColor = AndroidColor.rgb(255, 255, 255)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { KayroMessRoot() }
    }
}

@Composable
private fun KayroMessRoot() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kayromess_v2", Context.MODE_PRIVATE) }
    var chats by remember { mutableStateOf(loadChats(prefs)) }
    LaunchedEffect(Unit) { darkPalette = prefs.getBoolean("dark_theme", false) }
    var tab by rememberSaveable { mutableStateOf("Чаты") }
    var activeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var createOpen by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<Pair<Long, KMMessage>?>(null) }
    var forwardingMessage by remember { mutableStateOf<Pair<Long, KMMessage>?>(null) }
    var infoMessage by remember { mutableStateOf<KMMessage?>(null) }
    var editor by remember { mutableStateOf<Pair<Long, KMMessage>?>(null) }
    var profileName by rememberSaveable { mutableStateOf(prefs.getString("name", "Пользователь") ?: "Пользователь") }
    var profileUsername by rememberSaveable { mutableStateOf(prefs.getString("username", "kirox") ?: "kirox") }
    var profileBio by rememberSaveable { mutableStateOf(prefs.getString("bio", "") ?: "") }
    var profileEdit by remember { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var replyToId by rememberSaveable { mutableStateOf<Long?>(null) }
    var settingsPage by rememberSaveable { mutableStateOf("") }
    var securityLock by rememberSaveable { mutableStateOf(prefs.getBoolean("security_lock", false)) }
    var notifications by rememberSaveable { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var hidePhone by rememberSaveable { mutableStateOf(prefs.getBoolean("hide_phone", true)) }
    var hideLastSeen by rememberSaveable { mutableStateOf(prefs.getBoolean("hide_last_seen", false)) }
    var stickerPanel by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val active = chats.firstOrNull { it.id == activeId }

    fun persist(updated: List<KMChat>) {
        chats = updated
        saveChats(prefs, updated)
    }
    fun updateMessage(chatId: Long, message: KMMessage) {
        persist(chats.map { c -> if (c.id == chatId) c.copy(messages = c.messages.map { if (it.id == message.id) message else it }) else c })
    }
    fun sendMessage(text: String) {
        val target = chats.firstOrNull { it.id == activeId } ?: return
        if (text.isBlank()) return
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val message = KMMessage(System.currentTimeMillis(), text.trim(), true, now, replyToId = replyToId, replyPreview = target.messages.firstOrNull { it.id == replyToId }?.text?.take(90) ?: "")
        persist(chats.map { if (it.id == target.id) it.copy(messages = it.messages + message) else it })
        draft = ""
        replyToId = null
    }
    fun sendMedia(uri: Uri, type: String) {
        val target = chats.firstOrNull { it.id == activeId } ?: return
        try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val message = KMMessage(System.currentTimeMillis(), "", true, now, replyToId = replyToId, mediaUri = uri.toString(), mediaType = type, replyPreview = target.messages.firstOrNull { it.id == replyToId }?.text?.take(90) ?: "Вложение")
        persist(chats.map { if (it.id == target.id) it.copy(messages = it.messages + message) else it })
        replyToId = null
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { sendMedia(it, "image") } }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { sendMedia(it, "video") } }
    val gifPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { sendMedia(it, "gif") } }

    MaterialTheme(colorScheme = if (darkPalette) darkColorScheme(primary = Accent, background = Page, surface = SurfaceWhite, onSurface = Ink, onBackground = Ink) else lightColorScheme(primary = Accent, onPrimary = Color.White, background = Page, surface = SurfaceWhite, onSurface = Ink, onBackground = Ink, outline = Line, secondary = Muted)) {
        Surface(Modifier.fillMaxSize(), color = Page) {
            if (active != null) {
                ChatPage(
                    chat = active, draft = draft, replyToId = replyToId,
                    onDraft = { draft = it }, onBack = { activeId = null; replyToId = null },
                    onSend = { sendMessage(draft) }, onMessageTap = { selectedMessage = active.id to it },
                    onCancelReply = { replyToId = null },
                    onReplyJump = { /* ChatPage handles scrolling internally */ },
                    onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                    onPickVideo = { videoPicker.launch(arrayOf("video/*")) },
                    onPickGif = { gifPicker.launch(arrayOf("image/gif")) },
                    onOpenStickers = { stickerPanel = true },
                    onSendSticker = { sticker ->
                        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        persist(chats.map { c -> if (c.id == active.id) c.copy(messages = c.messages + KMMessage(System.currentTimeMillis(), sticker, true, now, replyToId = replyToId, mediaType = "sticker", replyPreview = c.messages.firstOrNull { it.id == replyToId }?.text?.take(90) ?: "")) else c })
                        replyToId = null
                        stickerPanel = false
                    },
                    onTitleTap = { settingsPage = "Информация о чате" }
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.weight(1f).fillMaxWidth().pointerInput(tab) {
                            var drag = 0f
                            detectHorizontalDragGestures(onHorizontalDrag = { _, amount -> drag += amount }, onDragEnd = {
                                val index = MAIN_TABS.indexOf(tab).coerceAtLeast(0)
                                if (drag < -70f) tab = MAIN_TABS[(index + 1).coerceAtMost(MAIN_TABS.lastIndex)]
                                if (drag > 70f) tab = MAIN_TABS[(index - 1).coerceAtLeast(0)]
                                drag = 0f
                            }, onDragCancel = { drag = 0f })
                        }
                    ) {
                        AnimatedContent(targetState = if (settingsPage.isNotBlank()) "settings:$settingsPage" else tab, transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) }, label = "main-pages") { page ->
                            if (page.startsWith("settings:")) {
                                SettingsPage(
                                    title = page.removePrefix("settings:"),
                                    dark = darkPalette, securityLock = securityLock, notifications = notifications,
                                    hidePhone = hidePhone, hideLastSeen = hideLastSeen,
                                    onBack = { settingsPage = "" },
                                    onDark = { darkPalette = it; prefs.edit().putBoolean("dark_theme", it).apply() },
                                    onSecurity = { securityLock = it; prefs.edit().putBoolean("security_lock", it).apply() },
                                    onNotifications = { notifications = it; prefs.edit().putBoolean("notifications", it).apply() },
                                    onHidePhone = { hidePhone = it; prefs.edit().putBoolean("hide_phone", it).apply() },
                                    onHideLastSeen = { hideLastSeen = it; prefs.edit().putBoolean("hide_last_seen", it).apply() }
                                )
                            } else when (page) {
                                "Профиль" -> ProfilePage(profileName, profileUsername, profileBio, onEdit = { profileEdit = true }, onOptionClick = { settingsPage = it })
                                "Контакты" -> ContactsPage(chats.filter { it.kind == "Личный чат" }, onOpen = { activeId = it.id })
                                "Каналы" -> MainListPage("Каналы", chats.filter { it.kind == "Канал" || it.kind == "Группа" }, { activeId = it.id }, { createOpen = true }, showSearch = false)
                                else -> MainListPage("KayroMess", chats.filter { it.title.contains(search, true) || it.subtitle.contains(search, true) }, { activeId = it.id }, { createOpen = true }, search, { search = it }, showSearch = true)
                            }
                        }
                    }
                    BottomNavBar(tab, { tab = it; settingsPage = "" })
                }
            }
        }
    }

    if (createOpen) CreateChatSheet(onDismiss = { createOpen = false }) { title, kind ->
        val newChat = KMChat(System.currentTimeMillis(), title, kind, if (kind == "Канал") "Новый канал" else "Недавно создано")
        persist(listOf(newChat) + chats)
        createOpen = false
        activeId = newChat.id
    }

    selectedMessage?.let { (chatId, message) ->
        MessageActionsSheet(message, onDismiss = { selectedMessage = null },
            onReact = { reaction -> updateMessage(chatId, message.copy(reaction = if (message.reaction == reaction) "" else reaction)); selectedMessage = null },
            onReply = { replyToId = message.id; activeId = chatId; selectedMessage = null },
            onEdit = { if (message.mine) editor = chatId to message; selectedMessage = null },
            onDelete = { persist(chats.map { c -> if (c.id == chatId) c.copy(messages = c.messages.filterNot { it.id == message.id }) else c }); selectedMessage = null },
            onCopy = { clipboard.setText(AnnotatedString(message.text)); selectedMessage = null },
            onForward = { forwardingMessage = chatId to message; selectedMessage = null },
            onInfo = { infoMessage = message; selectedMessage = null }
        )
    }

    forwardingMessage?.let { (sourceChatId, message) ->
        ForwardPickerSheet(chats.filter { it.id != sourceChatId && it.kind != "Канал" }, onDismiss = { forwardingMessage = null }) { target ->
            val source = chats.firstOrNull { it.id == sourceChatId }?.title ?: "Неизвестный чат"
            val forwarded = message.copy(id = System.currentTimeMillis(), mine = true, time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), reply = "", replyToId = null, forwardedFrom = source)
            persist(chats.map { if (it.id == target.id) it.copy(messages = it.messages + forwarded) else it })
            forwardingMessage = null
        }
    }

    infoMessage?.let { message ->
        AlertDialog(onDismissRequest = { infoMessage = null }, title = { Text("Информация о сообщении") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Отправлено: ${SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(message.id))}")
                Text(if (message.edited) "Сообщение изменено" else "Сообщение не изменялось")
                Text(if (message.mine) "Отправитель: вы" else "Отправитель: собеседник")
                Text("Статус: сохранено на этом устройстве. Данные о прочтении пока не синхронизируются с сервером.", fontSize = 12.sp, color = Muted)
            } }, confirmButton = { TextButton(onClick = { infoMessage = null }) { Text("Закрыть") } })
    }

    editor?.let { (chatId, old) -> EditMessageDialog(old.text, onDismiss = { editor = null }) { newText -> updateMessage(chatId, old.copy(text = newText, edited = true)); editor = null } }

    if (profileEdit) ProfileEditDialog(profileName, profileUsername, profileBio, onDismiss = { profileEdit = false }) { n, u, b ->
        profileName = n; profileUsername = u.removePrefix("@"); profileBio = b
        prefs.edit().putString("name", n).putString("username", profileUsername).putString("bio", b).apply()
        profileEdit = false
    }

    if (stickerPanel) StickerPickerSheet(onDismiss = { stickerPanel = false }, onPick = { sticker ->
        val target = chats.firstOrNull { it.id == activeId }
        if (target != null) {
            val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            persist(chats.map { c -> if (c.id == target.id) c.copy(messages = c.messages + KMMessage(System.currentTimeMillis(), sticker, true, now, replyToId = replyToId, mediaType = "sticker", replyPreview = c.messages.firstOrNull { it.id == replyToId }?.text?.take(90) ?: "")) else c })
            replyToId = null
        }
        stickerPanel = false
    })
}

@Composable
private fun BottomNavBar(currentTab: String, onTab: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(SurfaceWhite).border(1.dp, Line).padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf("Чаты" to "chat_bubble", "Контакты" to "add_contact", "Каналы" to "channel_broadcast", "Профиль" to "profile").forEach { (label, icon) ->
            Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onTab(label) }.padding(horizontal = 10.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SpriteOrText(icon, "•", 22)
                Text(label, fontSize = 10.sp, color = if (currentTab == label) Accent else Muted)
            }
        }
    }
}

@Composable
private fun MainListPage(title: String, items: List<KMChat>, onOpen: (KMChat) -> Unit, onCreate: () -> Unit, search: String = "", onSearch: (String) -> Unit = {}, showSearch: Boolean = false) {
    Column(Modifier.fillMaxSize().background(Page)) {
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Общение без лишнего", fontSize = 12.sp, color = Muted) }
            Box(Modifier.size(42.dp).clip(CircleShape).background(PaleAccent).clickable { onCreate() }, Alignment.Center) { SpriteOrText("compose_new_chat", "+", 22) }
        }
        if (showSearch) TextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(54.dp), placeholder = { Text("Поиск", color = Muted) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = TextFieldDefaults.colors(focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        if (items.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Пока здесь пусто", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 17.sp); Spacer(Modifier.height(6.dp)); Text("Создай чат, группу или канал", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(16.dp)); Button(onClick = onCreate, shape = RoundedCornerShape(14.dp)) { Text("Создать") } } }
        else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) { items(items, key = { it.id }) { chat -> ChatRow(chat) { onOpen(chat) } } }
    }
}

@Composable
private fun ChatRow(chat: KMChat, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.background(SurfaceWhite).padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(51.dp).clip(CircleShape).background(PaleAccent), Alignment.Center) { Text(chat.title.take(1).uppercase(), color = Accent, fontWeight = FontWeight.Bold, fontSize = 19.sp) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) { Text(chat.title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(chat.messages.lastOrNull()?.let { if (it.mediaType.isNotBlank()) "Вложение" else it.text } ?: chat.subtitle.ifBlank { chat.kind }, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        chat.messages.lastOrNull()?.let { Text(it.time, color = Muted, fontSize = 11.sp) }
    }
    HorizontalDivider(color = Line, thickness = 0.6.dp, modifier = Modifier.padding(start = 82.dp))
}

@Composable
private fun ContactsPage(contacts: List<KMChat>, onOpen: (KMChat) -> Unit) {
    Column(Modifier.fillMaxSize().background(Page)) {
        Text("Контакты", Modifier.padding(20.dp), fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        if (contacts.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { Text("Контактов пока нет", color = Muted) }
        else LazyColumn { items(contacts, key = { it.id }) { ChatRow(it) { onOpen(it) } } }
    }
}

@Composable
private fun ProfilePage(name: String, username: String, bio: String, onEdit: () -> Unit, onOptionClick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Page).verticalScroll(rememberScrollState())) {
        Text("Профиль", Modifier.padding(start = 20.dp, top = 22.dp, bottom = 18.dp), fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(82.dp).clip(CircleShape).background(PaleAccent), Alignment.Center) { Text(name.take(1).uppercase(), fontSize = 30.sp, color = Accent, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp)); Text(name, fontSize = 19.sp, color = Ink, fontWeight = FontWeight.Bold); Text("@$username", color = Accent, fontSize = 13.sp)
            if (bio.isNotBlank()) Text(bio, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
            Spacer(Modifier.height(16.dp)); OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp)) { SpriteOrText("edit_profile", "✎", 17); Spacer(Modifier.width(7.dp)); Text("Изменить профиль") }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.background(SurfaceWhite).fillMaxWidth().padding(horizontal = 18.dp)) {
            ProfileItem("account_security", "Безопасность", "Защита приложения") { onOptionClick("Безопасность") }
            ProfileItem("notification_bell_filled", "Уведомления", "Настройки уведомлений") { onOptionClick("Уведомления") }
            ProfileItem("privacy_shield", "Конфиденциальность", "Видимость данных") { onOptionClick("Конфиденциальность") }
            ProfileItem("appearance_theme", "Оформление", "Тема приложения") { onOptionClick("Оформление") }
            ProfileItem("language_globe", "Язык", "Русский") { onOptionClick("Язык") }
            ProfileItem("gift", "Kayro Coins", "Внутренние подарки") { onOptionClick("Kayro Coins") }
            ProfileItem("about_info", "О приложении", "KayroMess") { onOptionClick("О приложении") }
        }
    }
}

@Composable
private fun ProfileItem(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Page), Alignment.Center) { SpriteOrText(icon, "•", 20) }
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text(subtitle, color = Muted, fontSize = 11.sp) }
        Text("›", color = Muted, fontSize = 22.sp)
    }
    HorizontalDivider(color = Line, thickness = 0.6.dp)
}

@Composable
private fun SettingsPage(title: String, dark: Boolean, securityLock: Boolean, notifications: Boolean, hidePhone: Boolean, hideLastSeen: Boolean, onBack: () -> Unit, onDark: (Boolean) -> Unit, onSecurity: (Boolean) -> Unit, onNotifications: (Boolean) -> Unit, onHidePhone: (Boolean) -> Unit, onHideLastSeen: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().background(Page).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", Modifier.clickable { onBack() }.padding(end = 14.dp), fontSize = 30.sp, color = Ink)
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        when (title) {
            "Безопасность" -> Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(18.dp)) {
                SettingSwitch("Блокировка приложения", "Локальная настройка. Биометрическая проверка пока не подключена.", securityLock, onSecurity)
                Text("Не используй эту настройку как замену системной блокировке телефона.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            }
            "Уведомления" -> Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(18.dp)) {
                SettingSwitch("Уведомления приложения", "Сохранение предпочтения уведомлений на устройстве.", notifications, onNotifications)
                Text("Push-уведомления между разными пользователями появятся после подключения сервера.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            }
            "Конфиденциальность" -> Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(18.dp)) {
                SettingSwitch("Скрывать номер телефона", "Локальная настройка видимости.", hidePhone, onHidePhone)
                SettingSwitch("Скрывать время посещения", "Локальная настройка видимости.", hideLastSeen, onHideLastSeen)
                Text("Пока эти параметры сохраняются в приложении и не передаются другим пользователям.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            }
            "Оформление" -> Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(18.dp)) {
                SettingChoice("Светлая тема", !dark) { onDark(false) }
                SettingChoice("Тёмная тема", dark) { onDark(true) }
            }
            "Язык" -> Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(18.dp)) {
                SettingChoice("Русский", true) { }
                Text("Другие языки пока не добавлены.", color = Muted, fontSize = 12.sp)
            }
            "Kayro Coins" -> Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(18.dp)) {
                Text("Kayro Coins", color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Внутренняя концепция валюты для подарков. Баланс и переводы между пользователями пока не подключены.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            "Информация о чате" -> Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(18.dp)) { Text("Информация о чате", color = Ink, fontWeight = FontWeight.Bold); Text("Профиль чата будет расширен в следующих версиях.", color = Muted, fontSize = 13.sp) }
            else -> Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(18.dp)) { Text("KayroMess", color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("Версия приложения: ранняя сборка. Чаты и настройки пока хранятся локально на устройстве.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium); Text(subtitle, color = Muted, fontSize = 11.sp) }
        Switch(checked = checked, onCheckedChange = onChange)
    }
    HorizontalDivider(color = Line)
}

@Composable
private fun SettingChoice(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Ink, fontSize = 15.sp)
        RadioButton(selected = selected, onClick = onClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatPage(chat: KMChat, draft: String, replyToId: Long?, onDraft: (String) -> Unit, onBack: () -> Unit, onSend: () -> Unit, onMessageTap: (KMMessage) -> Unit, onCancelReply: () -> Unit, onReplyJump: (Long) -> Unit, onPickImage: () -> Unit, onPickVideo: () -> Unit, onPickGif: () -> Unit, onOpenStickers: () -> Unit, onSendSticker: (String) -> Unit, onTitleTap: () -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showAttach by remember { mutableStateOf(false) }
    var highlightedId by remember { mutableStateOf<Long?>(null) }
    val replyMessage = chat.messages.firstOrNull { it.id == replyToId }
    LaunchedEffect(chat.messages.size) { if (chat.messages.isNotEmpty()) listState.animateScrollToItem(chat.messages.lastIndex) }
    Column(Modifier.fillMaxSize().background(Page)) {
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", Modifier.clickable { onBack() }.padding(horizontal = 9.dp), fontSize = 31.sp, color = Ink)
            Box(Modifier.size(42.dp).clip(CircleShape).background(PaleAccent), Alignment.Center) { Text(chat.title.take(1).uppercase(), color = Accent, fontWeight = FontWeight.Bold) }
            Column(Modifier.weight(1f).clickable { onTitleTap() }.padding(start = 10.dp)) { Text(chat.title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 16.sp); Text(chat.kind, color = Muted, fontSize = 11.sp) }
            SpriteOrText("more_options", "···", 22)
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (chat.messages.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(top = 150.dp), Alignment.Center) { Text("Начните общение", color = Muted, fontSize = 14.sp) } }
            items(chat.messages, key = { it.id }) { message ->
                MessageBubble(message, highlighted = highlightedId == message.id, onTap = { onMessageTap(message) }, onReplyTap = {
                    val index = chat.messages.indexOfFirst { it.id == message.replyToId }
                    if (index >= 0) scope.launch { listState.animateScrollToItem(index); highlightedId = message.replyToId; delay(900); highlightedId = null }
                })
            }
        }
        Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 10.dp, vertical = 8.dp)) {
            AnimatedVisibility(replyMessage != null) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(PaleAccent, RoundedCornerShape(10.dp)).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Ответ на сообщение", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Text(replyMessage?.text ?: "Вложение", color = Ink, fontSize = 12.sp, maxLines = 1) }
                    Text("×", Modifier.clickable { onCancelReply() }, color = Accent, fontSize = 20.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { showAttach = true }, Alignment.Center) { SpriteOrText("attachment_paperclip", "+", 22) }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(Page).padding(horizontal = 15.dp, vertical = 12.dp)) {
                    if (draft.isEmpty()) Text("Сообщение", color = Muted, fontSize = 14.sp)
                    BasicTextField(value = draft, onValueChange = onDraft, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 15.sp), cursorBrush = SolidColor(Accent), maxLines = 5, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { onSend() }))
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(46.dp).clip(CircleShape).background(Accent).clickable { onSend() }, Alignment.Center) { SpriteOrText("send_message", "➤", 22, tint = Color.White) }
            }
        }
    }
    if (showAttach) AttachmentSheet(onDismiss = { showAttach = false }, onPhoto = { showAttach = false; onPickImage() }, onVideo = { showAttach = false; onPickVideo() }, onGif = { showAttach = false; onPickGif() }, onSticker = { showAttach = false; onOpenStickers() })
}

@Composable
private fun MessageBubble(message: KMMessage, highlighted: Boolean, onTap: () -> Unit, onReplyTap: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start) {
        Column(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = if (message.mine) 17.dp else 5.dp, bottomEnd = if (message.mine) 5.dp else 17.dp)).background(if (highlighted) Color(0xFFFFE8A3) else if (message.mine) PaleAccent else SurfaceWhite).clickable { onTap() }.padding(horizontal = 13.dp, vertical = 9.dp)) {
            if (message.forwardedFrom.isNotBlank()) Text("Переслано из ${message.forwardedFrom}", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            if (message.replyToId != null) {
                val preview = "Ответ на сообщение"
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(if (message.mine) Color.White.copy(alpha = 0.55f) else PaleAccent).clickable { onReplyTap() }.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(30.dp).background(Accent, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(7.dp)); Text(message.replyPreview.ifBlank { preview }, color = Accent, fontSize = 11.sp, maxLines = 2)
                }
                Spacer(Modifier.height(5.dp))
            }
            when (message.mediaType) {
                "image", "gif" -> MediaImage(message.mediaUri, message.mediaType == "gif")
                "video" -> MediaVideo(message.mediaUri)
                "sticker" -> Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), Alignment.Center) { Text(message.text, color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            }
            if (message.text.isNotBlank() && message.mediaType != "sticker") Text(message.text, color = Ink, fontSize = 15.sp, lineHeight = 20.sp)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) { if (message.edited) Text("изм. ", color = Muted, fontSize = 9.sp); Text(message.time, color = Muted, fontSize = 10.sp) }
        }
        if (message.reaction.isNotBlank()) Text(message.reaction, Modifier.padding(horizontal = 8.dp).offset(y = (-3).dp), fontSize = 14.sp)
    }
}

@Composable
private fun MediaImage(uri: String, animated: Boolean) {
    val parsed = remember(uri) { Uri.parse(uri) }
    AndroidView(factory = { ctx ->
        ImageView(ctx).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            if (animated && Build.VERSION.SDK_INT >= 28) {
                try {
                    val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(ctx.contentResolver, parsed))
                    setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable) drawable.start()
                } catch (_: Exception) { setImageURI(parsed) }
            } else setImageURI(parsed)
        }
    }, update = { view ->
        if (view.drawable == null) view.setImageURI(parsed)
    }, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(10.dp)))
    if (animated) Text("GIF", color = Muted, fontSize = 9.sp)
}

@Composable
private fun MediaVideo(uri: String) {
    val parsed = remember(uri) { Uri.parse(uri) }
    AndroidView(factory = { ctx -> VideoView(ctx).apply { setMediaController(MediaController(ctx)); setVideoURI(parsed); setOnPreparedListener { it.isLooping = false } } }, modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(10.dp)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentSheet(onDismiss: () -> Unit, onPhoto: () -> Unit, onVideo: () -> Unit, onGif: () -> Unit, onSticker: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 25.dp)) {
            Text("Вложения", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            ActionRow("photo_gallery", "Фото", "▧", onPhoto); ActionRow("video_camera", "Видео", "▶", onVideo); ActionRow("video_player", "GIF", "GIF", onGif); ActionRow("emoji_face", "Стикеры", "✦", onSticker)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StickerPickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val stickers = listOf("ПРИВЕТ!", "СПАСИБО", "ОК", "КЛАСС!", "УРА!", "НА СВЯЗИ", "ХОРОШО", "ПОЗДРАВЛЯЮ")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Стикеры", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            stickers.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { label -> Box(Modifier.weight(1f).padding(vertical = 5.dp).height(76.dp).clip(RoundedCornerShape(16.dp)).background(PaleAccent).clickable { onPick(label) }, Alignment.Center) { Text(label, color = Accent, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) } } } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(message: KMMessage, onDismiss: () -> Unit, onReact: (String) -> Unit, onReply: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onCopy: () -> Unit, onForward: () -> Unit, onInfo: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFD5D8DE)) }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 22.dp)) {
            Text("Реакция", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium); Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("👍", "❤️", "🔥", "😂", "😮", "👏").forEach { reaction -> Box(Modifier.size(43.dp).clip(CircleShape).background(if (message.reaction == reaction) PaleAccent else Page).clickable { onReact(reaction) }, Alignment.Center) { Text(reaction, fontSize = 21.sp) } } }
            Spacer(Modifier.height(15.dp)); HorizontalDivider(color = Line); Spacer(Modifier.height(8.dp))
            ActionRow("reply_message", "Ответить", "↩", onReply)
            if (message.mine && message.text.isNotBlank()) ActionRow("edit_message", "Изменить", "✎", onEdit)
            ActionRow("copy", "Копировать", "▢", onCopy); ActionRow("forward_message", "Переслать", "➦", onForward); ActionRow("about_info", "Информация", "i", onInfo)
            if (message.mine) ActionRow("delete_message", "Удалить", "⌫", onDelete, danger = true)
        }
    }
}

@Composable
private fun ActionRow(icon: String, label: String, fallback: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(vertical = 11.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        SpriteOrText(icon, fallback, 21, tint = if (danger) Color(0xFFD64B55) else Ink); Spacer(Modifier.width(14.dp)); Text(label, color = if (danger) Color(0xFFD64B55) else Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForwardPickerSheet(chats: List<KMChat>, onDismiss: () -> Unit, onSelect: (KMChat) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Text("Переслать в…", Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (chats.isEmpty()) Text("Нет других чатов для пересылки", Modifier.padding(20.dp), color = Muted)
            LazyColumn(Modifier.heightIn(max = 420.dp)) { items(chats, key = { it.id }) { chat -> ChatRow(chat) { onSelect(chat) } } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChatSheet(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var kind by remember { mutableStateOf("Личный чат") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Создать", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Личный чат", "Группа", "Канал").forEach { option -> FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(option) }) } }
            Spacer(Modifier.height(12.dp)); OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (kind == "Личный чат") "Имя контакта" else "Название") }, singleLine = true, shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(16.dp)); Button(onClick = { if (title.isNotBlank()) onCreate(title.trim(), kind) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), enabled = title.isNotBlank()) { Text("Создать $kind") }
            Spacer(Modifier.height(10.dp)); Text("Чаты, группы и каналы пока создаются локально на этом устройстве.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProfileEditDialog(name: String, username: String, bio: String, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var n by remember { mutableStateOf(name) }; var u by remember { mutableStateOf(username) }; var b by remember { mutableStateOf(bio) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Личные данные") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(n, { n = it }, label = { Text("Имя") }, singleLine = true); OutlinedTextField(u, { u = it }, label = { Text("Юзернейм") }, prefix = { Text("@") }, singleLine = true); OutlinedTextField(b, { b = it }, label = { Text("О себе") }, minLines = 2) } }, confirmButton = { TextButton(onClick = { if (n.isNotBlank() && u.isNotBlank()) onSave(n.trim(), u.trim(), b.trim()) }) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@Composable
private fun EditMessageDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Изменить сообщение") }, text = { OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), minLines = 2) }, confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onSave(value.trim()) }) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}

@Composable
private fun SpriteOrText(name: String, fallback: String, size: Int, tint: Color = Ink) {
    val context = LocalContext.current
    val id = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    if (id != 0) Image(painter = androidx.compose.ui.res.painterResource(id), contentDescription = null, modifier = Modifier.size(size.dp))
    else Text(fallback, color = tint, fontSize = (size * 0.82f).sp, fontWeight = FontWeight.Medium)
}

private fun saveChats(prefs: android.content.SharedPreferences, chats: List<KMChat>) {
    val array = JSONArray()
    chats.forEach { chat ->
        val obj = JSONObject().put("id", chat.id).put("title", chat.title).put("kind", chat.kind).put("subtitle", chat.subtitle)
        val messages = JSONArray()
        chat.messages.forEach { m -> messages.put(JSONObject().put("id", m.id).put("text", m.text).put("mine", m.mine).put("time", m.time).put("reaction", m.reaction).put("reply", m.reply).put("edited", m.edited).put("replyToId", m.replyToId ?: JSONObject.NULL).put("forwardedFrom", m.forwardedFrom).put("mediaUri", m.mediaUri).put("mediaType", m.mediaType).put("replyPreview", m.replyPreview)) }
        obj.put("messages", messages); array.put(obj)
    }
    prefs.edit().putString("chats", array.toString()).apply()
}

private fun loadChats(prefs: android.content.SharedPreferences): List<KMChat> {
    val raw = prefs.getString("chats", null)
    if (raw.isNullOrBlank()) return listOf(KMChat(1, "KayroMess Support", "Личный чат", "Поддержка KayroMess"), KMChat(2, "Избранное", "Личный чат", "Личные заметки"), KMChat(3, "Новости KayroMess", "Канал", "Обновления приложения"))
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i); val messagesJson = o.optJSONArray("messages") ?: JSONArray()
            val messages = (0 until messagesJson.length()).map { j ->
                val m = messagesJson.getJSONObject(j); val replyId = if (m.isNull("replyToId")) null else m.optLong("replyToId")
                KMMessage(m.optLong("id"), m.optString("text"), m.optBoolean("mine"), m.optString("time"), m.optString("reaction"), m.optString("reply"), m.optBoolean("edited"), replyId, m.optString("forwardedFrom"), m.optString("mediaUri"), m.optString("mediaType"), m.optString("replyPreview"))
            }
            KMChat(o.optLong("id"), o.optString("title"), o.optString("kind"), o.optString("subtitle"), messages)
        }
    } catch (_: Exception) { listOf(KMChat(1, "KayroMess Support", "Личный чат", "Поддержка KayroMess")) }
}
