package com.example.imagegen.store

import android.content.Context
import android.graphics.Bitmap
import com.example.imagegen.model.HistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

/**
 * 生图历史记录存储（图片存文件，元数据存 SharedPreferences）
 */
class HistoryStore(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    private val historyDir = File(context.filesDir, "history")
    private val gson = Gson()
    
    init {
        if (!historyDir.exists()) {
            historyDir.mkdirs()
        }
    }
    
    /**
     * 保存一条历史记录（图片 + 提示词 + 模型 + 质量）
     */
    fun saveHistory(bitmap: Bitmap, prompt: String, model: String, quality: String): HistoryItem {
        val id = System.currentTimeMillis()
        val file = File(historyDir, "img_$id.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        
        val item = HistoryItem(
            id = id,
            imagePath = file.absolutePath,
            prompt = prompt,
            model = model,
            quality = quality,
            timestamp = id
        )
        
        val list = getHistory().toMutableList()
        list.add(0, item) // 最新的排前面
        
        // 限制最多 50 条，超出删除图片文件
        if (list.size > MAX_HISTORY) {
            list.subList(MAX_HISTORY, list.size).forEach { old ->
                File(old.imagePath).delete()
            }
            list.subList(MAX_HISTORY, list.size).clear()
        }
        
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
        return item
    }
    
    /**
     * 获取所有历史记录（按时间倒序）
     */
    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson<List<HistoryItem>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 删除一条历史记录
     */
    fun deleteHistory(id: Long) {
        val list = getHistory()
        val target = list.firstOrNull { it.id == id } ?: return
        File(target.imagePath).delete()
        val newList = list.filter { it.id != id }
        prefs.edit().putString(KEY_HISTORY, gson.toJson(newList)).apply()
    }
    
    /**
     * 清空所有历史
     */
    fun clearAll() {
        getHistory().forEach { File(it.imagePath).delete() }
        prefs.edit().remove(KEY_HISTORY).apply()
    }
    
    companion object {
        private const val KEY_HISTORY = "history_list"
        private const val MAX_HISTORY = 50
    }
}
