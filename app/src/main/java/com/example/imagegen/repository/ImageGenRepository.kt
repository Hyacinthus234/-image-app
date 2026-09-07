package com.example.imagegen.repository

import com.example.imagegen.api.GenerateRequest
import com.example.imagegen.api.Model
import com.example.imagegen.api.RetrofitClient

class ImageGenRepository {
    
    suspend fun getModels(baseUrl: String, apiKey: String): Result<List<Model>> {
        return try {
            val api = RetrofitClient.getApi(baseUrl)
            val response = api.getModels("Bearer $apiKey")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.models)
            } else {
                Result.failure(Exception("获取模型列表失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
        return try {
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
                if (body.success && body.imageUrl != null) {
                    Result.success(body.imageUrl)
                } else {
                    Result.failure(Exception(body.message ?: "生成图片失败"))
                }
            } else {
                Result.failure(Exception("生成图片失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
