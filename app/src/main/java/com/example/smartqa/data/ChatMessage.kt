package com.example.smartqa.data

data class ChatMessage(
    val id: String,
    val content: String,
    val type: MessageType,
    val timestamp: String
)

enum class MessageType {
    AI, USER
}