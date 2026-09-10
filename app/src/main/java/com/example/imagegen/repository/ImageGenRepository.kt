package com.example.imagegen.repository

import com.example.imagegen.api.GenerateRequest
import com.example.imagegen.api.GeneratedImage
import com.example.imagegen.api.Model
import com.example.imagegen.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ImageGenRepository {
    
    companion object {
        // 内置常用模型（当 API 不支持模型列表接口时使用）
        private val fallbackModels = listOf(
            Model(id = "gpt-image-2", `object` = "model"),
            Model(id = "gemini-3.1-flash-image", `object` = "model")
        )
        
        /** multipart 里参考图的字段名（OpenAI /images/edits 的多图约定）。 */
        private const val REFERENCE_FIELD = "image[]"
    }
    
    suspend fun getModels(baseUrl: String, apiKey: String): Result<List<Model>> {
        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(baseUrl)
                val response = api.getModels("Bearer $apiKey")
                if (response.isSuccessful && response.body() != null) {
                    val models = response.body()!!.data
                    if (models.isNotEmpty()) {
                        Result.success(models)
                    } else {
                        // 空列表，使用内置模型
                        Result.success(fallbackModels)
                    }
                } else {
                    // 接口不支持或失败，使用内置模型
                    Result.success(fallbackModels)
                }
            } catch (e: Throwable) {
                // 任何错误都回退到内置模型，保证可用
                Result.success(fallbackModels)
            }
        }
    }
    
    /**
     * 生成图片。
     *
     * 没有参考图时走纯文生图（`POST images/generations`，JSON）；
     * 带参考图时走图生图（`POST images/edits`，multipart 上传，支持多张）。
     */
    suspend fun generateImage(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        prompt: String,
        quality: String,
        width: Int,
        height: Int,
        referenceImages: List<ByteArray> = emptyList()
    ): Result<GeneratedImage> {
        return withContext(Dispatchers.IO) {
            try {
                // 映射质量参数
                val qualityValue = when (quality) {
                    "low" -> "low"
                    "medium" -> "medium"
                    "high" -> "high"
                    "ultra" -> "high"
                    else -> "medium"
                }
                val sizeValue = "${width}x${height}"
                
                val api = RetrofitClient.getApi(baseUrl)
                val auth = "Bearer $apiKey"
                
                val response = if (referenceImages.isEmpty()) {
                    api.generateImage(
                        auth,
                        GenerateRequest(
                            model = modelId,
                            prompt = prompt,
                            n = 1,
                            size = sizeValue,
                            quality = qualityValue
                        )
                    )
                } else {
                    val textType = "text/plain".toMediaType()
                    api.editImage(
                        auth = auth,
                        images = buildReferenceParts(referenceImages),
                        prompt = prompt.toRequestBody(textType),
                        model = modelId.toRequestBody(textType),
                        n = "1".toRequestBody(textType),
                        size = sizeValue.toRequestBody(textType),
                        quality = qualityValue.toRequestBody(textType)
                    )
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val item = body.data.firstOrNull()
                    when {
                        item?.b64_json != null -> {
                            Result.success(GeneratedImage(base64 = item.b64_json))
                        }
                        item?.url != null -> {
                            Result.success(GeneratedImage(url = item.url))
                        }
                        else -> {
                            Result.failure(Exception("API 未返回图片数据"))
                        }
                    }
                } else {
                    // 解析错误响应体中的错误信息
                    val errorMsg = parseError(response.errorBody()?.string())
                    val endpoint = if (referenceImages.isEmpty()) "生成" else "参考图生成"
                    Result.failure(
                        Exception(
                            "$endpoint 失败 (HTTP ${response.code()})" +
                                if (errorMsg != null) "：$errorMsg" else ""
                        )
                    )
                }
            } catch (e: Throwable) {
                Result.failure(Exception(friendlyError(e)))
            }
        }
    }
    
    /** 把参考图字节数组打包成 multipart 的多个同名 part。 */
    private fun buildReferenceParts(images: List<ByteArray>): List<MultipartBody.Part> {
        val mediaType = "image/jpeg".toMediaType()
        return images.mapIndexed { index, bytes ->
            MultipartBody.Part.createFormData(
                REFERENCE_FIELD,
                "reference_${index + 1}.jpg",
                bytes.toRequestBody(mediaType)
            )
        }
    }
    
    private fun parseError(errorBody: String?): String? {
        if (errorBody.isNullOrEmpty()) return null
        return try {
            // 尝试解析 OpenAI 格式错误 { error: { message: "..." } }
            val json = org.json.JSONObject(errorBody)
            val error = json.optJSONObject("error")
            error?.optString("message")?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            // 解析失败，截取前 200 字符
            errorBody.take(200)
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
