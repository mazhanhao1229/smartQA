package com.example.smartqa.di

import android.content.Context
import androidx.room.Room
import com.example.smartqa.data.local.ChatDatabase
import com.example.smartqa.data.local.dao.ChatMessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChatDatabase {
        return Room.databaseBuilder(
            context,
            ChatDatabase::class.java,
            "smartqa_db"
        ).build()
    }

    @Provides
    fun provideChatMessageDao(database: ChatDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }
}
