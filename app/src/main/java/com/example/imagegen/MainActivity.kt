package com.example.imagegen

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.imagegen.api.GeneratedImage
import com.example.imagegen.api.Model
import com.example.imagegen.databinding.ActivityMainBinding
import com.example.imagegen.databinding.DialogFullscreenImageBinding
import com.example.imagegen.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var preferences: ApiPreferences
    
    private var selectedModel: Model? = null
    private var currentBitmap: Bitmap? = null
    
    companion object {
        private const val REQUEST_WRITE_PERMISSION = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        preferences = ApiPreferences(this)
        
        setupUI()
        loadSavedSettings()
        observeViewModel()
    }
    
    private fun setupUI() {
        binding.btnLoadModels.setOnClickListener {
            loadModels()
        }
        
        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        // 质量选择器
        val qualityOptions = arrayOf("低质量（快速）", "中等质量（推荐）", "高质量（精细）", "超高质量（最慢）")
        val qualityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, qualityOptions)
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerQuality.adapter = qualityAdapter
        binding.spinnerQuality.setSelection(1) // 默认中等质量
        
        binding.btnGenerate.setOnClickListener {
            generateImage()
        }
        
        // 保存图片
        binding.btnSaveImage.setOnClickListener {
            saveImage()
        }
        
        // 点击图片放大
        binding.ivGeneratedImage.setOnClickListener {
            showFullscreenImage()
        }
    }
    
    private fun loadSavedSettings() {
        binding.etBaseUrl.setText(preferences.getBaseUrl())
        binding.etApiKey.setText(preferences.getApiKey())
    }
    
    private fun saveSettings() {
        val baseUrl = binding.etBaseUrl.text.toString().trim()
        val apiKey = binding.etApiKey.text.toString().trim()
        
        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "Base URL 不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isValidUrl(baseUrl)) {
            Toast.makeText(this, "Base URL 必须以 http:// 或 https:// 开头", Toast.LENGTH_LONG).show()
            return
        }
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API Key 不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        
        preferences.saveBaseUrl(baseUrl)
        preferences.saveApiKey(apiKey)
        Toast.makeText(this, "✅ 设置已保存", Toast.LENGTH_SHORT).show()
    }
    
    private fun loadModels() {
        val baseUrl = binding.etBaseUrl.text.toString().trim()
        val apiKey = binding.etApiKey.text.toString().trim()
        
        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "请先输入 Base URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isValidUrl(baseUrl)) {
            Toast.makeText(this, "Base URL 格式错误，需要 http:// 或 https:// 开头", Toast.LENGTH_LONG).show()
            return
        }
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请先输入 API Key", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.loadModels(baseUrl, apiKey)
    }
    
    private fun observeViewModel() {
        // 观察模型列表
        viewModel.models.observe(this) { models ->
            if (models.isNotEmpty()) {
                setupModelSpinner(models)
                Toast.makeText(this, "✅ 已加载 ${models.size} 个模型", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 观察生成的图片
        viewModel.generatedImage.observe(this) { image ->
            displayImage(image)
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnGenerate.isEnabled = !isLoading
            binding.btnLoadModels.isEnabled = !isLoading
            binding.btnSaveSettings.isEnabled = !isLoading
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun displayImage(image: GeneratedImage) {
        binding.cardResult.visibility = View.VISIBLE
        
        if (image.url != null) {
            // 用 Glide 下载 Bitmap（同时用于显示和保存）
            Glide.with(this)
                .asBitmap()
                .load(image.url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        currentBitmap = resource
                        binding.ivGeneratedImage.setImageBitmap(resource)
                    }
                    
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        Toast.makeText(this@MainActivity, "图片加载失败", Toast.LENGTH_SHORT).show()
                    }
                })
        } else if (image.base64 != null) {
            // 解码 base64 图片
            try {
                val bytes = Base64.decode(image.base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    currentBitmap = bitmap
                    binding.ivGeneratedImage.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this, "图片解码失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "图片解码失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupModelSpinner(models: List<Model>) {
        val modelNames = models.map { it.id }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerModel.adapter = adapter
        
        binding.spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < models.size) {
                    selectedModel = models[position]
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedModel = null
            }
        }
        
        if (models.isNotEmpty()) {
            selectedModel = models[0]
        }
    }
    
    private fun generateImage() {
        val baseUrl = binding.etBaseUrl.text.toString().trim()
        val apiKey = binding.etApiKey.text.toString().trim()
        val prompt = binding.etPrompt.text.toString().trim()
        
        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "请先输入 Base URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isValidUrl(baseUrl)) {
            Toast.makeText(this, "Base URL 格式错误", Toast.LENGTH_LONG).show()
            return
        }
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请先输入 API Key", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (prompt.isEmpty()) {
            Toast.makeText(this, "请输入图片描述（提示词）", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedModel == null) {
            Toast.makeText(this, "请先点击「加载模型」选择模型", Toast.LENGTH_SHORT).show()
            return
        }
        
        val quality = when (binding.spinnerQuality.selectedItemPosition) {
            0 -> "low"
            1 -> "medium"
            2 -> "high"
            3 -> "ultra"
            else -> "medium"
        }
        
        val width = binding.etWidth.text.toString().toIntOrNull() ?: 1024
        val height = binding.etHeight.text.toString().toIntOrNull() ?: 1024
        
        viewModel.generateImage(
            baseUrl = baseUrl,
            apiKey = apiKey,
            modelId = selectedModel!!.id,
            prompt = prompt,
            quality = quality,
            width = width,
            height = height
        )
    }
    
    private fun saveImage() {
        val bitmap = currentBitmap
        if (bitmap == null) {
            Toast.makeText(this, "请先生成图片", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Android 10+ 无需权限，Android 9- 需要权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            doSaveImage(bitmap)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_PERMISSION
            )
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                currentBitmap?.let { doSaveImage(it) }
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun doSaveImage(bitmap: Bitmap) {
        try {
            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(bitmap)
            } else {
                saveToExternalStorage(bitmap)
            }
            
            if (success) {
                Toast.makeText(this, "✅ 图片已保存到相册", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ 保存失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ 保存失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun saveToMediaStore(bitmap: Bitmap): Boolean {
        val fileName = "AI_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI图片生成器")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return false
        return contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            true
        } ?: false
    }
    
    private fun saveToExternalStorage(bitmap: Bitmap): Boolean {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "AI图片生成器"
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "AI_${System.currentTimeMillis()}.png")
        return FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            // 通知相册刷新
            MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            true
        }
    }
    
    private fun showFullscreenImage() {
        val bitmap = currentBitmap
        if (bitmap == null) {
            Toast.makeText(this, "请先生成图片", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = DialogFullscreenImageBinding.inflate(layoutInflater)
        dialogBinding.ivFullscreen.setImageBitmap(bitmap)
        // 点击任意位置关闭
        dialogBinding.root.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }
    
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)
    }
}
