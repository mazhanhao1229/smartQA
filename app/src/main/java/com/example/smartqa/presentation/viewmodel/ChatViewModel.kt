package com.example.smartqa.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartqa.data.ChatMessage
import com.example.smartqa.data.MessageType
import com.example.smartqa.data.remote.model.Message
import com.example.smartqa.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    // ── UI 状态 ──────────────────────────────────────────────

    data class UiState(
        /** 当前展示的全部消息（含流式构建中的占位消息）。 */
        val messages: List<ChatMessage> = emptyList(),
        /** 是否正在等待 AI 回复（网络请求进行中）。 */
        val isLoading: Boolean = false,
        /** 非空时触发 Snackbar 展示错误信息。 */
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** 当前正在进行的流式请求 Job，发送新消息时用于取消旧请求。 */
    private var currentStreamJob: Job? = null

    // ── 初始化：加载本地历史 ─────────────────────────────────

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getRecentMessages(50).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    // ── 公开操作 ────────────────────────────────────────────

    /** 发送一条用户消息并触发 AI 流式回复。 */
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // 1. 取消上一个还在进行的流
        currentStreamJob?.cancel()

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            type = MessageType.USER,
            timestamp = nowTimeString()
        )

        // 流式占位消息 —— 在 tokens 到达时原地更新其 content
        val streamingId = "streaming_${UUID.randomUUID()}"
        val placeholderMessage = ChatMessage(
            id = streamingId,
            content = "",
            type = MessageType.AI,
            timestamp = nowTimeString()
        )

        // 2. 立即插入 UI 列表
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + placeholderMessage,
                isLoading = true
            )
        }

        // 3. 持久化用户消息（后台）
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMessage(userMessage)
        }

        // 4. 发起流式请求
        val apiMessages = buildContextMessages(userMessage)
        currentStreamJob = viewModelScope.launch {
            var fullContent = ""
            try {
                repository.streamChat(apiMessages).cancellable().collect { token ->
                    fullContent += token
                    // 原地替换占位消息内容，LazyColumn 只重组该 item
                    _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.id == streamingId) msg.copy(content = fullContent) else msg
                        }
                        state.copy(messages = updated)
                    }
                }
                // 流正常结束 —— 替换为正式消息并落库
                finishStreaming(streamingId, fullContent)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 被取消（用户发了新消息），保留当前已收到的部分内容
                cancelStreaming(streamingId, fullContent)
            } catch (e: Exception) {
                // 网络/解析错误 —— 移除占位消息并提示用户
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.filter { it.id != streamingId },
                        isLoading = false,
                        error = e.message ?: "请求失败，请重试"
                    )
                }
            }
        }
    }

    /** 清空全部对话。 */
    fun clearHistory() {
        currentStreamJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllMessages()
            _uiState.update { it.copy(messages = emptyList(), isLoading = false) }
        }
    }

    /** 消费错误（Snackbar 展示后调用）。 */
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── 内部辅助 ────────────────────────────────────────────

    /** 流正常结束时：替换占位消息 id 并保存到 Room。 */
    private fun finishStreaming(streamingId: String, fullContent: String) {
        val finalId = UUID.randomUUID().toString()
        val finalMessage = ChatMessage(
            id = finalId,
            content = fullContent.ifBlank { "（回复为空）" },
            type = MessageType.AI,
            timestamp = nowTimeString()
        )
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == streamingId) finalMessage else msg
            }
            state.copy(messages = updated, isLoading = false)
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMessage(finalMessage)
        }
    }

    /** 流被取消时：将已收到内容保存为正式消息并落库。 */
    private fun cancelStreaming(streamingId: String, partialContent: String) {
        if (partialContent.isBlank()) {
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.filter { it.id != streamingId },
                    isLoading = false
                )
            }
            return
        }
        val finalMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = "$partialContent…",
            type = MessageType.AI,
            timestamp = nowTimeString()
        )
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == streamingId) finalMessage else msg
            }
            state.copy(messages = updated, isLoading = false)
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMessage(finalMessage)
        }
    }

    /** 将 UI 消息列表转为 API 所需的对话上下文。 */
    private fun buildContextMessages(newestUserMessage: ChatMessage): List<Message> {
        val currentMessages = _uiState.value.messages
        val apiMessages = mutableListOf<Message>()

        // 系统提示词
        apiMessages.add(
            Message(
                role = "system",
                content = "你是一个智能问答助手。请用简洁清晰的中文回答用户问题。如果涉及代码，请使用 Markdown 代码块格式。"
            )
        )

        // 取最近 20 条已有消息作为上下文（跳过流式占位消息）
        val contextMessages = currentMessages
            .filter { !it.id.startsWith("streaming_") }
            .takeLast(20)

        for (msg in contextMessages) {
            val role = if (msg.type == MessageType.USER) "user" else "assistant"
            apiMessages.add(Message(role = role, content = msg.content))
        }

        // 确保新用户消息在末尾（若已被包含则跳过）
        val alreadyIncluded = apiMessages.any {
            it.role == "user" && it.content == newestUserMessage.content
        }
        if (!alreadyIncluded) {
            apiMessages.add(Message(role = "user", content = newestUserMessage.content))
        }

        return apiMessages
    }

    /** 格式化当前时间为 "HH:mm"。 */
    private fun nowTimeString(): String {
        val cal = Calendar.getInstance()
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
}
