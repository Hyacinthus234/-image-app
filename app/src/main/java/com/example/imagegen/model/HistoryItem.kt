package com.example.imagegen.model

/**
 * 生图历史记录
 */
data class HistoryItem(
    val id: Long,
    val imagePath: String,
    val prompt: String,
    val model: String,
    val quality: String,
    val timestamp: Long
)
