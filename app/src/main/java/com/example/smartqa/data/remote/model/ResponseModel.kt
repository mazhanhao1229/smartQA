package com.example.smartqa.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── 非流式响应 ──────────────────────────────────────────────

/** 非流式（stream=false）聊天补全响应。 */
@JsonClass(generateAdapter = false)
data class ChatResponse(
    @Json(name = "choices") val choices: List<Choice>
)

@JsonClass(generateAdapter = false)
data class Choice(
    @Json(name = "message") val message: ResponseMessage
)

@JsonClass(generateAdapter = false)
data class ResponseMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

// ── 流式（SSE chunk）响应 ───────────────────────────────────

/** 流式返回的单个 SSE 数据块。 */
@JsonClass(generateAdapter = false)
data class StreamChunk(
    @Json(name = "choices") val choices: List<DeltaChoice>?,
    @Json(name = "error") val error: StreamError? = null
)

@JsonClass(generateAdapter = false)
data class DeltaChoice(
    @Json(name = "delta") val delta: Delta,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = false)
data class Delta(
    @Json(name = "role") val role: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = false)
data class StreamError(
    @Json(name = "message") val message: String,
    @Json(name = "type") val type: String
)
