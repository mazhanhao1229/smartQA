package com.example.smartqa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartqa.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    /**
     * 按时间正序返回最近 [limit] 条消息 —— UI 展示用。
     * 先用子查询取最新的 N 条 id，再按时间正序排列，保证聊天列表从旧到新。
     */
    @Query("""
        SELECT * FROM chat_messages
        WHERE id IN (
            SELECT id FROM chat_messages ORDER BY timestamp DESC LIMIT :limit
        )
        ORDER BY timestamp ASC
    """)
    fun getRecentMessages(limit: Int = 50): Flow<List<ChatMessageEntity>>

    /** 插入或替换单条消息（id 冲突时覆盖，用于流式结束后的最终保存）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(entity: ChatMessageEntity)

    /** 清空全部历史消息。 */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    /** 查询消息总数（可用于判断是否需要提示欢迎语）。 */
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int
}
