package com.aida.s34597506.medtrack.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aida.s34597506.medtrack.data.entities.SymptomEntity
import com.aida.s34597506.medtrack.data.repository.MedTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SymptomUiState(
    val isLoading: Boolean = false,
    val symptoms: List<SymptomEntity> = emptyList(),
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

class SymptomViewModel(
    private val repository: MedTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SymptomUiState())
    val uiState: StateFlow<SymptomUiState> = _uiState.asStateFlow()

    fun loadSymptoms(patientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getSymptomsForPatient(patientId).collect { symptoms ->
                    _uiState.update { it.copy(symptoms = symptoms, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load symptoms: ${e.message}"
                    )
                }
            }
        }
    }

    fun addSymptom(symptom: SymptomEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, saveSuccess = false, errorMessage = null) }
            try {
                repository.insertSymptom(symptom)
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save symptom: ${e.message}"
                    )
                }
            }
        }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

// Factory for SymptomViewModel
class SymptomViewModelFactory(
    private val repository: MedTrackRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SymptomViewModel::class.java)) {
            return SymptomViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}