package com.example.imagegen.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagegen.api.GeneratedImage
import com.example.imagegen.api.Model
import com.example.imagegen.repository.ImageGenRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    private val repository = ImageGenRepository()
    
    private val _models = MutableLiveData<List<Model>>()
    val models: LiveData<List<Model>> = _models
    
    private val _generatedImage = MutableLiveData<GeneratedImage>()
    val generatedImage: LiveData<GeneratedImage> = _generatedImage
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // 全局异常处理，确保协程异常不会导致应用崩溃
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _errorMessage.postValue(throwable.message ?: "发生未知错误")
        _isLoading.postValue(false)
    }
    
    fun loadModels(baseUrl: String, apiKey: String) {
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            try {
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
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "加载模型失败"
                _isLoading.value = false
            }
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
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            try {
                repository.generateImage(baseUrl, apiKey, modelId, prompt, quality, width, height).fold(
                    onSuccess = { image ->
                        _generatedImage.value = image
                        _isLoading.value = false
                    },
                    onFailure = { error ->
                        _errorMessage.value = error.message ?: "生成图片失败"
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "生成图片失败"
                _isLoading.value = false
            }
        }
    }
}
