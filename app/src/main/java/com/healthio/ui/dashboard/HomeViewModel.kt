package com.healthio.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.FastingRepository
import com.healthio.core.data.QuotesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.Locale

enum class TimerState {
    FASTING, EATING
}

data class HomeUiState(
    val timerState: TimerState = TimerState.FASTING,
    val progress: Float = 0f, // Deprecated, but keeping to avoid break if I miss something
    val elapsedMillis: Long = 0L, // New field for 24h timer
    val timeDisplay: String = "00:00:00",
    val startTime: Long? = null,
    val showFeedbackDialog: Boolean = false,
    val completedDuration: String = "",
    val feedbackQuote: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FastingRepository(application)
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private val targetFastDuration = Duration.ofHours(16).toMillis()

    init {
        viewModelScope.launch {
            combine(repository.isFasting, repository.startTime) { isFasting, startTime ->
                Pair(isFasting, startTime)
            }.collect { (isFasting, startTime) ->
                updateState(isFasting, startTime)
            }
        }
        startTimer()
    }

    private fun updateState(isFasting: Boolean, startTime: Long?) {
        _uiState.value = _uiState.value.copy(
            timerState = if (isFasting) TimerState.FASTING else TimerState.EATING,
            startTime = startTime
        )
        calculateProgress()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                calculateProgress()
                delay(1000) // Update every second
            }
        }
    }

    private fun calculateProgress() {
        val currentState = _uiState.value
        val now = System.currentTimeMillis()
        
        if (currentState.timerState == TimerState.FASTING && currentState.startTime != null) {
            val elapsed = now - currentState.startTime
            // Progress for legacy/target goal logic (can be used for text feedback if needed)
            val progress = (elapsed.toFloat() / targetFastDuration).coerceIn(0f, 1f)
            
            val timeString = formatDuration(elapsed)
            
            _uiState.value = currentState.copy(
                progress = progress,
                elapsedMillis = elapsed,
                timeDisplay = timeString
            )
        } else {
            // Eating state (or no active fast)
            _uiState.value = currentState.copy(
                progress = 0f,
                elapsedMillis = 0L,
                timeDisplay = "Ready to Fast"
            )
        }
    }

    private fun formatDuration(millis: Long): String {
        val duration = Duration.ofMillis(millis)
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            duration.toHours(),
            duration.toMinutesPart(),
            duration.toSecondsPart()
        )
    }

    fun startFastNow() {
        viewModelScope.launch {
            repository.startFast(System.currentTimeMillis())
        }
    }

    fun startFastAt(timestamp: Long) {
        val clampedTime = if (timestamp > System.currentTimeMillis()) System.currentTimeMillis() else timestamp
        viewModelScope.launch {
            repository.startFast(clampedTime)
        }
    }

    fun requestEndFast() {
        val currentState = _uiState.value
        val now = System.currentTimeMillis()
        val elapsed = if (currentState.startTime != null) now - currentState.startTime else 0L
        
        _uiState.value = currentState.copy(
            showFeedbackDialog = true,
            completedDuration = formatDuration(elapsed),
            feedbackQuote = QuotesRepository.getRandomQuote()
        )
    }

    fun confirmEndFast() {
        val currentState = _uiState.value
        val now = System.currentTimeMillis()
        val startTime = currentState.startTime ?: return

        viewModelScope.launch {
            repository.logCompletedFast(startTime, now)
            repository.endFast()
            _uiState.value = _uiState.value.copy(showFeedbackDialog = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}