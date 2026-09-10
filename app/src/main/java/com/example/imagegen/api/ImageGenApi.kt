package com.example.imagegen.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImageGenApi {
    
    // 获取模型列表（OpenAI 兼容格式）
    @GET("models")
    suspend fun getModels(
        @Header("Authorization") auth: String
    ): Response<ModelsResponse>
    
    // 生成图片（OpenAI 兼容格式，纯文生图）
    @POST("images/generations")
    suspend fun generateImage(
        @Header("Authorization") auth: String,
        @Body request: GenerateRequest
    ): Response<ImageResponse>
    
    // 图生图 / 参考图编辑（OpenAI 兼容的 /images/edits，multipart 上传）
    // 多张参考图复用同一个 "image[]" 字段名重复出现。
    @Multipart
    @POST("images/edits")
    suspend fun editImage(
        @Header("Authorization") auth: String,
        @Part images: List<MultipartBody.Part>,
        @Part("prompt") prompt: RequestBody,
        @Part("model") model: RequestBody,
        @Part("n") n: RequestBody,
        @Part("size") size: RequestBody,
        @Part("quality") quality: RequestBody
    ): Response<ImageResponse>
}

// OpenAI 模型列表响应
data class ModelsResponse(
    val data: List<Model> = emptyList()
)

data class Model(
    val id: String,
    val `object`: String? = null,
    val created: Long? = null,
    val owned_by: String? = null
)

// OpenAI 生图请求
data class GenerateRequest(
    val model: String,
    val prompt: String,
    val n: Int = 1,
    val size: String = "1024x1024",
    val quality: String? = null
)

// OpenAI 生图响应
data class ImageResponse(
    val data: List<ImageData> = emptyList(),
    val created: Long? = null
)

data class ImageData(
    val b64_json: String? = null,
    val url: String? = null,
    val revised_prompt: String? = null
)

// 生图结果（url 或 base64 二选一）
data class GeneratedImage(
    val url: String? = null,
    val base64: String? = null
)
