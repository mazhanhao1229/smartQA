package com.example.smartqa.data.repository

import android.util.Log
import com.example.smartqa.BuildConfig
import com.example.smartqa.data.ChatMessage
import com.example.smartqa.data.local.dao.ChatMessageDao
import com.example.smartqa.data.local.entity.toChatMessage
import com.example.smartqa.data.local.entity.toEntity
import com.example.smartqa.data.remote.model.ChatRequest
import com.example.smartqa.data.remote.model.Message
import com.example.smartqa.data.remote.model.StreamChunk
import com.example.smartqa.domain.repository.ChatRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dao: ChatMessageDao,
    private val moshi: Moshi,
    private val okHttpClient: OkHttpClient
) : ChatRepository {

    // ── 本地数据 ─────────────────────────────────────────────

    override fun getRecentMessages(limit: Int): Flow<List<ChatMessage>> {
        return dao.getRecentMessages(limit).map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    override suspend fun saveMessage(message: ChatMessage) {
        withContext(Dispatchers.IO) {
            dao.insertMessage(message.toEntity())
        }
    }

    override suspend fun clearAllMessages() {
        withContext(Dispatchers.IO) {
            dao.deleteAllMessages()
        }
    }

    // ── 流式远程请求 ─────────────────────────────────────────

    /**
     * 通过 OkHttp 发起 POST 流式请求，逐行解析 SSE 数据并发射 token 片段。
     */
    override fun streamChat(messages: List<Message>): Flow<String> = flow {
        val requestBody = ChatRequest(
            model = BuildConfig.API_MODEL,
            messages = messages,
            stream = true
        )

        val jsonBody = moshi.adapter(ChatRequest::class.java).toJson(requestBody)

        // Debug: 输出完整请求信息
        Log.d(TAG, "请求 URL: ${BuildConfig.API_BASE_URL}/v1/chat/completions")
        Log.d(TAG, "API Key 长度: ${BuildConfig.API_KEY.length} 字符, 前8位: ${if (BuildConfig.API_KEY.isEmpty()) "(空)" else BuildConfig.API_KEY.take(8)}…")
        Log.d(TAG, "Model: ${BuildConfig.API_MODEL}")
        Log.d(TAG, "请求体: $jsonBody")

        // 注意: Authorization header 已由 NetworkModule 的 OkHttp interceptor 统一注入，此处不再重复添加
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        Log.d(TAG, "最终请求头: ${request.headers}")

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: response.message
            Log.e(TAG, "API 错误 (${response.code}): $errorBody")
            throw IOException("API 请求失败 (${response.code}): $errorBody")
        }

        val body = response.body ?: throw IOException("空响应体")
        val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
        val chunkAdapter = moshi.adapter(StreamChunk::class.java)

        var line = reader.readLine()
        while (line != null) {
            // SSE 规范：data 前缀行
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()

                // [DONE] 表示流结束
                if (data.isNotEmpty() && data != "[DONE]") {
                    try {
                        val chunk = chunkAdapter.fromJson(data)
                        if (chunk?.error != null) {
                            throw IOException("API 流式错误: ${chunk.error.message}")
                        }
                        val content = chunk?.choices?.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) {
                            emit(content)
                        }
                    } catch (e: IOException) {
                        throw e
                    } catch (_: Exception) {
                        // 忽略单条 JSON 解析失败
                    }
                }
            }
            line = reader.readLine()
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "ChatRepository"
    }
}
