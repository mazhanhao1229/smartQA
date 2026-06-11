package com.example.smartqa.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.smartqa.data.local.dao.ChatMessageDao
import com.example.smartqa.data.local.entity.ChatMessageEntity

@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}
