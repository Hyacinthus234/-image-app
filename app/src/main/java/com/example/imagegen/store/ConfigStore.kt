package com.example.imagegen.store

import android.content.Context
import com.example.imagegen.model.ApiConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 多配置存储（支持保存多个中转站配置，一键切换）
 */
class ConfigStore(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("api_configs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * 保存配置（同名则覆盖，否则新增）
     */
    fun saveConfig(config: ApiConfig) {
        val list = getConfigs().toMutableList()
        val index = list.indexOfFirst { it.name == config.name }
        if (index >= 0) {
            list[index] = config
        } else {
            list.add(config)
        }
        prefs.edit().putString(KEY_CONFIGS, gson.toJson(list)).apply()
    }
    
    /**
     * 获取所有已保存的配置
     */
    fun getConfigs(): List<ApiConfig> {
        val json = prefs.getString(KEY_CONFIGS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ApiConfig>>() {}.type
            gson.fromJson<List<ApiConfig>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 按名称查找配置
     */
    fun getConfig(name: String): ApiConfig? {
        return getConfigs().firstOrNull { it.name == name }
    }
    
    /**
     * 删除配置
     */
    fun deleteConfig(name: String) {
        val list = getConfigs().filter { it.name != name }
        prefs.edit().putString(KEY_CONFIGS, gson.toJson(list)).apply()
    }
    
    companion object {
        private const val KEY_CONFIGS = "configs_list"
    }
}
