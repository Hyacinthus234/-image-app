package com.example.imagegen.repository

import com.example.imagegen.api.GenerateRequest
import com.example.imagegen.api.Model
import com.example.imagegen.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageGenRepository {
    
    suspend fun getModels(baseUrl: String, apiKey: String): Result<List<Model>> {
        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(baseUrl)
                val response = api.getModels("Bearer $apiKey")
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.models)
                } else {
                    Result.failure(Exception("获取模型列表失败 (HTTP ${response.code()})"))
                }
            } catch (e: Throwable) {
                Result.failure(Exception(friendlyError(e)))
            }
        }
    }
    
    suspend fun generateImage(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        prompt: String,
        quality: String,
        width: Int,
        height: Int
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val steps = when (quality) {
                    "low" -> 15
                    "medium" -> 25
                    "high" -> 35
                    "ultra" -> 50
                    else -> 25
                }
                
                val request = GenerateRequest(
                    model = modelId,
                    prompt = prompt,
                    quality = quality,
                    width = width,
                    height = height,
                    steps = steps
                )
                
                val api = RetrofitClient.getApi(baseUrl)
                val response = api.generateImage("Bearer $apiKey", request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    when {
                        body.success && body.imageUrl != null -> {
                            Result.success(body.imageUrl)
                        }
                        body.success && body.imageBase64 != null -> {
                            // 如果返回 base64，需要转 data URI（暂未处理，先返回错误提示）
                            Result.failure(Exception("API 返回了 base64 格式，暂不支持"))
                        }
                        else -> {
                            Result.failure(Exception(body.message ?: "生成图片失败"))
                        }
                    }
                } else {
                    Result.failure(Exception("生成图片失败 (HTTP ${response.code()})"))
                }
            } catch (e: Throwable) {
                Result.failure(Exception(friendlyError(e)))
            }
        }
    }
    
    private fun friendlyError(e: Throwable): String {
        return when (e) {
            is IllegalArgumentException -> e.message ?: "参数格式错误"
            is java.net.UnknownHostException -> "无法连接到服务器，请检查 Base URL 是否正确"
            is java.net.SocketTimeoutException -> "连接超时，请稍后重试"
            is java.net.ConnectException -> "网络连接失败，请检查网络和 Base URL"
            is java.io.IOException -> "网络请求失败：${e.message}"
            else -> e.message ?: "发生未知错误"
        }
    }
}
