package com.example.imagegen

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.imagegen.databinding.ActivityHistoryBinding
import com.example.imagegen.databinding.DialogFullscreenImageBinding
import com.example.imagegen.model.HistoryItem
import com.example.imagegen.store.HistoryStore
import com.example.imagegen.util.ImageSaver

class HistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyStore: HistoryStore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        historyStore = HistoryStore(this)
        
        setupButtons()
    }
    
    override fun onResume() {
        super.onResume()
        refreshList()
    }
    
    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnClear.setOnClickListener {
            if (historyStore.getHistory().isEmpty()) {
                Toast.makeText(this, "暂无历史记录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            historyStore.clearAll()
            refreshList()
            Toast.makeText(this, "✅ 已清空历史记录", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun refreshList() {
        val items = historyStore.getHistory()
        if (items.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerViewHistory.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerViewHistory.visibility = View.VISIBLE
            binding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewHistory.adapter = HistoryAdapter(items, object : HistoryAdapter.OnItemActionListener {
                override fun onView(item: HistoryItem) {
                    viewImage(item)
                }
                
                override fun onCopyPrompt(item: HistoryItem) {
                    copyPrompt(item)
                }
                
                override fun onSave(item: HistoryItem) {
                    saveImage(item)
                }
            })
        }
    }
    
    private fun viewImage(item: HistoryItem) {
        val bitmap = BitmapFactory.decodeFile(item.imagePath)
        if (bitmap == null) {
            Toast.makeText(this, "图片文件已丢失", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = DialogFullscreenImageBinding.inflate(layoutInflater)
        dialogBinding.ivFullscreen.setImageBitmap(bitmap)
        dialogBinding.root.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }
    
    private fun copyPrompt(item: HistoryItem) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("提示词", item.prompt))
        Toast.makeText(this, "✅ 提示词已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveImage(item: HistoryItem) {
        val bitmap = BitmapFactory.decodeFile(item.imagePath)
        if (bitmap == null) {
            Toast.makeText(this, "图片文件已丢失", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (ImageSaver.saveToGallery(this, bitmap)) {
            Toast.makeText(this, "✅ 图片已保存到相册", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ 保存失败", Toast.LENGTH_SHORT).show()
        }
    }
}
