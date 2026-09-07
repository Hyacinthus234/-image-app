package com.example.imagegen

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.imagegen.api.Model
import com.example.imagegen.databinding.ActivityMainBinding
import com.example.imagegen.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var preferences: ApiPreferences
    
    private var selectedModel: Model? = null
    
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
                Toast.makeText(this, "✅ 成功加载 ${models.size} 个模型", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 观察生成的图片
        viewModel.generatedImageUrl.observe(this) { imageUrl ->
            if (!imageUrl.isNullOrEmpty()) {
                binding.cardResult.visibility = View.VISIBLE
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.ivGeneratedImage)
            }
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
    
    private fun setupModelSpinner(models: List<Model>) {
        val modelNames = models.map { it.name }
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
        
        val width = binding.etWidth.text.toString().toIntOrNull() ?: 512
        val height = binding.etHeight.text.toString().toIntOrNull() ?: 512
        
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
    
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)
    }
}
