package com.healthio.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthio.core.data.FastingRepository
import com.healthio.core.database.FastingLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FastingRepository(application)

    private val _logs = MutableStateFlow<List<FastingLog>>(emptyList())
    val logs: StateFlow<List<FastingLog>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.fastingHistory.collect { history ->
                _logs.value = history
            }
        }
    }
}
