package com.healthio.ui.vision

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.ai.FoodAnalysis
import com.healthio.core.ai.GeminiRepository
import com.healthio.ui.settings.SettingsViewModel
import com.healthio.core.data.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class VisionState {
    object Idle : VisionState()
    data class Review(val bitmap: Bitmap) : VisionState() // New state
    object Analyzing : VisionState()
    data class Success(val analysis: FoodAnalysis) : VisionState()
    data class Error(val message: String) : VisionState()
}

class VisionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeminiRepository()
    private val context = application.applicationContext

    private val _state = MutableStateFlow<VisionState>(VisionState.Idle)
    val state: StateFlow<VisionState> = _state.asStateFlow()

    fun onImageCaptured(bitmap: Bitmap) {
        _state.value = VisionState.Review(bitmap)
    }

    fun analyzeImage(userContext: String) {
        val currentState = _state.value
        if (currentState !is VisionState.Review) return
        
        val bitmap = currentState.bitmap

        viewModelScope.launch {
            _state.value = VisionState.Analyzing
            
            // Get API Key
            val apiKey = context.dataStore.data.map { it[SettingsViewModel.GEMINI_API_KEY] }.first()
            
            if (apiKey.isNullOrEmpty()) {
                _state.value = VisionState.Error("Missing Gemini API Key. Please set it in Settings.")
                return@launch
            }

            val result = repository.analyzeImage(bitmap, apiKey, userContext)
            result.onSuccess { analysis ->
                _state.value = VisionState.Success(analysis)
            }.onFailure { exception ->
                _state.value = VisionState.Error("AI Error: ${exception.localizedMessage}")
            }
        }
    }
    
    fun reset() {
        _state.value = VisionState.Idle
    }
}