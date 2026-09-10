package com.example.imagegen

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.imagegen.api.GeneratedImage
import com.example.imagegen.api.Model
import com.example.imagegen.databinding.ActivityMainBinding
import com.example.imagegen.databinding.DialogFullscreenImageBinding
import com.example.imagegen.databinding.ItemReferenceImageBinding
import com.example.imagegen.model.ApiConfig
import com.example.imagegen.store.ConfigStore
import com.example.imagegen.store.HistoryStore
import com.example.imagegen.util.ImageSaver
import com.example.imagegen.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var preferences: ApiPreferences
    private lateinit var configStore: ConfigStore
    private lateinit var historyStore: HistoryStore
    
    private var selectedModel: Model? = null
    private var currentBitmap: Bitmap? = null
    
    /** 参考图字节（上传用）与缩略图（显示用），两者下标一一对应。 */
    private val referenceBytes = mutableListOf<ByteArray>()
    private val referenceThumbs = mutableListOf<Bitmap>()
    
    /** 从相册一次选多张参考图。 */
    private val pickReferenceImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        uris.forEach { addReferenceImage(it) }
    }
    
    companion object {
        private const val REQUEST_WRITE_PERMISSION = 100
        /** 参考图上传前的最长边：过大的图既慢，也容易被服务端拒绝。 */
        private const val MAX_REFERENCE_EDGE = 1536
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        preferences = ApiPreferences(this)
        configStore = ConfigStore(this)
        historyStore = HistoryStore(this)
        
        setupUI()
        loadSavedSettings()
        observeViewModel()
    }
    
    override fun onResume() {
        super.onResume()
        refreshConfigSpinner()
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
        
        // 历史记录
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        
        // 添加参考图（相册多选）
        binding.btnAddReference.setOnClickListener {
            pickReferenceImages.launch("image/*")
        }
    }
    
    private fun loadSavedSettings() {
        binding.etBaseUrl.setText(preferences.getBaseUrl())
        binding.etApiKey.setText(preferences.getApiKey())
    }
    
    private fun refreshConfigSpinner() {
        val configs = configStore.getConfigs()
        if (configs.isEmpty()) {
            val emptyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("（暂无保存的配置）"))
            emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerConfig.adapter = emptyAdapter
            return
        }
        
        val names = configs.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerConfig.adapter = adapter
        
        binding.spinnerConfig.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < configs.size) {
                    val config = configs[position]
                    binding.etBaseUrl.setText(config.baseUrl)
                    binding.etApiKey.setText(config.apiKey)
                    binding.etConfigName.setText(config.name)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun saveSettings() {
        val baseUrl = binding.etBaseUrl.text.toString().trim()
        val apiKey = binding.etApiKey.text.toString().trim()
        val configName = binding.etConfigName.text.toString().trim()
        
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
        
        if (configName.isEmpty()) {
            Toast.makeText(this, "请填写「配置名称」用于保存（如 deepkey）", Toast.LENGTH_LONG).show()
            return
        }
        
        // 保存到配置列表
        configStore.saveConfig(ApiConfig(configName, baseUrl, apiKey))
        
        // 同时保存为当前使用的配置
        preferences.saveBaseUrl(baseUrl)
        preferences.saveApiKey(apiKey)
        
        refreshConfigSpinner()
        Toast.makeText(this, "✅ 配置「$configName」已保存", Toast.LENGTH_SHORT).show()
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
        viewModel.models.observe(this) { models ->
            if (models.isNotEmpty()) {
                setupModelSpinner(models)
                Toast.makeText(this, "✅ 已加载 ${models.size} 个模型", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.generatedImage.observe(this) { image ->
            displayImage(image)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnGenerate.isEnabled = !isLoading
            binding.btnLoadModels.isEnabled = !isLoading
            binding.btnSaveSettings.isEnabled = !isLoading
        }
        
        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun displayImage(image: GeneratedImage) {
        binding.cardResult.visibility = View.VISIBLE
        
        if (image.url != null) {
            Glide.with(this)
                .asBitmap()
                .load(image.url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        currentBitmap = resource
                        binding.ivGeneratedImage.setImageBitmap(resource)
                        saveToHistory(resource)
                    }
                    
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        Toast.makeText(this@MainActivity, "图片加载失败", Toast.LENGTH_SHORT).show()
                    }
                })
        } else if (image.base64 != null) {
            try {
                val bytes = Base64.decode(image.base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    currentBitmap = bitmap
                    binding.ivGeneratedImage.setImageBitmap(bitmap)
                    saveToHistory(bitmap)
                } else {
                    Toast.makeText(this, "图片解码失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "图片解码失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveToHistory(bitmap: Bitmap) {
        val prompt = binding.etPrompt.text.toString().trim()
        val model = selectedModel?.id ?: "未知"
        val quality = when (binding.spinnerQuality.selectedItemPosition) {
            0 -> "低质量"
            1 -> "中等质量"
            2 -> "高质量"
            3 -> "超高质量"
            else -> "中等质量"
        }
        historyStore.saveHistory(bitmap, prompt, model, quality)
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
            height = height,
            referenceImages = referenceBytes.toList()
        )
    }
    
    private fun saveImage() {
        val bitmap = currentBitmap
        if (bitmap == null) {
            Toast.makeText(this, "请先生成图片", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            if (ImageSaver.saveToGallery(this, bitmap)) {
                Toast.makeText(this, "✅ 图片已保存到相册", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ 保存失败", Toast.LENGTH_SHORT).show()
            }
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
                currentBitmap?.let {
                    if (ImageSaver.saveToGallery(this, it)) {
                        Toast.makeText(this, "✅ 图片已保存到相册", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
            }
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
        dialogBinding.root.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }
    
    // ---------------- 参考图（垫图） ----------------
    
    /** 读取相册图片 → 压缩 → 入列并刷新缩略图。 */
    private fun addReferenceImage(uri: Uri) {
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) { loadReferenceImage(uri) }
            if (loaded == null) {
                Toast.makeText(this@MainActivity, "这张图读取失败，已跳过", Toast.LENGTH_SHORT).show()
                return@launch
            }
            referenceBytes.add(loaded.first)
            referenceThumbs.add(loaded.second)
            refreshReferenceImages()
        }
    }
    
    /** 解码 + 按最长边缩放 + 压成 JPEG。返回 (上传字节, 缩略图)。 */
    private fun loadReferenceImage(uri: Uri): Pair<ByteArray, Bitmap>? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_REFERENCE_EDGE)
            }
            val decoded = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null
            
            val scaled = scaleDown(decoded, MAX_REFERENCE_EDGE)
            val bytes = ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.toByteArray()
            }
            bytes to scaled
        } catch (e: Exception) {
            null
        }
    }
    
    /** 按 2 的幂求采样率，先把解码尺寸压到接近目标值。 */
    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= maxEdge) {
            longest /= 2
            sample *= 2
        }
        return sample
    }
    
    /** 解码后若仍超过 maxEdge，再做一次精确缩放。 */
    private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxEdge) return source
        val ratio = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
    
    /** 重建参考图缩略图行与计数。 */
    private fun refreshReferenceImages() {
        binding.llReferences.removeAllViews()
        referenceThumbs.forEachIndexed { index, bitmap ->
            val item = ItemReferenceImageBinding.inflate(layoutInflater, binding.llReferences, false)
            item.ivThumb.setImageBitmap(bitmap)
            item.btnRemove.setOnClickListener {
                if (index in referenceBytes.indices) {
                    referenceBytes.removeAt(index)
                    referenceThumbs.removeAt(index)
                    refreshReferenceImages()
                }
            }
            binding.llReferences.addView(item.root)
        }
        binding.svReferences.visibility = if (referenceThumbs.isEmpty()) View.GONE else View.VISIBLE
        binding.tvReferenceCount.text = "${referenceThumbs.size} 张"
    }
    
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)
    }
}
