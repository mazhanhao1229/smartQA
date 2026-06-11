package com.example.smartqa

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartqa.data.ChatMessage
import com.example.smartqa.data.MessageType
import com.example.smartqa.presentation.viewmodel.ChatViewModel
import com.example.smartqa.ui.components.ChatBubble
import com.example.smartqa.ui.components.ChatInputBar
import com.example.smartqa.ui.theme.Accent
import com.example.smartqa.ui.theme.Bg
import com.example.smartqa.ui.theme.Fg
import com.example.smartqa.ui.theme.Muted
import com.example.smartqa.ui.theme.SmartQATheme
import dagger.hilt.android.AndroidEntryPoint

// ── Activity 入口（Hilt 注入点） ──────────────────────────────

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartQATheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// ── 聊天主界面（ViewModel 驱动） ─────────────────────────────

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // ---- 观察 ViewModel 状态 ----
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 更多菜单展开状态
    var menuExpanded by remember { mutableStateOf(false) }
    // 清空确认对话框
    var showClearDialog by remember { mutableStateOf(false) }

    // ---- 错误 → Snackbar ----
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    // ---- 新消息 / 流式内容更新 → 自动滚到底部 ----
    LaunchedEffect(
        uiState.messages.size,
        // 监听最后一条消息的 content 变化来触发流式更新时的滚动
        uiState.messages.lastOrNull()?.content
    ) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    // ---- 清空确认对话框 ----
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空对话") },
            text = { Text("确定要清空全部对话记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearDialog = false
                }) {
                    Text("确认清空", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // ---- UI 布局 ----
    Column(modifier = modifier.fillMaxSize().background(Bg)) {
        // 顶部栏（包含更多菜单）
        Header(
            onClearHistory = { showClearDialog = true },
            menuExpanded = menuExpanded,
            onMenuToggle = { menuExpanded = it }
        )

        // 消息列表 + 加载指示器
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp, 8.dp, 8.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 空状态提示
                if (uiState.messages.isEmpty()) {
                    item {
                        Text(
                            text = "👋 你好！我是 AI 助手，输入问题开始对话吧。",
                            color = Muted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 160.dp)
                        )
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
            }

            // ---- 流式加载进度条（底部细线） ----
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp, start = 8.dp, end = 8.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Accent,
                    trackColor = Color.Transparent
                )
            }
        }

        // Snackbar 宿主
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 底部输入栏
        ChatInputBar(
            onSendMessage = { text ->
                if (uiState.isLoading) {
                    Toast.makeText(context, "请等待当前回复完成", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.sendMessage(text)
                }
            }
        )

        // 底部导航指示条
        NavBar()
    }
}

// ── 顶部栏（含更多菜单 → 清空对话） ──────────────────────────

@Composable
fun Header(
    onClearHistory: () -> Unit,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp, 4.dp, 4.dp, 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.title_ai_qa),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Fg,
            letterSpacing = (-0.02).sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = {},
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = stringResource(id = R.string.label_history),
                    tint = Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
            // 更多按钮 → 下拉菜单
            Box {
                IconButton(
                    onClick = { onMenuToggle(true) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(id = R.string.label_more),
                        tint = Muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { onMenuToggle(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("清空对话") },
                        onClick = {
                            onMenuToggle(false)
                            onClearHistory()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

// ── 底部导航指示条 ─────────────────────────────────────────

@Composable
fun NavBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF4A4A52))
        )
    }
}

// ── Preview（不含 ViewModel 依赖） ──────────────────────────

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    SmartQATheme {
        // 预览使用假状态，不依赖 Hilt
        val fakeMessages = listOf(
            ChatMessage("1", "你好！我是 AI 助手。", MessageType.AI, "10:32"),
            ChatMessage("2", "实现 LazyColumn？", MessageType.USER, "10:33"),
        )
        // 简单预览，不注入 ViewModel
        Column(modifier = Modifier.fillMaxSize().background(Bg)) {
            Header(onClearHistory = {}, menuExpanded = false, onMenuToggle = {})
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(fakeMessages) { ChatBubble(message = it) }
                }
            }
            ChatInputBar(onSendMessage = {})
            NavBar()
        }
    }
}
