package com.example.smartqa.domain.repository

import com.example.smartqa.data.ChatMessage
import com.example.smartqa.data.remote.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * 聊天数据仓库接口 —— 定义本地持久化 + 远程 API 的抽象。
 * 具体实现在 data 层。
 */
interface ChatRepository {

    /** 从 Room 加载最近 [limit] 条消息，按时间正序实时更新（返回 Flow）。 */
    fun getRecentMessages(limit: Int = 50): Flow<List<ChatMessage>>

    /** 将单条消息写入 Room。 */
    suspend fun saveMessage(message: ChatMessage)

    /** 清空本地全部聊天记录。 */
    suspend fun clearAllMessages()

    /**
     * 发起流式聊天补全请求。
     * 参数 [messages] 为完整的对话上下文（system + 历史 user/assistant）。
     * 返回一个冷 Flow，每个元素为模型即时生成的一个 token 片段。
     */
    fun streamChat(messages: List<Message>): Flow<String>
}
