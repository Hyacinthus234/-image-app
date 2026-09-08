package com.example.imagegen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.imagegen.model.HistoryItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val items: List<HistoryItem>,
    private val listener: OnItemActionListener
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    
    interface OnItemActionListener {
        fun onView(item: HistoryItem)
        fun onCopyPrompt(item: HistoryItem)
        fun onSave(item: HistoryItem)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, listener)
    }
    
    override fun getItemCount(): Int = items.size
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        private val tvPrompt: TextView = view.findViewById(R.id.tvPrompt)
        private val tvMeta: TextView = view.findViewById(R.id.tvMeta)
        private val btnView: TextView = view.findViewById(R.id.btnView)
        private val btnCopyPrompt: TextView = view.findViewById(R.id.btnCopyPrompt)
        private val btnSave: TextView = view.findViewById(R.id.btnSave)
        
        fun bind(item: HistoryItem, listener: OnItemActionListener) {
            // 加载缩略图
            val file = File(item.imagePath)
            if (file.exists()) {
                Glide.with(ivThumbnail.context)
                    .load(file)
                    .centerCrop()
                    .into(ivThumbnail)
            }
            
            // 提示词
            tvPrompt.text = item.prompt
            
            // 模型 + 时间
            val timeStr = formatTime(item.timestamp)
            tvMeta.text = "模型：${item.model}  ·  ${timeStr}"
            
            // 按钮
            btnView.setOnClickListener { listener.onView(item) }
            btnCopyPrompt.setOnClickListener { listener.onCopyPrompt(item) }
            btnSave.setOnClickListener { listener.onSave(item) }
        }
        
        private fun formatTime(timestamp: Long): String {
            return try {
                val format = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                format.format(Date(timestamp))
            } catch (e: Exception) {
                ""
            }
        }
    }
}
