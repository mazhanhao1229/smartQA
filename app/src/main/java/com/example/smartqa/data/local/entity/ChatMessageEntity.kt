package com.example.smartqa.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.smartqa.data.ChatMessage
import com.example.smartqa.data.MessageType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Room 持久化实体 —— 与内存中的 [ChatMessage] 通过扩展函数互转。
 * timestamp 使用 Long（epoch 毫秒）以保证正确排序。
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "content")
    val content: String,

    /** true = 用户发送，false = AI 回复 */
    @ColumnInfo(name = "is_user")
    val isUser: Boolean,

    /** 消息创建时间（epoch 毫秒），用于排序 */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)

/**
 * Room 实体 → UI 层数据类。
 * 将 Long 时间戳格式化为 "HH:mm" 展示用字符串。
 */
fun ChatMessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id,
        content = content,
        type = if (isUser) MessageType.USER else MessageType.AI,
        timestamp = formatTime(timestamp)
    )
}

/**
 * UI 层数据类 → Room 实体。
 * 始终使用当前系统时间作为存储时间戳，保证增量插入顺序正确。
 */
fun ChatMessage.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        content = content,
        isUser = type == MessageType.USER,
        timestamp = System.currentTimeMillis()
    )
}

/** 将 epoch 毫秒转为 "HH:mm" 展示格式 */
private fun formatTime(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}
