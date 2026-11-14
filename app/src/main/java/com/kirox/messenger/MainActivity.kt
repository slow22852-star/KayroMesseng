package com.kirox.messenger

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF858B96)
private val Line = Color(0xFFE9EBEF)
private val Accent = Color(0xFF5D70D6)
private val Page = Color(0xFFF7F8FA)
private val SurfaceWhite = Color.White
private val PaleAccent = Color(0xFFEEF0FF)

data class KMMessage(
    val id: Long,
    val text: String,
    val mine: Boolean,
    val time: String,
    val reaction: String = "",
    val reply: String = "",
    val edited: Boolean = false
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
        window.statusBarColor = android.graphics.Color.rgb(247, 248, 250)
        window.navigationBarColor = android.graphics.Color.rgb(255, 255, 255)
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { KayroMessRoot() }
    }
}

@Composable
private fun KayroMessRoot() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kayromess_v2", Context.MODE_PRIVATE) }
    var chats by remember { mutableStateOf(loadChats(prefs)) }
    var tab by remember { mutableStateOf("Чаты") }
    var activeId by remember { mutableStateOf<Long?>(null) }
    var search by remember { mutableStateOf("") }
    var createOpen by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<Pair<Long, KMMessage>?>(null) }
    var editor by remember { mutableStateOf<KMMessage?>(null) }
    var profileName by remember { mutableStateOf(prefs.getString("name", "Пользователь") ?: "Пользователь") }
    var profileUsername by remember { mutableStateOf(prefs.getString("username", "kirox") ?: "kirox") }
    var profileBio by remember { mutableStateOf(prefs.getString("bio", "") ?: "") }
    var profileEdit by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val active = chats.firstOrNull { it.id == activeId }

    fun persist(updated: List<KMChat>) {
        chats = updated
        saveChats(prefs, updated)
    }
    fun updateMessage(chatId: Long, message: KMMessage) {
        persist(chats.map { chat ->
            if (chat.id == chatId) chat.copy(messages = chat.messages.map { if (it.id == message.id) message else it })
            else chat
        })
    }
    fun sendMessage(text: String) {
        val target = active ?: return
        if (text.isBlank()) return
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val message = KMMessage(System.currentTimeMillis(), text.trim(), true, now, reply = replyTo)
        persist(chats.map { if (it.id == target.id) it.copy(messages = it.messages + message) else it })
        draft = ""
        replyTo = ""
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent, onPrimary = Color.White, background = Page,
            surface = SurfaceWhite, onSurface = Ink, onBackground = Ink,
            outline = Line, secondary = Muted
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Page) {
            AnimatedContent(
                targetState = active,
                transitionSpec = { androidx.compose.animation.fadeIn(tween(160)) togetherWith androidx.compose.animation.fadeOut(tween(120)) },
                label = "screen"
            ) { chat ->
                if (chat != null) {
                    ChatPage(
                        chat = chat,
                        draft = draft,
                        replyTo = replyTo,
                        onDraft = { draft = it },
                        onBack = { activeId = null },
                        onSend = { sendMessage(draft) },
                        onMessageTap = { selectedMessage = chat.id to it },
                        onCancelReply = { replyTo = "" },
                        onTitleTap = { tab = "Профиль чата"; activeId = null }
                    )
                } else {
                    when (tab) {
                        "Профиль" -> ProfilePage(
                            name = profileName, username = profileUsername, bio = profileBio,
                            onEdit = { profileEdit = true },
                            onTheme = { /* light theme is the default in this build */ }
                        )
                        "Контакты" -> ContactsPage(chats.filter { it.kind == "Личный чат" }) { activeId = it.id }
                        "Каналы" -> MainListPage(
                            title = "Каналы",
                            items = chats.filter { it.kind == "Канал" || it.kind == "Группа" },
                            onOpen = { activeId = it.id },
                            onCreate = { createOpen = true }
                        )
                        "Профиль чата" -> MainListPage("Чаты", chats, { activeId = it.id }, { createOpen = true })
                        else -> MainListPage(
                            title = "KayroMess",
                            items = chats.filter { it.kind != "Канал" && it.kind != "Группа" || tab == "Чаты" }
                                .filter { it.title.contains(search, true) || it.subtitle.contains(search, true) },
                            onOpen = { activeId = it.id },
                            onCreate = { createOpen = true },
                            search = search,
                            onSearch = { search = it },
                            currentTab = tab,
                            onTab = { tab = it }
                        )
                    }
                }
            }
        }
    }

    if (createOpen) {
        CreateChatSheet(
            onDismiss = { createOpen = false },
            onCreate = { title, kind ->
                val newChat = KMChat(System.currentTimeMillis(), title, kind, if (kind == "Канал") "Новый канал" else "Недавно создано")
                persist(listOf(newChat) + chats)
                createOpen = false
                activeId = newChat.id
            }
        )
    }

    selectedMessage?.let { (chatId, message) ->
        MessageActionsSheet(
            message = message,
            onDismiss = { selectedMessage = null },
            onReact = { reaction ->
                updateMessage(chatId, message.copy(reaction = if (message.reaction == reaction) "" else reaction))
                selectedMessage = null
            },
            onReply = {
                replyTo = message.text.take(90)
                activeId = chatId
                selectedMessage = null
            },
            onEdit = {
                if (message.mine) editor = message
                selectedMessage = null
            },
            onDelete = {
                persist(chats.map { chat ->
                    if (chat.id == chatId) chat.copy(messages = chat.messages.filterNot { it.id == message.id }) else chat
                })
                selectedMessage = null
            },
            onCopy = {
                clipboard.setText(AnnotatedString(message.text))
                selectedMessage = null
            },
            onForward = {
                val target = chats.firstOrNull { it.id != chatId && it.kind != "Канал" }
                if (target != null) {
                    val forwarded = message.copy(id = System.currentTimeMillis(), mine = true, time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), reply = "Переслано")
                    persist(chats.map { if (it.id == target.id) it.copy(messages = it.messages + forwarded) else it })
                }
                selectedMessage = null
            },
            onInfo = { selectedMessage = null }
        )
    }

    editor?.let { old ->
        EditMessageDialog(old.text, onDismiss = { editor = null }) { newText ->
            updateMessage(activeId ?: chats.firstOrNull { c -> c.messages.any { it.id == old.id } }?.id ?: -1, old.copy(text = newText, edited = true))
            editor = null
        }
    }

    if (profileEdit) {
        ProfileEditDialog(profileName, profileUsername, profileBio, onDismiss = { profileEdit = false }) { n, u, b ->
            profileName = n; profileUsername = u.removePrefix("@"); profileBio = b
            prefs.edit().putString("name", n).putString("username", profileUsername).putString("bio", b).apply()
            profileEdit = false
        }
    }
}

@Composable
private fun MainListPage(
    title: String,
    items: List<KMChat>,
    onOpen: (KMChat) -> Unit,
    onCreate: () -> Unit,
    search: String = "",
    onSearch: (String) -> Unit = {},
    currentTab: String = "Чаты",
    onTab: (String) -> Unit = {}
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Общение без лишнего", fontSize = 12.sp, color = Muted)
            }
            Box(Modifier.size(42.dp).clip(CircleShape).background(PaleAccent).clickable { onCreate() }, Alignment.Center) {
                SpriteOrText("compose_new_chat", "+", 22)
            }
        }
        if (title == "KayroMess") {
            Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Поиск", color = Muted) }, singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Page, unfocusedContainerColor = Page,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }
        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Пока здесь пусто", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Создай чат, группу или канал", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onCreate, shape = RoundedCornerShape(14.dp)) { Text("Создать") }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(items, key = { it.id }) { chat ->
                    ChatRow(chat) { onOpen(chat) }
                }
            }
        }
        if (title == "KayroMess") {
            Row(Modifier.fillMaxWidth().background(SurfaceWhite).border(1.dp, Line).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Чаты" to "chat_bubble", "Контакты" to "add_contact", "Каналы" to "channel_broadcast", "Профиль" to "profile").forEach { (label, icon) ->
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).clickable { onTab(label) }.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SpriteOrText(icon, "•", 22)
                        Text(label, fontSize = 10.sp, color = if (currentTab == label) Accent else Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: KMChat, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.background(SurfaceWhite)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(51.dp).clip(CircleShape).background(PaleAccent), Alignment.Center) {
            Text(chat.title.take(1).uppercase(), color = Accent, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(chat.messages.lastOrNull()?.text ?: chat.subtitle.ifBlank { chat.kind }, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        chat.messages.lastOrNull()?.let { Text(it.time, color = Muted, fontSize = 11.sp) }
    }
    HorizontalDivider(color = Line, thickness = 0.6.dp, modifier = Modifier.padding(start = 82.dp))
}

@Composable
private fun ChatPage(
    chat: KMChat, draft: String, replyTo: String,
    onDraft: (String) -> Unit, onBack: () -> Unit, onSend: () -> Unit,
    onMessageTap: (KMMessage) -> Unit, onCancelReply: () -> Unit, onTitleTap: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Page)) {
        Row(
            Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", Modifier.clickable { onBack() }.padding(horizontal = 9.dp), fontSize = 31.sp, color = Ink)
            Box(Modifier.size(42.dp).clip(CircleShape).background(PaleAccent), Alignment.Center) {
                Text(chat.title.take(1).uppercase(), color = Accent, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).clickable { onTitleTap() }.padding(start = 10.dp)) {
                Text(chat.title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 16.sp)
                Text(chat.kind, color = Muted, fontSize = 11.sp)
            }
            SpriteOrText("more_options", "···", 22)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chat.messages.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 150.dp), Alignment.Center) {
                        Text("Начните общение", color = Muted, fontSize = 14.sp)
                    }
                }
            }
            items(chat.messages, key = { it.id }) { message ->
                MessageBubble(message) { onMessageTap(message) }
            }
        }
        Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 10.dp, vertical = 8.dp)) {
            AnimatedVisibility(replyTo.isNotBlank()) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(PaleAccent, RoundedCornerShape(10.dp)).padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ответ: $replyTo", Modifier.weight(1f), color = Accent, fontSize = 12.sp, maxLines = 1)
                    Text("×", Modifier.clickable { onCancelReply() }, color = Accent, fontSize = 20.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(Page).padding(horizontal = 15.dp, vertical = 12.dp)) {
                    if (draft.isEmpty()) Text("Сообщение", color = Muted, fontSize = 14.sp)
                    BasicTextField(
                        value = draft, onValueChange = onDraft, modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 15.sp),
                        cursorBrush = SolidColor(Accent), maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() })
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(46.dp).clip(CircleShape).background(Accent).clickable { onSend() }, Alignment.Center) {
                    SpriteOrText("send_message", "➤", 22, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: KMMessage, onTap: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start) {
        Column(
            Modifier.widthIn(max = 300.dp).clip(
                RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = if (message.mine) 17.dp else 5.dp, bottomEnd = if (message.mine) 5.dp else 17.dp)
            ).background(if (message.mine) Color(0xFFE9EDFF) else SurfaceWhite)
                .clickable { onTap() }
                .padding(horizontal = 13.dp, vertical = 9.dp)
        ) {
            if (message.reply.isNotBlank()) {
                Text(if (message.reply == "Переслано") "Переслано" else "↳ ${message.reply}", color = Accent, fontSize = 11.sp, maxLines = 1)
                Spacer(Modifier.height(3.dp))
            }
            Text(message.text, color = Ink, fontSize = 15.sp, lineHeight = 20.sp)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (message.edited) Text("изм. ", color = Muted, fontSize = 9.sp)
                Text(message.time, color = Muted, fontSize = 10.sp)
            }
        }
        if (message.reaction.isNotBlank()) {
            Text(message.reaction, Modifier.padding(horizontal = 8.dp).offset(y = (-3).dp), fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(
    message: KMMessage, onDismiss: () -> Unit, onReact: (String) -> Unit,
    onReply: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    onCopy: () -> Unit, onForward: () -> Unit, onInfo: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFD5D8DE)) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 22.dp)) {
            Text("Реакция", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("👍", "❤️", "🔥", "😂", "😮", "👏").forEach { reaction ->
                    Box(
                        Modifier.size(43.dp).clip(CircleShape)
                            .background(if (message.reaction == reaction) PaleAccent else Page)
                            .clickable { onReact(reaction) },
                        Alignment.Center
                    ) { Text(reaction, fontSize = 21.sp) }
                }
            }
            Spacer(Modifier.height(15.dp))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(8.dp))
            ActionRow("reply_message", "Ответить", "↩", onReply)
            if (message.mine) {
                ActionRow("edit_message", "Изменить", "✎", onEdit)
            }
            ActionRow("copy", "Копировать", "▢", onCopy)
            ActionRow("forward_message", "Переслать", "➦", onForward)
            ActionRow("about_info", "Информация", "i", onInfo)
            if (message.mine) ActionRow("delete_message", "Удалить", "⌫", onDelete, danger = true)
        }
    }
}

@Composable
private fun ActionRow(icon: String, label: String, fallback: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteOrText(icon, fallback, 21, tint = if (danger) Color(0xFFD64B55) else Ink)
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (danger) Color(0xFFD64B55) else Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CreateChatSheet(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("Личный чат") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Создать", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Личный чат", "Группа", "Канал").forEach { option ->
                    FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(option) })
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text(if (kind == "Личный чат") "Имя контакта" else "Название") },
                singleLine = true, shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (title.isNotBlank()) onCreate(title.trim(), kind) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = title.isNotBlank()
            ) { Text("Создать $kind") }
            Spacer(Modifier.height(10.dp))
            Text("Пока чаты, группы и каналы создаются локально на этом устройстве.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ContactsPage(contacts: List<KMChat>, onOpen: (KMChat) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Контакты", Modifier.padding(20.dp), fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        if (contacts.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Контактов пока нет", color = Muted) }
        else LazyColumn { items(contacts, key = { it.id }) { ChatRow(it) { onOpen(it) } } }
    }
}

@Composable
private fun ProfilePage(name: String, username: String, bio: String, onEdit: () -> Unit, onTheme: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Page)) {
        Text("Профиль", Modifier.padding(start = 20.dp, top = 22.dp, bottom = 18.dp), fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Column(Modifier.fillMaxWidth().background(SurfaceWhite).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(82.dp).clip(CircleShape).background(PaleAccent), Alignment.Center) {
                Text(name.take(1).uppercase(), fontSize = 30.sp, color = Accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(name, fontSize = 19.sp, color = Ink, fontWeight = FontWeight.Bold)
            Text("@$username", color = Accent, fontSize = 13.sp)
            if (bio.isNotBlank()) Text(bio, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp)) {
                SpriteOrText("edit_profile", "✎", 17); Spacer(Modifier.width(7.dp)); Text("Изменить профиль")
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.background(SurfaceWhite).fillMaxWidth().padding(horizontal = 18.dp)) {
            ProfileItem("account_security", "Безопасность", "Настройки аккаунта") {}
            ProfileItem("notification_bell_filled", "Уведомления", "Настройки уведомлений") {}
            ProfileItem("appearance_theme", "Оформление", "Светлая тема включена") { onTheme() }
            ProfileItem("gift", "Kayro Coins", "Внутренние подарки") {}
            ProfileItem("about_info", "О приложении", "KayroMess") {}
        }
    }
}

@Composable
private fun ProfileItem(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Page), Alignment.Center) { SpriteOrText(icon, "•", 20) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        Text("›", color = Muted, fontSize = 22.sp)
    }
    HorizontalDivider(color = Line, thickness = 0.6.dp)
}

@Composable
private fun ProfileEditDialog(name: String, username: String, bio: String, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var n by remember { mutableStateOf(name) }
    var u by remember { mutableStateOf(username) }
    var b by remember { mutableStateOf(bio) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Личные данные") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(n, { n = it }, label = { Text("Имя") }, singleLine = true)
                OutlinedTextField(u, { u = it }, label = { Text("Юзернейм") }, prefix = { Text("@") }, singleLine = true)
                OutlinedTextField(b, { b = it }, label = { Text("О себе") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { if (n.isNotBlank() && u.isNotBlank()) onSave(n.trim(), u.trim(), b.trim()) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun EditMessageDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить сообщение") },
        text = { OutlinedTextField(value, { value = it }, modifier = Modifier.fillMaxWidth(), minLines = 2) },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onSave(value.trim()) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun SpriteOrText(name: String, fallback: String, size: Int, tint: Color = Ink) {
    val context = LocalContext.current
    val id = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    if (id != 0) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id),
            contentDescription = null,
            modifier = Modifier.size(size.dp)
        )
    } else {
        Text(fallback, color = tint, fontSize = (size * 0.82f).sp, fontWeight = FontWeight.Medium)
    }
}

private fun saveChats(prefs: SharedPreferences, chats: List<KMChat>) {
    val array = JSONArray()
    chats.forEach { chat ->
        val obj = JSONObject().put("id", chat.id).put("title", chat.title).put("kind", chat.kind).put("subtitle", chat.subtitle)
        val messages = JSONArray()
        chat.messages.forEach { m ->
            messages.put(JSONObject().put("id", m.id).put("text", m.text).put("mine", m.mine).put("time", m.time)
                .put("reaction", m.reaction).put("reply", m.reply).put("edited", m.edited))
        }
        obj.put("messages", messages)
        array.put(obj)
    }
    prefs.edit().putString("chats", array.toString()).apply()
}

private fun loadChats(prefs: SharedPreferences): List<KMChat> {
    val raw = prefs.getString("chats", null)
    if (raw.isNullOrBlank()) return listOf(
        KMChat(1, "KayroMess Support", "Личный чат", "Поддержка KayroMess"),
        KMChat(2, "Избранное", "Личный чат", "Личные заметки"),
        KMChat(3, "Новости KayroMess", "Канал", "Обновления приложения")
    )
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val messagesJson = o.optJSONArray("messages") ?: JSONArray()
            val messages = (0 until messagesJson.length()).map { j ->
                val m = messagesJson.getJSONObject(j)
                KMMessage(m.optLong("id"), m.optString("text"), m.optBoolean("mine"), m.optString("time"),
                    m.optString("reaction"), m.optString("reply"), m.optBoolean("edited"))
            }
            KMChat(o.optLong("id"), o.optString("title"), o.optString("kind"), o.optString("subtitle"), messages)
        }
    } catch (_: Exception) {
        listOf(KMChat(1, "KayroMess Support", "Личный чат", "Поддержка KayroMess"))
    }
}
