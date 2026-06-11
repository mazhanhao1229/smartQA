package com.example.smartqa.data.remote

import com.example.smartqa.data.remote.model.ChatRequest
import com.example.smartqa.data.remote.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit 接口 —— 仅定义非流式端点（用于 fallback 或未来扩展）。
 * 流式请求通过 OkHttp 直接发起，见 [com.example.smartqa.data.repository.ChatRepositoryImpl]。
 */
interface ApiService {

    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
