package com.example.imagegen.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String = ""
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    fun getApi(baseUrl: String): ImageGenApi {
        val normalizedUrl = ensureTrailingSlash(baseUrl.trim())
        
        // 验证 URL 格式，避免创建 Retrofit 时崩溃
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            throw IllegalArgumentException("Base URL 必须以 http:// 或 https:// 开头")
        }
        
        // 如果Base URL变化了，重新创建Retrofit实例
        if (retrofit == null || currentBaseUrl != normalizedUrl) {
            currentBaseUrl = normalizedUrl
            retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ImageGenApi::class.java)
    }
    
    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}
