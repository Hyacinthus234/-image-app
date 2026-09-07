package com.example.imagegen.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ImageGenApi {
    
    // 获取模型列表
    @GET("models")
    suspend fun getModels(
        @Header("Authorization") apiKey: String
    ): Response<ModelsResponse>
    
    // 生成图片
    @POST("generate")
    suspend fun generateImage(
        @Header("Authorization") apiKey: String,
        @Body request: GenerateRequest
    ): Response<GenerateResponse>
}

// 数据模型
data class ModelsResponse(
    val models: List<Model>
)

data class Model(
    val id: String,
    val name: String,
    val description: String?
)

data class GenerateRequest(
    val model: String,
    val prompt: String,
    val quality: String,  // "low", "medium", "high", "ultra"
    val width: Int = 512,
    val height: Int = 512,
    val steps: Int = 20
)

data class GenerateResponse(
    val success: Boolean,
    val imageUrl: String?,
    val imageBase64: String?,
    val message: String?
)
