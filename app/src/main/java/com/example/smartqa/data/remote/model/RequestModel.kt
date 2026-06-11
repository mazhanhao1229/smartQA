package com.example.smartqa.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 聊天补全请求体，兼容 OpenAI / SiliconFlow / DeepSeek 格式。
 */
@JsonClass(generateAdapter = false)
data class ChatRequest(
    /** 模型名称，可通过 BuildConfig 覆盖，默认 DeepSeek-V3 */
    @Json(name = "model") val model: String = DEFAULT_MODEL,
    /** 对话消息列表（含 system / user / assistant） */
    @Json(name = "messages") val messages: List<Message>,
    /** 是否流式返回 */
    @Json(name = "stream") val stream: Boolean = true,
    /** 采样温度 0-2 */
    @Json(name = "temperature") val temperature: Float = 0.7f,
    /** 最大生成 token 数 */
    @Json(name = "max_tokens") val maxTokens: Int = 8192
) {
    companion object {
        const val DEFAULT_MODEL = "deepseek-ai/DeepSeek-V3"
    }
}

/**
 * 单条对话消息，对应 API 的 role + content。
 */
@JsonClass(generateAdapter = false)
data class Message(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)
