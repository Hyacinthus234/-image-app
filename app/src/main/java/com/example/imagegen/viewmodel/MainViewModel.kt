package com.example.imagegen.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagegen.api.Model
import com.example.imagegen.repository.ImageGenRepository
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    private val repository = ImageGenRepository()
    
    private val _models = MutableLiveData<List<Model>>()
    val models: LiveData<List<Model>> = _models
    
    private val _generatedImageUrl = MutableLiveData<String>()
    val generatedImageUrl: LiveData<String> = _generatedImageUrl
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    fun loadModels(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getModels(baseUrl, apiKey).fold(
                onSuccess = { modelList ->
                    _models.value = modelList
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "加载模型失败"
                    _isLoading.value = false
                }
            )
        }
    }
    
    fun generateImage(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        prompt: String,
        quality: String,
        width: Int,
        height: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.generateImage(baseUrl, apiKey, modelId, prompt, quality, width, height).fold(
                onSuccess = { imageUrl ->
                    _generatedImageUrl.value = imageUrl
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "生成图片失败"
                    _isLoading.value = false
                }
            )
        }
    }
}
