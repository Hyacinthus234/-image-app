package com.example.imagegen

import android.content.Context
import android.content.SharedPreferences

class ApiPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "image_gen_prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        
        // deepkey.top 默认配置（OpenAI 兼容）
        private const val DEFAULT_BASE_URL = "https://deepkey.top/v1"
        private const val DEFAULT_API_KEY = "sk-SgA6IHW1bCpB5ZUX7Al5IiXbTNnWeydUBfxzMEWAFCDaJhzY"
    }
    
    fun saveBaseUrl(baseUrl: String) {
        prefs.edit().putString(KEY_BASE_URL, baseUrl).apply()
    }
    
    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }
    
    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }
    
    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
