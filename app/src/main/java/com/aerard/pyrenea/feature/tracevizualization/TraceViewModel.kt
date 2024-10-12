package com.aerard.pyrenea.feature.tracevizualization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.aerard.pyrenea.PyreneaApp
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream


class TraceViewModel(private val repository: TraceRepository): ViewModel() {
    val state = repository.data.asStateFlow()

    fun loadTraceFile(input: InputStream) {
        viewModelScope.launch {
            repository.loadTrace(input)
        }
    }

    fun clear() {
        viewModelScope.launch {
            repository.clearTrace()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T: ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                return TraceViewModel(
                    repository = (application as PyreneaApp).traceRepository
                ) as T
            }

        }
    }

}